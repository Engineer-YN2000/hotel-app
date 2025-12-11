import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import './ReservationCompletePage.css';

/**
 * P-040 予約完了ページ
 * 予約確定後に表示し、予約IDを案内する。
 * P-030からの正規遷移でのみアクセス可能（直接URL入力は不可）
 */
const ReservationCompletePage = () => {
  const { t } = useTranslation();
  const { reservationId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  // P-030からの正規遷移かどうかをチェック
  const fromConfirm = location.state?.fromConfirm;

  useEffect(() => {
    // 直接URLアクセスの場合はトップページへリダイレクト
    if (!fromConfirm) {
      navigate('/', { replace: true });
    }
  }, [fromConfirm, navigate]);

  const handleBackToTop = () => {
    navigate('/');
  };

  // 直接アクセス時はリダイレクト中なので何も表示しない
  if (!fromConfirm) {
    return null;
  }

  return (
    <div className="reservation-complete-page">
      <header>
        <h1>{t('reservation.completePage.title')}</h1>
      </header>
      <main>
        <div className="success-message-box">
          <div className="success-icon">✓</div>
          <h2>{t('reservation.completePage.thankYou')}</h2>
          <p>{t('reservation.completePage.confirmedMessage')}</p>
          <p className="reservation-id-display">
            {t('reservation.completePage.reservationId')}:{' '}
            <strong>{reservationId}</strong>
          </p>
        </div>

        <div className="button-container">
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleBackToTop}
          >
            {t('buttons.backToTop')}
          </button>
        </div>
      </main>
    </div>
  );
};

export default ReservationCompletePage;
