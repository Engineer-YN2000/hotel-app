package com.example.hotel.utils;

import java.time.LocalDate;

/**
 * 価格計算ユーティリティクラス ホテルごとの料金体系とその日の需要定数を考慮したダイナミックプライシングを実装
 */
public class PriceCalculator {

  // 価格設定定数
  private static final int BASE_PRICE_PER_PERSON = 8000; // 一人当たりの基準価格（円）
  private static final double[] CAPACITY_MULTIPLIERS = {1.0, // 1人: 基準価格
      0.9, // 2人: 10%割引
      0.85, // 3人: 15%割引
      0.8, // 4人: 20%割引
      0.75 // 5人以上: 25%割引
  };

  // ダイナミックプライシング定数
  private static final int HOTEL_PRICE_VARIATION_COUNT = 5; // ホテル価格バリエーション数
  private static final double HOTEL_PRICE_VARIATION_STEP = 0.1; // ホテル価格変動幅（10%刻み）
  private static final double HOTEL_PRICE_BASE_OFFSET = 0.2; // ホテル価格基準オフセット（±20%範囲）
  private static final int DEMAND_VARIATION_CYCLE = 20; // 需要変動サイクル（日）
  private static final double DEMAND_VARIATION_STEP = 0.01; // 需要変動幅（1%刻み）
  private static final double DEMAND_BASE_FACTOR = 0.9; // 需要基準係数（90%から開始）

  // 注意: ThreadLocalは使用後に必ずクリアが必要（メモリリーク防止）
  // 本番環境ではSpringのリクエストスコープ使用を推奨
  private static final ThreadLocal<LocalDate> REQUEST_DATE = new ThreadLocal<>();

  /**
   * リクエストスコープの日付を設定します
   */
  public static void setRequestDate(LocalDate date) {
    REQUEST_DATE.set(date);
  }

  /**
   * リクエストスコープの日付をクリアします
   */
  public static void clearRequestDate() {
    REQUEST_DATE.remove();
  }

  /**
   * 部屋の定員に基づいて価格を計算します（後方互換性のため） リクエストスコープの日付が設定されている場合はそれを使用、されていない場合は現在日を使用
   *
   * @param capacity
   *          部屋の定員
   * @return 計算された価格（部屋全体）
   */
  public static Integer calculatePrice(Integer capacity) {
    LocalDate date = REQUEST_DATE.get();
    if (date == null) {
      date = LocalDate.now();
    }
    return calculatePrice(capacity, 1L, date);
  }

  /**
   * ホテルIDと日付を考慮したダイナミックプライシング価格計算 同じホテルでは同じ料金体系を使用し、その日の需要定数で価格を調整
   *
   * @param capacity
   *          部屋の定員
   * @param hotelId
   *          ホテルID（料金体系の決定に使用）
   * @param date
   *          宿泊予定日（需要定数の決定に使用）
   * @return 計算された価格（部屋全体）
   */
  public static Integer calculatePrice(Integer capacity, Long hotelId, LocalDate date) {
    if (capacity == null || capacity <= 0) {
      return BASE_PRICE_PER_PERSON;
    }

    // ホテルごとの料金体系係数（ホテルIDに基づく一貫した値）
    double hotelPriceMultiplier = 1.0
        + ((hotelId % HOTEL_PRICE_VARIATION_COUNT) * HOTEL_PRICE_VARIATION_STEP
            - HOTEL_PRICE_BASE_OFFSET);

    // capacityに基づく基本価格の計算
    double capacityMultiplier = capacity <= CAPACITY_MULTIPLIERS.length
        ? CAPACITY_MULTIPLIERS[capacity - 1]
        : CAPACITY_MULTIPLIERS[CAPACITY_MULTIPLIERS.length - 1];

    int basePrice = (int) (BASE_PRICE_PER_PERSON * capacityMultiplier * capacity
        * hotelPriceMultiplier);

    // その日の需要定数（日付に基づく一貫した値）
    int dayOfYear = date.getDayOfYear();
    double demandFactor = DEMAND_BASE_FACTOR
        + ((dayOfYear % DEMAND_VARIATION_CYCLE) * DEMAND_VARIATION_STEP);

    // 最終価格の計算
    int finalPrice = (int) (basePrice * demandFactor);

    return Math.max(finalPrice, BASE_PRICE_PER_PERSON); // 最低価格を保証
  }
}
