package com.example.hotel.utils;

import java.time.LocalDate;

/**
 * 価格計算ユーティリティクラス
 * <p>
 * ホテルごとの料金体系とその日の需要定数を考慮したダイナミックプライシングを実装
 * <h3>【重要】価格計算の責務範囲:</h3>
 * <ul>
 * <li>このクラスは「1泊あたりの部屋料金」のみを計算</li>
 * <li>宿泊日数による乗算は呼び出し側（DTOやサービス層）で実行</li>
 * <li>複数泊の総額計算は RoomTypeResultDto.calculateTotalPrice() で実装</li>
 * </ul>
 */
public class PriceCalculator {

  /**
   * ユーティリティクラスのため、インスタンス化を防ぐprivateコンストラクタ
   */
  private PriceCalculator() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

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

  /**
   * 部屋の定員に基づいて価格を計算します（後方互換性のため） デフォルトでhotelId=1、現在日を使用
   *
   * @param capacity
   *          部屋の定員
   * @return 計算された価格（1泊あたりの部屋全体料金・円）
   */
  public static Integer calculatePrice(Integer capacity) {
    return calculatePrice(capacity, 1, LocalDate.now());
  }

  /**
   * ホテルIDと日付を考慮したダイナミックプライシング価格計算
   * <p>
   * 同じホテルでは同じ料金体系を使用し、その日の需要定数で価格を調整
   * <h4>【価格計算仕様】</h4>
   * <ul>
   * <li>戻り値: 1泊あたりの部屋全体料金（宿泊日数の乗算は呼び出し側で実行）</li>
   * <li>基準価格: BASE_PRICE_PER_PERSON × 定員数 × 定員割引率</li>
   * <li>変動要素: ホテル料金係数 × その日の需要係数</li>
   * </ul>
   * <h4>【引数制約】</h4>
   * <ul>
   * <li>hotelId: null禁止。ダイナミックプライシングに必須のホテル識別子</li>
   * <li>date: null禁止。需要ベースの価格調整に必須の日付情報</li>
   * <li>capacity: nullまたは0以下の場合は基準価格を返却</li>
   * </ul>
   *
   * @param capacity
   *          部屋の定員（nullまたは0以下の場合は基準価格を適用）
   * @param hotelId
   *          ホテルID（料金体系の決定に使用、null禁止）
   * @param date
   *          宿泊予定日（需要定数の決定に使用、null禁止）
   * @return 計算された価格（1泊あたりの部屋全体料金・円）
   * @throws IllegalArgumentException
   *           hotelIdまたはdateがnullの場合
   */
  public static Integer calculatePrice(Integer capacity, Integer hotelId, LocalDate date) {
    // 引数の適正性検証（ダイナミックプライシングに必須のパラメータ）
    if (hotelId == null) {
      throw new IllegalArgumentException("ホテルIDはnullであってはなりません。ダイナミックプライシングにはホテル識別子が必要です。");
    }
    if (date == null) {
      throw new IllegalArgumentException("宿泊日はnullであってはなりません。需要ベースの価格調整には日付情報が必要です。");
    }

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
