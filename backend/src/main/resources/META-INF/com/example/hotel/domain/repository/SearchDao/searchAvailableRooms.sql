-- P-010 空室検索クエリ (flowchart_top_page.dot)
-- 指定された条件で、ホテル情報、部屋タイプ情報、および
-- 該当期間中の「予約済み室数」を取得する。
-- パフォーマンス最適化: CTEの代わりに単一クエリでJOINを使用
--
-- 【集計ロジック】
-- reserved_count: 検索期間と重複する予約の室数合計
-- CASE文により、有効な予約（reservation_idが存在）のみを集計対象とする
-- これにより、LEFT JOINでマッチしない場合の不正な集計を防止
--
-- 【重要】ダブルブッキング防止について
-- このクエリは検索時点での在庫状況表示用です。
-- 実際の予約処理時には、以下の対策が必要です：
-- 1. 予約ボタン押下時にリアルタイムで在庫再確認
-- 2. 悲観的ロック（SELECT FOR UPDATE）による排他制御
-- 3. 楽観的ロック（バージョン管理）による競合検出
-- 4. トランザクション内での在庫チェック＋予約登録の原子性保証

SELECT
    h.hotel_id,
    h.hotel_name,
    rt.room_type_id,
    rt.room_type_name,
    COALESCE(SUM(CASE
        WHEN res.reservation_id IS NOT NULL
        THEN rd.room_count
        ELSE 0
    END), 0) AS reserved_count,
    ad.area_id
FROM
    hotels h
JOIN
    area_details ad ON h.area_id = ad.area_id
JOIN
    room_types rt ON h.hotel_id = rt.hotel_id
LEFT JOIN
    reservation_details rd ON rt.room_type_id = rd.room_type_id
LEFT JOIN
    reservations res ON rd.reservation_id = res.reservation_id
    AND (
        /*%if checkInDate != null */
        -- 予約のチェックアウト日が検索のチェックイン日より後（重複チェック）
        res.check_out_date > /* checkInDate */'2025-01-01'
        /*%end */
        /*%if checkInDate != null && checkOutDate != null */
        AND
        /*%end */
        /*%if checkOutDate != null */
        -- 予約のチェックイン日が検索のチェックアウト日より前（重複チェック）
        res.check_in_date < /* checkOutDate */'2025-01-02'
        /*%end */
    )
    -- 予約ステータス条件をJOIN条件に含めることで効率化
    -- ReservationStatus.TENTATIVE (10): 仮予約（一時的な予約）
    -- ReservationStatus.CONFIRMED (20): 確定予約
    -- ※ これらのステータスを「予約済み」としてカウントする
    -- ※ 予約ステータス値はJava定数クラスから動的に渡される
    /*%if reservedStatuses != null */
    AND res.reservation_status IN /* reservedStatuses */(10, 20)
    /*%end */
WHERE 1 = 1
    /*%if prefectureId != null */
    -- 都道府県IDで絞り込み（UIから動的に渡される値）
    AND ad.prefecture_id = /* prefectureId */1
    /*%end */
GROUP BY
    h.hotel_id,
    h.hotel_name,
    rt.room_type_id,
    rt.room_type_name,
    ad.area_id
