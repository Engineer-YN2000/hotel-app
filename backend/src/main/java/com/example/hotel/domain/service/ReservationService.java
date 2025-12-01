package com.example.hotel.domain.service;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotel.domain.repository.ReservationDao;
import com.example.hotel.domain.repository.ReservationDetailDao;
import com.example.hotel.presentation.dto.reservation.ReservationRequestDto;
import com.example.hotel.domain.model.Reservation;
import com.example.hotel.domain.model.ReservationDetail;
import com.example.hotel.domain.model.RoomStockInfo;
import com.example.hotel.domain.constants.ReservationStatus;

import java.time.LocalDateTime;
import java.util.Map;

import org.seasar.doma.jdbc.Result;

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

  /**
   * 仮予約（在庫ロック）を作成します。
   * 必要なバリデーション・在庫チェック・予約/明細登録を一括で行います。
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
    for (ReservationRequestDto.RoomRequest roomReq : request.getRooms()) {
      RoomStockInfo stockInfo = stockCache.get(roomReq.getRoomTypeId());
      if (stockInfo == null) {
        throw new IllegalArgumentException(
            messageSource.getMessage("error.room.type.notfound", null, null));
      }
      int currentUsed = reservationDao.countReservedRoom(roomReq.getRoomTypeId(),
          ReservationStatus.RESERVED_STATUSES, request.getCheckInDate(), request.getCheckOutDate());
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
        now.plusMinutes(15) // pendingLimitAt (仮予約期限: 15分後)
    );
    Result<Reservation> insertResult = reservationDao.insert(reservation);
    // immutableエンティティのため、Result.getEntity()から自動採番されたIDを取得
    Integer newReservationId = insertResult.getEntity().getReservationId();

    // 4. 予約明細レコード登録
    for (ReservationRequestDto.RoomRequest roomReq : request.getRooms()) {
      ReservationDetail detail = new ReservationDetail(null, // reservationDetailId (自動採番)
          newReservationId, roomReq.getRoomTypeId(), roomReq.getRoomCount(),
          roomReq.getPrice() * roomReq.getRoomCount());
      reservationDetailDao.insert(detail);
    }

    // 5. 予約ID返却
    return newReservationId;
  }
}
