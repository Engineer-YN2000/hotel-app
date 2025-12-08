package com.example.hotel.presentation.dto.top;

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
 * HotelResultDtoの子要素として使用される。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeResultDto {
  private Integer roomTypeId;
  private Integer hotelId;
  private String roomTypeName;
  private Integer roomCapacity;
  private Integer availableStock;
  private Integer price;

  /**
   * 価格を自動計算する外部ロジックをコンストラクタに組み込む（後方互換性）
   */
  public RoomTypeResultDto(Integer roomTypeId, String roomTypeName, Integer roomCapacity,
      Integer availableStock) {
    this.roomTypeId = roomTypeId;
    this.roomTypeName = roomTypeName;
    this.roomCapacity = roomCapacity;
    this.availableStock = availableStock;
    this.price = PriceCalculator.calculatePrice(roomCapacity);
  }

  /**
   * ホテルIDと日付を考慮したダイナミックプライシング対応コンストラクタ
   */
  public RoomTypeResultDto(Integer roomTypeId, String roomTypeName, Integer roomCapacity,
      Integer availableStock, Integer hotelId, LocalDate date) {
    this.roomTypeId = roomTypeId;
    this.roomTypeName = roomTypeName;
    this.roomCapacity = roomCapacity;
    this.availableStock = availableStock;
    this.price = PriceCalculator.calculatePrice(roomCapacity, hotelId, date);
  }

  /**
   * チェックイン日〜チェックアウト日の範囲で価格を計算し、合算した総額を設定するコンストラクタ
   *
   * UIに表示される価格は「ユーザーが指定した日数宿泊した際の料金」であるため、
   * 各宿泊日の価格を日毎に計算してループで合算する。
   *
   * @param roomTypeId 部屋タイプID
   * @param roomTypeName 部屋タイプ名
   * @param roomCapacity 定員数
   * @param availableStock 残在庫数
   * @param hotelId ホテルID（価格計算用）
   * @param checkInDate チェックイン日（宿泊開始日）
   * @param checkOutDate チェックアウト日（宿泊終了日、この日は宿泊しない）
   */
  public RoomTypeResultDto(Integer roomTypeId, String roomTypeName, Integer roomCapacity,
      Integer availableStock, Integer hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
    this.roomTypeId = roomTypeId;
    this.roomTypeName = roomTypeName;
    this.roomCapacity = roomCapacity;
    this.availableStock = availableStock;
    this.hotelId = hotelId;
    this.price = PriceCalculator.calculateTotalPrice(roomCapacity, hotelId, checkInDate,
        checkOutDate);
  }
}
