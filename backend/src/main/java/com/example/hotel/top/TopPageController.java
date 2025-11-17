package com.example.hotel.top;

import com.example.hotel.domain.service.RoomStockCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P-010 (トップページ) 関連のコントローラ。 起動時に {@link com.example.hotel.config.StartupDatabaseLoader} が Doma
 * を通じてDBから取得し、 {@link com.example.hotel.domain.service.RoomStockCacheService} に格納した
 * 「部屋タイプごとの定員・総在庫」のキャッシュをフロントへ返すAPIを提供します。
 */
@RestController
public class TopPageController {

  private final RoomStockCacheService cacheService;

  public TopPageController(RoomStockCacheService cacheService) {
    this.cacheService = cacheService;
  }

  /**
   * トップページの初期表示用データを返します。 返却内容: - message: サーバー状態の簡易メッセージ - roomTypesCacheData:
   * 起動時にキャッシュされた「部屋タイプごとの定員・総在庫」(Map<roomTypeId, RoomStockInfo>) 備考: /index.html
   * などの静的リソースはWorkbox等で配信／キャッシュされ、 本APIは動的データのみを返します。
   */
  @GetMapping("/api/initial-data")
  public Map<String, Object> getInitialData() {
    // 起動時にロード済みの「部屋タイプごとの定員・総在庫」をキャッシュから取得して返す
    var roomTypes = cacheService.getStockCache();
    return Map.of("message", "サーバー起動成功", "roomTypesCacheData", roomTypes);
  }
}
