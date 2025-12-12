package com.example.hotel.domain.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 予約アクセストークン生成・検証サービス
 *
 * HMAC-SHA256を使用して、予約IDから推測不可能なアクセストークンを生成します。
 * これにより、認証なしのフローでもIDの総当たり攻撃を防止できます。
 *
 * 【セキュリティ設計】
 * - トークン = Base64URL(HMAC-SHA256(secretKey, reservationId))
 * - シークレットキーは環境変数または設定ファイルで管理
 * - トークンなしまたは不正なトークンでのアクセスは拒否
 *
 * 【使用箇所】
 * - POST /api/reservations/pending → トークン生成して返却
 * - GET /api/reservations/{id} → トークン検証
 * - POST /api/reservations/{id}/customer-info → トークン検証
 * - POST /api/reservations/{id}/confirm → トークン検証
 * - POST /api/reservations/{id}/cancel → トークン検証
 * - POST /api/reservations/{id}/expire → トークン検証
 */
@Component
@PropertySource(value = "classpath:runtime-config.properties", ignoreResourceNotFound = true)
@Slf4j
public class ReservationAccessTokenService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final MessageSource messageSource;

  /**
   * コンストラクタ
   *
   * @param messageSource メッセージソース
   */
  public ReservationAccessTokenService(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * HMACシークレットキー
   *
   * プロパティキー reservation.access-token.secret で設定（必須）。
   * 環境変数またはプロパティファイルで設定可能。
   * 未設定の場合はアプリケーション起動時にエラーとなる。
   */
  @Value("${reservation.access-token.secret}")
  private String secretKey;

  private SecretKeySpec secretKeySpec;

  /**
   * 初期化処理
   *
   * シークレットキーからHMAC用のキースペックを生成します。
   */
  @PostConstruct
  public void init() {
    if (secretKey == null || secretKey.isBlank()) {
      throw new IllegalStateException(
          messageSource.getMessage("error.access.token.secret.notconfigured", null, null));
    }
    this.secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),
        HMAC_ALGORITHM);
  }

  /**
   * 予約IDからアクセストークンを生成します。
   *
   * @param reservationId 予約ID
   * @return Base64URL エンコードされたアクセストークン
   * @throws IllegalStateException HMAC計算に失敗した場合
   */
  public String generateToken(Integer reservationId) {
    if (reservationId == null) {
      throw new IllegalArgumentException("reservationId must not be null");
    }

    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(secretKeySpec);
      byte[] hmacBytes = mac.doFinal(reservationId.toString().getBytes(StandardCharsets.UTF_8));
      // Base64URLエンコード（パディングなし、URL安全）
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }
    catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Failed to generate access token", e);
    }
  }

  /**
   * アクセストークンを検証します。
   *
   * @param reservationId 予約ID
   * @param token 検証対象のアクセストークン
   * @return トークンが有効な場合true、無効な場合false
   */
  public boolean validateToken(Integer reservationId, String token) {
    if (reservationId == null || token == null || token.isBlank()) {
      return false;
    }

    String expectedToken = generateToken(reservationId);
    // タイミング攻撃対策：固定時間比較
    return constantTimeEquals(expectedToken, token);
  }

  /**
   * 固定時間での文字列比較（タイミング攻撃対策）
   *
   * @param expected 期待値
   * @param actual 実際の値
   * @return 一致する場合true
   */
  private boolean constantTimeEquals(String expected, String actual) {
    if (expected.length() != actual.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < expected.length(); i++) {
      result |= expected.charAt(i) ^ actual.charAt(i);
    }
    return result == 0;
  }
}
