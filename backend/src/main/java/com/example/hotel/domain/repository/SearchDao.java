package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.AvailableRoomInfo;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.SelectOptions;

import java.time.LocalDate;
import java.util.List;

/**
 * 空室検索用DAO。地域・日付条件でホテル/部屋タイプと予約済み室数を取得する。
 */
@ConfigAutowireable
@Dao
public interface SearchDao {

  /**
   * 指定された条件（都道府県、日付）に基づいて利用可能なホテルと部屋タイプ、 及びその期間中の「予約済み室数」を取得する。
   *
   * @param prefectureId
   *          都道府県Id (prefectures.prefecture_id)
   * @param checkInDate
   *          チェックイン日 (reservations.check_in_date)
   * @param checkOutDate
   *          チェックアウト日 (reservations.check_out_date)
   * @param reservedStatuses
   *          予約済みとしてカウントする予約ステータスのリスト
   * @param options
   *          SelectOptions（Doma2が自動使用）
   * @return List<AvailableRoomInfo>
   */
  @Select
  List<AvailableRoomInfo> searchAvailableRooms(Integer prefectureId, LocalDate checkInDate,
      LocalDate checkOutDate, java.util.List<Integer> reservedStatuses, SelectOptions options);
}
