package com.example.hotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 予約関連の設定プロパティ
 *
 * reservation.propertiesの設定値をバインド
 */
@Component
@ConfigurationProperties(prefix = "reservation")
@PropertySource("classpath:reservation.properties")
@Getter
@Setter
public class ReservationProperties {

  /**
   * 仮予約関連の設定
   */
  private Tentative tentative = new Tentative();

  /**
   * デフォルト到着時刻（HH:mm形式）
   *
   * 顧客が到着時刻を指定しなかった場合に適用される。
   */
  private String defaultArrivalTime;

  /**
   * 仮予約関連の設定プロパティ
   */
  @Getter
  @Setter
  public static class Tentative {
    /**
     * 仮予約の有効期限（分）
     *
     * 仮予約作成後、この時間内に本予約に移行しない場合は自動キャンセル対象となる。
     */
    private int expiryMinutes;
  }
}
