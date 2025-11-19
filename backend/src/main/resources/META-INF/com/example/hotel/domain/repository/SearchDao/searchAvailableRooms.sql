-- P-010 空室検索クエリ (flowchart_top_page.dot)
-- 指定された条件で、ホテル情報、部屋タイプ情報、および
-- 該当期間中の「予約済み室数」を取得する。

WITH target_rooms AS (
    -- 1. 都道府県（prefectureId）で絞り込み
    SELECT
        h.hotel_id,
        h.hotel_name,
        rt.room_type_id,
        rt.room_type_name
    FROM
        hotels h
    JOIN
        area_details ad ON h.area_id = ad.area_id
    JOIN
        room_types rt ON h.hotel_id = rt.hotel_id
    JOIN
        rooms r ON rt.room_type_id = r.room_type_id
    WHERE 1 = 1
        /*%if prefectureId != null */
        AND ad.prefecture_id = /* prefectureId */1
        /*%end */
    GROUP BY
        h.hotel_id,
        h.hotel_name,
        rt.room_type_id,
        rt.room_type_name
),
reserved_counts AS (
    -- 2. 該当期間中に予約されている室数を計算
    SELECT
        rd.room_type_id,
        -- 予約済み室数をカウント
        COALESCE(SUM(rd.room_count), 0) AS reserved_count
    FROM
        reservation_details rd
    JOIN
        reservations r ON rd.reservation_id = r.reservation_id
    WHERE 1 = 1
        /*%if checkInDate != null */
        AND r.check_out_date > /* checkInDate */'2025-12-20'
        /*%end */
        /*%if checkOutDate != null */
        AND r.check_in_date < /* checkOutDate */'2025-12-25'
        /*%end */
        -- 予約ステータスが「有効（10）」または「仮押さえ（0）」のものをカウント
        AND r.reservation_status IN (0, 10)
        -- 1. で絞り込んだ部屋タイプに限定
        AND rd.room_type_id IN (SELECT room_type_id FROM target_rooms)
    GROUP BY
        rd.room_type_id
)

SELECT
    tr.hotel_id,
    tr.hotel_name,
    tr.room_type_id,
    tr.room_type_name,
    COALESCE(rc.reserved_count, 0) AS reserved_count
FROM
    target_rooms tr
LEFT JOIN
    reserved_counts rc ON tr.room_type_id = rc.room_type_id
