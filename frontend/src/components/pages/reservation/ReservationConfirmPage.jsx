import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ReservationSummary, ServerError } from '../../common';
import useReservation from '../../../hooks/useReservation';
import useCancelReservation from '../../../hooks/useCancelReservation';
import './ReservationConfirmPage.css';

/**
 * P-030 予約確認ページ
 * 予約内容と顧客情報を確認し、予約を確定する。
 */
const ReservationConfirmPage = () => {
  const { t } = useTranslation();
  const { reservationId } = useParams();
  const [searchParams] = useSearchParams();
  const accessToken = searchParams.get('token');
  const navigate = useNavigate();

  const { reservation, loading, errorState } = useReservation(
    reservationId,
    accessToken,
  );
  const { isCancelling, handleCancel } = useCancelReservation(
    reservationId,
    accessToken,
    {
      confirmMessage: t('reservation.confirmPage.confirmCancel'),
      errorMessage: t('reservation.confirmPage.cancelError'),
    },
  );
  const [isConfirming, setIsConfirming] = useState(false);

  /**
   * 予約確定ボタン押下時の処理
   * 予約ステータスをCONFIRMED（20）に更新後、完了ページへ遷移
   */
  const handleConfirm = async () => {
    setIsConfirming(true);
    try {
      const res = await fetch(
        `/api/reservations/${reservationId}/confirm?token=${encodeURIComponent(accessToken)}`,
        {
          method: 'POST',
        },
      );

      if (res.ok) {
        // P-040（予約完了）へ遷移
        navigate(`/reservation/${reservationId}/complete`);
      } else if (res.status === 410) {
        // 410 Gone: 予約期限切れ → P-910（SessionExpiredError）へ遷移
        navigate('/session-expired', { state: { reservationId, accessToken } });
      } else {
        throw new Error('Failed to confirm reservation');
      }
    } catch (e) {
      console.error('Error confirming reservation:', e);
      alert(t('reservation.confirmPage.confirmError'));
    } finally {
      setIsConfirming(false);
    }
  };

  /**
   * 戻るボタン押下時の処理
   * P-020（予約詳細入力）へ戻る
   */
  const handleBack = () => {
    navigate(
      `/reservation/${reservationId}?token=${encodeURIComponent(accessToken)}`,
    );
  };

  if (loading) {
    return (
      <div className="reservation-confirm-page">
        <div className="loading-container">
          {t('reservation.confirmPage.loading')}
        </div>
      </div>
    );
  }
  if (errorState === 'SERVER_ERROR') {
    return <ServerError />;
  }
  if (errorState === 'NOT_FOUND') {
    // 無効な予約IDまたはTENTATIVE以外のステータス → トップページへ
    navigate('/');
    return null;
  }

  const customerInfo = reservation?.customerInfo;

  // 名前の表示順序を言語設定に応じて決定
  const nameDisplayOrder = t('reservation.customerForm.nameDisplayOrder');
  const isFamilyFirst = nameDisplayOrder === 'familyFirst';
  const displayName = isFamilyFirst
    ? `${customerInfo?.reserverLastName} ${customerInfo?.reserverFirstName}`
    : `${customerInfo?.reserverFirstName} ${customerInfo?.reserverLastName}`;

  return (
    <div className="reservation-confirm-page">
      <header>
        <h1>{t('reservation.confirmPage.title')}</h1>
      </header>
      <main>
        <p className="instruction">
          {t('reservation.confirmPage.instruction')}
        </p>

        {/* 予約サマリー（共通コンポーネント） */}
        <ReservationSummary reservation={reservation} />

        {/* 宿泊者情報（確認用表示） */}
        <section className="customer-info-confirm">
          <h3>{t('reservation.confirmPage.customerInfoTitle')}</h3>
          <dl className="confirm-list">
            <dt>{t('reservation.confirmPage.guestName')}</dt>
            <dd>
              {displayName} {t('reservation.confirmPage.honorific')}
            </dd>

            <dt>{t('reservation.confirmPage.phoneNumber')}</dt>
            <dd>
              {customerInfo?.phoneNumber ||
                t('reservation.confirmPage.notRegistered')}
            </dd>

            <dt>{t('reservation.confirmPage.emailAddress')}</dt>
            <dd>
              {customerInfo?.emailAddress ||
                t('reservation.confirmPage.notRegistered')}
            </dd>

            <dt>{t('reservation.confirmPage.arriveAt')}</dt>
            <dd>{reservation.arriveAt.substring(0, 5)}</dd>
          </dl>
        </section>

        <div className="button-container">
          <button
            type="button"
            className="btn btn-danger"
            onClick={handleCancel}
            disabled={isCancelling || isConfirming}
          >
            {isCancelling
              ? t('reservation.confirmPage.cancellingButton')
              : t('reservation.confirmPage.cancelButton')}
          </button>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleBack}
            disabled={isCancelling || isConfirming}
          >
            {t('reservation.confirmPage.backButton')}
          </button>

          <button
            type="button"
            className="btn btn-primary btn-lg"
            onClick={handleConfirm}
            disabled={isConfirming || isCancelling}
          >
            {isConfirming
              ? t('reservation.confirmPage.confirmingButton')
              : t('reservation.confirmPage.confirmButton')}
          </button>
        </div>
      </main>
    </div>
  );
};

export default ReservationConfirmPage;
