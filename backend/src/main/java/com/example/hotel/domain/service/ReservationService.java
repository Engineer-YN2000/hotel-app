package com.example.hotel.domain.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotel.config.ReservationProperties;
import com.example.hotel.domain.repository.ReservationDao;
import com.example.hotel.domain.repository.ReservationDetailDao;
import com.example.hotel.domain.repository.ReserverDao;
import com.example.hotel.domain.security.SessionTokenService;
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
import com.example.hotel.domain.exception.SessionTokenMismatchException;

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
 * 【三層防御の設計意図】
 * 以下のメソッドでは期限切れ検証を3段階で行っている。これは冗長に見えるが、
 * それぞれ異なる目的を持つ意図的な設計である：
 * - {@link #upsertCustomerInfo(Integer, CustomerRequestDto)}
 * - {@link #confirmReservation(Integer)}
 *
 * 1. 事前チェック（早期リターン）
 *    - 目的: Fail Fast原則に基づき、無効なリクエストを即座に拒否
 *    - 効果: 期限切れ予約に対する無駄なDB更新処理を回避し、リソースを節約
 *    - 意図: 「期限切れ予約に更新ロジックを働かせること自体がおかしい」という設計思想
 *
 * 2. 原子的更新（WHERE句に期限条件）
 *    - 目的: TOCTOU（Time-of-check to time-of-use）競合への安全弁
 *    - 効果: 事前チェック後〜UPDATE実行までの間に期限切れになるケースを防御
 *    - 意図: レースコンディション対策としての最終防御ライン
 *
 * 3. 事後チェック（更新件数0時の再検証）
 *    - 目的: エラー原因の細分化と適切な例外選択
 *    - 効果: 期限切れ（ReservationExpiredException）と他の原因（IllegalStateException）を区別
 *    - 意図: クライアントに正確なエラー情報を返し、適切なUXを提供
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
  private final SessionTokenService sessionTokenService;

  /**
   * 仮予約作成結果
   */
  public record TentativeReservationResult(Integer reservationId, String sessionToken) {
  }

  /**
   * 仮予約（在庫ロック）を作成します。
   * 必要なバリデーション・在庫チェック・予約/明細登録を一括で行います。
   *
   * 【同時実行制御】
   * SELECT FOR UPDATEによる悲観的ロックを使用して、
   * 在庫チェックと予約登録の間に他のトランザクションが介入することを防ぎます。
   * これにより、同時リクエストによるダブルブッキングを防止します。
   *
   * 【セッショントークン】
   * 仮予約作成時にセッショントークンを生成し、DBに保存します。
   * 以降の操作時にこのトークンを検証することで、異なるタブ/端末からの
   * 同時操作を検知・禁止できます。
   *
   * @param request 仮予約リクエストDTO
   * @return 予約IDとセッショントークンを含む結果オブジェクト
   * @throws IllegalArgumentException パラメータ不正時
   * @throws IllegalStateException 在庫不足時
   */
  @Transactional(rollbackFor = Exception.class)
  public TentativeReservationResult createTentativeReservation(ReservationRequestDto request) {
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

    // 3. 仮予約レコード登録（一時的なセッショントークンで登録、INSERT後に正式なトークンで更新）
    // 注: INSERT前にはreservationIdが不明なため、プレースホルダーを使用
    String placeholderToken = "PENDING";

    // 4. 仮予約レコード登録
    LocalDateTime now = LocalDateTime.now();
    Reservation reservation = new Reservation(null, // reservationId (自動採番)
        null, // reserverId (仮予約用IDは必要に応じて指定)
        placeholderToken, // sessionToken（プレースホルダー、INSERT後に更新）
        now, // reservedAt (予約日時)
        request.getCheckInDate(), request.getCheckOutDate(), null, // arriveAt（仮予約時はnull、顧客情報登録時に設定）
        ReservationStatus.TENTATIVE, // 仮予約ステータス
        now.plusMinutes(reservationProperties.getTentative().getExpiryMinutes()) // pendingLimitAt (仮予約期限)
    );
    Result<Reservation> insertResult = reservationDao.insert(reservation);
    // immutableエンティティのため、Result.getEntity()から自動採番されたIDを取得
    Integer newReservationId = insertResult.getEntity().getReservationId();

    // 5. 正式なセッショントークンを生成してDBに保存
    // DB照合方式により、このトークンがDBに保存され、以降の検証で使用される
    // 新しいタブ/端末で再度仮予約を行うと、このトークンが上書きされ、古いトークンは無効になる
    String sessionToken = sessionTokenService.generateToken(newReservationId);
    reservationDao.updateSessionToken(newReservationId, sessionToken, ReservationStatus.TENTATIVE);

    // 6. 予約明細レコード登録
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

    // 7. 予約IDとセッショントークンを返却
    return new TentativeReservationResult(newReservationId, sessionToken);
  }

  /**
   * セッショントークンを検証します。
   *
   * 提供されたセッショントークンがDB上のトークンと一致し、
   * かつ有効期限内であることを検証します。
   *
   * @param reservationId 予約ID
   * @param sessionToken 検証対象のセッショントークン
   * @throws IllegalArgumentException sessionTokenがnullまたは空の場合
   * @throws SessionTokenMismatchException トークンが不一致または無効な場合
   */
  public void validateSessionToken(Integer reservationId, String sessionToken) {
    // 入力検証: null/空のトークンは明確なエラーレスポンスを返す
    if (sessionToken == null || sessionToken.isBlank()) {
      throw new IllegalArgumentException(
          messageSource.getMessage("error.session.token.required", null, null));
    }

    // HMACベースのトークン検証（有効期限10分もここでチェック）
    if (!sessionTokenService.validateToken(reservationId, sessionToken)) {
      throw new SessionTokenMismatchException(
          messageSource.getMessage("error.session.token.mismatch", null, null), reservationId);
    }
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

    // 顧客情報をDTOに変換（予約者の姓名が両方揃っている場合のみ）
    ReservationResponseDto.CustomerInfoDto customerInfo = null;
    if (first.getReserverFirstName() != null && first.getReserverLastName() != null) {
      customerInfo = new ReservationResponseDto.CustomerInfoDto(first.getReserverFirstName(),
          first.getReserverLastName(), first.getPhoneNumber(), first.getEmailAddress());
    }

    // arriveAtはreservationsテーブル由来のため、トップレベルに配置
    return new ReservationResponseDto(first.getReservationId(), first.getCheckInDate(),
        first.getCheckOutDate(), first.getHotelName(), rooms, totalFee, first.getArriveAt(),
        customerInfo);
  }

  /**
   * 顧客情報を登録・更新（アップサート）します。
   *
   * 予約者情報が存在しない場合は新規登録（INSERT）、
   * 存在する場合は更新（UPDATE）を行います。
   *
   * 【処理フロー】
   * 1. 仮予約の有効期限を事前確認（早期リターン）
   * 2. 予約者情報をreserversテーブルに登録または更新
   * 3. 予約レコードに予約者IDと到着予定時刻を更新（原子的更新）
   * 4. 更新失敗時はエラー原因を特定（事後検証）
   *
   * 本メソッドは三層防御パターンを採用しています。設計意図はクラスJavadocを参照してください。
   *
   * @param reservationId 予約ID
   * @param request 顧客情報リクエストDTO
   * @throws ReservationExpiredException 仮予約の有効期限切れ時
   * @throws IllegalStateException 更新失敗時
   */
  @Transactional(rollbackFor = Exception.class)
  public void upsertCustomerInfo(Integer reservationId, CustomerRequestDto request) {
    // 1. 事前チェック: 期限切れ予約は即座に拒否（Fail Fast）
    // 期限切れ予約に対して更新処理を実行すること自体が不適切であるため、
    // DBアクセス前に早期リターンする
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
    // 【原子的更新】SQLのWHERE句に pending_limit_at > NOW() 条件を含めることで、
    // 事前チェック後〜UPDATE実行までの間に期限切れになるレースコンディションを防御
    int updated = reservationDao.bindReserverAndArrivalTime(reservationId, targetReserverId,
        arriveAt, ReservationStatus.TENTATIVE);

    // 4. 事後チェック: 更新失敗時のエラー原因を細分化
    if (updated == 0) {
      // 期限切れが原因かを再確認して適切な例外を選択
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
   * 【ベストエフォート方式を採用する理由】
   * 本メソッドは{@link #confirmReservation}と異なり、例外をスローせず更新件数を返す。
   * この設計差異は以下の理由による：
   *
   * 1. ユーザー体験の観点:
   *    - キャンセルはユーザーが「やめる」操作であり、結果が同じ（予約が無効化）なら成功とみなせる
   *    - 既にタイムアウトやバッチ処理で無効化されていても、ユーザーの目的は達成されている
   *    - エラー表示でユーザーを混乱させる必要がない
   *
   * 2. バッチ処理との共存:
   *    - バッチ処理が定期的に期限切れ予約を整理するため、最終的な整合性は保証される
   *    - フロントエンドからのキャンセルは「早期クリーンアップ」の位置づけ
   *
   * 対照的に、{@link #confirmReservation}は予約確定という重要な操作であり、
   * 失敗時はユーザーに明確なフィードバックを返す必要があるため、例外をスローする。
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
   * 【ベストエフォート方式を採用する理由】
   * {@link #cancelReservation}と同様に、本メソッドは例外をスローせず更新件数を返す。
   * 期限切れ画面からの遷移は「後始末」の操作であり、既にバッチ処理でステータスが
   * 変更されていてもユーザー体験に影響しない。
   * 詳細は{@link #cancelReservation}のドキュメントを参照。
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
   * - pending_limit_atが現在時刻以降（まだ有効）
   *
   * 【厳密なバリデーションを行う理由】
   * 本メソッドは{@link #cancelReservation}や{@link #expireReservation}と異なり、
   * 失敗時に例外をスローする。この設計差異は以下の理由による：
   *
   * 1. 操作の重要性:
   *    - 予約確定はユーザーが「予約を完了した」と認識する重要な操作
   *    - 失敗した場合、ユーザーは「予約が取れた」と誤認するリスクがある
   *    - 明確なエラーフィードバックが必要
   *
   * 2. エラー原因の特定:
   *    - 期限切れの場合は再度予約を促す専用画面へ遷移
   *    - 顧客情報未登録の場合は入力画面へ戻す
   *    - ベストエフォートではこのような分岐ができない
   *
   * 本メソッドは三層防御パターンを採用しています。設計意図はクラスJavadocを参照してください。
   *
   * @param reservationId 確定する予約ID
   * @throws ReservationExpiredException 仮予約の有効期限切れ時
   * @throws IllegalStateException 顧客情報未登録時、または更新失敗時
   */
  @Transactional(rollbackFor = Exception.class)
  public void confirmReservation(Integer reservationId) {
    // 1. 事前チェック: 期限切れ予約は即座に拒否（Fail Fast）
    // 期限切れ予約に対して更新処理を実行すること自体が不適切であるため、
    // DBアクセス前に早期リターンする
    if (reservationDao.isExpired(reservationId, ReservationStatus.TENTATIVE)) {
      throw new ReservationExpiredException(
          messageSource.getMessage("error.reservation.expired", null, null), reservationId);
    }

    // 2. 事前チェック: 顧客情報未登録の予約は即座に拒否（Fail Fast）
    // 顧客情報未登録の予約に対して確定処理を実行すること自体が不適切であるため、
    // UPDATE前に早期リターンする
    Integer reserverId = reservationDao.selectReserverId(reservationId);
    if (reserverId == null) {
      throw new IllegalStateException(
          messageSource.getMessage("error.reservation.customer.notregistered", null, null));
    }

    // 3. 原子的更新: SQLのWHERE句に pending_limit_at > NOW() と reserver_id IS NOT NULL 条件を含めることで、
    // 事前チェック後〜UPDATE実行までの間に発生するレースコンディションを防御
    int updated = reservationDao.confirmReservation(reservationId, ReservationStatus.TENTATIVE,
        ReservationStatus.CONFIRMED);

    // 4. 事後チェック: 更新失敗時のエラー原因を細分化
    if (updated == 0) {
      // 期限切れが原因かを再確認
      if (reservationDao.isExpired(reservationId, ReservationStatus.TENTATIVE)) {
        throw new ReservationExpiredException(
            messageSource.getMessage("error.reservation.expired", null, null), reservationId);
      }
      // 顧客情報が削除された可能性を確認
      Integer currentReserverId = reservationDao.selectReserverId(reservationId);
      if (currentReserverId == null) {
        throw new IllegalStateException(
            messageSource.getMessage("error.reservation.customer.notregistered", null, null));
      }
      // その他の原因（予約が存在しない、ステータスが変更された等）
      throw new IllegalStateException(
          messageSource.getMessage("error.reservation.update.failed", null, null));
    }
    log.info("Reservation confirmed: id={}", reservationId);
  }
}
