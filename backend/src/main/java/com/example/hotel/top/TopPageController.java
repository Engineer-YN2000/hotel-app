package com.example.hotel.top;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P-010 (トップページ) 関連のコントローラ (flowchart_top_page.dot)
 */
@RestController
public class TopPageController {

  /**
   * トップページ描画 (flowchart_top_page.dot) ...の代わりとして、WorkBoxがキャッシュする初期データを返します。 (実際には /index.html
   * は静的配信されます)
   */
  @GetMapping("/api/initial-data")
  public Map<String, Object> getInitialData() {
    /**
     * flowchart_app_startup.dot でキャッシュした 「部屋タイプごとの定員・総在庫」 を返す処理
     */

    // 現時点ではモックデータを返す
    return Map.of("message", "サーバー起動成功", "roomTypesCacheData", "ここに定員と総在庫データが入ります");
  }
}
