package com.example.hotel.presentation.controller.common;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.hotel.presentation.dto.common.ApiErrorResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * グローバル例外ハンドラー
 *
 * Jakarta Bean Validation のエラーを統一されたAPIエラーレスポンス形式に変換する。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Jakarta Bean Validation エラーハンドラー (POST/PUT @RequestBody)
   *
   * @Valid アノテーションによるバリデーションエラーをキャッチし、
   * 統一されたAPIエラーレスポンス形式で返却する。
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponseDto> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    // 最初のエラーメッセージを取得
    String errorMessage = ex.getBindingResult().getFieldErrors().stream().findFirst()
        .map(error -> error.getDefaultMessage()).orElse("validation.api.businessRuleViolation");

    log.warn(messageSource.getMessage("log.reservation.violation.general",
        new Object[]{errorMessage, ex.getBindingResult().getTarget()}, Locale.getDefault()));

    ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create(errorMessage, 422,
        request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
  }

  /**
   * Jakarta Bean Validation エラーハンドラー (GET @ModelAttribute)
   *
   * GETリクエストのクエリパラメータバインディング時の @Valid バリデーションエラーをキャッチし、
   * 統一されたAPIエラーレスポンス形式で返却する。
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiErrorResponseDto> handleBindException(BindException ex,
      HttpServletRequest request) {

    // 最初のエラーメッセージを取得
    String errorMessage = ex.getBindingResult().getFieldErrors().stream().findFirst()
        .map(error -> error.getDefaultMessage()).orElse("validation.api.businessRuleViolation");

    log.warn(messageSource.getMessage("log.reservation.violation.general",
        new Object[]{errorMessage, ex.getBindingResult().getTarget()}, Locale.getDefault()));

    ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create(errorMessage, 422,
        request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
  }
}
