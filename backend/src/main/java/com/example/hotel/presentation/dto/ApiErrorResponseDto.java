package com.example.hotel.presentation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * APIエラーレスポンス専用DTO 成功時のレスポンスDTOとエラー時のレスポンスを明確に分離し、 REST APIの設計原則に従った統一的なエラーレスポンス構造を提供します。 【設計原則】 -
 * 成功時とエラー時のレスポンス構造を完全に分離 - 国際化対応：messageには翻訳キーを格納 - セキュリティ考慮：詳細なエラー情報は含めない - 標準化：一貫したエラーレスポンス形式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponseDto {

  /**
   * エラーの種別を示す固定値 フロントエンド側でエラーレスポンスの識別に使用
   */
  private String error = "validation_error";

  /**
   * 国際化対応のエラーメッセージキー フロントエンドのi18nシステムで翻訳される 例: "validation.date.checkInRequired"
   */
  private String message;

  /**
   * エラー発生時刻（トレーサビリティのため） ISO-8601形式で出力される
   */
  private LocalDateTime timestamp;

  /**
   * リクエストパス（デバッグ支援のため） セキュリティ上、パラメータ情報は含めない
   */
  private String path;

  /**
   * HTTPステータスコード
   */
  private Integer status;

  /**
   * エラーレスポンス作成のファクトリメソッド
   *
   * @param message
   *          国際化対応のメッセージキー
   * @param status
   *          HTTPステータスコード
   * @param path
   *          リクエストパス
   * @return エラーレスポンスDTO
   */
  public static ApiErrorResponseDto create(String message, Integer status, String path) {
    return new ApiErrorResponseDto("validation_error", message, LocalDateTime.now(), path, status);
  }

  /**
   * エラーレスポンス作成の簡易ファクトリメソッド（パスなし）
   *
   * @param message
   *          国際化対応のメッセージキー
   * @param status
   *          HTTPステータスコード
   * @return エラーレスポンスDTO
   */
  public static ApiErrorResponseDto create(String message, Integer status) {
    return create(message, status, null);
  }
}
