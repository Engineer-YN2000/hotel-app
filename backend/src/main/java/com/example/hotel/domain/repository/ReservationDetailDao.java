package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.ReservationDetail;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

/**
 * reservation_detailsテーブルへのアクセスを提供するDAOインターフェース。
 * 予約詳細情報の登録など、予約管理の詳細永続化処理を担当します。
 */
@Dao
@ConfigAutowireable
public interface ReservationDetailDao {

  /**
   * 予約詳細レコードを新規挿入します。
   * reservation_detailsテーブルに新しい予約詳細情報を登録します。
   * @param reservationDetail 挿入する予約詳細エンティティ
   * @return Result<ReservationDetail> 挿入結果（主キーreservationDetailId含む）
   */
  @Insert
  Result<ReservationDetail> insert(ReservationDetail reservationDetail);
}
