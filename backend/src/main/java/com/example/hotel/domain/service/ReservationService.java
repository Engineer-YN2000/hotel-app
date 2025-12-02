package com.example.hotel.domain.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotel.config.ReservationProperties;
import com.example.hotel.domain.repository.ReservationDao;
import com.example.hotel.domain.repository.ReservationDetailDao;
import com.example.hotel.presentation.dto.reservation.ReservationRequestDto;
import com.example.hotel.utils.PriceCalculator;
import com.example.hotel.domain.model.Reservation;
import com.example.hotel.domain.model.ReservationDetail;
import com.example.hotel.domain.model.RoomStockInfo;
import com.example.hotel.domain.constants.ReservationStatus;

import java.time.LocalDateTime;
import java.util.Map;

import org.seasar.doma.jdbc.Result;
import org.seasar.doma.jdbc.SelectOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationDao reservationDao;
  private final ReservationDetailDao reservationDetailDao;
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
        request.getCheckInDate(), request.getCheckOutDate(), null, // arriveAt（仮予約はデフォルト15:00）
        ReservationStatus.TENTATIVE, // 仮予約ステータス
        now.plusMinutes(reservationProperties.getExpiryMinutes()) // pendingLimitAt (仮予約期限)
    );
    Result<Reservation> insertResult = reservationDao.insert(reservation);
    // immutableエンティティのため、Result.getEntity()から自動採番されたIDを取得
    Integer newReservationId = insertResult.getEntity().getReservationId();

    // 4. 予約明細レコード登録
    // 【セキュリティ対策】フロントエンドから送信されたpriceは使用せず、バックエンドで再計算
    // これにより、クライアント側での価格改竄攻撃を完全に防止
    for (ReservationRequestDto.RoomRequest roomReq : request.getRooms()) {
      RoomStockInfo stockInfo = stockCache.get(roomReq.getRoomTypeId());
      // 価格をバックエンドで再計算（同じ引数なら検索時と同一の結果）
      Integer calculatedPrice = PriceCalculator.calculatePrice(stockInfo.getRoomCapacity(),
          stockInfo.getHotelId(), request.getCheckInDate());

      ReservationDetail detail = new ReservationDetail(null, // reservationDetailId (自動採番)
          newReservationId, roomReq.getRoomTypeId(), roomReq.getRoomCount(),
          calculatedPrice * roomReq.getRoomCount());
      reservationDetailDao.insert(detail);
    }

    // 5. 予約ID返却
    return newReservationId;
  }
}
