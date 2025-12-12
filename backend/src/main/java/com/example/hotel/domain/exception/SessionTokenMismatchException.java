package com.example.hotel.domain.exception;

import lombok.Getter;

/**
 * セッショントークン不一致例外
 *
 * 異なるタブや端末からの操作を検知した場合にスローされます。
 * これは、同一予約に対して複数のセッションから同時に操作が
 * 行われようとした場合に発生します。
 */
@Getter
public class SessionTokenMismatchException extends RuntimeException {

  private final Integer reservationId;

  public SessionTokenMismatchException(String message, Integer reservationId) {
    super(message);
    this.reservationId = reservationId;
  }
}
