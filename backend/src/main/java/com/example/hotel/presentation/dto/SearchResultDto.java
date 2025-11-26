package com.example.hotel.presentation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 空室検索結果のルーDTO（成功レスポンス専用）
 *
 * 成功時の検索結果のみを表現し、エラー情報は含まない。
 * REST APIの設計原則に従い、成功とエラーのレスポンス構造を明確に分離。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

  /**
   * 検索結果のホテルリスト
   *
   * 空リストの場合は検索条件に合致するホテルが存在しないことを示す
   */
  private List<HotelResultDto> hotels;

  /**
   * 検索条件（リクエスト内容の確認用）
   *
   * フロントエンドでの検索条件表示や再検索に使用
   */
  private SearchCriteriaDto criteria;

  /**
   * 空結果（ホテル一覧なし）を生成するファクトリメソッド
   *
   * 検索条件に合致するホテルが存在しない場合に使用
   *
   * @return 空のホテルリストを持つ検索結果
   */
  public static SearchResultDto createEmptyResult() {
    return new SearchResultDto(Collections.emptyList(), null);
  }

  /**
   * 検索結果作成のファクトリメソッド
   *
   * @param hotels
   *          検索結果のホテルリスト
   * @param criteria
   *          検索条件
   * @return 検索結果DTO
   */
  public static SearchResultDto create(List<HotelResultDto> hotels, SearchCriteriaDto criteria) {
    return new SearchResultDto(hotels, criteria);
  }
}
