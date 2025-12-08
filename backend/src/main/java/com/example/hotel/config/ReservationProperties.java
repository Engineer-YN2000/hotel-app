package com.example.hotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 仮予約関連の設定プロパティ
 *
 * reservation.propertiesの「reservation.tentative.*」プレフィックスの設定値をバインド
 */
@Component
@ConfigurationProperties(prefix = "reservation.tentative")
@PropertySource("classpath:reservation.properties")
@Getter
@Setter
public class ReservationProperties {

  /**
   * 仮予約の有効期限（分）
   *
   * 仮予約作成後、この時間内に本予約に移行しない場合は自動キャンセル対象となる。
   */
  private int expiryMinutes;
}
