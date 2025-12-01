package com.example.hotel.presentation.dto.reservation;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 予約リクエストDTO
 *
 * フロントエンドから受信する予約リクエストのデータ構造。
 * チェックイン・チェックアウト日、部屋リスト（部屋タイプ・室数・価格）を保持。
 *
 * ・本DTOはAPIのリクエストボディとして利用される。
 * ・バリデーションはコントローラー層で実施。
 * ・部屋リストは1ホテル内の複数部屋タイプに対応。
 *
 * 設計方針: 不変性・シリアライズ性を重視し、Lombokで簡潔に定義。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequestDto {
  /**
   * チェックイン日（宿泊開始日）
   * ISO-8601形式で受信。過去日・未指定はコントローラーで検証。
   */
  private LocalDate checkInDate;

  /**
   * チェックアウト日（宿泊終了日）
   * ISO-8601形式で受信。チェックイン日との論理整合性をコントローラーで検証。
   */
  private LocalDate checkOutDate;

  /**
   * 予約対象部屋リスト
   * 1ホテル内の複数部屋タイプ・室数・価格を保持。
   */
  private List<RoomRequest> rooms;

  /**
   * 部屋リクエスト情報
   *
   * 1部屋タイプごとの予約情報（部屋タイプID・室数・価格）を保持。
   * ・roomTypeId: 部屋タイプ識別子
   * ・roomCount: 予約室数
   * ・price: 1室あたりの価格（税抜/税込はビジネスロジック依存）
   *
   * 設計方針: DTOのため不変性・シリアライズ性を重視。
   */
  @Data
  public static class RoomRequest {
    /** 部屋タイプID（DBのroom_typesテーブルと対応） */
    private Integer roomTypeId;
    /** 予約室数（1以上） */
    private Integer roomCount;
    /** 1室あたりの価格（単位: 円） */
    private Integer price;
  }
}
