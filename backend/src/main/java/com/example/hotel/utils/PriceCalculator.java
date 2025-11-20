package com.example.hotel.utils;

import java.util.Random;

/**
 * 価格計算ユーティリティクラス 部屋タイプの価格を効率的に生成します
 */
public class PriceCalculator {

  private static final Random RANDOM = new Random();
  private static final int BASE_PRICE = 10000;

  /**
   * 部屋の定員に基づいて価格を計算します
   *
   * @param capacity
   *          部屋の定員
   * @return 計算された価格
   */
  public static Integer calculatePrice(Integer capacity) {
    if (capacity == null || capacity <= 0) {
      return BASE_PRICE;
    }

    int variationAmount = RANDOM.nextInt(15) * 1000;
    return (BASE_PRICE + variationAmount) * capacity;
  }
}
