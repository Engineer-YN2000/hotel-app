/**
 * 多言語対応のための設定ファイル
 * 将来的な国際化対応のための基盤
 */

// サポートする言語の定義
export const SUPPORTED_LANGUAGES = {
  JA: 'ja',
  EN: 'en',
  KO: 'ko',
  ZH: 'zh',
};

// 現在の言語設定（将来的にはユーザー設定や自動検出に基づく）
export const getCurrentLanguage = () => {
  // ブラウザの言語設定を取得し、サポートする言語にフォールバック
  const browserLang = navigator.language.split('-')[0];
  return Object.values(SUPPORTED_LANGUAGES).includes(browserLang)
    ? browserLang
    : SUPPORTED_LANGUAGES.JA; // デフォルトは日本語
};

// 言語ファイルの動的インポート用の設定
export const getLanguageMessages = async (lang = getCurrentLanguage()) => {
  try {
    switch (lang) {
      case SUPPORTED_LANGUAGES.EN:
        return (await import('./messages/en')).default;
      case SUPPORTED_LANGUAGES.KO:
        return (await import('./messages/ko')).default;
      case SUPPORTED_LANGUAGES.ZH:
        return (await import('./messages/zh')).default;
      case SUPPORTED_LANGUAGES.JA:
      default:
        return (await import('./messages/ja')).default;
    }
  } catch (error) {
    console.warn(`言語ファイル読み込みエラー: ${lang}`, error);
    // フォールバックとして日本語を返す
    return (await import('./messages/ja')).default;
  }
};

// 言語切り替え用のヘルパー関数（将来使用）
export const switchLanguage = (newLang) => {
  if (Object.values(SUPPORTED_LANGUAGES).includes(newLang)) {
    localStorage.setItem('selectedLanguage', newLang);
    // アプリケーション全体の再レンダリングをトリガー
    window.dispatchEvent(new CustomEvent('languageChange', { detail: newLang }));
    return true;
  }
  return false;
};
