package com.example.hotel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Spring Boot標準のエラーハンドリングをカスタマイズ 404、405などのHTTPレベルエラーを適切に処理
 */
@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

  // HTTPステータスコード定数
  private static final int HTTP_STATUS_NOT_FOUND = 404;
  private static final int HTTP_STATUS_METHOD_NOT_ALLOWED = 405;
  private static final int HTTP_STATUS_UNPROCESSABLE_ENTITY = 422;
  private static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

  @RequestMapping("/error")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
    // 【セキュリティ設計】
    // エラーレスポンスにはURIやリクエスト詳細を含めず、最小限の情報のみ提供
    // 詳細なエラー情報はサーバーログに記録し、攻撃者による情報収集を防止
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

    if (status != null) {
      int statusCode;
      try {
        statusCode = Integer.parseInt(status.toString());
      }
      catch (NumberFormatException e) {
        log.warn("無効なステータスコード形式: {}", status);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorCode",
            "INVALID_STATUS_CODE", "status", HTTP_STATUS_INTERNAL_SERVER_ERROR));
      }

      // APIエンドポイントへのリクエストの場合、JSON形式でエラーレスポンスを返す
      String requestURI = uri != null ? uri.toString() : "unknown";

      switch (statusCode) {
        case HTTP_STATUS_NOT_FOUND :
          log.warn("リソースが見つかりません: {}", requestURI);
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(Map.of("errorCode", "RESOURCE_NOT_FOUND", "status", HTTP_STATUS_NOT_FOUND));

        case HTTP_STATUS_METHOD_NOT_ALLOWED :
          log.warn("許可されていないHTTPメソッド: {}", requestURI);
          return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
              Map.of("errorCode", "METHOD_NOT_ALLOWED", "status", HTTP_STATUS_METHOD_NOT_ALLOWED));

        case HTTP_STATUS_UNPROCESSABLE_ENTITY :
          log.warn("ビジネスルール違反: {}", requestURI);
          return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(Map.of("errorCode",
              "BUSINESS_RULE_VIOLATION", "status", HTTP_STATUS_UNPROCESSABLE_ENTITY));

        case HTTP_STATUS_INTERNAL_SERVER_ERROR :
          log.error("内部サーバーエラー: {}", requestURI);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorCode",
              "INTERNAL_SERVER_ERROR", "status", HTTP_STATUS_INTERNAL_SERVER_ERROR));

        default :
          log.warn("予期しないHTTPエラー: {} for {}", statusCode, requestURI);
          return ResponseEntity.status(statusCode)
              .body(Map.of("errorCode", "HTTP_ERROR", "status", statusCode));
      }
    }

    // ステータスコードが取得できない場合の汎用エラー
    log.error("ステータスコード不明のエラーが発生しました");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("errorCode", "UNKNOWN_ERROR", "status", HTTP_STATUS_INTERNAL_SERVER_ERROR));
  }
}
