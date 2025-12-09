UPDATE
    reservations
SET
    reserver_id = /* reserverId */1,
    arrive_at = /* arriveAt */'15:00:00'
WHERE
    reservation_id = /* reservationId */1
    AND reservation_status = /* tentativeStatus */10
    AND pending_limit_at > NOW()
