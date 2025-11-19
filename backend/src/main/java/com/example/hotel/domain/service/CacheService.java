package com.example.hotel.domain.service;

import com.example.hotel.domain.model.Prefecture;
import com.example.hotel.domain.model.RoomStockInfo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * アプリケーション起動時に「部屋タイプごとの定員・総在庫」を保持するキャッシュサービス
 */
@Service
public class CacheService {

  // 部屋タイプIDをキー、RoomStockInfoを値とするキャッシュマップ
  private Map<Integer, RoomStockInfo> stockCache = new ConcurrentHashMap<>();

  // 都道府県情報キャッシュ
  private List<Prefecture> prefectureCache = Collections.emptyList();

  /**
   * DBから取得した部屋タイプごとの定員・総在庫情報でキャッシュを更新する
   */
  public void updateCache(List<RoomStockInfo> stockInfoList) {
    if (stockInfoList == null || stockInfoList.isEmpty()) {
      this.stockCache.clear();
      return;
    }
    Map<Integer, RoomStockInfo> newCache = stockInfoList.stream()
        .collect(Collectors.toMap(RoomStockInfo::getRoomTypeId, Function.identity()));

    this.stockCache = new ConcurrentHashMap<>(newCache);
  }

  /**
   * DBから取得した都道府県情報でキャッシュを更新する
   */
  public void updatePrefectureCache(List<Prefecture> prefectures) {
    if (prefectures != null) {
      this.prefectureCache = List.copyOf(prefectures); // Immutable Listとして保持
    }
  }

  /**
   * キャッシュされた全在庫情報を取得する
   */
  public Map<Integer, RoomStockInfo> getStockCache() {
    // 読み取り専用のコピーを返す
    return Map.copyOf(this.stockCache);
  }

  /**
   * キャッシュされた都道府県情報を取得する
   */
  public List<Prefecture> getPrefectureCache() {
    return this.prefectureCache;
  }

  /**
   * キャッシュが空かどうかを返す
   */
  public boolean isEmpty() {
    return this.stockCache.isEmpty() && this.prefectureCache.isEmpty();
  }
}
