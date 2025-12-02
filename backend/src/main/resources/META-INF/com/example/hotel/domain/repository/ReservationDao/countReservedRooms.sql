SELECT
    COALESCE(SUM(rd.room_count), 0)
FROM
    reservation_details rd
JOIN
    reservations res ON rd.reservation_id = res.reservation_id
WHERE
    rd.room_type_id = /* roomTypeId */1
    AND res.reservation_status IN /* reservedStatuses */(10, 20)
    AND res.check_out_date > /* checkInDate */'2025-01-01'
    AND res.check_in_date < /* checkOutDate */'2025-01-02'
