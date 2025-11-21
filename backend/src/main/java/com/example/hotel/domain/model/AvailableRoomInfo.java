package com.example.hotel.domain.model;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Column;
import lombok.Data;

/**
 * 検索結果（ホテル、部屋タイプ、予約済み室数）をマッピングするドメインクラス
 */
@Data
@Entity(immutable = false)
public class AvailableRoomInfo {

  /**
   * ホテルID (hotels.hotel_id)
   */
  @Id
  @Column(name = "hotel_id")
  private Integer hotelId;

  /**
   * ホテル名 (hotels.hotel_name)
   */
  @Column(name = "hotel_name")
  private String hotelName;

  /**
   * 部屋タイプID (room_types.room_type_id)
   */
  @Column(name = "room_type_id")
  private Integer roomTypeId;

  /**
   * 部屋タイプ名 (room_types.room_type_name)
   */
  @Column(name = "room_type_name")
  private String roomTypeName;

  /**
   * 予約済み室数
   */
  @Column(name = "reserved_count")
  private Integer reservedCount;

  /**
   * 詳細地域ID (area_details.area_id)
   */
  @Column(name = "area_id")
  private Integer areaId;
}
