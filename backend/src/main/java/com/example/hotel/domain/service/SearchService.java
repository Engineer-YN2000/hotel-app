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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空室検索業務サービス。DAOの予約済み件数と起動時キャッシュ(総在庫)を突き合わせて残在庫を算出しDTOへ組み立てる。 注意: SQLクエリ内の予約ステータス値は
 * ReservationStatus.RESERVED_STATUSES (TENTATIVE, CONFIRMED) と対応
 *
 * @see ReservationStatus 予約ステータス定数の定義
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

  private final SearchDao searchDao;
  private final CacheService cacheService;

  public SearchService(SearchDao searchDao, CacheService cacheService) {
    this.searchDao = searchDao;
    this.cacheService = cacheService;
  }

  /**
   * 空室検索を実行し、キャッシュ上の総在庫と予約済み件数から残在庫付き結果DTOを生成する。 キャッシュが空の場合は空結果を返す。
   */
  public SearchResultDto searchAvailableHotels(SearchCriteriaDto criteria) {

    Map<Integer, RoomStockInfo> stockCache = cacheService.getStockCache();
    if (stockCache.isEmpty()) {
      log.warn("在庫キャッシュが空です。起動時ローダーを確認してください。");
      return SearchResultDto.createEmptyResult();
    }

    // 都道府県IDで検索を実行（prefecture-based search への移行完了）
    Integer searchPrefectureId = criteria.getPrefectureId();
    if (searchPrefectureId == null) {
      log.warn("都道府県IDが指定されていません。検索を中止します。");
      return SearchResultDto.createEmptyResult();
    }

    List<AvailableRoomInfo> dbRooms = searchDao.searchAvailableRooms(searchPrefectureId,
        criteria.getCheckInDate(), criteria.getCheckOutDate(), ReservationStatus.RESERVED_STATUSES,
        SelectOptions.get());

    log.debug("データベースから取得した部屋情報: {}件", dbRooms.size());
    if (log.isDebugEnabled()) {
      dbRooms.forEach(room -> log.debug(
          "取得データ: hotelId={}, hotelName={}, roomTypeId={}, reservedCount={}, areaId={}",
          room.getHotelId(), room.getHotelName(), room.getRoomTypeId(), room.getReservedCount(),
          room.getAreaId()));
    }

    List<HotelResultDto> hotelResults = calculateHotelResultRooms(dbRooms, stockCache,
        criteria.getCheckInDate());

    if (hotelResults.isEmpty()) {
      log.info("検索結果が0件です。DBから{}件取得したが、在庫計算後に0件になりました。", dbRooms.size());
      return SearchResultDto.createEmptyResult();
    }

    log.info("検索結果件数: {}", hotelResults.size());
    return new SearchResultDto(hotelResults, criteria, null);
  }

  /**
   * DAOから取得した行集合をホテル単位に集約し、部屋タイプ毎の残在庫を計算してホテル結果DTO一覧へ変換する。
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
        /*
         * 【重要】room_type_idをキーとするMapへの変換について 一見すると異なるホテルに同じroom_type_idが存在する場合に
         * Map変換時に重複エラーが発生する可能性があるように見えるが、 データベース設計上この問題は回避されている： 1. room_types.room_type_id は
         * PRIMARY KEY（自動採番） → システム全体で一意の値が保証される 2. room_types.hotel_id は FOREIGN KEY →
         * 各部屋タイプは特定のホテルに紐づく 3. 同一検索条件（都道府県）内では、room_type_idの重複は データベース制約上発生しない
         * したがって、Collectors.toMap()での重複キー例外は データ整合性が保たれている限り発生しない。
         */
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
