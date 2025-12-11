package com.example.hotel.domain.model;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import lombok.Data;

import java.time.LocalDate;

/**
 * 予約情報と関連情報を保持するエンティティ
 *
 * 予約テーブル、予約明細テーブル、部屋タイプテーブル、ホテルテーブル、
 * 予約者テーブルを結合したクエリ結果をマッピングする。
 * P-020（予約詳細入力）、P-030（予約確認）で使用。
 */
@Entity(immutable = false)
@Data
public class ReservationWithInfo {
  // 予約基本情報（reservationsテーブル由来）
  @Column(name = "reservation_id")
  private Integer reservationId;
  @Column(name = "check_in_date")
  private LocalDate checkInDate;
  @Column(name = "check_out_date")
  private LocalDate checkOutDate;
  @Column(name = "arrive_at")
  private String arriveAt;

  // ホテル情報（hotelsテーブル由来）
  @Column(name = "hotel_name")
  private String hotelName;

  // 部屋タイプ情報（room_types, reservation_detailsテーブル由来）
  @Column(name = "room_type_id")
  private Integer roomTypeId;
  @Column(name = "room_type_name")
  private String roomTypeName;
  @Column(name = "room_capacity")
  private Integer roomCapacity;
  @Column(name = "room_count")
  private Integer roomCount;
  @Column(name = "how_much")
  private Integer howMuch;

  // 予約者情報（reserversテーブル由来）
  @Column(name = "reserver_first_name")
  private String reserverFirstName;
  @Column(name = "reserver_last_name")
  private String reserverLastName;
  @Column(name = "e_mail_address")
  private String emailAddress;
  @Column(name = "phone_number")
  private String phoneNumber;
}
