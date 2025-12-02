import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import './ServerError.css';

/**
 * P-900 サーバーエラー画面
 * 予期せぬエラーが発生した場合に表示する。
 */
const ServerError = ({ onRetry }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const handleBackToTop = () => {
    if (onRetry) {
      onRetry();
    } else {
      navigate('/');
    }
  };

  return (
    <div className="container">
      <header>
        <h1>{t('app.title')}</h1>
      </header>
      <main>
        <div className="error-page-message">
          <h2>{t('messages.error.serverErrorTitle')}</h2>
          <p>{t('messages.error.apologyMessage')}</p>
          <p>{t('messages.error.unexpectedError')}</p>
          <p>{t('messages.error.retryOrBackToTop')}</p>

          <div className="button-container center">
            {/* P-900要件: 「トップへ戻る」ボタン [cite: 189] */}
            <button
              type="button"
              className="btn btn-link"
              onClick={handleBackToTop}
            >
              {t('buttons.backToTop')}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default ServerError;
