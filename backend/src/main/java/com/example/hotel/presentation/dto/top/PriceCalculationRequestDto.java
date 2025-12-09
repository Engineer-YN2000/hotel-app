package com.example.hotel.presentation.dto.top;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 価格再計算リクエストDTO
 *
 * チェックイン日・チェックアウト日が変更された際に、
 * 選択中の部屋の価格を再計算するために使用する。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationRequestDto {

  @NotNull(message = "{validation.checkInDate.required}")
  private LocalDate checkInDate;

  @NotNull(message = "{validation.checkOutDate.required}")
  private LocalDate checkOutDate;

  @NotEmpty(message = "{validation.rooms.required}")
  @Valid
  private List<RoomRequest> rooms;

  /**
   * 部屋情報リクエスト（内部クラス）
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoomRequest {
    @NotNull(message = "{validation.roomTypeId.required}")
    private Integer roomTypeId;

    @NotNull(message = "{validation.hotelId.required}")
    private Integer hotelId;
  }
}
