UPDATE
    reservations
SET
    reservation_status = /* expiredStatus */40
WHERE
    reservation_id = /* reservationId */1
    AND reservation_status = /* tentativeStatus */10
    AND pending_limit_at <= NOW()
