package com.example.hotel.presentation.dto.top;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 価格再計算レスポンスDTO
 *
 * 各部屋タイプの再計算された価格を返却する。
 * 内部の価格計算ロジック（係数、需要変動等）は隠蔽し、
 * 計算結果のみをクライアントに提供する。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResponseDto {

  private List<RoomPriceDto> rooms;

  /**
   * 部屋タイプ別の価格情報
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoomPriceDto {
    private Integer roomTypeId;
    private Integer hotelId;
    /** 宿泊期間の総額（1部屋あたり） */
    private Integer price;
  }

  /**
   * 空の結果を生成するファクトリメソッド
   */
  public static PriceCalculationResponseDto createEmpty() {
    return new PriceCalculationResponseDto(List.of());
  }
}
