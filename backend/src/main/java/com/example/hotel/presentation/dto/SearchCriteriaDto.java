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

  // 後方互換性: 旧システムのareaIdフィールド（移行期間中のみ保持）
  // TODO: 都道府県ベース検索への完全移行後に削除予定
  @Deprecated(since = "2025-11", forRemoval = true)
  private Integer areaId;
}
