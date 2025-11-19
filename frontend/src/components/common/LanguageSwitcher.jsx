import React from 'react';
import { useTranslation } from 'react-i18next';
import './LanguageSwitcher.css';

/**
 * 言語切り替えコンポーネント（Java MessageSourceと連携）
 */
const LanguageSwitcher = () => {
  const { i18n } = useTranslation();

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  return (
    <div className="language-switcher">
      <button
        onClick={() => changeLanguage('ja')}
        className={i18n.language === 'ja' ? 'active' : ''}
      >
        日本語
      </button>
      <button
        onClick={() => changeLanguage('en')}
        className={i18n.language === 'en' ? 'active' : ''}
      >
        English
      </button>
    </div>
  );
};

export default LanguageSwitcher;
