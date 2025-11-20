package com.example.hotel.presentation.dto;

import com.example.hotel.utils.PriceCalculator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 1部屋タイプ分の検索結果DTO。残在庫と簡易的な価格を保持する。
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
   * 価格を自動計算する外部ロジックをコンストラクタに組み込む
   */
  public RoomTypeResultDto(Integer roomTypeId, String roomTypeName, Integer capacity,
      Integer availableStock) {
    this.roomTypeId = roomTypeId;
    this.roomTypeName = roomTypeName;
    this.capacity = capacity;
    this.availableStock = availableStock;
    this.price = PriceCalculator.calculatePrice(capacity);
  }
}
