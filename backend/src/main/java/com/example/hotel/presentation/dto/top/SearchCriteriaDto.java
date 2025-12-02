package com.example.hotel.presentation.dto.top;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/**
 * 空室検索条件入力DTO
 *
 * チェックイン/アウト日・都道府県・人数などの検索パラメータを保持する。
 * TopPageControllerの検索APIで使用される。
 */
@Data
public class SearchCriteriaDto {

  @NotNull(message = "{validation.checkin.date.required}")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkInDate;

  @NotNull(message = "{validation.checkout.date.required}")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkOutDate;

  @NotNull(message = "{validation.prefecture.id.required}")
  @Min(value = 1, message = "{validation.prefecture.id.range}")
  @Max(value = 47, message = "{validation.prefecture.id.range}")
  private Integer prefectureId;

  @NotNull(message = "{validation.guest.count.required}")
  @Min(value = 1, message = "{validation.guest.count.range}")
  @Max(value = 99, message = "{validation.guest.count.range}")
  private Integer guestCount;
}
