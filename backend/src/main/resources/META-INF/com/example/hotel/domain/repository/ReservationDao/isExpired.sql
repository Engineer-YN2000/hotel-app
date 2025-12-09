SELECT
    COUNT(*) > 0
FROM
    reservations
WHERE
    reservation_id = /* reservationId */1
    AND reservation_status = /* tentativeStatus */10
    AND pending_limit_at < NOW()
