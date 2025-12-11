UPDATE
    reservations
SET
    reservation_status = /* confirmedStatus */20
WHERE
    reservation_id = /* reservationId */1
    AND reservation_status = /* tentativeStatus */10
    AND pending_limit_at > NOW()
    AND reserver_id IS NOT NULL
