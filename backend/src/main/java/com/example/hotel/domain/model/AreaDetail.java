package com.example.hotel.domain.model;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;
import org.seasar.doma.Column;
import lombok.Data;

/**
 * 詳細地域エンティティ データベースのarea_detailsテーブルに対応
 */
@Data
@Entity(immutable = false)
@Table(name = "area_details")
public class AreaDetail {

  @Id
  @Column(name = "area_id")
  private Integer areaId;

  @Column(name = "area_name")
  private String areaName;

  @Column(name = "prefecture_id")
  private Integer prefectureId;
}
