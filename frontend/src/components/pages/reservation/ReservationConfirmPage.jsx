import React from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import './ReservationConfirmPage.css';

/**
 * P-030 予約確認ページ（ダミー実装）
 *
 * 顧客情報入力完了後に遷移する確認ページ。
 * 本格実装時は予約情報の全詳細を表示し、最終確定ボタンを配置する。
 */
const ReservationConfirmPage = () => {
  const { t } = useTranslation();
  const { reservationId } = useParams();
  const navigate = useNavigate();

  const handleBack = () => {
    navigate(`/reservation/${reservationId}`);
  };

  const handleConfirm = () => {
    // TODO: 本格実装時は予約確定APIを呼び出す
    alert(t('reservation.confirmPage.confirmNotImplemented'));
  };

  return (
    <div className="reservation-confirm-page">
      <header>
        <h1>{t('reservation.confirmPage.title')}</h1>
      </header>
      <main>
        <div className="dummy-content">
          <div className="dummy-message">
            <p>{t('reservation.confirmPage.dummyMessage')}</p>
            <p className="reservation-id">
              {t('reservation.confirmPage.reservationId')}:{' '}
              <strong>{reservationId}</strong>
            </p>
          </div>
          <div className="button-container">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleBack}
            >
              {t('reservation.confirmPage.backButton')}
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleConfirm}
            >
              {t('reservation.confirmPage.confirmButton')}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default ReservationConfirmPage;
