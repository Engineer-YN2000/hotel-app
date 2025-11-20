package com.example.hotel.top;

import com.example.hotel.domain.service.CacheService;
import com.example.hotel.domain.service.SearchService;
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

  private final CacheService cacheService;
  private final SearchService searchService;

  public TopPageController(CacheService cacheService, SearchService searchService) {
    this.cacheService = cacheService;
    this.searchService = searchService;
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
        "prefectures", prefectureList, "recommendedCheckin", LocalDate.now().plusDays(1),
        "recommendedCheckout", LocalDate.now().plusDays(2));
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
      if (criteria.getPrefectureId() == null || criteria.getPrefectureId() <= 0) {
        log.warn("ビジネスルール違反 - 無効な都道府県ID: {}", criteria.getPrefectureId());
        return ResponseEntity.status(422).body(null); // 422レスポンスはボディなnullで返却
      }

      if (criteria.getGuestCount() == null || criteria.getGuestCount() <= 0
          || criteria.getGuestCount() > 99) {
        log.warn("ビジネスルール違反 - 無効な宿泊人数: {}", criteria.getGuestCount());
        return ResponseEntity.status(422).body(null); // 422レスポンスはボディなnullで返却
      }

      log.info("検索リクエスト受信: {}", criteria);
      SearchResultDto result = searchService.searchAvailableHotels(criteria);
      log.info("検索レスポンス: ホテル数={}", result.getHotels() != null ? result.getHotels().size() : 0);

      return ResponseEntity.ok(result);

    }
    catch (NumberFormatException e) {
      // 数値変換エラー（不正なパラメータ形式）
      log.warn("数値変換エラー: {}", e.getMessage());
      return ResponseEntity.status(422).body(null);
    }
    catch (IllegalArgumentException e) {
      // ビジネスロジック由来のバリデーションエラー（システムの整合性違反）
      log.warn("ビジネスロジック違反: {}", e.getMessage());
      return ResponseEntity.status(422).body(null);
    }
    catch (Exception e) {
      // 予期せぬシステムエラー（DB接続エラー、外部API障害など）
      log.error("空室検索中に予期せぬエラーが発生しました", e);
      return ResponseEntity.internalServerError().build(); // 500: サーバーエラー
    }
  }
}
