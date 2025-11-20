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

  @RequestMapping("/error")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

    if (status != null) {
      int statusCode = Integer.parseInt(status.toString());

      // APIエンドポイントへのリクエストの場合、JSON形式でエラーレスポンスを返す
      String requestURI = uri != null ? uri.toString() : "unknown";

      switch (statusCode) {
        case 404 :
          log.warn("リソースが見つかりません: {}", requestURI);
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(Map.of("errorCode", "RESOURCE_NOT_FOUND", "status", 404));

        case 405 :
          log.warn("許可されていないHTTPメソッド: {}", requestURI);
          return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
              .body(Map.of("errorCode", "METHOD_NOT_ALLOWED", "status", 405));

        case 422 :
          log.warn("ビジネスルール違反: {}", requestURI);
          return ResponseEntity.status(422)
              .body(Map.of("errorCode", "BUSINESS_RULE_VIOLATION", "status", 422));

        case 500 :
          log.error("内部サーバーエラー: {}", requestURI);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("errorCode", "INTERNAL_SERVER_ERROR", "status", 500));

        default :
          log.warn("予期しないHTTPエラー: {} for {}", statusCode, requestURI);
          return ResponseEntity.status(statusCode)
              .body(Map.of("errorCode", "HTTP_ERROR", "status", statusCode));
      }
    }

    // ステータスコードが取得できない場合の汎用エラー
    log.error("ステータスコード不明のエラーが発生しました");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("errorCode", "UNKNOWN_ERROR", "status", 500));
  }
}
