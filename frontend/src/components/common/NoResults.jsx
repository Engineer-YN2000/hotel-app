import React from 'react';
import { useTranslation } from 'react-i18next';

/**
 * C-040 検索結果なし (状態 1-C)
 */
const NoResults = () => {
  const { t } = useTranslation();

  return (
    <section className="no-results-message">
      <p>{t('messages.noResults.title')}</p>
      <p>{t('messages.noResults.subtitle')}</p>
    </section>
  );
};

export default NoResults;
