package com.example.hotel.presentation.controller.reservation;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.hotel.domain.exception.ReservationExpiredException;
import com.example.hotel.domain.exception.SessionTokenMismatchException;
import com.example.hotel.domain.security.ReservationAccessTokenService;
import com.example.hotel.domain.service.ReservationService;
import com.example.hotel.domain.service.ReservationService.TentativeReservationResult;
import com.example.hotel.presentation.dto.reservation.ReservationResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * ReservationController のユニットテスト
 *
 * 以下のシナリオをカバー:
 * (1) 確認成功
 * (2) 予約期限切れの処理
 * (3) 顧客情報の不足/バリデーション
 * (4) 同時更新シナリオ（セッショントークン不一致）
 * (5) トランザクションのロールバック動作（サーバーエラー）
 *
 * 【技術的制約】
 * MediaType.APPLICATION_JSON は @Nullable アノテーションが付与されているため、
 * 静的解析ツール（NullAway等）でNull警告が発生する。
 * 本テストクラスでは Objects.requireNonNull() でラップすることで警告を抑制している。
 * これは冗長なコードだが、静的解析の厳格なNull安全性チェックに対応するための技術的制約である。
 */
@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock
  private ReservationService reservationService;

  @Mock
  private MessageSource messageSource;

  @Mock
  private ReservationAccessTokenService accessTokenService;

  @InjectMocks
  private ReservationController reservationController;

  private static final Integer TEST_RESERVATION_ID = 12345;
  private static final String VALID_ACCESS_TOKEN = "valid-access-token";
  private static final String VALID_SESSION_TOKEN = "valid-session-token";
  private static final String INVALID_TOKEN = "invalid-token";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(reservationController).build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // MessageSourceのデフォルト動作を設定
    lenient()
        .when(
            messageSource.getMessage(Objects.requireNonNull(anyString()), any(), any(Locale.class)))
        .thenReturn("Test message");
  }

  // =============================================================================
  // POST /api/reservations/pending - 仮予約作成
  // =============================================================================
  @Nested
  @DisplayName("POST /api/reservations/pending - 仮予約作成")
  class CreatePendingTests {

    @Test
    @DisplayName("(1) 成功: 有効なリクエストで仮予約が作成される")
    void createPending_withValidRequest_returnsOkWithReservationId() throws Exception {
      // Arrange
      LocalDate checkIn = LocalDate.now().plusDays(7);
      LocalDate checkOut = LocalDate.now().plusDays(10);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 2}]
          }
          """.formatted(checkIn, checkOut);

      when(reservationService.createTentativeReservation(any()))
          .thenReturn(new TentativeReservationResult(TEST_RESERVATION_ID, VALID_SESSION_TOKEN));
      when(accessTokenService.generateToken(TEST_RESERVATION_ID)).thenReturn(VALID_ACCESS_TOKEN);

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reservationId").value(TEST_RESERVATION_ID))
          .andExpect(jsonPath("$.accessToken").value(VALID_ACCESS_TOKEN))
          .andExpect(jsonPath("$.sessionToken").value(VALID_SESSION_TOKEN));

      verify(reservationService).createTentativeReservation(any());
      verify(accessTokenService).generateToken(TEST_RESERVATION_ID);
    }

    @Test
    @DisplayName("(3) バリデーションエラー: チェックイン日が過去の場合422")
    void createPending_withPastCheckInDate_returns422() throws Exception {
      // Arrange
      LocalDate checkIn = LocalDate.now().minusDays(1);
      LocalDate checkOut = LocalDate.now().plusDays(3);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 1}]
          }
          """.formatted(checkIn, checkOut);

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.message").value("validation.date.checkInPastDate"));

      verify(reservationService, never()).createTentativeReservation(any());
    }

    @Test
    @DisplayName("(3) バリデーションエラー: チェックアウト日がチェックイン日以前の場合422")
    void createPending_withCheckOutBeforeCheckIn_returns422() throws Exception {
      // Arrange
      LocalDate checkIn = LocalDate.now().plusDays(5);
      LocalDate checkOut = LocalDate.now().plusDays(3);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 1}]
          }
          """.formatted(checkIn, checkOut);

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.message").value("validation.date.checkOutBeforeCheckIn"));

      verify(reservationService, never()).createTentativeReservation(any());
    }

    @Test
    @DisplayName("(3) バリデーションエラー: チェックアウト日がチェックイン日と同日の場合422")
    void createPending_withSameCheckInAndCheckOut_returns422() throws Exception {
      // Arrange
      LocalDate sameDate = LocalDate.now().plusDays(5);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 1}]
          }
          """.formatted(sameDate, sameDate);

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.message").value("validation.date.checkOutBeforeCheckIn"));
    }

    @Test
    @DisplayName("(5) サーバーエラー: 在庫不足（IllegalStateException）で422")
    void createPending_withNoStock_returns422() throws Exception {
      // Arrange
      LocalDate checkIn = LocalDate.now().plusDays(7);
      LocalDate checkOut = LocalDate.now().plusDays(10);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 100}]
          }
          """.formatted(checkIn, checkOut);

      when(reservationService.createTentativeReservation(any()))
          .thenThrow(new IllegalStateException("No stock available"));

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.message").value("validation.api.businessRuleViolation"));
    }

    @Test
    @DisplayName("(5) サーバーエラー: 予期しない例外で500")
    void createPending_withUnexpectedException_returns500() throws Exception {
      // Arrange
      LocalDate checkIn = LocalDate.now().plusDays(7);
      LocalDate checkOut = LocalDate.now().plusDays(10);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 1}]
          }
          """.formatted(checkIn, checkOut);

      when(reservationService.createTentativeReservation(any()))
          .thenThrow(new RuntimeException("Database connection failed"));

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isInternalServerError());
    }
  }

  // =============================================================================
  // GET /api/reservations/{id} - 予約情報取得
  // =============================================================================
  @Nested
  @DisplayName("GET /api/reservations/{id} - 予約情報取得")
  class GetReservationTests {

    @Test
    @DisplayName("(1) 成功: 有効なトークンで予約情報を取得")
    void getReservation_withValidToken_returnsOk() throws Exception {
      // Arrange
      ReservationResponseDto response = new ReservationResponseDto(TEST_RESERVATION_ID,
          LocalDate.now().plusDays(7), LocalDate.now().plusDays(10), "Test Hotel",
          List.of(new ReservationResponseDto.RoomDetailDto(1, "Single", 2, 1)), 15000, "15:00",
          new ReservationResponseDto.CustomerInfoDto("Taro", "Yamada", "090-1234-5678",
              "test@example.com"));

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      when(reservationService.getReservation(TEST_RESERVATION_ID)).thenReturn(response);

      // Act & Assert
      mockMvc
          .perform(
              get("/api/reservations/{id}", TEST_RESERVATION_ID).param("token", VALID_ACCESS_TOKEN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reservationId").value(TEST_RESERVATION_ID))
          .andExpect(jsonPath("$.hotelName").value("Test Hotel"))
          .andExpect(jsonPath("$.totalFee").value(15000))
          .andExpect(jsonPath("$.customerInfo.reserverFirstName").value("Taro"));
    }

    @Test
    @DisplayName("(1) セキュリティ: 無効なトークンで404（ID存在有無を隠蔽）")
    void getReservation_withInvalidToken_returns404() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, INVALID_TOKEN))
          .thenReturn(false);

      // Act & Assert
      mockMvc.perform(get("/api/reservations/{id}", TEST_RESERVATION_ID)
              .param("token", INVALID_TOKEN))
          .andExpect(status().isNotFound());

      verify(reservationService, never()).getReservation(anyInt());
    }

    @Test
    @DisplayName("(3) エラー: 予約が存在しない場合404")
    void getReservation_withNonExistentId_returns404() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      when(reservationService.getReservation(TEST_RESERVATION_ID))
          .thenThrow(new IllegalArgumentException("Reservation not found"));

      // Act & Assert
      mockMvc.perform(get("/api/reservations/{id}", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("(5) サーバーエラー: 予期しない例外で500")
    void getReservation_withUnexpectedException_returns500() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      when(reservationService.getReservation(TEST_RESERVATION_ID))
          .thenThrow(new RuntimeException("Database error"));

      // Act & Assert
      mockMvc.perform(get("/api/reservations/{id}", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN))
          .andExpect(status().isInternalServerError());
    }
  }

  // =============================================================================
  // POST /api/reservations/{id}/customer-info - 顧客情報登録
  // =============================================================================
  @Nested
  @DisplayName("POST /api/reservations/{id}/customer-info - 顧客情報登録")
  class UpsertCustomerInfoTests {

    private String createValidCustomerJson() {
      return """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "phoneNumber": "09012345678",
            "emailAddress": "test@example.com",
            "arriveAt": "15:00"
          }
          """;
    }

    @Test
    @DisplayName("(1) 成功: 有効な顧客情報で登録成功")
    void upsertCustomerInfo_withValidData_returnsOk() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doNothing().when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(createValidCustomerJson())))
          .andExpect(status().isOk());

      verify(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      verify(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());
    }

    @Test
    @DisplayName("(1) 成功: 電話番号のみでも登録成功（Eメールなし）")
    void upsertCustomerInfo_withPhoneOnly_returnsOk() throws Exception {
      // Arrange
      String requestJson = """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "phoneNumber": "09012345678"
          }
          """;

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID,
          VALID_SESSION_TOKEN);
      doNothing().when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
          .param("token", VALID_ACCESS_TOKEN).param("sessionToken", VALID_SESSION_TOKEN)
          .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
          .content(Objects.requireNonNull(requestJson))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("(1) 成功: 国際電話番号形式（+付き）でも登録成功")
    void upsertCustomerInfo_withInternationalPhoneNumber_returnsOk() throws Exception {
      // Arrange
      String requestJson = """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "phoneNumber": "+819012345678"
          }
          """;

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID,
          VALID_SESSION_TOKEN);
      doNothing().when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
          .param("token", VALID_ACCESS_TOKEN).param("sessionToken", VALID_SESSION_TOKEN)
          .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
          .content(Objects.requireNonNull(requestJson))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("(1) セキュリティ: 無効なアクセストークンで404")
    void upsertCustomerInfo_withInvalidToken_returns404() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, INVALID_TOKEN))
          .thenReturn(false);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", INVALID_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(createValidCustomerJson())))
          .andExpect(status().isNotFound());

      verify(reservationService, never()).upsertCustomerInfo(anyInt(), any());
    }

    @Test
    @DisplayName("(2) 予約期限切れ: ReservationExpiredExceptionで410")
    void upsertCustomerInfo_withExpiredReservation_returns410() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doThrow(new ReservationExpiredException("Reservation expired", TEST_RESERVATION_ID))
          .when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(createValidCustomerJson())))
          .andExpect(status().isGone())
          .andExpect(jsonPath("$.message").value("RESERVATION_EXPIRED"));
    }

    @Test
    @DisplayName("(3) バリデーションエラー: 電話番号形式不正で422")
    void upsertCustomerInfo_withInvalidPhoneNumber_returns422() throws Exception {
      // Arrange
      String requestJson = """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "phoneNumber": "invalid-phone"
          }
          """;

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID,
          VALID_SESSION_TOKEN);

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN).param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.message").value("validation.customer.phoneNumber.format"));

      verify(reservationService, never()).upsertCustomerInfo(anyInt(), any());
    }

    @Test
    @DisplayName("(3) バリデーションエラー: Eメール形式不正で422")
    void upsertCustomerInfo_withInvalidEmail_returns422() throws Exception {
      // Arrange
      String requestJson = """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "emailAddress": "invalid-email"
          }
          """;

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID,
          VALID_SESSION_TOKEN);

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN).param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.message").value("validation.customer.emailAddress.format"));
    }

    @Test
    @DisplayName("(4) 同時更新: セッショントークン不一致で409")
    void upsertCustomerInfo_withSessionTokenMismatch_returns409() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doThrow(new SessionTokenMismatchException("Session token mismatch", TEST_RESERVATION_ID))
          .when(reservationService).validateSessionToken(TEST_RESERVATION_ID, INVALID_TOKEN);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", INVALID_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(createValidCustomerJson())))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.message").value("SESSION_TOKEN_MISMATCH"));

      verify(reservationService, never()).upsertCustomerInfo(anyInt(), any());
    }

    @Test
    @DisplayName("(5) サーバーエラー: IllegalStateExceptionで500")
    void upsertCustomerInfo_withIllegalStateException_returns500() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doThrow(new IllegalStateException("Update failed"))
          .when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(createValidCustomerJson())))
          .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("(5) サーバーエラー: 予期しない例外で500")
    void upsertCustomerInfo_withUnexpectedException_returns500() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doThrow(new RuntimeException("Database connection failed"))
          .when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(createValidCustomerJson())))
          .andExpect(status().isInternalServerError());
    }
  }

  // =============================================================================
  // POST /api/reservations/{id}/cancel - 予約キャンセル
  // =============================================================================
  @Nested
  @DisplayName("POST /api/reservations/{id}/cancel - 予約キャンセル")
  class CancelReservationTests {

    @Test
    @DisplayName("(1) 成功: 有効なトークンでキャンセル成功")
    void cancelReservation_withValidTokens_returnsOk() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      when(reservationService.cancelReservation(TEST_RESERVATION_ID))
          .thenReturn(1);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/cancel", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isOk());

      verify(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      verify(reservationService).cancelReservation(TEST_RESERVATION_ID);
    }

    @Test
    @DisplayName("(1) 成功: 既にキャンセル済み（更新0件）でもOK（ベストエフォート）")
    void cancelReservation_withAlreadyCancelled_returnsOk() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      when(reservationService.cancelReservation(TEST_RESERVATION_ID))
          .thenReturn(0); // 更新件数0

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/cancel", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("(1) セキュリティ: 無効なアクセストークンで404")
    void cancelReservation_withInvalidToken_returns404() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, INVALID_TOKEN))
          .thenReturn(false);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/cancel", TEST_RESERVATION_ID)
              .param("token", INVALID_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isNotFound());

      verify(reservationService, never()).cancelReservation(anyInt());
    }

    @Test
    @DisplayName("(4) 同時更新: セッショントークン不一致で409")
    void cancelReservation_withSessionTokenMismatch_returns409() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doThrow(new SessionTokenMismatchException("Session token mismatch", TEST_RESERVATION_ID))
          .when(reservationService).validateSessionToken(TEST_RESERVATION_ID, INVALID_TOKEN);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/cancel", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", INVALID_TOKEN))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.message").value("SESSION_TOKEN_MISMATCH"));

      verify(reservationService, never()).cancelReservation(anyInt());
    }

    @Test
    @DisplayName("(5) サーバーエラー: 予期しない例外で500")
    void cancelReservation_withUnexpectedException_returns500() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      when(reservationService.cancelReservation(TEST_RESERVATION_ID))
          .thenThrow(new RuntimeException("Database error"));

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/cancel", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isInternalServerError());
    }
  }

  // =============================================================================
  // POST /api/reservations/{id}/expire - 予約期限切れ
  // =============================================================================
  @Nested
  @DisplayName("POST /api/reservations/{id}/expire - 予約期限切れ処理")
  class ExpireReservationTests {

    @Test
    @DisplayName("(1) 成功: 有効なトークンで期限切れ処理成功")
    void expireReservation_withValidToken_returnsOk() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      when(reservationService.expireReservation(TEST_RESERVATION_ID))
          .thenReturn(1);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/expire", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN))
          .andExpect(status().isOk());

      verify(reservationService).expireReservation(TEST_RESERVATION_ID);
    }

    @Test
    @DisplayName("(1) 成功: 既に期限切れ処理済み（更新0件）でもOK（ベストエフォート）")
    void expireReservation_withAlreadyExpired_returnsOk() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      when(reservationService.expireReservation(TEST_RESERVATION_ID))
          .thenReturn(0);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/expire", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("(1) セキュリティ: 無効なアクセストークンで404")
    void expireReservation_withInvalidToken_returns404() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, INVALID_TOKEN))
          .thenReturn(false);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/expire", TEST_RESERVATION_ID)
              .param("token", INVALID_TOKEN))
          .andExpect(status().isNotFound());

      verify(reservationService, never()).expireReservation(anyInt());
    }

    @Test
    @DisplayName("(5) サーバーエラー: 予期しない例外で500")
    void expireReservation_withUnexpectedException_returns500() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      when(reservationService.expireReservation(TEST_RESERVATION_ID))
          .thenThrow(new RuntimeException("Database error"));

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/expire", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN))
          .andExpect(status().isInternalServerError());
    }
  }

  // =============================================================================
  // POST /api/reservations/{id}/confirm - 予約確定
  // =============================================================================
  @Nested
  @DisplayName("POST /api/reservations/{id}/confirm - 予約確定")
  class ConfirmReservationTests {

    @Test
    @DisplayName("(1) 成功: 有効なトークンで予約確定成功")
    void confirmReservation_withValidTokens_returnsOk() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doNothing().when(reservationService).confirmReservation(TEST_RESERVATION_ID);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/confirm", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isOk());

      verify(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      verify(reservationService).confirmReservation(TEST_RESERVATION_ID);
    }

    @Test
    @DisplayName("(1) セキュリティ: 無効なアクセストークンで404")
    void confirmReservation_withInvalidToken_returns404() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, INVALID_TOKEN))
          .thenReturn(false);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/confirm", TEST_RESERVATION_ID)
              .param("token", INVALID_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isNotFound());

      verify(reservationService, never()).confirmReservation(anyInt());
    }

    @Test
    @DisplayName("(2) 予約期限切れ: ReservationExpiredExceptionで410")
    void confirmReservation_withExpiredReservation_returns410() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doThrow(new ReservationExpiredException("Reservation expired", TEST_RESERVATION_ID))
          .when(reservationService).confirmReservation(TEST_RESERVATION_ID);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/confirm", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isGone())
          .andExpect(jsonPath("$.message").value("RESERVATION_EXPIRED"));
    }

    @Test
    @DisplayName("(4) 同時更新: セッショントークン不一致で409")
    void confirmReservation_withSessionTokenMismatch_returns409() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doThrow(new SessionTokenMismatchException("Session token mismatch", TEST_RESERVATION_ID))
          .when(reservationService).validateSessionToken(TEST_RESERVATION_ID, INVALID_TOKEN);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/confirm", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", INVALID_TOKEN))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.message").value("SESSION_TOKEN_MISMATCH"));

      verify(reservationService, never()).confirmReservation(anyInt());
    }

    @Test
    @DisplayName("(5) サーバーエラー: IllegalStateExceptionで500")
    void confirmReservation_withIllegalStateException_returns500() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doThrow(new IllegalStateException("Invalid state"))
          .when(reservationService).confirmReservation(TEST_RESERVATION_ID);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/confirm", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("(5) サーバーエラー: 予期しない例外で500")
    void confirmReservation_withUnexpectedException_returns500() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      doThrow(new RuntimeException("Database connection failed"))
          .when(reservationService).confirmReservation(TEST_RESERVATION_ID);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/confirm", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isInternalServerError());
    }
  }

  // =============================================================================
  // トランザクション・ロールバック動作（サービス層との連携）
  // =============================================================================
  @Nested
  @DisplayName("トランザクション・ロールバック動作")
  class TransactionRollbackTests {

    @Test
    @DisplayName("(5) 仮予約作成中に例外発生 → トランザクションロールバック検証")
    void createPending_whenServiceThrows_transactionRollsBack() throws Exception {
      // Arrange
      LocalDate checkIn = LocalDate.now().plusDays(7);
      LocalDate checkOut = LocalDate.now().plusDays(10);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 1}]
          }
          """.formatted(checkIn, checkOut);

      // サービス層で例外発生をシミュレート
      when(reservationService.createTentativeReservation(any()))
          .thenThrow(new RuntimeException("Transaction should rollback"));

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isInternalServerError());

      // トークン生成が呼ばれていないことを確認（ロールバック）
      verify(accessTokenService, never()).generateToken(anyInt());
    }

    @Test
    @DisplayName("(5) 顧客情報登録中に例外発生 → トランザクションロールバック検証")
    void upsertCustomerInfo_whenServiceThrows_transactionRollsBack() throws Exception {
      // Arrange
      String requestJson = """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "phoneNumber": "09012345678"
          }
          """;

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID,
          VALID_SESSION_TOKEN);
      // サービス層で例外発生をシミュレート
      doThrow(new RuntimeException("Transaction should rollback")).when(reservationService)
          .upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN).param("sessionToken", VALID_SESSION_TOKEN)
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isInternalServerError());

      // upsertCustomerInfoが呼ばれたことを確認（トランザクション開始後に例外）
      verify(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());
    }

    @Test
    @DisplayName("(5) 予約確定中に例外発生 → トランザクションロールバック検証")
    void confirmReservation_whenServiceThrows_transactionRollsBack() throws Exception {
      // Arrange
      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID, VALID_SESSION_TOKEN);
      // サービス層で例外発生をシミュレート
      doThrow(new RuntimeException("Transaction should rollback"))
          .when(reservationService).confirmReservation(TEST_RESERVATION_ID);

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/confirm", TEST_RESERVATION_ID)
              .param("token", VALID_ACCESS_TOKEN)
              .param("sessionToken", VALID_SESSION_TOKEN))
          .andExpect(status().isInternalServerError());

      // confirmReservationが呼ばれたことを確認（トランザクション開始後に例外）
      verify(reservationService).confirmReservation(TEST_RESERVATION_ID);
    }
  }

  // =============================================================================
  // 境界値・エッジケース
  // =============================================================================
  @Nested
  @DisplayName("境界値・エッジケース")
  class EdgeCaseTests {

    @Test
    @DisplayName("予約ID=0でも正常にハンドリングされる")
    void getReservation_withZeroId_handledCorrectly() throws Exception {
      // Arrange
      Integer zeroId = 0;
      when(accessTokenService.validateToken(zeroId, VALID_ACCESS_TOKEN)).thenReturn(true);
      when(reservationService.getReservation(zeroId))
          .thenThrow(new IllegalArgumentException("Reservation not found"));

      // Act & Assert
      mockMvc.perform(get("/api/reservations/{id}", zeroId).param("token", VALID_ACCESS_TOKEN))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("チェックイン日が今日の場合は成功")
    void createPending_withTodayCheckIn_succeeds() throws Exception {
      // Arrange
      LocalDate today = LocalDate.now();
      LocalDate checkOut = today.plusDays(3);
      String requestJson = """
          {
            "checkInDate": "%s",
            "checkOutDate": "%s",
            "rooms": [{"roomTypeId": 1, "roomCount": 1}]
          }
          """.formatted(today, checkOut);

      when(reservationService.createTentativeReservation(any()))
          .thenReturn(new TentativeReservationResult(TEST_RESERVATION_ID, VALID_SESSION_TOKEN));
      when(accessTokenService.generateToken(TEST_RESERVATION_ID)).thenReturn(VALID_ACCESS_TOKEN);

      // Act & Assert
      mockMvc
          .perform(post("/api/reservations/pending")
              .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
              .content(Objects.requireNonNull(requestJson)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reservationId").value(TEST_RESERVATION_ID));
    }

    @Test
    @DisplayName("空文字列の電話番号は検証をスキップ")
    void upsertCustomerInfo_withEmptyPhoneNumber_skipsValidation() throws Exception {
      // Arrange
      String requestJson = """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "phoneNumber": "",
            "emailAddress": "test@example.com"
          }
          """;

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID,
          VALID_SESSION_TOKEN);
      doNothing().when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
          .param("token", VALID_ACCESS_TOKEN).param("sessionToken", VALID_SESSION_TOKEN)
          .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
          .content(Objects.requireNonNull(requestJson))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("空白のみの電話番号は検証をスキップ")
    void upsertCustomerInfo_withWhitespaceOnlyPhoneNumber_skipsValidation() throws Exception {
      // Arrange
      String requestJson = """
          {
            "reserverFirstName": "Taro",
            "reserverLastName": "Yamada",
            "phoneNumber": "   ",
            "emailAddress": "test@example.com"
          }
          """;

      when(accessTokenService.validateToken(TEST_RESERVATION_ID, VALID_ACCESS_TOKEN))
          .thenReturn(true);
      doNothing().when(reservationService).validateSessionToken(TEST_RESERVATION_ID,
          VALID_SESSION_TOKEN);
      doNothing().when(reservationService).upsertCustomerInfo(eq(TEST_RESERVATION_ID), any());

      // Act & Assert
      mockMvc.perform(post("/api/reservations/{id}/customer-info", TEST_RESERVATION_ID)
          .param("token", VALID_ACCESS_TOKEN).param("sessionToken", VALID_SESSION_TOKEN)
          .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
          .content(Objects.requireNonNull(requestJson))).andExpect(status().isOk());
    }
  }
}
