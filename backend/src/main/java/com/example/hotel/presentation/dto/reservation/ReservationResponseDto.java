package com.example.hotel.presentation.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponseDto {
  private Integer reservationId;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private String hotelName;
  private List<RoomDetailDto> rooms;
  private Integer totalFee;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class RoomDetailDto {
    private Integer roomTypeId;
    private String roomTypeName;
    private Integer roomCapacity;
    private Integer roomCount;
  }
}
