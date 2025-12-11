import { useEffect, useState } from 'react';

/**
 * 予約データ取得用カスタムフック
 *
 * 指定された予約IDとアクセストークンに基づいて予約情報をAPIから取得する。
 * P-020（予約詳細入力）およびP-030（予約確認）で共通利用。
 *
 * @param {string} reservationId - 予約ID
 * @param {string} accessToken - アクセストークン（HMAC-SHA256署名）
 * @returns {{
 *   reservation: object|null,
 *   loading: boolean,
 *   errorState: 'NOT_FOUND'|'SERVER_ERROR'|null
 * }}
 */
const useReservation = (reservationId, accessToken) => {
  const [reservation, setReservation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);

  useEffect(() => {
    const fetchReservation = async () => {
      // トークンがない場合は即座にNOT_FOUND
      if (!accessToken) {
        setErrorState('NOT_FOUND');
        setLoading(false);
        return;
      }

      try {
        const res = await fetch(
          `/api/reservations/${reservationId}?token=${encodeURIComponent(accessToken)}`,
        );
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
  }, [reservationId, accessToken]);

  return { reservation, loading, errorState };
};

export default useReservation;
