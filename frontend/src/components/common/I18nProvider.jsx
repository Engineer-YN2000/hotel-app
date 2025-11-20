import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';

/**
 * i18next初期化チェック用コンポーネント
 * JavaのMessageSourceが完全に読み込まれるまで待機する機能と同等
 */
const I18nProvider = ({ children }) => {
  const { i18n, t } = useTranslation();
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    if (i18n.isInitialized) {
      setIsReady(true);
    } else {
      i18n.on('initialized', () => {
        setIsReady(true);
      });
    }
  }, [i18n]);

  if (!isReady) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          fontSize: '18px',
        }}
      >
        {t('messages.loading.initializing', '初期化中...')}
      </div>
    );
  }

  return children;
};

export default I18nProvider;
