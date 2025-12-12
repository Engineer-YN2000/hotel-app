package com.example.hotel.presentation.controller.reservation;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.hotel.domain.exception.ReservationExpiredException;
import com.example.hotel.domain.exception.SessionTokenMismatchException;
import com.example.hotel.domain.security.ReservationAccessTokenService;
import com.example.hotel.domain.service.ReservationService;
import com.example.hotel.presentation.dto.common.ApiErrorResponseDto;
import com.example.hotel.presentation.dto.reservation.CustomerRequestDto;
import com.example.hotel.presentation.dto.reservation.ReservationRequestDto;
import com.example.hotel.presentation.dto.reservation.ReservationResponseDto;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * 予約管理REST APIコントローラ
 *
 * 【エンドポイント】
 * - POST /api/reservations/pending : 仮予約（在庫ロック）作成
 * - GET /api/reservations/{id}?token={token} : 予約情報取得
 * - POST /api/reservations/{id}/customer-info?token={token} : 顧客情報登録
 * - POST /api/reservations/{id}/cancel?token={token} : 予約キャンセル
 * - POST /api/reservations/{id}/expire?token={token} : 予約期限切れ処理
 * - POST /api/reservations/{id}/confirm?token={token} : 予約確定
 *
 * 【セキュリティ設計】
 * - エラーレスポンスにはrequestフィールドを含めない
 * - 詳細エラー情報はログにのみ出力
 * - HMAC-SHA256トークンによるアクセス制御（ID総当たり攻撃防止）
 */
@RestController
@RequestMapping("/api/reservations")
@Slf4j
public class ReservationController {

  private final ReservationService reservationService;
  private final MessageSource messageSource;
  private final ReservationAccessTokenService accessTokenService;

  public ReservationController(ReservationService reservationService, MessageSource messageSource,
      ReservationAccessTokenService accessTokenService) {
    this.reservationService = reservationService;
    this.messageSource = messageSource;
    this.accessTokenService = accessTokenService;
  }

  /**
   * 仮予約（在庫ロック）を作成するAPI
   *
   * 【ビジネスロジック検証】
   * 1. チェックイン日の過去日検証
   * 2. チェックアウト日の論理的整合性検証
   *
   * @param request 仮予約リクエストDTO
   * @return 成功時: 予約IDを含むJSONレスポンス
   *         失敗時: エラーレスポンスDTO (422 Unprocessable Entity)
   */
  @PostMapping("/pending")
  public ResponseEntity<?> createPending(@Valid @RequestBody ReservationRequestDto request) {
    try {
      // ビジネスロジック違反の検証
      // 【セキュリティ設計】
      // エラーレスポンスにはrequestフィールドを含めない。
      // 【注意】@Valid によるバリデーション（必須チェック等）は GlobalExceptionHandler で処理

      // 【検証1】チェックイン日の過去日検証
      LocalDate today = LocalDate.now();
      if (request.getCheckInDate().isBefore(today)) {
        log.warn(messageSource.getMessage("log.reservation.violation.checkin.past.date",
            new Object[]{request.getCheckInDate(), today, request}, Locale.getDefault()));
        // 【注意】messageKeyはフロントエンドi18n用キー（frontend/src/i18n/messages/）
        // バックエンドのmessages.propertiesではない
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.date.checkInPastDate", 422, "/api/reservations/pending");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      // 【検証2】チェックアウト日の論理的整合性検証
      if (request.getCheckOutDate().isBefore(request.getCheckInDate())
          || request.getCheckOutDate().isEqual(request.getCheckInDate())) {
        log.warn(messageSource.getMessage("log.reservation.violation.checkout.before.checkin",
            new Object[]{request.getCheckInDate(), request.getCheckOutDate(), request},
            Locale.getDefault()));
        // 【注意】messageKeyはフロントエンドi18n用キー（frontend/src/i18n/messages/）
        // バックエンドのmessages.propertiesではない
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.date.checkOutBeforeCheckIn", 422, "/api/reservations/pending");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      log.info(messageSource.getMessage("log.reservation.request.received", new Object[]{request},
          Locale.getDefault()));

      var result = reservationService.createTentativeReservation(request);

      log.debug(messageSource.getMessage("log.reservation.success",
          new Object[]{result.reservationId()}, Locale.getDefault()));

      // アクセストークンを生成
      String accessToken = accessTokenService.generateToken(result.reservationId());

      // 成功時: ID、アクセストークン、セッショントークンを返す
      return ResponseEntity.ok(Map.of("reservationId", result.reservationId(), "accessToken",
          accessToken, "sessionToken", result.sessionToken()));
    }
    catch (IllegalStateException e) {
      // 在庫不足エラー (No Stock)
      log.warn(messageSource.getMessage("log.reservation.violation.stock.shortage",
          new Object[]{e.getMessage(), request}, Locale.getDefault()));

      // 【注意】messageKeyはフロントエンドi18n用キー（frontend/src/i18n/messages/）
      // バックエンドのmessages.propertiesではない
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto
          .create("validation.api.businessRuleViolation", 422, "/api/reservations/pending");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);

    }
    catch (IllegalArgumentException e) {
      // その他ビジネスロジックエラー
      log.warn(messageSource.getMessage("log.reservation.violation.general",
          new Object[]{e.getMessage(), request}, Locale.getDefault()));
      // 【注意】messageKeyはフロントエンドi18n用キー（frontend/src/i18n/messages/）
      // バックエンドのmessages.propertiesではない
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto
          .create("validation.api.businessRuleViolation", 422, "/api/reservations/pending");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);

    }
    catch (Exception e) {
      // サーバーエラー
      log.error(
          messageSource.getMessage("log.unexpected.error.reservation", null, Locale.getDefault()),
          e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 予約情報を取得するAPI
   *
   * 指定された予約IDに紐づく予約情報（ホテル名、部屋タイプ、
   * 宿泊日程、合計料金等）を取得する。
   *
   * 【セキュリティ】
   * - アクセストークンによる認可チェック必須
   * - トークン不正時は404を返却（ID存在有無を隠蔽）
   *
   * 【エラーハンドリング設計】
   * - 予約が存在しない場合: 404 Not Found
   * - サーバーエラー: 500 Internal Server Error
   *
   * @param id 予約ID
   * @param token アクセストークン（HMAC-SHA256署名）
   * @return 成功時: 予約情報レスポンスDTO (200 OK)
   *         予約未存在またはトークン不正: 404 Not Found
   *         エラー時: 500 Internal Server Error
   */
  @GetMapping("/{id}")
  public ResponseEntity<ReservationResponseDto> getReservation(@PathVariable Integer id,
      @RequestParam String token) {
    // トークン検証（不正時は404で存在有無を隠蔽）
    if (!accessTokenService.validateToken(id, token)) {
      log.debug("Invalid access token for reservation: id={}", id);
      return ResponseEntity.notFound().build();
    }

    try {
      ReservationResponseDto response = reservationService.getReservation(id);
      return ResponseEntity.ok(response);
    }
    catch (IllegalArgumentException e) {
      // 予約が見つからない場合 → 404 Not Found
      log.warn(messageSource.getMessage("log.reservation.notfound",
          new Object[]{id, e.getMessage()}, Locale.getDefault()));
      return ResponseEntity.notFound().build();
    }
    catch (Exception e) {
      // サーバーエラー → 500 Internal Server Error
      log.error(
          messageSource.getMessage("log.unexpected.error.reservation", null, Locale.getDefault()),
          e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** 電話番号の正規表現パターン（国際電話対応: +付き数字列） */
  private static final String PHONE_NUMBER_PATTERN = "^(\\+)?[0-9]+$";

  /** Eメールアドレスの正規表現パターン */
  private static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

  /**
   * 仮予約に顧客情報を登録・更新（アップサート）するAPI
   *
   * 【セキュリティ】
   * - アクセストークンによる認可チェック必須
   * - セッショントークンによる同時操作防止
   * - トークン不正時は404を返却（ID存在有無を隠蔽）
   *
   * 【ビジネスロジック検証】
   * 1. 電話番号のフォーマット検証（入力がある場合）
   * 2. Eメールアドレスのフォーマット検証（入力がある場合）
   *
   * @param id 予約ID
   * @param token アクセストークン（HMAC-SHA256署名）
   * @param sessionToken セッショントークン（10分間有効、同時操作防止用）
   * @param request 顧客情報リクエストDTO
   * @return 成功時: 200 OK
   *         トークン不正: 404 Not Found
   *         セッショントークン不一致: 409 Conflict
   *         バリデーションエラー時: 422 Unprocessable Entity
   *         サーバーエラー時: 500 Internal Server Error
   */
  @PostMapping("/{id}/customer-info")
  public ResponseEntity<?> upsertCustomerInfo(@PathVariable Integer id, @RequestParam String token,
      @RequestParam String sessionToken, @Valid @RequestBody CustomerRequestDto request) {
    // トークン検証（不正時は404で存在有無を隠蔽）
    if (!accessTokenService.validateToken(id, token)) {
      log.warn("Invalid access token for customer-info: id={}", id);
      return ResponseEntity.notFound().build();
    }

    try {
      // セッショントークン検証（同時操作防止）
      reservationService.validateSessionToken(id, sessionToken);

      // 【検証1】電話番号のフォーマット検証（入力がある場合のみ）
      if (!isBlank(request.getPhoneNumber())
          && !request.getPhoneNumber().matches(PHONE_NUMBER_PATTERN)) {
        log.warn(messageSource.getMessage("log.customer.violation.phone.format",
            new Object[]{request.getPhoneNumber(), id}, Locale.getDefault()));
        // 【注意】messageKeyはフロントエンドi18n用キー（frontend/src/i18n/messages/）
        // バックエンドのmessages.propertiesではない
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create(
            "validation.customer.phoneNumber.format", 422,
            "/api/reservations/" + id + "/customer-info");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      // 【検証2】Eメールアドレスのフォーマット検証（入力がある場合のみ）
      if (!isBlank(request.getEmailAddress())
          && !request.getEmailAddress().matches(EMAIL_PATTERN)) {
        log.warn(messageSource.getMessage("log.customer.violation.email.format",
            new Object[]{request.getEmailAddress(), id}, Locale.getDefault()));
        // 【注意】messageKeyはフロントエンドi18n用キー（frontend/src/i18n/messages/）
        // バックエンドのmessages.propertiesではない
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create(
            "validation.customer.emailAddress.format", 422,
            "/api/reservations/" + id + "/customer-info");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      log.info(messageSource.getMessage("log.customer.request.received", new Object[]{id, request},
          Locale.getDefault()));

      reservationService.upsertCustomerInfo(id, request);

      log.info(
          messageSource.getMessage("log.customer.success", new Object[]{id}, Locale.getDefault()));

      // 成功時: 200 OK
      return ResponseEntity.ok().build();

    }
    catch (SessionTokenMismatchException e) {
      // セッショントークン不一致（別タブ/端末からの同時操作）
      log.warn("Session token mismatch for customer-info: id={}, message={}", id, e.getMessage());
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create("SESSION_TOKEN_MISMATCH", 409,
          "/api/reservations/" + id + "/customer-info");
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    catch (ReservationExpiredException e) {
      // 仮予約期限切れ → P-910（SessionExpiredError）へ遷移させる
      log.warn(messageSource.getMessage("log.reservation.violation.expired",
          new Object[]{e.getReservationId(), e.getMessage()}, Locale.getDefault()));
      // フロントエンドで期限切れを識別できるようにerrorコードを返却
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create("RESERVATION_EXPIRED", 410,
          "/api/reservations/" + id + "/customer-info");
      return ResponseEntity.status(HttpStatus.GONE).body(errorResponse);
    }
    catch (IllegalStateException e) {
      // 更新失敗（期限切れ以外のビジネスエラー）
      log.warn(messageSource.getMessage("log.customer.violation.state",
          new Object[]{e.getMessage(), id}, Locale.getDefault()));
      return ResponseEntity.internalServerError().build();
    }
    catch (Exception e) {
      // サーバーエラー
      log.error(
          messageSource.getMessage("log.unexpected.error.reservation", null, Locale.getDefault()),
          e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 文字列が空かどうかを判定する
   *
   * @param value 検査対象の文字列
   * @return nullまたは空文字列の場合true
   */
  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * 仮予約をキャンセルするAPI
   *
   * P-020（予約詳細入力）のキャンセルボタン押下時に呼び出されます。
   * 以下の条件を満たす場合のみステータス30（CANCELLED）に更新します:
   * - 予約ステータスがTENTATIVE（10）
   * - pending_limit_atが現在時刻以降（まだ有効）
   *
   * 条件を満たさない場合でも200 OKを返します（ベストエフォート処理）。
   * バッチ処理が最終的な整合性を保証するため、フロントエンドでのエラーハンドリングは不要です。
   *
   * 【セキュリティ】
   * - アクセストークンによる認可チェック必須
   * - セッショントークンによる同時操作防止
   * - トークン不正時は404を返却（ID存在有無を隠蔽）
   *
   * @param id 予約ID
   * @param token アクセストークン（HMAC-SHA256署名）
   * @param sessionToken セッショントークン（10分間有効、同時操作防止用）
   * @return 成功時: 200 OK
   *         トークン不正: 404 Not Found
   *         セッショントークン不一致: 409 Conflict
   *         エラー時: 500 Internal Server Error
   */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<?> cancelReservation(@PathVariable Integer id, @RequestParam String token,
      @RequestParam String sessionToken) {
    // トークン検証（不正時は404で存在有無を隠蔽）
    if (!accessTokenService.validateToken(id, token)) {
      log.debug("Invalid access token for cancel: id={}", id);
      return ResponseEntity.notFound().build();
    }

    try {
      // セッショントークン検証（同時操作防止）
      reservationService.validateSessionToken(id, sessionToken);

      log.debug("Cancel reservation request received: id={}", id);
      int updated = reservationService.cancelReservation(id);
      log.debug("Cancel reservation completed: id={}, updated={}", id, updated);
      return ResponseEntity.ok().build();
    }
    catch (SessionTokenMismatchException e) {
      // セッショントークン不一致（別タブ/端末からの同時操作）
      log.warn("Session token mismatch for cancel: id={}, message={}", id, e.getMessage());
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create("SESSION_TOKEN_MISMATCH", 409,
          "/api/reservations/" + id + "/cancel");
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    catch (Exception e) {
      // サーバーエラー
      log.error("Unexpected error during reservation cancel: id={}", id, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 仮予約を期限切れ（EXPIRED）ステータスに更新するAPI
   *
   * P-910（予約有効時間切れ画面）からトップページに戻る際に呼び出されます。
   * 以下の条件を満たす場合のみステータス40（EXPIRED）に更新します:
   * - 予約ステータスがTENTATIVE（10）
   * - pending_limit_atが現在時刻以前（タイムアウト済み）
   *
   * 条件を満たさない場合でも200 OKを返します（ベストエフォート処理）。
   * バッチ処理が最終的な整合性を保証するため、フロントエンドでのエラーハンドリングは不要です。
   *
   * 【セキュリティ】
   * - アクセストークンによる認可チェック必須
   * - トークン不正時は404を返却（ID存在有無を隠蔽）
   *
   * @param id 予約ID
   * @param token アクセストークン（HMAC-SHA256署名）
   * @return 成功時: 200 OK
   *         トークン不正: 404 Not Found
   *         エラー時: 500 Internal Server Error
   */
  @PostMapping("/{id}/expire")
  public ResponseEntity<?> expireReservation(@PathVariable Integer id, @RequestParam String token) {
    // トークン検証（不正時は404で存在有無を隠蔽）
    if (!accessTokenService.validateToken(id, token)) {
      log.debug("Invalid access token for expire: id={}", id);
      return ResponseEntity.notFound().build();
    }

    try {
      log.debug("Expire reservation request received: id={}", id);
      int updated = reservationService.expireReservation(id);
      log.debug("Expire reservation completed: id={}, updated={}", id, updated);
      return ResponseEntity.ok().build();
    }
    catch (Exception e) {
      // サーバーエラー
      log.error("Unexpected error during reservation expire: id={}", id, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 仮予約を確定（CONFIRMED）ステータスに更新するAPI
   *
   * P-030（予約確認）の確定ボタン押下時に呼び出されます。
   * 以下の条件を満たす場合のみステータス20（CONFIRMED）に更新します:
   * - 予約ステータスがTENTATIVE（10）
   * - pending_limit_atが現在時刻以降（まだ有効）
   *
   * 【セキュリティ】
   * - アクセストークンによる認可チェック必須
   * - セッショントークンによる同時操作防止
   * - トークン不正時は404を返却（ID存在有無を隠蔽）
   *
   * @param id 予約ID
   * @param token アクセストークン（HMAC-SHA256署名）
   * @param sessionToken セッショントークン（10分間有効、同時操作防止用）
   * @return 成功時: 200 OK
   *         トークン不正: 404 Not Found
   *         セッショントークン不一致: 409 Conflict
   *         期限切れ時: 410 Gone
   *         エラー時: 500 Internal Server Error
   */
  @PostMapping("/{id}/confirm")
  public ResponseEntity<?> confirmReservation(@PathVariable Integer id, @RequestParam String token,
      @RequestParam String sessionToken) {
    // トークン検証（不正時は404で存在有無を隠蔽）
    if (!accessTokenService.validateToken(id, token)) {
      log.debug("Invalid access token for confirm: id={}", id);
      return ResponseEntity.notFound().build();
    }

    try {
      // セッショントークン検証（同時操作防止）
      reservationService.validateSessionToken(id, sessionToken);

      log.debug("Confirm reservation request received: id={}", id);
      reservationService.confirmReservation(id);
      log.debug("Confirm reservation completed: id={}", id);
      return ResponseEntity.ok().build();
    }
    catch (SessionTokenMismatchException e) {
      // セッショントークン不一致（別タブ/端末からの同時操作）
      log.warn("Session token mismatch for confirm: id={}, message={}", id, e.getMessage());
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create("SESSION_TOKEN_MISMATCH", 409,
          "/api/reservations/" + id + "/confirm");
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    catch (ReservationExpiredException e) {
      // 仮予約期限切れ → P-910（SessionExpiredError）へ遷移させる
      log.warn(messageSource.getMessage("log.reservation.violation.expired",
          new Object[]{e.getReservationId(), e.getMessage()}, Locale.getDefault()));
      // フロントエンドで期限切れを識別できるようにerrorコードを返却
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create("RESERVATION_EXPIRED", 410,
          "/api/reservations/" + id + "/confirm");
      return ResponseEntity.status(HttpStatus.GONE).body(errorResponse);
    }
    catch (IllegalStateException e) {
      // 更新失敗（期限切れ以外のビジネスエラー）
      log.warn(messageSource.getMessage("log.reservation.violation.state",
          new Object[]{e.getMessage(), id}, Locale.getDefault()));
      return ResponseEntity.internalServerError().build();
    }
    catch (Exception e) {
      log.error("Unexpected error during reservation confirm: id={}", id, e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
