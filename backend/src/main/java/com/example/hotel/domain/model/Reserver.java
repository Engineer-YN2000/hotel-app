package com.example.hotel.domain.model;

import org.seasar.doma.*;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * 予約者エンティティ データベースのreserversテーブルに対応
 */
@Entity(immutable = true)
@Table(name = "reservers")
@Value
@AllArgsConstructor
public class Reserver {
  @Id
  @GeneratedValue(strategy = org.seasar.doma.GenerationType.IDENTITY)
  @Column(name = "reserver_id")
  private final Integer reserverId;

  @Column(name = "reserver_first_name")
  private final String reserverFirstName;

  @Column(name = "reserver_last_name")
  private final String reserverLastName;

  @Column(name = "phone_number")
  private final String phoneNumber;

  @Column(name = "e_mail_address")
  private final String emailAddress;
}
