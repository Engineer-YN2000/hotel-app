package com.example.hotel.domain.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotel.config.ReservationProperties;
import com.example.hotel.domain.repository.ReservationDao;
import com.example.hotel.domain.repository.ReservationDetailDao;
import com.example.hotel.domain.repository.ReserverDao;
import com.example.hotel.presentation.dto.reservation.CustomerRequestDto;
import com.example.hotel.presentation.dto.reservation.ReservationRequestDto;
import com.example.hotel.presentation.dto.reservation.ReservationResponseDto;
import com.example.hotel.utils.PriceCalculator;
import com.example.hotel.domain.model.Reservation;
import com.example.hotel.domain.model.ReservationDetail;
import com.example.hotel.domain.model.RoomStockInfo;
import com.example.hotel.domain.model.ReservationWithInfo;
import com.example.hotel.domain.model.Reserver;
import com.example.hotel.domain.constants.ReservationStatus;
import com.example.hotel.domain.exception.ReservationExpiredException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

import org.seasar.doma.jdbc.Result;
import org.seasar.doma.jdbc.SelectOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 予約管理業務サービス
 *
 * 【主要責務】
 * 仮予約の作成、予約情報の取得など、
 * 予約に関するビジネスロジックを担当する。
 *
 * 【トランザクション管理】
 * 仮予約作成時は悲観的ロックを使用し、
 * ダブルブッキングを防止する。
 *
 * @see ReservationDao 予約データアクセス
 * @see ReservationDetailDao 予約明細データアクセス
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationDao reservationDao;
  private final ReservationDetailDao reservationDetailDao;
  private final ReserverDao reserverDao;
  private final CacheService cacheService;
  private final MessageSource messageSource;
  private final ReservationProperties reservationProperties;

  /**
   * 仮予約（在庫ロック）を作成します。
   * 必要なバリデーション・在庫チェック・予約/明細登録を一括で行います。
   *
   * 【同時実行制御】
   * SELECT FOR UPDATEによる悲観的ロックを使用して、
   * 在庫チェックと予約登録の間に他のトランザクションが介入することを防ぎます。
   * これにより、同時リクエストによるダブルブッキングを防止します。
   *
   * @param request 仮予約リクエストDTO
   * @return 予約ID（自動採番）
   * @throws IllegalArgumentException パラメータ不正時
   * @throws IllegalStateException 在庫不足時
   */
  @Transactional(rollbackFor = Exception.class)
  public Integer createTentativeReservation(ReservationRequestDto request) {
    // 1. 在庫キャッシュ取得
    Map<Integer, RoomStockInfo> stockCache = cacheService.getStockCache();

    // 2. バリデーション・在庫チェック
    // 【悲観的ロック】SelectOptions.get().forUpdate()を使用して、同一部屋タイプ・期間の予約レコードをロック
    // これにより、他のトランザクションはこのトランザクションがコミット/ロールバックするまで待機する
    SelectOptions lockOptions = SelectOptions.get().forUpdate();
    for (ReservationRequestDto.RoomRequest roomReq : request.getRooms()) {
      RoomStockInfo stockInfo = stockCache.get(roomReq.getRoomTypeId());
      if (stockInfo == null) {
        throw new IllegalArgumentException(
            messageSource.getMessage("error.room.type.notfound", null, null));
      }
      // SELECT FOR UPDATEで行ロックを取得しつつ、予約済み室数をカウント
      int currentUsed = reservationDao.countReservedRooms(roomReq.getRoomTypeId(),
          ReservationStatus.RESERVED_STATUSES, request.getCheckInDate(), request.getCheckOutDate(),
          lockOptions);
      if ((stockInfo.getTotalStock() - currentUsed) < roomReq.getRoomCount()) {
        throw new IllegalStateException(
            messageSource.getMessage("error.room.stock.insufficient", null, null));
      }
    }

    // 3. 仮予約レコード登録
    LocalDateTime now = LocalDateTime.now();
    Reservation reservation = new Reservation(null, // reservationId (自動採番)
        null, // reserverId (仮予約用IDは必要に応じて指定)
        now, // reservedAt (予約日時)
        request.getCheckInDate(), request.getCheckOutDate(), null, // arriveAt（仮予約時はnull、顧客情報登録時に設定）
        ReservationStatus.TENTATIVE, // 仮予約ステータス
        now.plusMinutes(reservationProperties.getTentative().getExpiryMinutes()) // pendingLimitAt (仮予約期限)
    );
    Result<Reservation> insertResult = reservationDao.insert(reservation);
    // immutableエンティティのため、Result.getEntity()から自動採番されたIDを取得
    Integer newReservationId = insertResult.getEntity().getReservationId();

    // 4. 予約明細レコード登録
    // 【セキュリティ対策】フロントエンドから送信されたpriceは使用せず、バックエンドで再計算
    // これにより、クライアント側での価格改竄攻撃を完全に防止
    for (ReservationRequestDto.RoomRequest roomReq : request.getRooms()) {
      RoomStockInfo stockInfo = stockCache.get(roomReq.getRoomTypeId());
      // 価格をバックエンドで再計算（チェックイン日〜チェックアウト日の範囲でループして合算）
      Integer calculatedPrice = PriceCalculator.calculateTotalPrice(stockInfo.getRoomCapacity(),
          stockInfo.getHotelId(), request.getCheckInDate(), request.getCheckOutDate());

      ReservationDetail detail = new ReservationDetail(null, // reservationDetailId (自動採番)
          newReservationId, roomReq.getRoomTypeId(), roomReq.getRoomCount(),
          calculatedPrice * roomReq.getRoomCount());
      reservationDetailDao.insert(detail);
    }

    // 5. 予約ID返却
    return newReservationId;
  }

  /**
   * 予約IDを指定して予約情報を取得します。
   *
   * 【処理概要】
   * 予約テーブルと予約明細テーブルを結合し、
   * ホテル名、部屋タイプ情報、合計料金を含む
   * レスポンスDTOを構築して返却する。
   *
   * 【ステータス制限】
   * TENTATIVE（10）ステータスの予約のみ取得可能。
   * キャンセル済みや確定済みの予約は取得できない。
   *
   * @param reservationId 予約ID
   * @return 予約情報レスポンスDTO
   * @throws IllegalArgumentException 指定された予約IDの予約が存在しない場合
   */
  public ReservationResponseDto getReservation(Integer reservationId) {
    List<ReservationWithInfo> rawList = reservationDao.selectByIdWithDetails(reservationId,
        ReservationStatus.TENTATIVE);

    if (rawList.isEmpty()) {
      throw new IllegalArgumentException(
          messageSource.getMessage("error.reservation.notfound", null, null));
    }

    ReservationWithInfo first = rawList.get(0);

    int totalFee = rawList.stream().mapToInt(ReservationWithInfo::getHowMuch).sum();

    List<ReservationResponseDto.RoomDetailDto> rooms = rawList
        .stream().map(r -> new ReservationResponseDto.RoomDetailDto(r.getRoomTypeId(),
            r.getRoomTypeName(), r.getRoomCapacity(), r.getRoomCount()))
        .collect(Collectors.toList());

    // 顧客情報をDTOに変換（予約者が未登録の場合はnull）
    ReservationResponseDto.CustomerInfoDto customerInfo = null;
    if (first.getReserverFirstName() != null || first.getReserverLastName() != null) {
      customerInfo = new ReservationResponseDto.CustomerInfoDto(first.getReserverFirstName(),
          first.getReserverLastName(), first.getPhoneNumber(), first.getEmailAddress(),
          first.getArriveAt());
    }

    return new ReservationResponseDto(first.getReservationId(), first.getCheckInDate(),
        first.getCheckOutDate(), first.getHotelName(), rooms, totalFee, customerInfo);
  }

  /**
   * 顧客情報を登録・更新（アップサート）します。
   *
   * 予約者情報が存在しない場合は新規登録（INSERT）、
   * 存在する場合は更新（UPDATE）を行います。
   *
   * 【処理フロー】
   * 1. 仮予約の有効期限を確認
   * 2. 予約者情報をreserversテーブルに登録または更新
   * 3. 予約レコードに予約者IDと到着予定時刻を更新
   *
   * @param reservationId 予約ID
   * @param request 顧客情報リクエストDTO
   * @throws ReservationExpiredException 仮予約の有効期限切れ時
   * @throws IllegalStateException 更新失敗時
   */
  @Transactional(rollbackFor = Exception.class)
  public void upsertCustomerInfo(Integer reservationId, CustomerRequestDto request) {
    // 1. 仮予約の有効期限確認
    if (reservationDao.isExpired(reservationId, ReservationStatus.TENTATIVE)) {
      throw new ReservationExpiredException(
          messageSource.getMessage("error.reservation.expired", null, null), reservationId);
    }
    // 2. 現在の予約者情報を確認
    Integer currentReserverId = reservationDao.selectReserverId(reservationId);
    Integer targetReserverId = null;

    if (currentReserverId != null) {
      // 既存の予約者情報がある場合は更新
      Reserver updateReserver = new Reserver(currentReserverId, request.getReserverFirstName(),
          request.getReserverLastName(), request.getPhoneNumber(), request.getEmailAddress());
      reserverDao.update(updateReserver);
      targetReserverId = currentReserverId;
    }
    else {
      // 予約者情報がない場合は新規登録
      Reserver reserver = new Reserver(null, // reserverId (自動採番)
          request.getReserverFirstName(), request.getReserverLastName(), request.getPhoneNumber(),
          request.getEmailAddress());
      Result<Reserver> result = reserverDao.insert(reserver);
      targetReserverId = result.getEntity().getReserverId();
    }

    // 3. 予約レコードに予約者IDと到着予定時刻を紐付け
    // 到着時刻が未入力の場合はデフォルト値を適用
    java.time.LocalTime arriveAt = request.getArriveAt();
    if (arriveAt == null) {
      arriveAt = java.time.LocalTime.parse(reservationProperties.getDefaultArrivalTime());
    }
    // 【TOCTOU競合対策】SQLのWHERE句に pending_limit_at > NOW() 条件を含めることで、
    // 検証と更新を原子的に実行。事前のisExpiredチェックは早期エラー返却用。
    int updated = reservationDao.bindReserverAndArrivalTime(reservationId, targetReserverId,
        arriveAt, ReservationStatus.TENTATIVE);
    if (updated == 0) {
      // 更新失敗時は期限切れを再確認して適切な例外をスロー
      if (reservationDao.isExpired(reservationId, ReservationStatus.TENTATIVE)) {
        throw new ReservationExpiredException(
            messageSource.getMessage("error.reservation.expired", null, null), reservationId);
      }
      // 期限切れ以外の原因（予約が存在しない、ステータスが変更された等）
      throw new IllegalStateException(
          messageSource.getMessage("error.reservation.update.failed", null, null));
    }
  }

  /**
   * 仮予約をキャンセルします。
   *
   * P-020（予約詳細入力）のキャンセルボタン押下時に呼び出されます。
   * 以下の条件を満たす場合のみ更新します:
   * - 予約ステータスがTENTATIVE（10）
   * - pending_limit_atが現在時刻以降（まだ有効）
   *
   * 条件を満たさない場合（既にタイムアウト済み、バッチで処理済み等）は更新件数0を返します。
   * これはベストエフォートの処理であり、バッチ処理が最終的な整合性を保証します。
   *
   * @param reservationId キャンセルする予約ID
   * @return 更新件数（条件を満たさない場合は0）
   */
  @Transactional(rollbackFor = Exception.class)
  public int cancelReservation(Integer reservationId) {
    int updated = reservationDao.cancelReservation(reservationId, ReservationStatus.TENTATIVE,
        ReservationStatus.CANCELLED);
    if (updated > 0) {
      log.info("Reservation cancelled: id={}", reservationId);
    }
    else {
      log.info("Reservation not cancelled (already processed or not eligible): id={}",
          reservationId);
    }
    return updated;
  }

  /**
   * 仮予約を期限切れ（EXPIRED）ステータスに更新します。
   *
   * P-910（予約有効時間切れ画面）からトップページに戻る際に呼び出されます。
   * 以下の条件を満たす場合のみ更新します:
   * - 予約ステータスがTENTATIVE（10）
   * - pending_limit_atが現在時刻以前（タイムアウト済み）
   *
   * 条件を満たさない場合（既に別ステータス、バッチで処理済み等）は更新件数0を返します。
   * これはベストエフォートの処理であり、バッチ処理が最終的な整合性を保証します。
   *
   * @param reservationId 期限切れにする予約ID
   * @return 更新件数（条件を満たさない場合は0）
   */
  @Transactional(rollbackFor = Exception.class)
  public int expireReservation(Integer reservationId) {
    int updated = reservationDao.expireReservation(reservationId, ReservationStatus.TENTATIVE,
        ReservationStatus.EXPIRED);
    if (updated > 0) {
      log.info("Reservation expired: id={}", reservationId);
    }
    else {
      log.info("Reservation not expired (already processed or not eligible): id={}", reservationId);
    }
    return updated;
  }

  /**
   * 仮予約を確定（CONFIRMEDステータス）に更新します。
   *
   * P-030（予約確認）の確定ボタン押下時に呼び出されます。
   * 以下の条件を満たす場合のみ更新します:
   * - 予約ステータスがTENTATIVE（10）
   * - pending_limit_atが現在時刻以前（まだ有効）
   *
   * @param reservationId 確定する予約ID
   * @throws ReservationExpiredException 仮予約の有効期限切れ時
   * @throws IllegalStateException 更新失敗時
   */
  @Transactional(rollbackFor = Exception.class)
  public void confirmReservation(Integer reservationId) {
    // 事前の期限切れチェック（早期エラー返却用）
    if (reservationDao.isExpired(reservationId, ReservationStatus.TENTATIVE)) {
      throw new ReservationExpiredException(
          messageSource.getMessage("error.reservation.expired", null, null), reservationId);
    }

    // 【TOCTOU競合対策】SQLのWHERE句に pending_limit_at > NOW() 条件を含めることで、
    // 検証と更新を原子的に実行
    int updated = reservationDao.confirmReservation(reservationId, ReservationStatus.TENTATIVE,
        ReservationStatus.CONFIRMED);
    if (updated == 0) {
      // 更新失敗時は期限切れを再確認して適切な例外をスロー
      if (reservationDao.isExpired(reservationId, ReservationStatus.TENTATIVE)) {
        throw new ReservationExpiredException(
            messageSource.getMessage("error.reservation.expired", null, null), reservationId);
      }
      // 期限切れ以外の原因（予約が存在しない、ステータスが変更された等）
      throw new IllegalStateException(
          messageSource.getMessage("error.reservation.update.failed", null, null));
    }
    log.info("Reservation confirmed: id={}", reservationId);
  }
}
