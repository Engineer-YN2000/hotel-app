import React from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import './ReservationCompletePage.css';

/**
 * P-040 予約完了ページ
 * 予約確定後に表示し、予約IDを案内する。
 */
const ReservationCompletePage = () => {
  const { t } = useTranslation();
  const { reservationId } = useParams();
  const navigate = useNavigate();

  const handleBackToTop = () => {
    navigate('/');
  };

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
