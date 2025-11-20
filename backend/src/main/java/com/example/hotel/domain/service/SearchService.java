package com.example.hotel.domain.service;

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
 * 空室検索業務サービス。DAOの予約済み件数と起動時キャッシュ(総在庫)を突き合わせて残在庫を算出しDTOへ組み立てる。
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

    // 都道府県IDで検索を実行
    Integer searchPrefectureId = criteria.getPrefectureId() != null ? criteria.getPrefectureId()
        : criteria.getAreaId();

    List<AvailableRoomInfo> dbRooms = searchDao.searchAvailableRooms(searchPrefectureId,
        criteria.getCheckInDate(), criteria.getCheckOutDate(), SelectOptions.get());

    List<HotelResultDto> hotelResults = calculateHotelResultRooms(dbRooms, stockCache);

    if (hotelResults.isEmpty()) {
      log.info("検索結果が0件です。");
      return SearchResultDto.createEmptyResult();
    }

    log.info("検索結果件数: {}", hotelResults.size());
    return new SearchResultDto(hotelResults, criteria);
  }

  /**
   * DAOから取得した行集合をホテル単位に集約し、部屋タイプ毎の残在庫を計算してホテル結果DTO一覧へ変換する。
   */
  private List<HotelResultDto> calculateHotelResultRooms(List<AvailableRoomInfo> dbRooms,
      Map<Integer, RoomStockInfo> stockCache) {

    Map<Integer, RoomTypeResultDto> roomTypeResults = dbRooms.stream()
        .filter(dbRoom -> stockCache.containsKey(dbRoom.getRoomTypeId())).map(dbRoom -> {
          RoomStockInfo cacheInfo = stockCache.get(dbRoom.getRoomTypeId());
          int totalStock = cacheInfo.getTotalStock();
          int reservedCount = dbRoom.getReservedCount();
          int availableStock = totalStock - reservedCount;

          return new RoomTypeResultDto(dbRoom.getRoomTypeId(), dbRoom.getRoomTypeName(),
              cacheInfo.getRoomCapacity(), availableStock, Long.valueOf(dbRoom.getHotelId()),
              java.time.LocalDate.now());
        }).filter(roomDto -> roomDto.getAvailableStock() > 0)
        .collect(Collectors.toMap(RoomTypeResultDto::getRoomTypeId, dto -> dto));

    Map<Integer, List<AvailableRoomInfo>> groupedByHotel = dbRooms.stream()
        .filter(dbRoom -> roomTypeResults.containsKey(dbRoom.getRoomTypeId()))
        .collect(Collectors.groupingBy(AvailableRoomInfo::getHotelId));

    return groupedByHotel.entrySet().stream().map(entry -> {
      Integer hotelId = entry.getKey();
      String hotelName = entry.getValue().get(0).getHotelName();

      List<RoomTypeResultDto> roomsForHotel = entry.getValue().stream()
          .map(dbRoom -> roomTypeResults.get(dbRoom.getRoomTypeId())).distinct()
          .collect(Collectors.toList());
      return new HotelResultDto(hotelId, hotelName, roomsForHotel);
    }).collect(Collectors.toList());
  }
}
