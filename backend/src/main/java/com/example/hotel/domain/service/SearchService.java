package com.example.hotel.domain.service;

import com.example.hotel.domain.constants.ReservationStatus;
import com.example.hotel.domain.model.AvailableRoomInfo;
import com.example.hotel.domain.model.RoomStockInfo;
import com.example.hotel.domain.repository.SearchDao;
import com.example.hotel.presentation.dto.SearchCriteriaDto;
import com.example.hotel.presentation.dto.SearchResultDto;
import com.example.hotel.presentation.dto.HotelResultDto;
import com.example.hotel.presentation.dto.RoomTypeResultDto;
import lombok.extern.slf4j.Slf4j;
import org.seasar.doma.jdbc.SelectOptions;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空室検索業務サービス
 *
 * 【主要責務】
 * DAOの予約済み件数と起動時キャッシュ(総在庫)を突き合わせて残在庫を算出し、
 * 検索結果DTOへ組み立てを行う。
 *
 * 【重要な注意事項】
 * SQLクエリ内の予約ステータス値は ReservationStatus.RESERVED_STATUSES (TENTATIVE, CONFIRMED) と対応している。
 *
 * @see ReservationStatus 予約ステータス定数の定義
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

  private final SearchDao searchDao;
  private final CacheService cacheService;
  private final MessageSource messageSource;

  public SearchService(SearchDao searchDao, CacheService cacheService,
      MessageSource messageSource) {
    this.searchDao = searchDao;
    this.cacheService = cacheService;
    this.messageSource = messageSource;
  }

  /**
   * 空室検索を実行し、残在庫付き結果DTOを生成する。
   *
   * 【処理概要】
   * キャッシュ上の総在庫と予約済み件数から残在庫を計算し、
   * 検索結果DTOを構築して返却する。
   *
   * 【戻り値の条件】
   * - キャッシュが空の場合: 空結果を返す
   * - 検索結果が0件の場合: 空結果を返す
   * - 正常な検索結果がある場合: ホテル・部屋情報を含む結果を返す
   *
   * @param criteria 検索条件
   * @return 検索結果DTO
   */
  public SearchResultDto searchAvailableHotels(SearchCriteriaDto criteria) {
    // 【セキュリティ設計】
    // サービス層ではエラー時も空結果を返却し、内部エラー状態を外部に漏らさない
    // 詳細なエラー情報はサーバーログに記録し、攻撃者によるシステム内部状態の推測を防止

    Map<Integer, RoomStockInfo> stockCache = cacheService.getStockCache();
    if (stockCache.isEmpty()) {
      log.warn(messageSource.getMessage("log.service.cache.empty", null, Locale.getDefault()));
      return SearchResultDto.createEmptyResult();
    }

    // 都道府県IDで検索を実行（prefecture-based search への移行完了）
    Integer searchPrefectureId = criteria.getPrefectureId();
    if (searchPrefectureId == null) {
      log.warn(messageSource.getMessage("log.service.prefecture.id.missing", new Object[]{criteria},
          Locale.getDefault()));
      return SearchResultDto.createEmptyResult();
    }

    List<AvailableRoomInfo> dbRooms = searchDao.searchAvailableRooms(searchPrefectureId,
        criteria.getCheckInDate(), criteria.getCheckOutDate(), ReservationStatus.RESERVED_STATUSES,
        SelectOptions.get());

    log.debug(messageSource.getMessage("log.service.rooms.retrieved", new Object[]{dbRooms.size()},
        Locale.getDefault()));
    if (log.isDebugEnabled()) {
      dbRooms.forEach(room -> log.debug(messageSource.getMessage(
          "log.service.room.data.detail", new Object[]{room.getHotelId(), room.getHotelName(),
              room.getRoomTypeId(), room.getReservedCount(), room.getAreaId()},
          Locale.getDefault())));
    }

    List<HotelResultDto> hotelResults = calculateHotelResultRooms(dbRooms, stockCache,
        criteria.getCheckInDate());

    if (hotelResults.isEmpty()) {
      log.info(messageSource.getMessage("log.service.search.result.empty",
          new Object[]{dbRooms.size()}, Locale.getDefault()));
      return SearchResultDto.createEmptyResult();
    }

    log.info(messageSource.getMessage("log.service.search.result.count",
        new Object[]{hotelResults.size()}, Locale.getDefault()));
    return SearchResultDto.create(hotelResults, criteria);
  }

  /**
   * DAOから取得した行集合をホテル単位に集約し、部屋タイプ毎の残在庫を計算してホテル結果DTO一覧へ変換する。
   *
   * 【重要】データベース設計による一意性保証について:
   * このメソッドでは room_type_id をキーとする Map 変換を行うが、重複キー例外は発生しない。
   *
   * 理由:
   * 1. room_types.room_type_id は PRIMARY KEY（自動採番）
   *    → システム全体で一意の値が保証される
   * 2. room_types.hotel_id は FOREIGN KEY
   *    → 各部屋タイプは特定のホテルに紐づく
   * 3. 同一検索条件（都道府県）内では、room_type_id の重複はデータベース制約上発生しない
   *
   * したがって、Collectors.toMap() での重複キー例外は
   * データ整合性が保たれている限り発生しない。
   *
   * @param dbRooms DAOから取得した部屋情報一覧
   * @param stockCache 部屋タイプ別在庫キャッシュ
   * @param checkInDate チェックイン日（価格計算用）
   * @return ホテル結果DTO一覧
   */
  private List<HotelResultDto> calculateHotelResultRooms(List<AvailableRoomInfo> dbRooms,
      Map<Integer, RoomStockInfo> stockCache, java.time.LocalDate checkInDate) {

    Map<Integer, RoomTypeResultDto> roomTypeResults = dbRooms.stream()
        .filter(dbRoom -> stockCache.containsKey(dbRoom.getRoomTypeId())).map(dbRoom -> {
          RoomStockInfo cacheInfo = stockCache.get(dbRoom.getRoomTypeId());
          int totalStock = cacheInfo.getTotalStock();
          int reservedCount = dbRoom.getReservedCount();
          int availableStock = totalStock - reservedCount;

          return new RoomTypeResultDto(dbRoom.getRoomTypeId(), dbRoom.getRoomTypeName(),
              cacheInfo.getRoomCapacity(), availableStock, dbRoom.getHotelId(), checkInDate);
        }).filter(roomDto -> roomDto.getAvailableStock() > 0)
        .collect(Collectors.toMap(RoomTypeResultDto::getRoomTypeId, dto -> dto));

    Map<Integer, List<AvailableRoomInfo>> groupedByHotel = dbRooms.stream()
        .filter(dbRoom -> roomTypeResults.containsKey(dbRoom.getRoomTypeId()))
        .collect(Collectors.groupingBy(AvailableRoomInfo::getHotelId));

    return groupedByHotel.entrySet().stream().map(entry -> {
      Integer hotelId = entry.getKey();
      String hotelName = entry.getValue().get(0).getHotelName();
      Integer areaId = entry.getValue().get(0).getAreaId(); // 詳細地域ID

      List<RoomTypeResultDto> roomsForHotel = entry.getValue().stream()
          .map(dbRoom -> roomTypeResults.get(dbRoom.getRoomTypeId())).distinct()
          .collect(Collectors.toList());
      return new HotelResultDto(hotelId, hotelName, areaId, roomsForHotel);
    }).collect(Collectors.toList());
  }
}
