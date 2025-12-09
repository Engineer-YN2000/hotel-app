package com.example.hotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.PropertySource;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * PriceCalculator用のプロパティ設定クラス
 *
 * price-calculator.propertiesの「price.*」プレフィックスの設定値をバインド
 */
@Component
@PropertySource("classpath:price-calculator.properties")
@ConfigurationProperties(prefix = "price")
@Getter
@Setter
public class PriceProperties {

  /** 1人あたりの基本料金 */
  private int basePerPerson;

  /** 定員別の料金倍率リスト */
  private List<Double> capacityMultipliers;

  /** ホテル価格の基本倍率 */
  private double hotelPriceBaseMultiplier;

  /** ホテル価格のバリエーション数 */
  private int hotelVariationCount;

  /** ホテル価格のバリエーションステップ */
  private double hotelVariationStep;

  /** ホテル価格の基本オフセット */
  private double hotelBaseOffset;

  /** 需要変動サイクル */
  private int demandVariationCycle;

  /** 需要変動ステップ */
  private double demandVariationStep;

  /** 需要の基本係数 */
  private double demandBaseFactor;
}
