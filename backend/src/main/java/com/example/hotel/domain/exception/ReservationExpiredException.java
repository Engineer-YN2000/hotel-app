package com.example.hotel.domain.exception;

/**
 * 仮予約の有効期限切れ例外
 *
 * 仮予約（TENTATIVE）の有効期限（pending_limit_at）が
 * 現在時刻を過ぎている場合にスローされる。
 *
 * 【プレゼンテーション層でのハンドリング】
 * この例外をキャッチして、期限切れ専用ページへの遷移を制御する。
 */
public class ReservationExpiredException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * 期限切れとなった予約ID
   */
  private final Integer reservationId;

  /**
   * 指定されたメッセージで例外を構築します。
   *
   * @param message エラーメッセージ
   * @param reservationId 期限切れとなった予約ID
   */
  public ReservationExpiredException(String message, Integer reservationId) {
    super(message);
    this.reservationId = reservationId;
  }

  /**
   * 期限切れとなった予約IDを取得します。
   *
   * @return 予約ID
   */
  public Integer getReservationId() {
    return reservationId;
  }
}
