import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ReservationSummary,
  CustomerInputForm,
  ServerError,
} from '../../common';
import './ReservationInputPage.css';

/**
 * P-020 予約詳細入力ページ
 * 予約サマリーと顧客情報入力フォームを表示する。
 */
const ReservationInputPage = () => {
  const { t } = useTranslation();
  const { reservationId } = useParams();
  const navigate = useNavigate();

  const [reservation, setReservation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);
  const [isCancelling, setIsCancelling] = useState(false);

  useEffect(() => {
    const fetchReservation = async () => {
      try {
        const res = await fetch(`/api/reservations/${reservationId}`);
        if (res.ok) {
          const data = await res.json();
          setReservation(data);
        } else if (res.status === 404) {
          setErrorState('NOT_FOUND');
        } else {
          throw new Error('Server Error');
        }
      } catch (e) {
        console.error(e);
        setErrorState('SERVER_ERROR');
      } finally {
        setLoading(false);
      }
    };
    fetchReservation();
  }, [reservationId]);

  const handleSubmitCustomerInfo = async (formData) => {
    try {
      const res = await fetch(
        `/api/reservations/${reservationId}/customer-info`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(formData),
        },
      );

      if (res.ok) {
        console.log(t('reservation.inputPage.processingComplete'));
        // P-030（確認画面）へ遷移
        navigate(`/reservation/${reservationId}/confirm`);
      } else if (res.status === 410) {
        // 410 Gone: 予約期限切れ → P-910（SessionExpiredError）へ遷移
        // キャンセル処理のためreservationIdを渡す
        navigate('/session-expired', { state: { reservationId } });
      } else {
        // その他のエラー → ServerErrorページへ
        setErrorState('SERVER_ERROR');
      }
    } catch (e) {
      console.error(e);
      alert(t('reservation.inputPage.networkError'));
    }
  };

  /**
   * キャンセルボタン押下時の処理
   * 予約ステータスをCANCELLED（30）に更新後、トップページへ遷移
   */
  const handleCancel = async () => {
    if (!window.confirm(t('reservation.inputPage.confirmCancel'))) {
      return;
    }

    setIsCancelling(true);
    try {
      const res = await fetch(`/api/reservations/${reservationId}/cancel`, {
        method: 'POST',
      });

      if (res.ok) {
        // キャンセル成功 → トップページへ遷移
        navigate('/');
      } else if (res.status === 404) {
        // 予約が見つからない場合もトップページへ遷移
        console.warn('Reservation not found, navigating to top page');
        navigate('/');
      } else {
        throw new Error('Failed to cancel reservation');
      }
    } catch (e) {
      console.error('Error cancelling reservation:', e);
      alert(t('reservation.inputPage.cancelError'));
    } finally {
      setIsCancelling(false);
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
    // 無効な予約IDまたはTENTATIVE以外のステータス → トップページへ
    navigate('/');
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
          initialData={reservation?.customerInfo}
        />
      </main>
    </div>
  );
};

export default ReservationInputPage;
