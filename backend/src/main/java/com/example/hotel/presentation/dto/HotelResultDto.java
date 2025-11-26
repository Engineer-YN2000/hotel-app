package com.example.hotel.presentation.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 1ホテル分の検索結果DTO。ホテル基本情報と利用可能な部屋タイプ一覧を保持する。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelResultDto {
  private Integer hotelId;
  private String hotelName;
  private Integer areaId; // 詳細地域ID（絞り込み用）
  private List<RoomTypeResultDto> roomTypes;
}
