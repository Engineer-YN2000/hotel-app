package com.example.hotel.domain.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
 * セッショントークン生成・検証サービス
 *
 * 予約作成時のセッションを識別するための時限トークンを生成・検証します。
 * これにより、異なるタブや端末からの同時操作を検知できます。
 *
 * 【セキュリティ設計】
 * - トークン = Base64URL(timestamp + ":" + HMAC-SHA256(secretKey, reservationId + ":" + timestamp))
 * - タイムスタンプをトークンに埋め込み、10分間の有効期限を検証
 * - ReservationAccessTokenServiceとは異なる秘密鍵を使用
 *
 * 【使用箇所】
 * - POST /api/reservations/pending → トークン生成してDBに保存、レスポンスで返却
 * - POST /api/reservations/{id}/customer-info → トークン検証
 * - POST /api/reservations/{id}/confirm → トークン検証
 * - POST /api/reservations/{id}/cancel → トークン検証
 */
@Component
@PropertySource(value = "classpath:runtime-config.properties", ignoreResourceNotFound = true)
@Slf4j
public class SessionTokenService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  /** セッショントークンの有効期限（秒） - 10分 */
  private static final long TOKEN_VALIDITY_SECONDS = 10 * 60;

  private final MessageSource messageSource;

  public SessionTokenService(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * セッショントークン用HMACシークレットキー
   *
   * プロパティキー reservation.session-token.secret で設定（必須）。
   * ReservationAccessTokenServiceとは異なるキーを使用すること。
   */
  @Value("${reservation.session-token.secret}")
  private String secretKey;

  private SecretKeySpec secretKeySpec;

  /**
   * 初期化処理
   */
  @PostConstruct
  public void init() {
    if (secretKey == null || secretKey.isBlank()) {
      throw new IllegalStateException(
          messageSource.getMessage("error.session.token.secret.notconfigured", null, null));
    }
    this.secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),
        HMAC_ALGORITHM);
  }

  /**
   * 予約IDに対するセッショントークンを生成します。
   *
   * トークン形式: Base64URL(timestamp:HMAC)
   * - timestamp: トークン生成時刻（Unix秒）
   * - HMAC: HMAC-SHA256(secretKey, reservationId:timestamp)
   *
   * @param reservationId 予約ID
   * @return 時限セッショントークン
   * @throws IllegalStateException HMAC計算に失敗した場合
   */
  public String generateToken(Integer reservationId) {
    if (reservationId == null) {
      throw new IllegalArgumentException("reservationId must not be null");
    }

    long timestamp = Instant.now().getEpochSecond();
    String payload = reservationId + ":" + timestamp;

    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(secretKeySpec);
      byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String hmac = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);

      // タイムスタンプとHMACを結合してBase64エンコード
      String tokenData = timestamp + ":" + hmac;
      return Base64.getUrlEncoder().withoutPadding()
          .encodeToString(tokenData.getBytes(StandardCharsets.UTF_8));

    }
    catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Failed to generate session token", e);
    }
  }

  /**
   * セッショントークンを検証します。
   *
   * 検証項目:
   * 1. トークンのフォーマットが正しいこと
   * 2. HMACが正しいこと（改ざんされていないこと）
   * 3. 有効期限内であること（生成から10分以内）
   *
   * @param reservationId 予約ID
   * @param token 検証対象のセッショントークン
   * @return トークンが有効な場合true、無効な場合false
   */
  public boolean validateToken(Integer reservationId, String token) {
    if (reservationId == null || token == null || token.isBlank()) {
      return false;
    }

    try {
      // Base64デコード
      String tokenData = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);

      // タイムスタンプとHMACを分離
      String[] parts = tokenData.split(":", 2);
      if (parts.length != 2) {
        log.debug("Invalid session token format: reservationId={}", reservationId);
        return false;
      }

      long timestamp = Long.parseLong(parts[0]);
      String providedHmac = parts[1];

      // 有効期限チェック（10分）
      long currentTime = Instant.now().getEpochSecond();
      if (currentTime - timestamp > TOKEN_VALIDITY_SECONDS) {
        log.debug("Session token expired: reservationId={}, tokenAge={}s", reservationId,
            currentTime - timestamp);
        return false;
      }

      // HMACを再計算して比較
      String payload = reservationId + ":" + timestamp;
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(secretKeySpec);
      byte[] expectedHmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String expectedHmac = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(expectedHmacBytes);

      // タイミング攻撃対策: 固定時間比較
      return constantTimeEquals(expectedHmac, providedHmac);

    }
    catch (IllegalArgumentException e) {
      // Base64デコードエラー、数値パースエラーなど
      log.debug("Session token validation failed: reservationId={}, error={}", reservationId,
          e.getMessage());
      return false;
    }
    catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Failed to validate session token", e);
    }
  }

  /**
   * 固定時間での文字列比較（タイミング攻撃対策）
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
