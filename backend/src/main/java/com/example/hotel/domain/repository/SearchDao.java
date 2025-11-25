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
   * 指定された条件（都道府県、日付）に基づいて利用可能なホテルと部屋タイプ、 及びその期間中の「予約済み室数」を取得する。 SQLパラメータ参照: comment-style parameter
   * binding の具体例は以下のSQLファイルを参照
   * {@code META-INF/com/example/hotel/domain/repository/SearchDao/searchAvailableRooms.sql}
   *
   * @param prefectureId
   *          都道府県ID (prefectures.prefecture_id) - UIから動的に渡される値
   * @param checkInDate
   *          チェックイン日 (reservations.check_in_date) - 検索対象期間の開始日
   * @param checkOutDate
   *          チェックアウト日 (reservations.check_out_date) - 検索対象期間の終了日
   * @param reservedStatuses
   *          予約済みとしてカウントする予約ステータスのリスト (ReservationStatus定数値)
   * @param options
   *          SelectOptions（Doma2が自動使用） - ページング、ソートオプション
   * @return 利用可能なホテル・部屋タイプ・予約済み室数のリスト
   */
  @Select
  List<AvailableRoomInfo> searchAvailableRooms(Integer prefectureId, LocalDate checkInDate,
      LocalDate checkOutDate, java.util.List<Integer> reservedStatuses, SelectOptions options);
}
