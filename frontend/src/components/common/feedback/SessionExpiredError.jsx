import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useLocation } from 'react-router-dom';
import './SessionExpiredError.css';

/**
 * P-910 予約有効時間切れ画面
 * 仮予約の有効期限が経過した場合に表示する。
 * トップページへ戻る際に予約ステータスをEXPIRED（40）に更新する。
 */
const SessionExpiredError = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const reservationId = location.state?.reservationId;
  const accessToken = location.state?.accessToken;

  const handleBackToTop = async () => {
    // 予約IDとトークンがある場合はexpire APIを呼び出してステータスをEXPIREDに更新
    if (reservationId && accessToken) {
      try {
        const res = await fetch(
          `/api/reservations/${reservationId}/expire?token=${encodeURIComponent(accessToken)}`,
          {
            method: 'POST',
          }
        );
        if (!res.ok) {
          // エラー時もログのみ（ベストエフォート処理、バッチが最終保証）
          console.error('Failed to expire reservation: status=', res.status);
        }
      } catch (e) {
        // ネットワークエラー等 - ログのみ（ベストエフォート処理）
        console.error('Network error while expiring reservation:', e);
      }
    }
    navigate('/');
  };

  return (
    <div className="container">
      <header>
        <h1>{t('app.title')}</h1>
      </header>
      <main>
        <div className="timeout-error-page-message">
          <h2>{t('messages.sessionExpired.title')}</h2>
          <p>{t('messages.error.apologyMessage')}</p>
          <p>{t('messages.sessionExpired.timeoutMessage')}</p>
          <p>{t('messages.sessionExpired.retryMessage')}</p>

          <div className="button-container center">
            <button
              type="button"
              className="btn btn-link"
              onClick={handleBackToTop}
            >
              {t('buttons.backToTop')}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default SessionExpiredError;
