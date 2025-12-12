import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

/**
 * 予約キャンセル処理用カスタムフック
 *
 * P-020（予約詳細入力）およびP-030（予約確認）で共通利用。
 * キャンセルAPI呼び出しとトップページへの遷移を行う。
 *
 * @param {string} reservationId - 予約ID
 * @param {string} accessToken - アクセストークン（HMAC-SHA256署名）
 * @param {string} sessionToken - セッショントークン（10分間有効、同時操作防止用）
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
  sessionToken,
  { confirmMessage, errorMessage },
) => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [isCancelling, setIsCancelling] = useState(false);

  const handleCancel = useCallback(async () => {
    if (!window.confirm(confirmMessage)) {
      return;
    }

    setIsCancelling(true);
    try {
      const res = await fetch(
        `/api/reservations/${reservationId}/cancel?token=${encodeURIComponent(accessToken)}&sessionToken=${encodeURIComponent(sessionToken)}`,
        {
          method: 'POST',
        },
      );

      if (res.ok) {
        // キャンセル成功 → トップページへ遷移
        navigate('/');
      } else if (res.status === 409) {
        // 409 Conflict: セッショントークン不一致（別タブ/端末からの操作検出）
        alert(t('reservation.common.sessionConflict'));
        navigate('/', { replace: true });
      } else {
        throw new Error('Failed to cancel reservation');
      }
    } catch (e) {
      console.error('Error cancelling reservation:', e);
      alert(errorMessage);
    } finally {
      setIsCancelling(false);
    }
  }, [
    reservationId,
    accessToken,
    sessionToken,
    confirmMessage,
    errorMessage,
    navigate,
    t,
  ]);

  return { isCancelling, handleCancel };
};

export default useCancelReservation;
