import React from 'react';

/**
 * P-900 サーバーエラー画面
 * 予期せぬエラーが発生した場合に表示する。
 */
const ServerError = ({ onRetry }) => {
  return (
    <div className="container">
      <header>
        <h1>ホテル予約サイト</h1>
      </header>
      <main>
        <div className="error-page-message">
          <h2>サーバーエラー</h2>
          <p>大変申し訳ございません。</p>
          <p>サーバー側で予期せぬエラーが発生しました。</p>
          <p>
            時間をおいて再度お試しいただくか、トップページへお戻りください。
          </p>

          <div className="button-container center">
            {/* P-900要件: 「トップへ戻る」ボタン [cite: 189] */}
            <button type="button" className="btn btn-link" onClick={onRetry}>
              トップへ戻る
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default ServerError;
