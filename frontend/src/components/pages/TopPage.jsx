import React, { useState } from 'react';
import CalendarModal from '../common/CalendarModal'; // C-016
import './TopPage.css'; // CSSは分割

/**
 * P-010 ホテル検索ページ (初期状態 1-A)
 * (flowchart_top_page.dot)
 */
const TopPage = () => {
  // 状態定義 (1-A)
  const [showCalendar, setShowCalendar] = useState(null); // 'checkin' or 'checkout'
  const [showRefineForm, setShowRefineForm] = useState(false); // C-020
  const [showResults, setShowResults] = useState(false); // C-030
  const [showNoResults, setShowNoResults] = useState(false); // C-040

  const handleSearch = (e) => {
    e.preventDefault();
    // flowchart_top_page.dot の「DB検索」を実行
    console.log('「検索」ボタンをクリック');
    // TODO: API通信 (search_database)

    // この後のステップで、結果に応じて setShowResults や setShowNoResults を更新します
  };

  return (
    <div className="container">
      <header>
        <h1>ホテル予約サイト</h1>
      </header>

      <main>
        {/* P-010 ホテル検索 (1-A) */}
        <section className="search-section">
          <h2>ホテル検索</h2>

          {/* C-010: 検索フォーム */}
          <form className="search-form" onSubmit={handleSearch}>
            {/* C-011: チェックイン */}
            <div className="form-group">
              <label htmlFor="checkin">チェックイン予定日</label>
              <input
                type="text"
                id="checkin"
                placeholder="年 /月 /日"
                onFocus={() => setShowCalendar('checkin')}
              />
            </div>

            {/* C-012: チェックアウト */}
            <div className="form-group">
              <label htmlFor="checkout">チェックアウト予定日</label>
              <input
                type="text"
                id="checkout"
                placeholder="年 /月 /日"
                onFocus={() => setShowCalendar('checkout')}
              />
            </div>

            {/* C-013: 地域選択 */}
            <div className="form-group">
              <label htmlFor="area">宿泊予定の地域</label>
              <select id="area" defaultValue="">
                <option value="" disabled>
                  選択してください
                </option>
                <option value="tokyo">東京</option>
                <option value="osaka">大阪</option>
                {/* TODO: DBから取得 */}
              </select>
            </div>

            {/* C-014: 人数選択 */}
            <div className="form-group">
              <label htmlFor="guests">宿泊予定人数（合計）</label>
              <input type="number" id="guests" defaultValue="1" min="1" />
            </div>

            {/* C-015: 検索ボタン */}
            <div className="form-actions">
              <button type="submit" className="search-button">
                検索する
              </button>
            </div>
          </form>
        </section>

        {/* 絞り込みフォーム (C-020) - 初期状態非表示 */}
        {showRefineForm && (
          <section className="refine-section">
            {/* TODO: P-010 (1-B) の実装 */}
          </section>
        )}

        {/* 検索結果 (C-030) - 初期状態非表示 */}
        {showResults && (
          <section className="results-section">
            {/* TODO: P-010 (1-B) の実装 */}
          </section>
        )}

        {/* 結果なし (C-040) - 初期状態非表示 */}
        {showNoResults && (
          <section className="no-results-section">
            {/* TODO: P-010 (1-C) の実装 */}
          </section>
        )}

        {/* カレンダーモーダル (C-016) */}
        {showCalendar && (
          <CalendarModal
            onClose={() => setShowCalendar(null)}
            target={showCalendar}
          />
        )}
      </main>
    </div>
  );
};

export default TopPage;
