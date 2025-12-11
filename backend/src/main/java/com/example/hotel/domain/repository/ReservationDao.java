package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.Reservation;
import com.example.hotel.domain.model.ReservationWithInfo;

import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import org.seasar.doma.jdbc.SelectOptions;

import java.time.LocalDate;
import java.time.LocalTime;
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

  /**
   * 仮予約に予約者IDと到着予定時刻を紐付けます。
   *
   * 指定された予約IDかつTENTATIVEステータス、かつ有効期限内のレコードのみを更新対象とします。
   * WHERE句に pending_limit_at > NOW() を含めることで、TOCTOU競合を防止します。
   *
   * @param reservationId 予約ID
   * @param reserverId 予約者ID
   * @param arriveAt 到着予定時刻
   * @param tentativeStatus 仮予約ステータス（ReservationStatus.TENTATIVEを指定）
   * @return 更新件数（0の場合は対象レコードなしまたは期限切れ）
   */
  @Update(sqlFile = true)
  int bindReserverAndArrivalTime(Integer reservationId, Integer reserverId, LocalTime arriveAt,
      Integer tentativeStatus);

  /**
   * 指定された予約IDの仮予約が期限切れかどうかを判定します。
   *
   * 予約ステータスがTENTATIVEかつpending_limit_atが現在時刻を過ぎている場合にtrueを返します。
   *
   * @param reservationId 予約ID
   * @param tentativeStatus 仮予約ステータス（ReservationStatus.TENTATIVEを指定）
   * @return 期限切れの場合true、そうでない場合false
   */
  @Select
  boolean isExpired(Integer reservationId, Integer tentativeStatus);

  /**
   * 予約IDとステータスを指定して予約情報を取得します。
   *
   * @param reservationId 予約ID
   * @param tentativeStatus 仮予約ステータス（ReservationStatus.TENTATIVEを指定）
   * @return 予約情報リスト（該当なしの場合は空リスト）
   */
  @Select
  List<ReservationWithInfo> selectByIdWithDetails(Integer reservationId, Integer tentativeStatus);

  @Select
  Integer selectReserverId(Integer reservationId);

  /**
   * 仮予約をキャンセル（CANCELLEDステータス）に更新します。
   *
   * 指定された予約IDがTENTATIVEステータスかつpending_limit_atが現在時刻以降の場合のみ更新します。
   * P-020（予約詳細入力）のキャンセルボタン押下時に使用されます。
   *
   * 条件を満たさない場合（既にタイムアウト済み、バッチで処理済み等）は更新件数0を返します。
   * これはベストエフォートの処理であり、バッチ処理が最終的な整合性を保証します。
   *
   * @param reservationId 予約ID
   * @param tentativeStatus 仮予約ステータス（ReservationStatus.TENTATIVEを指定）
   * @param cancelledStatus キャンセルステータス（ReservationStatus.CANCELLEDを指定）
   * @return 更新件数（0の場合は対象レコードなしまたは条件不一致）
   */
  @Update(sqlFile = true)
  int cancelReservation(Integer reservationId, Integer tentativeStatus, Integer cancelledStatus);

  /**
   * 仮予約を期限切れ（EXPIRED）ステータスに更新します。
   *
   * 指定された予約IDがTENTATIVEステータスかつpending_limit_atが現在時刻以前の場合のみ更新します。
   * P-910（予約有効時間切れ画面）からトップページに戻る際に呼び出されます。
   *
   * @param reservationId 予約ID
   * @param tentativeStatus 仮予約ステータス（ReservationStatus.TENTATIVEを指定）
   * @param expiredStatus 期限切れステータス（ReservationStatus.EXPIREDを指定）
   * @return 更新件数（0の場合は対象レコードなしまたは条件不一致）
   */
  @Update(sqlFile = true)
  int expireReservation(Integer reservationId, Integer tentativeStatus, Integer expiredStatus);

  /**
   * 仮予約を確定（CONFIRMEDステータス）に更新します。
   *
   * 指定された予約IDがTENTATIVEステータスかつpending_limit_atが現在時刻以降の場合のみ更新します。
   * P-030（予約確認）の確定ボタン押下時に使用されます。
   *
   * @param reservationId 予約ID
   * @param tentativeStatus 仮予約ステータス（ReservationStatus.TENTATIVEを指定）
   * @param confirmedStatus 確定ステータス（ReservationStatus.CONFIRMEDを指定）
   * @return 更新件数（0の場合は対象レコードなしまたは条件不一致）
   */
  @Update(sqlFile = true)
  int confirmReservation(Integer reservationId, Integer tentativeStatus, Integer confirmedStatus);
}
