package com.example.hotel.config;

import com.example.hotel.domain.model.RoomStockInfo;
import com.example.hotel.domain.repository.RoomStockDao;
import com.example.hotel.domain.service.RoomStockCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class StartupDatabaseLoader implements CommandLineRunner {
  private final RoomStockDao roomStockDao;
  private final RoomStockCacheService cacheService;

  // Doma2 DaoとキャッシュサービスをDI
  public StartupDatabaseLoader(RoomStockDao roomStockDao, RoomStockCacheService cacheService) {
    this.roomStockDao = roomStockDao;
    this.cacheService = cacheService;
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("サーバー起動プロセス開始：在庫キャッシュの読み込み中...");

    final int MAX_RETRIES = 3;
    final int RETRY_WAIT_SECONDS = 5;

    int attempt = 0;
    boolean success = false;

    while (attempt < MAX_RETRIES) {
      attempt++;
      log.info("DB検索（{}回目）...", attempt);

      try {
        // DB検索
        List<RoomStockInfo> stockInfoList = roomStockDao.selectRoomStockInfo();
        if (stockInfoList.isEmpty()) {
          log.warn("DB検索成功。しかし、在庫情報が0件です。");
        }
        else {
          log.info("DB検索成功。{}件の部屋タイプ情報をキャッシュしました。", stockInfoList.size());
        }

        // キャッシュサービスにデータを登録
        cacheService.updateCache(stockInfoList);
        success = true;
        break;
      }
      catch (Exception e) {
        log.error("在庫キャッシュに失敗しました ({}回目)。", attempt, e);

        if (attempt < MAX_RETRIES) {
          log.info("リトライ回数+1。{}秒待機します...", RETRY_WAIT_SECONDS);
          TimeUnit.SECONDS.sleep(RETRY_WAIT_SECONDS);
        }
      }
    }

    if (success) {
      log.info("キャッシュ成功。リクエスト待ち受け状態へ移行します。");
    }
    else {
      log.error("リトライ上限({}回)に達しました。キャッシュに失敗しました。", MAX_RETRIES);
      throw new RuntimeException("サーバー起動時の必須データの読み込みに失敗しました。");
    }
  }
}
