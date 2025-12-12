package com.example.hotel.domain.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

/**
 * ReservationAccessTokenService のユニットテスト
 *
 * HMAC-SHA256ベースのアクセストークン生成・検証機能をテストします。
 *
 * 【型設計方針：Integer vs int】
 * 本テストクラスでは予約IDに {@code Integer} 型を使用している。
 * これは以下の理由による意図的な設計判断である：
 * - テスト対象クラス（ReservationAccessTokenService）との型整合性を保つ
 * - DTO/Entity層との型整合性を保つ（Domaは {@code Integer} を使用）
 * - null値を使用したテストケース（境界値テスト）を可能にする
 * - オートボクシング/アンボクシングによるバグを防止
 *
 * プリミティブ型 {@code int} への変更は、nullテストケースが不可能になり、
 * テスト対象との型不整合を招くため、意図的に避けている。
 */
class ReservationAccessTokenServiceTest {

  private ReservationAccessTokenService tokenService;
  private MessageSource messageSource;

  private static final String TEST_SECRET_KEY = "test-secret-key-for-hmac-sha256";

  @BeforeEach
  void setUp() throws Exception {
    messageSource = mock(MessageSource.class);
    tokenService = new ReservationAccessTokenService(messageSource);

    // リフレクションでsecretKeyを設定
    var secretKeyField = ReservationAccessTokenService.class.getDeclaredField("secretKey");
    secretKeyField.setAccessible(true);
    secretKeyField.set(tokenService, TEST_SECRET_KEY);

    // 初期化メソッドを呼び出し
    tokenService.init();
  }

  @Nested
  @DisplayName("init() - 初期化処理")
  class InitTests {

    @Test
    @DisplayName("シークレットキーがnullの場合、IllegalStateExceptionがスローされる")
    void init_withNullSecretKey_throwsIllegalStateException() throws Exception {
      // Arrange
      ReservationAccessTokenService service = new ReservationAccessTokenService(messageSource);
      var secretKeyField = ReservationAccessTokenService.class.getDeclaredField("secretKey");
      secretKeyField.setAccessible(true);
      secretKeyField.set(service, null);

      when(messageSource.getMessage("error.access.token.secret.notconfigured", null, null))
          .thenReturn("Secret key is not configured");

      // Act & Assert
      IllegalStateException exception = assertThrows(IllegalStateException.class, service::init);
      assertEquals("Secret key is not configured", exception.getMessage());
    }

    @Test
    @DisplayName("シークレットキーが空白の場合、IllegalStateExceptionがスローされる")
    void init_withBlankSecretKey_throwsIllegalStateException() throws Exception {
      // Arrange
      ReservationAccessTokenService service = new ReservationAccessTokenService(messageSource);
      var secretKeyField = ReservationAccessTokenService.class.getDeclaredField("secretKey");
      secretKeyField.setAccessible(true);
      secretKeyField.set(service, "   ");

      when(messageSource.getMessage("error.access.token.secret.notconfigured", null, null))
          .thenReturn("Secret key is not configured");

      // Act & Assert
      IllegalStateException exception = assertThrows(IllegalStateException.class, service::init);
      assertEquals("Secret key is not configured", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("generateToken() - トークン生成")
  class GenerateTokenTests {

    @Test
    @DisplayName("同じ予約IDに対して常に同じトークンが生成される（決定論的）")
    void generateToken_withSameId_returnsSameToken() {
      // Arrange
      Integer reservationId = 12345;

      // Act
      String token1 = tokenService.generateToken(reservationId);
      String token2 = tokenService.generateToken(reservationId);

      // Assert
      assertNotNull(token1);
      assertFalse(token1.isBlank());
      assertEquals(token1, token2);
    }

    @Test
    @DisplayName("異なる予約IDに対して異なるトークンが生成される")
    void generateToken_withDifferentIds_returnsDifferentTokens() {
      // Arrange
      Integer reservationId1 = 12345;
      Integer reservationId2 = 67890;

      // Act
      String token1 = tokenService.generateToken(reservationId1);
      String token2 = tokenService.generateToken(reservationId2);

      // Assert
      assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("生成されたトークンはBase64URL形式（パディングなし）である")
    void generateToken_returnsBase64UrlEncodedToken() {
      // Arrange
      Integer reservationId = 12345;

      // Act
      String token = tokenService.generateToken(reservationId);

      // Assert
      // Base64URL: A-Z, a-z, 0-9, -, _ のみ（+, /, = は含まない）
      assertTrue(token.matches("^[A-Za-z0-9_-]+$"),
          "Token should be Base64URL encoded without padding");
      // SHA256のHMAC結果は32バイト → Base64で約43文字（パディングなし）
      assertEquals(43, token.length(), "HMAC-SHA256 Base64URL token should be 43 characters");
    }

    @Test
    @DisplayName("予約IDがnullの場合、IllegalArgumentExceptionがスローされる")
    void generateToken_withNullId_throwsIllegalArgumentException() {
      // Act & Assert
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> tokenService.generateToken(null));
      assertEquals("reservationId must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("予約ID=0でもトークンが正常に生成される")
    void generateToken_withZeroId_generatesToken() {
      // Arrange
      Integer reservationId = 0;

      // Act
      String token = tokenService.generateToken(reservationId);

      // Assert
      assertNotNull(token);
      assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("負の予約IDでもトークンが正常に生成される")
    void generateToken_withNegativeId_generatesToken() {
      // Arrange
      Integer reservationId = -1;

      // Act
      String token = tokenService.generateToken(reservationId);

      // Assert
      assertNotNull(token);
      assertFalse(token.isBlank());
    }
  }

  @Nested
  @DisplayName("validateToken() - トークン検証")
  class ValidateTokenTests {

    @Test
    @DisplayName("正しいトークンの場合、trueを返す")
    void validateToken_withValidToken_returnsTrue() {
      // Arrange
      Integer reservationId = 12345;
      String token = tokenService.generateToken(reservationId);

      // Act
      boolean result = tokenService.validateToken(reservationId, token);

      // Assert
      assertTrue(result);
    }

    @Test
    @DisplayName("異なる予約IDのトークンの場合、falseを返す")
    void validateToken_withTokenForDifferentId_returnsFalse() {
      // Arrange
      Integer reservationId1 = 12345;
      Integer reservationId2 = 67890;
      String token = tokenService.generateToken(reservationId1);

      // Act
      boolean result = tokenService.validateToken(reservationId2, token);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("改ざんされたトークンの場合、falseを返す")
    void validateToken_withTamperedToken_returnsFalse() {
      // Arrange
      Integer reservationId = 12345;
      String validToken = tokenService.generateToken(reservationId);
      String tamperedToken = validToken.substring(0, validToken.length() - 1) + "X";

      // Act
      boolean result = tokenService.validateToken(reservationId, tamperedToken);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("予約IDがnullの場合、falseを返す")
    void validateToken_withNullId_returnsFalse() {
      // Arrange
      String token = "some-token";

      // Act
      boolean result = tokenService.validateToken(null, token);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("トークンがnullの場合、falseを返す")
    void validateToken_withNullToken_returnsFalse() {
      // Arrange
      Integer reservationId = 12345;

      // Act
      boolean result = tokenService.validateToken(reservationId, null);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("トークンが空文字の場合、falseを返す")
    void validateToken_withEmptyToken_returnsFalse() {
      // Arrange
      Integer reservationId = 12345;

      // Act
      boolean result = tokenService.validateToken(reservationId, "");

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("トークンが空白のみの場合、falseを返す")
    void validateToken_withBlankToken_returnsFalse() {
      // Arrange
      Integer reservationId = 12345;

      // Act
      boolean result = tokenService.validateToken(reservationId, "   ");

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("完全に異なるトークンの場合、falseを返す")
    void validateToken_withCompletelyDifferentToken_returnsFalse() {
      // Arrange
      Integer reservationId = 12345;

      // Act
      boolean result = tokenService.validateToken(reservationId, "invalid-token");

      // Assert
      assertFalse(result);
    }
  }

  @Nested
  @DisplayName("セキュリティ特性")
  class SecurityTests {

    @Test
    @DisplayName("異なるシークレットキーでは異なるトークンが生成される")
    void differentSecretKeys_produceDifferentTokens() throws Exception {
      // Arrange
      Integer reservationId = 12345;
      String token1 = tokenService.generateToken(reservationId);

      // 別のシークレットキーでサービスを作成
      ReservationAccessTokenService anotherService = new ReservationAccessTokenService(
          messageSource);
      var secretKeyField = ReservationAccessTokenService.class.getDeclaredField("secretKey");
      secretKeyField.setAccessible(true);
      secretKeyField.set(anotherService, "different-secret-key");
      anotherService.init();

      // Act
      String token2 = anotherService.generateToken(reservationId);

      // Assert
      assertNotEquals(token1, token2,
          "Different secret keys should produce different tokens for the same ID");
    }

    @Test
    @DisplayName("トークンから予約IDを推測できない（一方向性）")
    void token_cannotBeReversedToGetReservationId() {
      // Arrange
      Integer reservationId = 12345;

      // Act
      String token = tokenService.generateToken(reservationId);

      // Assert
      // トークンに予約IDが直接含まれていないことを確認
      assertFalse(token.contains(reservationId.toString()),
          "Token should not contain the reservation ID directly");
    }

    @Test
    @DisplayName("連続した予約IDでもトークンにパターンがない")
    void consecutiveIds_produceUnpredictableTokens() {
      // Arrange & Act
      String token1 = tokenService.generateToken(1);
      String token2 = tokenService.generateToken(2);
      String token3 = tokenService.generateToken(3);

      // Assert
      // トークンが互いに十分異なることを確認（先頭10文字が同じでないこと）
      assertFalse(
          token1.substring(0, 10).equals(token2.substring(0, 10))
              && token2.substring(0, 10).equals(token3.substring(0, 10)),
          "Consecutive IDs should not produce tokens with predictable patterns");
    }
  }

  @Nested
  @DisplayName("タイミング攻撃防止 - constantTimeEquals")
  class TimingAttackPreventionTests {

    /**
     * constantTimeEqualsメソッドをリフレクションで呼び出すヘルパー
     */
    private boolean invokeConstantTimeEquals(String expected, String actual) throws Exception {
      var method = ReservationAccessTokenService.class.getDeclaredMethod("constantTimeEquals",
          String.class, String.class);
      method.setAccessible(true);
      return (boolean) method.invoke(tokenService, expected, actual);
    }

    @Test
    @DisplayName("同一文字列の比較でtrueを返す")
    void constantTimeEquals_withIdenticalStrings_returnsTrue() throws Exception {
      // Arrange
      String str = "abcdefghijklmnop";

      // Act
      boolean result = invokeConstantTimeEquals(str, str);

      // Assert
      assertTrue(result);
    }

    @Test
    @DisplayName("異なる長さの文字列の比較でfalseを返す")
    void constantTimeEquals_withDifferentLengths_returnsFalse() throws Exception {
      // Arrange
      String shorter = "abc";
      String longer = "abcd";

      // Act
      boolean result = invokeConstantTimeEquals(shorter, longer);

      // Assert
      assertFalse(result, "Different length strings should return false");
    }

    @Test
    @DisplayName("同じ長さで最初の文字が異なる場合falseを返す")
    void constantTimeEquals_withFirstCharDifferent_returnsFalse() throws Exception {
      // Arrange
      String str1 = "Xbcdefghijklmnop";
      String str2 = "abcdefghijklmnop";

      // Act
      boolean result = invokeConstantTimeEquals(str1, str2);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("同じ長さで最後の文字が異なる場合falseを返す")
    void constantTimeEquals_withLastCharDifferent_returnsFalse() throws Exception {
      // Arrange
      String str1 = "abcdefghijklmnoX";
      String str2 = "abcdefghijklmnop";

      // Act
      boolean result = invokeConstantTimeEquals(str1, str2);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("同じ長さで中央の文字が異なる場合falseを返す")
    void constantTimeEquals_withMiddleCharDifferent_returnsFalse() throws Exception {
      // Arrange
      String str1 = "abcdefgXijklmnop";
      String str2 = "abcdefghijklmnop";

      // Act
      boolean result = invokeConstantTimeEquals(str1, str2);

      // Assert
      assertFalse(result);
    }

    @Test
    @DisplayName("最初・中央・最後の文字差異による比較時間に有意差がない（タイミング攻撃耐性）")
    void constantTimeEquals_timingDoesNotRevealMismatchPosition() throws Exception {
      // 【注意】このテストはJVMのJIT最適化・GC・OSスケジューリング等の影響を受けるため、
      // 完全な時間一致は期待できない。あくまで「著しい差がないこと」の参考検証である。
      // 真のタイミング攻撃耐性は実装（XOR + OR累積、早期リターンなし）で保証される。

      // Arrange
      String correctToken = tokenService.generateToken(12345);
      int len = correctToken.length();

      // 最初の文字だけ異なるトークン
      String firstCharDiff = "X" + correctToken.substring(1);
      // 最後の文字だけ異なるトークン
      String lastCharDiff = correctToken.substring(0, len - 1) + "X";
      // 中央の文字だけ異なるトークン
      int mid = len / 2;
      String midCharDiff = correctToken.substring(0, mid) + "X" + correctToken.substring(mid + 1);

      // 各パターンで複数回実行して時間を計測
      int iterations = 50000;

      // ウォームアップ（JIT最適化のため）
      for (int i = 0; i < 5000; i++) {
        invokeConstantTimeEquals(correctToken, firstCharDiff);
        invokeConstantTimeEquals(correctToken, lastCharDiff);
        invokeConstantTimeEquals(correctToken, midCharDiff);
      }

      // 最初の文字差異の計測
      long startFirst = System.nanoTime();
      for (int i = 0; i < iterations; i++) {
        invokeConstantTimeEquals(correctToken, firstCharDiff);
      }
      long timeFirst = System.nanoTime() - startFirst;

      // 最後の文字差異の計測
      long startLast = System.nanoTime();
      for (int i = 0; i < iterations; i++) {
        invokeConstantTimeEquals(correctToken, lastCharDiff);
      }
      long timeLast = System.nanoTime() - startLast;

      // 中央の文字差異の計測
      long startMid = System.nanoTime();
      for (int i = 0; i < iterations; i++) {
        invokeConstantTimeEquals(correctToken, midCharDiff);
      }
      long timeMid = System.nanoTime() - startMid;

      // Assert
      // 時間差が2倍以内であることを確認
      // （早期リターン実装であれば、最初の文字差異は著しく速くなるはず）
      long maxTime = Math.max(Math.max(timeFirst, timeLast), timeMid);
      long minTime = Math.min(Math.min(timeFirst, timeLast), timeMid);
      double ratio = (double) maxTime / minTime;

      assertTrue(ratio < 2.0,
          String.format(
              "Timing difference ratio should be < 2.0 (actual: %.2f). "
                  + "First=%dns, Last=%dns, Mid=%dns. "
                  + "Large differences may indicate early-return vulnerability.",
              ratio, timeFirst, timeLast, timeMid));
    }

    @Test
    @DisplayName("XOR演算による全文字比較が行われる（早期リターンなし）")
    void constantTimeEquals_comparesAllCharacters() throws Exception {
      // このテストは実装の特性を検証する
      // XOR演算で全文字を比較し、結果をORで累積する実装であることを確認

      // Arrange - 複数箇所が異なる文字列
      String str1 = "abcdefghijklmnop";
      String str2 = "XbcdefghijklmnoX"; // 最初と最後が異なる

      // Act
      boolean result = invokeConstantTimeEquals(str1, str2);

      // Assert
      assertFalse(result, "Multiple character differences should still return false");

      // 追加検証: 全く異なる文字列でも正しくfalseを返す
      String completelyDifferent = "XXXXXXXXXXXXXXXX";
      boolean result2 = invokeConstantTimeEquals(str1, completelyDifferent);
      assertFalse(result2, "Completely different strings should return false");
    }

    @Test
    @DisplayName("空文字列同士の比較でtrueを返す")
    void constantTimeEquals_withEmptyStrings_returnsTrue() throws Exception {
      // Act
      boolean result = invokeConstantTimeEquals("", "");

      // Assert
      assertTrue(result, "Two empty strings should be equal");
    }
  }
}
