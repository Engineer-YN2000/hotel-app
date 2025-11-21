package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.AreaDetail;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

/**
 * 詳細地域データアクセス
 */
@ConfigAutowireable
@Dao
public interface AreaDetailDao {

  /**
   * 指定された都道府県に紐づく詳細地域を取得
   *
   * @param prefectureId
   *          都道府県ID
   * @return 詳細地域のリスト
   */
  @Select
  List<AreaDetail> selectByPrefectureId(Integer prefectureId);

  /**
   * すべての詳細地域を取得
   *
   * @return 詳細地域のリスト
   */
  @Select
  List<AreaDetail> selectAll();
}
