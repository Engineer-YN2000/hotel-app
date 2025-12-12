package com.example.hotel.domain.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.example.hotel.domain.constants.ReservationStatus;
import com.example.hotel.domain.repository.ReservationDao;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * セッショントークン生成・検証サービス
 *
 * 予約作成時のセッションを識別するためのトークンを生成・検証します。
 * これにより、異なるタブや端末からの同時操作を即座にブロックできます。
 *
 * 【セキュリティ設計】
 * - トークン = Base64URL(random + ":" + HMAC-SHA256(secretKey, reservationId + ":" + random))
 * - ランダム値をトークンに埋め込み、改ざん防止とユニーク性を確保
 * - DB照合方式により、新しいセッション開始時に古いトークンを即座に無効化
 * - ReservationAccessTokenServiceとは異なる秘密鍵を使用
 *
 * 【DB照合方式の利点】
 * - 即時無効化: 新しいタブ/端末でトークン発行→古いトークンは即座に無効
 * - 単一セッション強制: 同一予約に対して1つのアクティブセッションのみ許可
 * - 明示的な状態管理: DBで有効なトークンを一元管理
 *
 * 【使用箇所】
 * - POST /api/reservations/pending → トークン生成してDBに保存、レスポンスで返却
 * - POST /api/reservations/{id}/customer-info → DB照合によるトークン検証
 * - POST /api/reservations/{id}/confirm → DB照合によるトークン検証
 * - POST /api/reservations/{id}/cancel → DB照合によるトークン検証
 */
@Component
@PropertySource(value = "classpath:runtime-config.properties", ignoreResourceNotFound = true)
@Slf4j
public class SessionTokenService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  /** ランダム値のバイト長 */
  private static final int RANDOM_BYTES_LENGTH = 16;

  private final MessageSource messageSource;
  private final ReservationDao reservationDao;
  private final SecureRandom secureRandom;

  public SessionTokenService(MessageSource messageSource, ReservationDao reservationDao) {
    this.messageSource = messageSource;
    this.reservationDao = reservationDao;
    this.secureRandom = new SecureRandom();
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
   * トークン形式: Base64URL(random:HMAC)
   * - random: セキュアランダム値（Base64エンコード）
   * - HMAC: HMAC-SHA256(secretKey, reservationId:random)
   *
   * 生成されたトークンはDBに保存する必要があります。
   * 新しいトークンを生成すると、古いトークンは自動的に無効になります（DB上書き）。
   *
   * @param reservationId 予約ID
   * @return セッショントークン
   * @throws IllegalStateException HMAC計算に失敗した場合
   */
  public String generateToken(Integer reservationId) {
    if (reservationId == null) {
      throw new IllegalArgumentException("reservationId must not be null");
    }

    // セキュアランダム値を生成
    byte[] randomBytes = new byte[RANDOM_BYTES_LENGTH];
    secureRandom.nextBytes(randomBytes);
    String random = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    String payload = reservationId + ":" + random;

    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(secretKeySpec);
      byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String hmac = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);

      // ランダム値とHMACを結合してBase64エンコード
      String tokenData = random + ":" + hmac;
      return Base64.getUrlEncoder().withoutPadding()
          .encodeToString(tokenData.getBytes(StandardCharsets.UTF_8));

    }
    catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Failed to generate session token", e);
    }
  }

  /**
   * セッショントークンを検証します（DB照合方式）。
   *
   * 検証項目:
   * 1. トークンのフォーマットが正しいこと
   * 2. HMACが正しいこと（改ざんされていないこと）
   * 3. DBに保存されているトークンと一致すること
   *
   * DB照合により、新しいセッションでトークンが発行された場合、
   * 古いトークンは即座に無効となります。
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

      // ランダム値とHMACを分離
      String[] parts = tokenData.split(":", 2);
      if (parts.length != 2) {
        log.debug("Invalid session token format: reservationId={}", reservationId);
        return false;
      }

      String random = parts[0];
      String providedHmac = parts[1];

      // HMACを再計算して比較（改ざん検知）
      String payload = reservationId + ":" + random;
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(secretKeySpec);
      byte[] expectedHmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String expectedHmac = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(expectedHmacBytes);

      // タイミング攻撃対策: 固定時間比較
      if (!constantTimeEquals(expectedHmac, providedHmac)) {
        log.debug("Session token HMAC mismatch: reservationId={}", reservationId);
        return false;
      }

      // DB照合: DBに保存されているトークンと一致するか確認
      // これにより、新しいセッションでトークンが発行された場合、古いトークンは無効となる
      // また、期限切れや確定済みの予約に対してはトークンが取得できない（nullが返る）
      String storedToken = reservationDao.selectSessionToken(reservationId,
          ReservationStatus.TENTATIVE);
      if (storedToken == null) {
        log.debug(
            "No session token found in DB: reservationId={} (not found, expired, or not TENTATIVE)",
            reservationId);
        return false;
      }

      // タイミング攻撃対策: 固定時間比較
      boolean matches = constantTimeEquals(token, storedToken);
      if (!matches) {
        log.debug("Session token does not match DB: reservationId={} (possible concurrent access)",
            reservationId);
      }
      return matches;

    }
    catch (IllegalArgumentException e) {
      // Base64デコードエラーなど
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
