package com.example.hotel.domain.model;

import org.seasar.doma.*;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * 予約詳細エンティティ データベースのreservation_detailsテーブルに対応
 */
@Entity(immutable = true)
@Table(name = "reservation_details")
@Value
@AllArgsConstructor
public class ReservationDetail {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reservation_detail_id")
  private final Integer reservationDetailId;

  @Column(name = "reservation_id")
  private final Integer reservationId;

  @Column(name = "room_type_id")
  private final Integer roomTypeId;

  @Column(name = "room_count")
  private final Integer roomCount;

  @Column(name = "how_much")
  private final Integer howMuch;
}
