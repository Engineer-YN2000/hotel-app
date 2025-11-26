package com.example.hotel.domain.constants;

/**
 * 予約ステータス定数 データベースで使用される予約ステータス値を定義
 */
public final class ReservationStatus {

  // コンストラクタを private にして instantiation を防ぐ
  private ReservationStatus() {
  }

  /** 予約なし（空室） */
  public static final int AVAILABLE = 0;

  /** 仮予約（一時的な予約） */
  public static final int TENTATIVE = 10;

  /** 確定予約 */
  public static final int CONFIRMED = 20;

  /** キャンセル */
  public static final int CANCELLED = 30;

  /**
   * 予約済みとしてカウントする予約ステータスのリスト 空室検索では「仮予約」と「確定予約」を予約済みとして除外する （つまり、AVAILABLE のみが検索対象となる空室）
   */
  public static final java.util.List<Integer> RESERVED_STATUSES = java.util.Arrays.asList(TENTATIVE,
      CONFIRMED);
}
