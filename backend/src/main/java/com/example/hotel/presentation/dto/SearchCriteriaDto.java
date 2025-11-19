package com.example.hotel.presentation.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/**
 * 空室検索条件入力DTO。チェックイン/アウト日・都道府県・人数などの検索パラメータを保持する。
 */
@Data
public class SearchCriteriaDto {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkInDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkOutDate;

  private Integer prefectureId;

  private Integer guestCount;

  // 後方互換性のためareaIdも保持（当面は使用しない）
  private Integer areaId;
}
