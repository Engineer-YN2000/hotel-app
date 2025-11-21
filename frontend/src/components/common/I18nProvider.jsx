import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import './I18nProvider.css';

/**
 * i18next初期化チェック用コンポーネント
 * JavaのMessageSourceが完全に読み込まれるまで待機する機能と同等
 */
const I18nProvider = ({ children }) => {
  // タイムアウト設定定数
  const INITIALIZATION_TIMEOUT_MS = 1000;
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
      }, INITIALIZATION_TIMEOUT_MS);

      return () => {
        i18n.off('initialized', handleInitialized);
        clearTimeout(timeoutId);
      };
    }

    // i18nが既に初期化されている場合でもクリーンアップ関数を返す
    return () => {
      // 既に初期化済みの場合は何もしない
    };
  }, [i18n, INITIALIZATION_TIMEOUT_MS]);

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
