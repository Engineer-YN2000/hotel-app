import { useEffect, useState } from 'react';

/**
 * 予約データ取得用カスタムフック
 *
 * 指定された予約IDとアクセストークンに基づいて予約情報をAPIから取得する。
 * P-020（予約詳細入力）およびP-030（予約確認）で共通利用。
 *
 * 【トランザクション整合性について】
 * reservationIdとaccessTokenは仮予約作成時に1対1で生成され、
 * 以降の画面遷移（P-020→P-030→P-040）で不変のまま引き継がれる。
 * 同一コンポーネントインスタンス内でこれらが変更されるのは、
 * ユーザーがURL直打ちやブラウザ履歴操作を行った場合のみ。
 * これらは正規フロー外の操作であり、バックエンドでのトークン検証により
 * 不正アクセスは404として処理される。
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
      // 【状態リセット】reservationIdまたはaccessTokenが変更された場合に備え、
      // 前回の状態をクリアしてから新規取得を開始（防御的プログラミング）
      setReservation(null);
      setErrorState(null);
      setLoading(true);

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
