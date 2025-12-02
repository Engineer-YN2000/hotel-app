import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import {
  commonMessages,
  calendarMessages,
  feedbackMessages,
  searchMessages,
  topPageMessages,
} from './messages';

/**
 * React i18next配置ファイル
 * JavaのMessageSourceやSpring BootのThymeleafテンプレートエンジンの
 * messages.propertiesファイルと同等の国際化機能を提供
 */

/**
 * メッセージをディープマージするユーティリティ関数
 * @param {Object} target - マージ先オブジェクト
 * @param {Object} source - マージ元オブジェクト
 * @returns {Object} マージされたオブジェクト
 */
const deepMerge = (target, source) => {
  const result = { ...target };
  for (const key of Object.keys(source)) {
    if (
      source[key] &&
      typeof source[key] === 'object' &&
      !Array.isArray(source[key])
    ) {
      result[key] = deepMerge(result[key] || {}, source[key]);
    } else {
      result[key] = source[key];
    }
  }
  return result;
};

/**
 * コンポーネント別メッセージをマージして言語リソースを構築
 */
const buildResources = () => {
  const messageModules = [
    commonMessages,
    calendarMessages,
    feedbackMessages,
    searchMessages,
    topPageMessages,
  ];

  const resources = {
    ja: { translation: {} },
    en: { translation: {} },
  };

  for (const module of messageModules) {
    if (module.ja) {
      resources.ja.translation = deepMerge(resources.ja.translation, module.ja);
    }
    if (module.en) {
      resources.en.translation = deepMerge(resources.en.translation, module.en);
    }
  }

  return resources;
};

const resources = buildResources();

i18n
  // ブラウザの言語を自動検出（JavaのLocaleResolver相当）
  .use(LanguageDetector)
  // React i18nextとの連携
  .use(initReactI18next)
  // 初期化
  .init({
    resources,

    // デフォルト言語（Spring Bootのspring.messages.basename相当）
    fallbackLng: 'ja',

    // デバッグモード（開発時のみ）
    debug: process.env.NODE_ENV === 'development',

    // 補間設定（MessageSourceのパラメータ置換相当）
    interpolation: {
      escapeValue: false, // Reactは既にXSS対策済み
    },

    // React Suspenseサポート
    react: {
      useSuspense: true,
    },

    // 言語検出設定
    detection: {
      // 検出順序（localStorage → navigator → fallback）
      order: ['localStorage', 'navigator', 'htmlTag'],
      // ローカルストレージのキー名
      lookupLocalStorage: 'i18nextLng',
      // キャッシュ設定
      caches: ['localStorage'],
    },
  });

export default i18n;
