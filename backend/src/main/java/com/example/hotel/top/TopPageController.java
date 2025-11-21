package com.example.hotel.top;

import com.example.hotel.domain.service.CacheService;
import com.example.hotel.domain.service.SearchService;
import com.example.hotel.domain.repository.AreaDetailDao;
import com.example.hotel.domain.model.AreaDetail;
import com.example.hotel.presentation.dto.SearchCriteriaDto;
import com.example.hotel.presentation.dto.SearchResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P-010 (トップページ) 関連のコントローラ。 起動時に {@link com.example.hotel.config.StartupDatabaseLoader} が Doma
 * を通じてDBから取得し、 {@link com.example.hotel.domain.service.CacheService} に格納した
 * 「部屋タイプごとの定員・総在庫」のキャッシュをフロントへ返すAPIを提供します。
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
  private static final int MIN_PREFECTURE_ID = 1;

  // HTTPステータスコード定数
  private static final int HTTP_STATUS_UNPROCESSABLE_ENTITY = 422;

  private final CacheService cacheService;
  private final SearchService searchService;
  private final AreaDetailDao areaDetailDao;

  public TopPageController(CacheService cacheService, SearchService searchService,
      AreaDetailDao areaDetailDao) {
    this.cacheService = cacheService;
    this.searchService = searchService;
    this.areaDetailDao = areaDetailDao;
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
   * 空室検索API。検索条件を受け取り、利用可能なホテル/部屋タイプの結果を返す。 受け取り: checkInDate, checkOutDate (ISO-8601), areaId,
   * guestCount 成功時: 200 OK + SearchResultDto、失敗時: 500 Internal Server Error
   *
   * @param criteria
   *          リクエストからバインドされた検索条件
   * @return 検索結果のレスポンス
   */
  @GetMapping("/api/search")
  public ResponseEntity<SearchResultDto> search(SearchCriteriaDto criteria) {
    try {
      // ビジネスロジック違反の検証（システムの整合性を脅かす不正な値）

      // 日付パラメータの検証
      if (criteria.getCheckInDate() == null) {
        log.warn("ビジネスルール違反 - チェックイン日が未指定");
        SearchResultDto errorResult = new SearchResultDto();
        errorResult.setErrorMessage("チェックイン日を入力してください");
        return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
      }

      if (criteria.getCheckOutDate() == null) {
        log.warn("ビジネスルール違反 - チェックアウト日が未指定");
        SearchResultDto errorResult = new SearchResultDto();
        errorResult.setErrorMessage("チェックアウト日を入力してください");
        return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
      }

      LocalDate today = LocalDate.now();
      if (criteria.getCheckInDate().isBefore(today)) {
        log.warn("ビジネスルール違反 - 過去のチェックイン日: {}", criteria.getCheckInDate());
        SearchResultDto errorResult = new SearchResultDto();
        errorResult.setErrorMessage("チェックイン日は今日以降の日付を入力してください");
        return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
      }

      if (criteria.getCheckOutDate().isBefore(criteria.getCheckInDate())
          || criteria.getCheckOutDate().isEqual(criteria.getCheckInDate())) {
        log.warn("ビジネスルール違反 - チェックアウト日がチェックイン日以前または同日: チェックイン={}, チェックアウト={}",
            criteria.getCheckInDate(), criteria.getCheckOutDate());
        SearchResultDto errorResult = new SearchResultDto();
        errorResult.setErrorMessage("チェックアウト日はチェックイン日の翌日以降を入力してください");
        return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
      }

      if (criteria.getPrefectureId() == null || criteria.getPrefectureId() < MIN_PREFECTURE_ID) {
        log.warn("ビジネスルール違反 - 無効な都道府県ID: {}", criteria.getPrefectureId());
        SearchResultDto errorResult = new SearchResultDto();
        errorResult.setErrorMessage("無効な都道府県が指定されました");
        return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
      }

      if (criteria.getGuestCount() == null || criteria.getGuestCount() < MIN_GUEST_COUNT
          || criteria.getGuestCount() > MAX_GUEST_COUNT) {
        log.warn("ビジネスルール違反 - 無効な宿泊人数: {}", criteria.getGuestCount());
        SearchResultDto errorResult = new SearchResultDto();
        errorResult
            .setErrorMessage("宿泊人数は" + MIN_GUEST_COUNT + "～" + MAX_GUEST_COUNT + "人の範囲で入力してください");
        return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
      }

      log.info("検索リクエスト受信: {}", criteria);
      SearchResultDto result = searchService.searchAvailableHotels(criteria);
      log.info("検索レスポンス: ホテル数={}", result.getHotels() != null ? result.getHotels().size() : 0);

      return ResponseEntity.ok(result);

    }
    catch (NumberFormatException e) {
      // 数値変換エラー（不正なパラメータ形式）
      log.warn("数値変換エラー: {}", e.getMessage());
      SearchResultDto errorResult = new SearchResultDto();
      errorResult.setErrorMessage("入力形式に誤りがあります");
      return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
    }
    catch (IllegalArgumentException e) {
      // ビジネスロジック由来のバリデーションエラー（システムの整合性違反）
      log.warn("ビジネスロジック違反: {}", e.getMessage());
      SearchResultDto errorResult = new SearchResultDto();
      errorResult.setErrorMessage(e.getMessage());
      return ResponseEntity.status(HTTP_STATUS_UNPROCESSABLE_ENTITY).body(errorResult);
    }
    catch (Exception e) {
      // 予期せぬシステムエラー（DB接続エラー、外部API障害など）
      log.error("空室検索中に予期せぬエラーが発生しました", e);
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
      if (prefectureId == null || prefectureId < MIN_PREFECTURE_ID) {
        log.warn("無効な都道府県ID: {}", prefectureId);
        return ResponseEntity.badRequest().build();
      }

      List<AreaDetail> areaDetails = areaDetailDao.selectByPrefectureId(prefectureId);
      log.info("詳細地域 取得結果: 都道府県ID={}, 件数={}", prefectureId, areaDetails.size());

      return ResponseEntity.ok(areaDetails);
    }
    catch (Exception e) {
      log.error("詳細地域取得中にエラーが発生しました", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
