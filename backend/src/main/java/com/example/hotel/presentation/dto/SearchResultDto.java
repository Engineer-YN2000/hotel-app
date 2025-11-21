package com.example.hotel.presentation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 空室検索結果のルートDTO。ホテル毎の利用可能な部屋タイプと残在庫/簡易価格を保持する。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

  private List<HotelResultDto> hotels;
  private SearchCriteriaDto criteria;
  private String errorMessage; // エラー時のメッセージ（正常時はnull）

  /**
   * 空結果（ホテル一覧なし）を生成するファクトリ。
   */
  public static SearchResultDto createEmptyResult() {
    return new SearchResultDto(Collections.emptyList(), null, null);
  }
}
