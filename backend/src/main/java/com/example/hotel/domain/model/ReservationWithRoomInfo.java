package com.example.hotel.domain.model;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import lombok.Data;

import java.time.LocalDate;

@Entity(immutable = false)
@Data
public class ReservationWithRoomInfo {
  @Column(name = "reservation_id")
  private Integer reservationId;
  @Column(name = "check_in_date")
  private LocalDate checkInDate;
  @Column(name = "check_out_date")
  private LocalDate checkOutDate;

  @Column(name = "hotel_name")
  private String hotelName;

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
}
