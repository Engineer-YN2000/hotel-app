package com.example.hotel.domain.model;

import org.seasar.doma.*;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.sql.Time;

/**
 * 予約エンティティ データベースのreservationsテーブルに対応
 */
@Entity(immutable = true)
@Table(name = "reservations")
@Value
@AllArgsConstructor
public class Reservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reservation_id")
  private final Integer reservationId;

  @Column(name = "reserver_id")
  private final Integer reserverId;

  @Column(name = "reserved_at")
  private final LocalDateTime reservedAt;

  @Column(name = "check_in_date")
  private final LocalDate checkInDate;

  @Column(name = "check_out_date")
  private final LocalDate checkOutDate;

  @Column(name = "arrive_at")
  private final Time arriveAt;

  @Column(name = "reservation_status")
  private final Integer reservationStatus;

  @Column(name = "pending_limit_at")
  private final LocalDateTime pendingLimitAt;
}
