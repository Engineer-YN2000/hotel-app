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
        <div className="calendar-header">
          <button className="nav-arrow">&lt;</button>
          <span>2025年 12月</span>
          <button className="nav-arrow">&gt;</button>
        </div>
        <div className="calendar-grid">
          {/* ヘッダー */}
          <div className="day-header">日</div>
          <div className="day-header">月</div>
          <div className="day-header">火</div>
          <div className="day-header">水</div>
          <div className="day-header">木</div>
          <div className="day-header">金</div>
          <div className="day-header">土</div>

          {/* 日付 (ダミー) */}
          <div className="day prev-month">30</div>
          <div className="day">1</div>
          <div className="day">2</div>
          <div className="day">3</div>
          <div className="day">4</div>
          <div className="day">5</div>
          <div className="day">6</div>
          <div className="day">7</div>
          <div className="day">8</div>
          <div className="day">9</div>
          <div className="day">10</div>
          <div className="day">11</div>
          <div className="day">12</div>
          <div className="day">13</div>
          <div className="day">14</div>
          <div className="day">15</div>
          <div className="day">16</div>
          <div className="day">17</div>
          <div className="day">18</div>
          <div className="day">19</div>
          <div className="day selected">20</div>
          <div className="day selected-start">21</div>
          <div className="day selected-end">22</div>
          <div className="day">23</div>
          <div className="day disabled">24</div>
          <div className="day disabled">25</div>
          <div className="day">26</div>
          <div className="day">27</div>
          <div className="day">28</div>
          <div className="day">29</div>
          <div className="day">30</div>
          <div className="day disabled">31</div>
          <div className="day next-month">1</div>
          <div className="day next-month">2</div>
          <div className="day next-month">3</div>
        </div>
        <button onClick={onClose} className="close-button">
          閉じる
        </button>
      </div>
    </div>
  );
};

export default CalendarModal;
