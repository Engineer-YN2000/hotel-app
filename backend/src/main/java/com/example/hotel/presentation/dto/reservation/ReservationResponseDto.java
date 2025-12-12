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

  /**
   * チェックイン予定時刻（HH:mm:ss形式）
   * reservationsテーブルのarrive_atカラムに対応。
   * 顧客情報登録時に設定され、未登録時はnull。
   */
  private String arriveAt;

  /** 予約者情報（reserversテーブル由来、未登録時はnull） */
  private CustomerInfoDto customerInfo;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class RoomDetailDto {
    private Integer roomTypeId;
    private String roomTypeName;
    private Integer roomCapacity;
    private Integer roomCount;
  }

  /**
   * 予約者情報DTO
   * reserversテーブルのデータを格納。
   * 予約者未登録時はReservationResponseDto.customerInfoがnullとなる。
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class CustomerInfoDto {
    private String reserverFirstName;
    private String reserverLastName;
    private String phoneNumber;
    private String emailAddress;
  }
}
