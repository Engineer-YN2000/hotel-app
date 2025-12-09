-- 予約IDに紐づく予約情報と部屋タイプ情報を取得する。
--
-- 【capacityにMINを使用する理由】
-- 同一room_type_id内で複数のroomsレコードが存在する場合、
-- room_capacityにはマスターデータ生成時の仕様により揺らぎが存在する。
-- 顧客への定員表示は「保証できる最小値」を採用することで、
-- 実際の部屋の定員が表示より少ないというクレームを防止する。

SELECT
    r.reservation_id,
    r.check_in_date,
    r.check_out_date,
    h.hotel_name,
    rd.room_type_id,
    rt.room_type_name,
    MIN(rm.room_capacity) AS room_capacity,
    rd.room_count,
    rd.how_much
FROM
    reservations r
    INNER JOIN reservation_details rd ON r.reservation_id = rd.reservation_id
    INNER JOIN room_types rt ON rd.room_type_id = rt.room_type_id
    INNER JOIN hotels h ON rt.hotel_id = h.hotel_id
    INNER JOIN rooms rm ON rt.room_type_id = rm.room_type_id
WHERE
    r.reservation_id = /* reservationId */1
GROUP BY
    r.reservation_id,
    r.check_in_date,
    r.check_out_date,
    h.hotel_name,
    rd.room_type_id,
    rt.room_type_name,
    rd.room_count,
    rd.how_much
