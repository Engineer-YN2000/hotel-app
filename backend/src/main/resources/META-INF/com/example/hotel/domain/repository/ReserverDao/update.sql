UPDATE
    reservers
SET
    reserver_first_name = /* reserver.reserverFirstName */'John',
    reserver_last_name = /* reserver.reserverLastName */'Doe',
    phone_number = /* reserver.phoneNumber */'+1234567890',
    e_mail_address = /* reserver.emailAddress */'test@example.com'
WHERE
    reserver_id = /* reserver.reserverId */1
