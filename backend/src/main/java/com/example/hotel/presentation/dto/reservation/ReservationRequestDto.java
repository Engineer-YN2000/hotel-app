package com.example.hotel.presentation.dto.reservation;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
  @NotNull(message = "{validation.checkin.date.required}")
  private LocalDate checkInDate;

  /**
   * チェックアウト日（宿泊終了日）
   * ISO-8601形式で受信。チェックイン日との論理整合性をコントローラーで検証。
   */
  @NotNull(message = "{validation.checkout.date.required}")
  private LocalDate checkOutDate;

  /**
   * 予約対象部屋リスト
   * 1ホテル内の複数部屋タイプ・室数・価格を保持。
   */
  @NotEmpty(message = "{validation.rooms.required}")
  @Valid
  private List<RoomRequest> rooms;

  /**
   * 部屋リクエスト情報
   *
   * 1部屋タイプごとの予約情報（部屋タイプID・室数）を保持。
   * ・roomTypeId: 部屋タイプ識別子
   * ・roomCount: 予約室数
   *
   * 【セキュリティ設計】
   * 価格(price)はフロントエンドから受け取らず、バックエンドで再計算する。
   * これにより、クライアント側での価格改竄攻撃を防止。
   *
   * 設計方針: DTOのため不変性・シリアライズ性を重視。
   */
  @Data
  public static class RoomRequest {
    /** 部屋タイプID（DBのroom_typesテーブルと対応） */
    @NotNull(message = "{validation.room.type.id.required}")
    @Positive(message = "{validation.room.type.id.positive}")
    private Integer roomTypeId;

    /** 予約室数（1以上） */
    @NotNull(message = "{validation.room.count.required}")
    @Positive(message = "{validation.room.count.positive}")
    private Integer roomCount;
  }
}
