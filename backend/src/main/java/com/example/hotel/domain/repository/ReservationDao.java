package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.Reservation;

import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import org.seasar.doma.jdbc.SelectOptions;

import java.time.LocalDate;
import java.util.List;

/**
 * reservationsテーブルへのアクセスを提供するDAOインターフェース。
 *
 * 仮予約・本予約の登録や、予約済み在庫数の集計など、
 * 予約管理に関する永続化処理を担当します。
 *
 */
@Dao
@ConfigAutowireable
public interface ReservationDao {

  /**
   * 予約レコード（仮予約・本予約）を新規挿入します。
   *
   * reservationsテーブルに新しい予約情報を登録します。
   * 仮予約の場合は、事前に用意した仮予約用reserver_idを利用してください。
   *
   * @param reservation 挿入する予約エンティティ
   * @return Result<Reservation> 挿入結果（主キーreservationId含む）
   */
  @Insert
  Result<Reservation> insert(Reservation reservation);

  /**
   * 指定した部屋タイプ・期間・ステータスに該当する予約済み（仮含む）在庫数を集計します。
   *
   * reservation_detailsとreservationsを結合し、
   * 対象部屋タイプ・期間・予約ステータス（仮予約・本予約）で絞り込んで合計予約数を返します。
   *
   * 【悲観的ロック（ダブルブッキング防止）】
   * SelectOptions.get().forUpdate() を渡すことでSELECT FOR UPDATEが適用されます。
   * 仮予約作成時には必ずforUpdateを使用してください。
   * ロックはトランザクション終了時（コミット/ロールバック）に解放されます。
   *
   * @param roomTypeId 部屋タイプID
   * @param reservedStatuses 集計対象とする予約ステータスのリスト（各値はReservationStatusで定義）
   * @param checkInDate チェックイン日（宿泊開始日）
   * @param checkOutDate チェックアウト日（宿泊終了日）
   * @param options SelectOptions（forUpdate()で悲観的ロック指定可）
   * @return 予約済み部屋数（該当がなければ０）
   */
  @Select
  int countReservedRooms(Integer roomTypeId, List<Integer> reservedStatuses, LocalDate checkInDate,
      LocalDate checkOutDate, SelectOptions options);
}
