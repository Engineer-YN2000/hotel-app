import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import {
  ReservationSummary,
  CustomerInputForm,
  ServerError,
} from '../../common';
import useReservation from '../../../hooks/useReservation';
import useCancelReservation from '../../../hooks/useCancelReservation';
import './ReservationInputPage.css';

/**
 * P-020 予約詳細入力ページ
 * 予約サマリーと顧客情報入力フォームを表示する。
 */
const ReservationInputPage = () => {
  const { t } = useTranslation();
  const { reservationId } = useParams();
  const [searchParams] = useSearchParams();
  const accessToken = searchParams.get('token');
  const sessionToken = searchParams.get('sessionToken');
  const navigate = useNavigate();

  const { reservation, loading, errorState } = useReservation(
    reservationId,
    accessToken,
  );
  const { isCancelling, handleCancel } = useCancelReservation(
    reservationId,
    accessToken,
    sessionToken,
    {
      confirmMessage: t('reservation.inputPage.confirmCancel'),
      errorMessage: t('reservation.inputPage.cancelError'),
    },
  );

  // 無効な予約IDまたはTENTATIVE以外のステータス → トップページへリダイレクト
  useEffect(() => {
    if (errorState === 'NOT_FOUND') {
      navigate('/', { replace: true });
    }
  }, [errorState, navigate]);

  const handleSubmitCustomerInfo = async (formData) => {
    try {
      const res = await fetch(
        `/api/reservations/${reservationId}/customer-info?token=${encodeURIComponent(accessToken)}&sessionToken=${encodeURIComponent(sessionToken)}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(formData),
        },
      );

      if (res.ok) {
        console.log(t('reservation.inputPage.processingComplete'));
        // P-030（確認画面）へ遷移（トークンを引き継ぐ）
        navigate(
          `/reservation/${reservationId}/confirm?token=${encodeURIComponent(accessToken)}&sessionToken=${encodeURIComponent(sessionToken)}`,
        );
      } else if (res.status === 409) {
        // 409 Conflict: セッショントークン不一致（別タブ/端末からの操作検出）
        alert(t('reservation.inputPage.sessionConflict'));
        navigate('/', { replace: true });
      } else if (res.status === 410) {
        // 410 Gone: 予約期限切れ → P-910（SessionExpiredError）へ遷移
        // キャンセル処理のためreservationIdとaccessTokenを渡す
        navigate('/session-expired', { state: { reservationId, accessToken } });
      } else {
        // その他のエラー → ServerErrorページへ
        navigate('/server-error');
      }
    } catch (e) {
      console.error(e);
      alert(t('reservation.inputPage.networkError'));
    }
  };

  if (loading) {
    return (
      <div className="reservation-input-page">
        <div className="loading-container">
          {t('reservation.inputPage.loading')}
        </div>
      </div>
    );
  }
  if (errorState === 'SERVER_ERROR') {
    return <ServerError />;
  }
  if (errorState === 'NOT_FOUND') {
    // useEffectでリダイレクト中
    return null;
  }

  return (
    <div className="reservation-input-page">
      <header>
        <h1>{t('reservation.inputPage.title')}</h1>
      </header>
      <main>
        <ReservationSummary reservation={reservation} />
        <CustomerInputForm
          onSubmit={handleSubmitCustomerInfo}
          onCancel={handleCancel}
          isCancelling={isCancelling}
          initialData={{
            ...reservation?.customerInfo,
            arriveAt: reservation?.arriveAt,
          }}
        />
      </main>
    </div>
  );
};

export default ReservationInputPage;
