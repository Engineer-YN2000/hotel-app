import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useLocation } from 'react-router-dom';
import './SessionExpiredError.css';

/**
 * P-910 予約有効時間切れ画面
 * 仮予約の有効期限が経過した場合に表示する。
 * トップページへ戻る際に予約ステータスをCANCELLEDに更新する。
 */
const SessionExpiredError = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const reservationId = location.state?.reservationId;

  const handleBackToTop = async () => {
    // 予約IDがある場合はキャンセルAPIを呼び出してステータスをCANCELLEDに更新
    if (reservationId) {
      try {
        await fetch(`/api/reservations/${reservationId}/cancel`, {
          method: 'POST',
        });
      } catch (e) {
        // キャンセル失敗してもトップページへは遷移する
        console.error('Failed to cancel reservation:', e);
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
