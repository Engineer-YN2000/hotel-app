package com.example.hotel.domain.model;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Column;

import lombok.Value;
import lombok.AllArgsConstructor;

/**
 * 部屋タイプごとの総在庫・定員情報(DB検索結果)を保持するドメインクラス
 */
@Value
@Entity(immutable = true)
@AllArgsConstructor
public class RoomStockInfo {

  /**
   * 部屋タイプID (room_types.room_type_id)
   */
  @Id
  @Column(name = "room_type_id")
  private final Integer roomTypeId;

  /**
   * ホテルID (room_types.hotel_id)
   * 価格計算時のホテル係数算出に使用
   */
  @Column(name = "hotel_id")
  private final Integer hotelId;

  /**
   * 部屋タイプ名 (room_types.room_type_name)
   */
  @Column(name = "room_type_name")
  private final String roomTypeName;

  /**
   * 定員 (room_types.room_capacity)
   */
  @Column(name = "room_capacity")
  private final Integer roomCapacity;

  /**
   * 総在庫 (総室数)
   */
  @Column(name = "total_stock")
  private final Integer totalStock;
}
