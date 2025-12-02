-- 起動時キャッシュ用クエリ
-- 部屋タイプごとの定員と総在庫（総室数）を取得する
SELECT
    rt.room_type_id,
    rt.hotel_id,
    rt.room_type_name,
    MIN(r.room_capacity) AS room_capacity,
    COUNT(r.room_id) AS total_stock
FROM
    room_types rt
JOIN
    rooms r ON rt.room_type_id = r.room_type_id
GROUP BY
    rt.room_type_id,
    rt.hotel_id,
    rt.room_type_name
