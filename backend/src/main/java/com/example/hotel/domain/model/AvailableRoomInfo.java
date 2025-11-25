package com.example.hotel.domain.model;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Column;
import lombok.Data;

/**
 * 検索結果（ホテル、部屋タイプ、予約済み室数）をマッピングするドメインクラス
 *
 * 【可変エンティティ設計の理由】
 * このクラスは@Entity(immutable = false)と@Dataを使用した可変エンティティです。
 *
 * ■ RoomStockInfoとの設計思想の違い:
 * - RoomStockInfo: 起動時キャッシュ用の不変エンティティ（@Value + immutable = true）
 *   → 部屋タイプの基本情報（定員、総在庫）は頻繁に変更されない静的データ
 * - AvailableRoomInfo: 検索処理用の可変エンティティ（@Data + immutable = false）
 *   → 予約状況に応じて動的に変化する時系列データ
 *
 * ■ 可変性が必要な理由:
 * 1. 予約済み室数（reservedCount）は予約状況により頻繁に変化
 * 2. 検索処理では複数のJOIN結果を段階的に集計・計算する必要がある
 * 3. 在庫計算ロジックで中間結果を一時的に保持・更新する処理が含まれる
 * 4. DomaのSQLマッピング処理において、結果セットから段階的にフィールドを設定
 *
 * ■ トレードオフ:
 * - メリット: 柔軟な集計処理、パフォーマンス向上（オブジェクト再生成コスト削減）
 * - デメリット: 予期しない変更のリスク（ただし、検索処理に限定された用途で制御）
 */
@Data
@Entity(immutable = false)
public class AvailableRoomInfo {

  /**
   * ホテルID (hotels.hotel_id)
   */
  @Id
  @Column(name = "hotel_id")
  private Integer hotelId;

  /**
   * ホテル名 (hotels.hotel_name)
   */
  @Column(name = "hotel_name")
  private String hotelName;

  /**
   * 部屋タイプID (room_types.room_type_id)
   */
  @Column(name = "room_type_id")
  private Integer roomTypeId;

  /**
   * 部屋タイプ名 (room_types.room_type_name)
   */
  @Column(name = "room_type_name")
  private String roomTypeName;

  /**
   * 予約済み室数
   */
  @Column(name = "reserved_count")
  private Integer reservedCount;

  /**
   * 詳細地域ID (area_details.area_id)
   */
  @Column(name = "area_id")
  private Integer areaId;
}
