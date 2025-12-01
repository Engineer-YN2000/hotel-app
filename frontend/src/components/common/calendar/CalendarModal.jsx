import React from 'react';
import { useTranslation } from 'react-i18next';
import './CalendarModal.css';

/**
 * C-016 カレンダーコンポーネント
 * (外見のみの実装)
 */
const CalendarModal = ({ onClose, target }) => {
  const { t } = useTranslation();

  // 要件 に従い、グレーアウトロジックは実装せず、外見のみとします。
  // (P-010/C-040の空き室グレーアウト表示)

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="calendar-modal" onClick={(e) => e.stopPropagation()}>
        <div
          className="calendar-header"
          aria-label={t('calendar.monthDisplay')}
        >
          <button
            type="button"
            className="nav-arrow"
            aria-label={t('calendar.previousMonth')}
          >
            &lt;
          </button>
          <span className="month-year">
            {t('calendar.monthYear', { year: 2025, month: 12 })}
          </span>
          <button
            type="button"
            className="nav-arrow"
            aria-label={t('calendar.nextMonth')}
          >
            &gt;
          </button>
        </div>
        <div
          className="calendar-grid"
          role="grid"
          aria-label={t('calendar.daysGrid')}
        >
          {/* ヘッダー */}
          <div className="day-header">{t('calendar.weekdays.sun')}</div>
          <div className="day-header">{t('calendar.weekdays.mon')}</div>
          <div className="day-header">{t('calendar.weekdays.tue')}</div>
          <div className="day-header">{t('calendar.weekdays.wed')}</div>
          <div className="day-header">{t('calendar.weekdays.thu')}</div>
          <div className="day-header">{t('calendar.weekdays.fri')}</div>
          <div className="day-header">{t('calendar.weekdays.sat')}</div>

          {/* 日付 (ダミー) */}
          <div className="day-cell day-other-month">30</div>
          <div className="day-cell">1</div>
          <div className="day-cell">2</div>
          <div className="day-cell">3</div>
          <div className="day-cell">4</div>
          <div className="day-cell">5</div>
          <div className="day-cell">6</div>
          <div className="day-cell">7</div>
          <div className="day-cell">8</div>
          <div className="day-cell">9</div>
          <div className="day-cell">10</div>
          <div className="day-cell">11</div>
          <div className="day-cell">12</div>
          <div className="day-cell">13</div>
          <div className="day-cell">14</div>
          <div className="day-cell">15</div>
          <div className="day-cell">16</div>
          <div className="day-cell">17</div>
          <div className="day-cell">18</div>
          <div className="day-cell">19</div>
          <div className="day-cell day-selected">20</div>
          <div className="day-cell day-selected">21</div>
          <div className="day-cell day-selected">22</div>
          <div className="day-cell">23</div>
          <div className="day-cell day-unavailable">24</div>
          <div className="day-cell day-unavailable">25</div>
          <div className="day-cell">26</div>
          <div className="day-cell">27</div>
          <div className="day-cell">28</div>
          <div className="day-cell">29</div>
          <div className="day-cell">30</div>
          <div className="day-cell day-unavailable">31</div>
          <div className="day-cell day-other-month">1</div>
          <div className="day-cell day-other-month">2</div>
          <div className="day-cell day-other-month">3</div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="close-button"
          aria-label={t('calendar.closeCalendar')}
        >
          {t('calendar.close')}
        </button>
      </div>
    </div>
  );
};

export default CalendarModal;
