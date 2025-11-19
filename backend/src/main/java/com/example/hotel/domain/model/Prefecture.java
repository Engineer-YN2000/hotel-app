package com.example.hotel.domain.model;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Column;
import org.seasar.doma.Table;

import lombok.Value;
import lombok.AllArgsConstructor;

/**
 * 都道府県マスタを表すドメインクラス。
 */
@Value
@Entity(immutable = true)
@Table(name = "prefectures")
@AllArgsConstructor
public class Prefecture {
  @Id
  @Column(name = "prefecture_id")
  private final Integer prefectureId;

  @Column(name = "prefecture_name")
  private final String prefectureName;
}
