package com.example.hotel.presentation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 空室検索結果のルートDTO。ホテル毎の利用可能な部屋タイプと残在庫/簡易価格を保持する。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

  private List<HotelResultDto> hotels;
  private SearchCriteriaDto criteria;

  /**
   * 空結果（ホテル一覧なし）を生成するファクトリ。
   */
  public static SearchResultDto createEmptytResult() {
    return new SearchResultDto(Collections.emptyList(), null);
  }

  /**
   * 1ホテル分の検索結果DTO。ホテル基本情報と利用可能な部屋タイプ一覧を保持する。
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HotelResultDto {
    private Integer hotelId;
    private String hotelName;
    private List<RoomTypeResultDto> roomTypes;
  }

  /**
   * 1部屋タイプ分の検索結果DTO。残在庫と簡易的な価格を保持する。
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoomTypeResultDto {
    private Integer roomTypeId;
    private String roomTypeName;
    private Integer capacity;
    private Integer availableStock;
    private Integer price;

    public RoomTypeResultDto(Integer roomTypeId, String roomTypeName, Integer capacity,
        Integer availableStock) {
      this.roomTypeId = roomTypeId;
      this.roomTypeName = roomTypeName;
      this.capacity = capacity;
      this.availableStock = availableStock;
      this.price = (10000 + new Random().nextInt(15) * 1000) * capacity;
    }
  }
}
