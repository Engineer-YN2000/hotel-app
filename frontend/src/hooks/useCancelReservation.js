import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * 予約キャンセル処理用カスタムフック
 *
 * P-020（予約詳細入力）およびP-030（予約確認）で共通利用。
 * キャンセルAPI呼び出しとトップページへの遷移を行う。
 *
 * @param {string} reservationId - 予約ID
 * @param {string} accessToken - アクセストークン（HMAC-SHA256署名）
 * @param {object} options - オプション
 * @param {string} options.confirmMessage - 確認ダイアログのメッセージ
 * @param {string} options.errorMessage - エラー時のアラートメッセージ
 * @returns {{
 *   isCancelling: boolean,
 *   handleCancel: () => Promise<void>
 * }}
 */
const useCancelReservation = (
  reservationId,
  accessToken,
  { confirmMessage, errorMessage },
) => {
  const navigate = useNavigate();
  const [isCancelling, setIsCancelling] = useState(false);

  const handleCancel = useCallback(async () => {
    if (!window.confirm(confirmMessage)) {
      return;
    }

    setIsCancelling(true);
    try {
      const res = await fetch(
        `/api/reservations/${reservationId}/cancel?token=${encodeURIComponent(accessToken)}`,
        {
          method: 'POST',
        },
      );

      if (res.ok) {
        // キャンセル成功 → トップページへ遷移
        navigate('/');
      } else {
        throw new Error('Failed to cancel reservation');
      }
    } catch (e) {
      console.error('Error cancelling reservation:', e);
      alert(errorMessage);
    } finally {
      setIsCancelling(false);
    }
  }, [reservationId, accessToken, confirmMessage, errorMessage, navigate]);

  return { isCancelling, handleCancel };
};

export default useCancelReservation;
