import React from 'react';
import { useTranslation } from 'react-i18next';
import './ReservationSummary.css';

/**
 * C-021 予約サマリーコンポーネント
 * 予約内容（ホテル名、日程、部屋情報、合計金額）を表示する。
 */
const ReservationSummary = ({ reservation }) => {
  const { t } = useTranslation();

  if (!reservation) return null;

  return (
    <section className="reservation-summary-card">
      <h3>{t('reservation.summary.title')}</h3>
      <div className="summary-row">
        <span className="summary-label">
          {t('reservation.summary.hotelName')}
        </span>
        <span className="summary-value">{reservation.hotelName}</span>
      </div>
      <div className="summary-row">
        <span className="summary-label">
          {t('reservation.summary.schedule')}
        </span>
        <span className="summary-value">
          {reservation.checkInDate} 〜 {reservation.checkOutDate}
        </span>
      </div>
      <div className="summary-row">
        <span className="summary-label">
          {t('reservation.summary.roomAndCount')}
        </span>
        <div className="summary-value">
          {reservation.rooms.map((room, index) => (
            <div key={index}>
              {t('reservation.summary.roomDetail', {
                roomTypeName: room.roomTypeName,
                roomCapacity: room.roomCapacity,
                count: room.roomCount,
              })}
            </div>
          ))}
        </div>
      </div>
      <div className="summary-row total-price">
        <span className="summary-label">
          {t('reservation.summary.totalFee')}
        </span>
        <span className="summary-value">
          ¥{reservation.totalFee.toLocaleString()}
        </span>
      </div>
    </section>
  );
};

export default ReservationSummary;
