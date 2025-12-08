package com.example.hotel.presentation.dto.reservation;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 顧客情報リクエストDTO
 *
 * 仮予約確定時にフロントエンドから受信する顧客情報のデータ構造。
 * 予約者名、連絡先（電話番号またはEメール）、到着予定時刻を保持。
 *
 * 【DB制約】
 * 連絡先は電話番号・Eメールアドレスのいずれか一方が必須。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRequestDto {

  @NotBlank(message = "{validation.customer.firstName.required}")
  private String reserverFirstName;

  @NotBlank(message = "{validation.customer.lastName.required}")
  private String reserverLastName;

  private String phoneNumber;

  private String emailAddress;

  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
  private LocalTime arriveAt;

  /**
   * 連絡先（電話番号またはEメールアドレス）の必須チェック
   *
   * DB制約により、いずれか一方が必須。
   *
   * @return 電話番号またはEメールアドレスのいずれかが入力されている場合true
   */
  @AssertTrue(message = "{validation.customer.contact.required}")
  public boolean isContactProvided() {
    return !isBlank(phoneNumber) || !isBlank(emailAddress);
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
