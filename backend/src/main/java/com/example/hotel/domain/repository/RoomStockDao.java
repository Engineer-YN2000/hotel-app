package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.RoomStockInfo;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

/**
 * 起動時キャッシュ用に部屋タイプごとの総在庫と定員を取得する Doma DAO。
 */
@Dao
@ConfigAutowireable
public interface RoomStockDao {
  /**
   * 部屋タイプごとの総在庫と定員を取得する
   *
   * @return RoomStockInfoのリスト
   */
  @Select
  List<RoomStockInfo> selectRoomStockInfo();
}
