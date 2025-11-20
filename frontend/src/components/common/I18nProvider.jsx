import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import './I18nProvider.css';

/**
 * i18next初期化チェック用コンポーネント
 * JavaのMessageSourceが完全に読み込まれるまで待機する機能と同等
 */
const I18nProvider = ({ children }) => {
  const { i18n, t } = useTranslation();
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const checkInitialization = () => {
      if (i18n.isInitialized) {
        setIsReady(true);
        return true;
      }
      return false;
    };

    if (!checkInitialization()) {
      const handleInitialized = () => {
        setIsReady(true);
      };

      i18n.on('initialized', handleInitialized);

      // フォールバック: 一定時間後に再チェック
      const timeoutId = setTimeout(() => {
        checkInitialization();
      }, 1000);

      return () => {
        i18n.off('initialized', handleInitialized);
        clearTimeout(timeoutId);
      };
    }
  }, [i18n]);

  if (!isReady) {
    return (
      <div className="i18n-loading-container">
        {t('messages.loading.initializing', '初期化中...')}
      </div>
    );
  }

  return children;
};

export default I18nProvider;
