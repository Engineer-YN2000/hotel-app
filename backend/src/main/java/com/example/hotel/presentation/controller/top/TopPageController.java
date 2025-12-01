package com.example.hotel.presentation.controller.top;

import com.example.hotel.domain.service.CacheService;
import com.example.hotel.domain.service.SearchService;
import com.example.hotel.domain.repository.AreaDetailDao;
import com.example.hotel.domain.model.AreaDetail;
import com.example.hotel.presentation.dto.common.ApiErrorResponseDto;
import com.example.hotel.presentation.dto.top.SearchCriteriaDto;
import com.example.hotel.presentation.dto.top.SearchResultDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * P-010 (トップページ) 関連のRESTコントローラ 【主要機能】 - 初期表示データの提供（都道府県リスト、推奨日付等） - 空室検索API（検索条件に基づくホテル・部屋情報の検索） -
 * 地域詳細情報の取得（絞り込み用） 【データソース】 起動時に {@link com.example.hotel.config.StartupDatabaseLoader} が Doma を通じて
 * DBから取得し、{@link com.example.hotel.domain.service.CacheService} に格納した
 * 「部屋タイプごとの定員・総在庫」のキャッシュをフロントエンドへ提供する。
 */
@RestController
@Slf4j
public class TopPageController {

  // 推奨日付設定定数
  private static final int RECOMMENDED_CHECK_IN_DAYS_FROM_TODAY = 1;
  private static final int RECOMMENDED_CHECK_OUT_DAYS_FROM_TODAY = 2;

  // バリデーション定数
  private static final int MIN_GUEST_COUNT = 1;
  private static final int MAX_GUEST_COUNT = 99;
  // 都道府県ID: データベースのAUTO_INCREMENTにより1(北海道)～47(沖縄県)の範囲で設定される
  // ID=0は存在しないため、MIN_PREFECTURE_ID=1で正しく境界値チェックが行われる
  private static final int MIN_PREFECTURE_ID = 1;
  private static final int MAX_PREFECTURE_ID = 47; // 沖縄県が最大ID

  private final CacheService cacheService;
  private final SearchService searchService;
  private final AreaDetailDao areaDetailDao;
  private final MessageSource messageSource;

  public TopPageController(CacheService cacheService, SearchService searchService,
      AreaDetailDao areaDetailDao, MessageSource messageSource) {
    this.cacheService = cacheService;
    this.searchService = searchService;
    this.areaDetailDao = areaDetailDao;
    this.messageSource = messageSource;
  }

  /**
   * トップページの初期表示用データを返します。 返却内容: - message: サーバー状態の簡易メッセージ - roomTypesCacheData:
   * 起動時にキャッシュされた「部屋タイプごとの定員・総在庫」(Map<roomTypeId, RoomStockInfo>) 備考: /index.html
   * などの静的リソースはWorkbox等で配信／キャッシュされ、 本APIは動的データのみを返します。
   */
  @GetMapping("/api/initial-data")
  public ResponseEntity<Map<String, Object>> getInitialData() {
    // 起動時にロード済みの「部屋タイプごとの定員・総在庫」をキャッシュから取得して返す
    // 都道府県はIDと名前を含むオブジェクトリストとして返す（より堅牢なUI実装のため）
    List<Map<String, Object>> prefectureList = cacheService.getPrefectureCache().stream()
        .map(prefecture -> {
          Map<String, Object> prefMap = new HashMap<>();
          prefMap.put("id", prefecture.getPrefectureId());
          prefMap.put("name", prefecture.getPrefectureName());
          return prefMap;
        }).toList();

    Map<String, Object> data = Map.of("roomTypesCacheData", cacheService.getStockCache(),
        "prefectures", prefectureList, "recommendedCheckin",
        LocalDate.now().plusDays(RECOMMENDED_CHECK_IN_DAYS_FROM_TODAY), "recommendedCheckout",
        LocalDate.now().plusDays(RECOMMENDED_CHECK_OUT_DAYS_FROM_TODAY));
    return ResponseEntity.ok(data);
  }

  /**
   * 空室検索API。検索条件を受け取り、利用可能なホテル/部屋タイプの結果を返す。
   *
   * @param criteria
   *          リクエストからバインドされた検索条件
   * @return 成功時: 200 OK + SearchResultDto、エラー時: 422 + ApiErrorResponseDto 【API設計改善】レスポンス型の明確な分離 -
   *         成功レスポンス: SearchResultDto（検索結果専用） - エラーレスポンス: ApiErrorResponseDto（エラー情報専用） - 戻り値型:
   *         ResponseEntity<?>（柔軟な型対応）
   */
  @GetMapping("/api/search")
  public ResponseEntity<?> search(SearchCriteriaDto criteria) {
    try {
      // ビジネスロジック違反の検証（システムの整合性を脅かす不正な値）
      //
      // 【セキュリティ設計】
      // エラーレスポンスにはcriteriaフィールドを含めない。これは以下のセキュリティリスクを防ぐため：
      // 1. 攻撃者がシステムの内部構造（バリデーションルール、有効範囲など）を推測することを防止
      // 2. エラーレスポンスを通じた情報収集攻撃の無効化
      // 3. パラメータ形式や受入可能値の推測を困難にする
      // デバッグに必要な詳細情報はサーバーログに記録し、セキュリティと開発効率を両立する。

      // 【検証1】チェックイン日必須パラメータの検証
      // 必須項目であるチェックイン日がnullの場合はエラーレスポンスを返却
      if (criteria.getCheckInDate() == null) {
        log.warn(messageSource.getMessage("log.business.rule.violation.checkin.required",
            new Object[]{criteria}, Locale.getDefault()));
        // 【API設計改善】成功レスポンスとエラーレスポンスを明確に分離
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.date.checkInRequired", 422, "/api/search");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      // 【検証2】チェックアウト日必須パラメータの検証
      // 必須項目であるチェックアウト日がnullの場合はエラーレスポンスを返却
      if (criteria.getCheckOutDate() == null) {
        log.warn(messageSource.getMessage("log.business.rule.violation.checkout.required",
            new Object[]{criteria}, Locale.getDefault()));
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.date.checkOutRequired", 422, "/api/search");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      // 【検証3】チェックイン日の過去日検証
      // 本日より前の日付での予約は受け付けない（ビジネスルール）
      LocalDate today = LocalDate.now();
      if (criteria.getCheckInDate().isBefore(today)) {
        log.warn(messageSource.getMessage("log.business.rule.violation.checkin.past.date",
            new Object[]{criteria.getCheckInDate(), today, criteria}, Locale.getDefault()));
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.date.checkInPastDate", 422, "/api/search");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      // 【検証4】チェックアウト日の論理的整合性検証
      // チェックアウト日がチェックイン日以前または同日の場合はエラー（最低1泊必要）
      if (criteria.getCheckOutDate().isBefore(criteria.getCheckInDate())
          || criteria.getCheckOutDate().isEqual(criteria.getCheckInDate())) {
        log.warn(messageSource.getMessage("log.business.rule.violation.checkout.before.checkin",
            new Object[]{criteria.getCheckInDate(), criteria.getCheckOutDate(), criteria},
            Locale.getDefault()));
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.date.checkOutBeforeCheckIn", 422, "/api/search");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      // 【検証5】都道府県ID範囲検証
      // 都道府県IDが1-47の有効範囲外またはnullの場合はエラー（データ整合性保護）
      if (criteria.getPrefectureId() == null || criteria.getPrefectureId() < MIN_PREFECTURE_ID
          || criteria.getPrefectureId() > MAX_PREFECTURE_ID) {
        log.warn(messageSource.getMessage(
            "log.business.rule.violation.invalid.prefecture.id", new Object[]{
                criteria.getPrefectureId(), MIN_PREFECTURE_ID, MAX_PREFECTURE_ID, criteria},
            Locale.getDefault()));
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.form.prefectureRequired", 422, "/api/search");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      // 【検証6】宿泊人数範囲検証
      // 宿泊人数が1-99人の有効範囲外またはnullの場合はエラー（システム制約）
      if (criteria.getGuestCount() == null || criteria.getGuestCount() < MIN_GUEST_COUNT
          || criteria.getGuestCount() > MAX_GUEST_COUNT) {
        log.warn(messageSource.getMessage("log.business.rule.violation.invalid.guest.count",
            new Object[]{criteria.getGuestCount(), MIN_GUEST_COUNT, MAX_GUEST_COUNT, criteria},
            Locale.getDefault()));
        ApiErrorResponseDto errorResponse = ApiErrorResponseDto
            .create("validation.guestCount.range", 422, "/api/search");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
      }

      log.info(messageSource.getMessage("log.search.request.received", new Object[]{criteria},
          Locale.getDefault()));
      SearchResultDto result = searchService.searchAvailableHotels(criteria);
      log.info(messageSource.getMessage("log.search.response",
          new Object[]{result.getHotels() != null ? result.getHotels().size() : 0},
          Locale.getDefault()));

      return ResponseEntity.ok(result);

    }
    catch (NumberFormatException e) {
      // 数値変換エラー（不正なパラメータ形式）
      log.warn(messageSource.getMessage("log.number.format.error",
          new Object[]{e.getMessage(), criteria}, Locale.getDefault()));
      ApiErrorResponseDto errorResponse = ApiErrorResponseDto
          .create("validation.api.invalidRequest", 422, "/api/search");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }
    catch (IllegalArgumentException e) {
      /**
       * ビジネスロジック例外処理 - API設計改善と国際化対応 【設計原則】 1. 成功レスポンスとエラーレスポンスの完全分離 2. 統一されたエラーレスポンス構造 3.
       * 国際化対応のメッセージキー使用 【API設計の利点】 - クライアント側でのレスポンスタイプ判定が容易 - エラーハンドリングの一貫性が向上 -
       * TypeScript等での型安全性が向上
       */
      log.warn(messageSource.getMessage("log.business.rule.violation.general",
          new Object[]{e.getMessage(), criteria}, Locale.getDefault()));

      // 【API設計改善】専用のエラーレスポンスDTOを使用
      String messageKey = "PRICE_CALCULATION_ERROR".equals(e.getMessage())
          ? "validation.api.serverError"
          : "validation.api.businessRuleViolation";

      ApiErrorResponseDto errorResponse = ApiErrorResponseDto.create(messageKey, 422,
          "/api/search");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }
    catch (Exception e) {
      /**
       * システム例外処理 - 全般的なエラーハンドリング 【想定される例外種別】 - DB接続エラー: DataAccessException系 - 外部API障害:
       * RestClientException系 - JVM障害: OutOfMemoryError, StackOverflowError等 - 予期しない
       * RuntimeException 【セキュリティ原則】 1. 詳細なエラー情報をクライアントに返却しない（情報漏洩防止） 2. ログには詳細情報を出力（システム管理者向け） 3.
       * 汎用的なHTTP 500エラーで応答（攻撃者への情報提供を回避） 【運用考慮事項】 - ERROR レベル: システム管理者が即座に対応すべき深刻度 - スタックトレース出力:
       * 根本原因特定のため必須 - レスポンスボディなし: 内部情報の漏洩を完全に防止
       */
      log.error(messageSource.getMessage("log.unexpected.error.search", null, Locale.getDefault()),
          e);
      return ResponseEntity.internalServerError().build(); // 500: サーバーエラー
    }
  }

  /**
   * 指定された都道府県に紐づく詳細地域を取得するAPI 絞り込みフォームで使用される
   *
   * @param prefectureId
   *          都道府県ID
   * @return 詳細地域のリスト
   */
  @GetMapping("/api/area-details")
  public ResponseEntity<List<AreaDetail>> getAreaDetails(Integer prefectureId) {
    try {
      // 【検証1】地域詳細取得用都道府県ID検証
      // 都道府県IDが1-47の有効範囲外またはnullの場合は空リストを返却（セキュリティ配慮）
      if (prefectureId == null || prefectureId < MIN_PREFECTURE_ID
          || prefectureId > MAX_PREFECTURE_ID) {
        log.warn(messageSource.getMessage("log.invalid.prefecture.id",
            new Object[]{prefectureId, MIN_PREFECTURE_ID, MAX_PREFECTURE_ID}, Locale.getDefault()));
        // セキュリティ上、詳細なバリデーション情報を漏らさず、空のリストを返却
        return ResponseEntity.ok(List.of());
      }

      List<AreaDetail> areaDetails = areaDetailDao.selectByPrefectureId(prefectureId);
      log.info(messageSource.getMessage("log.area.details.result",
          new Object[]{prefectureId, areaDetails.size()}, Locale.getDefault()));

      return ResponseEntity.ok(areaDetails);
    }
    catch (Exception e) {
      log.error(messageSource.getMessage("log.unexpected.error.area.details",
          new Object[]{prefectureId}, Locale.getDefault()), e);
      // セキュリティ上、システムエラー時も空のリストを返却して内部状態を隠蔽
      return ResponseEntity.ok(List.of());
    }
  }
}
