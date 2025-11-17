import React from 'react';
import './CalendarModal.css';

/**
 * C-016 カレンダーコンポーネント
 * (外見のみの実装)
 */
const CalendarModal = ({ onClose, target }) => {
  // 要件 に従い、グレーアウトロジックは実装せず、外見のみとします。
  // (P-010/C-040の空き室グレーアウト表示)

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="calendar-modal" onClick={(e) => e.stopPropagation()}>
        <div className="calendar-header" aria-label="カレンダーの月表示">
          <button type="button" className="nav-arrow" aria-label="前の月へ">
            &lt;
          </button>
          <span className="month-year">2025年 12月</span>
          <button type="button" className="nav-arrow" aria-label="次の月へ">
            &gt;
          </button>
        </div>
        <div className="calendar-grid" role="grid" aria-label="月の日付">
          {/* ヘッダー */}
          <div className="day-header">日</div>
          <div className="day-header">月</div>
          <div className="day-header">火</div>
          <div className="day-header">水</div>
          <div className="day-header">木</div>
          <div className="day-header">金</div>
          <div className="day-header">土</div>

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
          aria-label="カレンダーを閉じる"
        >
          閉じる
        </button>
      </div>
    </div>
  );
};

export default CalendarModal;
