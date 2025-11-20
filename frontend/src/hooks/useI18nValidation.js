import { useTranslation } from 'react-i18next';
import { useState, useCallback } from 'react';

/**
 * React i18nextベースのバリデーションフック
 * JavaのMessageSourceと同様の機能をReactで提供
 */
export const useI18nValidation = () => {
  const { t } = useTranslation(); // Spring BootのMessageSource.getMessage()と同等
  const [errors, setErrors] = useState({});

  // エラーをクリア
  const clearErrors = useCallback(() => {
    setErrors({});
  }, []);

  // 特定のフィールドのエラーをクリア
  const clearError = useCallback((field) => {
    setErrors((prev) => {
      const newErrors = { ...prev };
      delete newErrors[field];
      return newErrors;
    });
  }, []);

  // エラーを設定（メッセージキーまたは直接メッセージを受け入れ）
  const setError = useCallback(
    (field, messageKeyOrMessage, params = {}) => {
      // メッセージキーの場合はt()で翻訳、直接メッセージの場合はそのまま使用
      const errorMessage = messageKeyOrMessage.includes('.')
        ? t(messageKeyOrMessage, params) // MessageSource.getMessage(key, args, locale)と同等
        : messageKeyOrMessage; // 直接メッセージ

      setErrors((prev) => ({
        ...prev,
        [field]: errorMessage,
      }));
    },
    [t],
  );

  // 日付バリデーション（MessageSourceベース）
  const validateDates = useCallback(
    (checkIn, checkOut, initialCheckInDate, initialCheckOutDate) => {
      if (!checkIn || !checkOut) {
        return ''; // 空の場合はエラーなし（入力中の可能性）
      }

      const checkInDate = new Date(checkIn);
      const checkOutDate = new Date(checkOut);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      // 初期推奨日以前の選択をチェック
      if (initialCheckInDate) {
        const initialDate = new Date(initialCheckInDate);
        if (checkInDate < initialDate) {
          return t('validation.date.checkInPastDate'); // messages.propertiesのキー参照
        }
      }

      // 初期推奨チェックアウト日以前の選択をチェック
      if (initialCheckOutDate) {
        const initialOutDate = new Date(initialCheckOutDate);
        if (checkOutDate < initialOutDate) {
          return t('validation.date.checkOutPastDate');
        }
      }

      if (checkInDate < today) {
        return t('validation.date.checkInPastDate');
      }
      if (checkOutDate <= checkInDate) {
        return t('validation.date.checkOutBeforeCheckIn');
      }
      return '';
    },
    [t],
  );

  // フォームバリデーション
  const validateForm = useCallback(
    (formData) => {
      const newErrors = {};

      // 都道府県の必須チェック
      if (!formData.prefecture || formData.prefecture === '') {
        newErrors.prefecture = t('validation.form.prefectureRequired');
      }

      // 宿泊人数のチェック
      const guestCount = parseInt(formData.guestCount, 10);
      if (isNaN(guestCount) || guestCount < 1) {
        newErrors.guestCount = t('validation.form.guestCountMin');
      } else if (guestCount > 99) {
        newErrors.guestCount = t('validation.form.guestCountMax');
      }

      setErrors(newErrors);
      return Object.keys(newErrors).length === 0;
    },
    [t],
  );

  // APIエラーハンドリング（MessageSourceベース）
  const handleApiError = useCallback(
    (error, field = 'api') => {
      let messageKey;

      if (error.message?.includes('Network')) {
        messageKey = 'validation.api.networkError';
      } else if (error.message?.includes('500')) {
        messageKey = 'validation.api.serverError';
      } else {
        messageKey = 'validation.api.searchFailed';
      }

      const message = t(messageKey);
      setError(field, messageKey);
      return message;
    },
    [t, setError],
  );

  // メッセージ取得のヘルパー（MessageSource.getMessage()のラッパー）
  const getMessage = useCallback(
    (key, params = {}) => {
      return t(key, params);
    },
    [t],
  );

  return {
    errors,
    clearErrors,
    clearError,
    setError,
    validateDates,
    validateForm,
    handleApiError,
    getMessage,
    hasErrors: Object.keys(errors).length > 0,
    getError: (field) => errors[field] || '',
    // 追加のヘルパー関数
    getLabel: (key) => t(`labels.${key}`),
    getButton: (key) => t(`buttons.${key}`),
    getValidationMessage: (key) => t(`validation.${key}`),
  };
};
