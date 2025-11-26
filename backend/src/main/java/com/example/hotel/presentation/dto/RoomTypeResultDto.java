package com.example.hotel.presentation.dto;

import com.example.hotel.utils.PriceCalculator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

/**
 * 部屋タイプ別の検索結果DTO
 *
 * 一つの部屋タイプに対する残在庫情報と価格情報を保持し、
 * 複数泊の総額計算機能を提供します。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeResultDto {
  private Integer roomTypeId;
  private String roomTypeName;
  private Integer capacity;
  private Integer availableStock;
  private Integer price;

  /**
   * 価格を自動計算する外部ロジックをコンストラクタに組み込む（後方互換性）
   */
  public RoomTypeResultDto(Integer roomTypeId, String roomTypeName, Integer capacity,
      Integer availableStock) {
    this.roomTypeId = roomTypeId;
    this.roomTypeName = roomTypeName;
    this.capacity = capacity;
    this.availableStock = availableStock;
    this.price = PriceCalculator.calculatePrice(capacity);
  }

  /**
   * ホテルIDと日付を考慮したダイナミックプライシング対応コンストラクタ
   */
  public RoomTypeResultDto(Integer roomTypeId, String roomTypeName, Integer capacity,
      Integer availableStock, Integer hotelId, LocalDate date) {
    this.roomTypeId = roomTypeId;
    this.roomTypeName = roomTypeName;
    this.capacity = capacity;
    this.availableStock = availableStock;
    this.price = PriceCalculator.calculatePrice(capacity, hotelId, date);
  }
}
