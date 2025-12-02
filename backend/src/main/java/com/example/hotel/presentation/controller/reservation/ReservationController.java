package com.example.hotel.presentation.controller.reservation;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.hotel.domain.service.ReservationService;
import com.example.hotel.presentation.dto.common.ApiErrorResponseDto;
import com.example.hotel.presentation.dto.reservation.ReservationRequestDto;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reservations")
@Slf4j
public class ReservationController {

  private final ReservationService reservationService;
  private final MessageSource messageSource;

  public ReservationController(ReservationService reservationService, MessageSource messageSource) {
    this.reservationService = reservationService;
    this.messageSource = messageSource;
  }

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

      Integer reservationId = reservationService.createTentativeReservation(request);

      log.info(messageSource.getMessage("log.reservation.success", new Object[]{reservationId},
          Locale.getDefault()));

      // 成功時: IDを返す
      return ResponseEntity.ok(Map.of("reservationId", reservationId));

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
      return ResponseEntity.internalServerError().body(Map.of("error", "SERVER_ERROR"));
    }
  }
}
