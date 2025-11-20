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

  // 翻訳キーのプレフィックス定数（保守性と可読性のため）
  const TRANSLATION_KEY_PREFIXES = [
    'validation.',
    'messages.',
    'labels.',
    'buttons.',
    'app.',
  ];

  // エラーを設定（メッセージキーまたは直接メッセージを受け入れ）
  const setError = useCallback(
    (field, messageKeyOrMessage, params = {}) => {
      // 定義されたプレフィックスで翻訳キーか判定
      const isTranslationKey = TRANSLATION_KEY_PREFIXES.some((prefix) =>
        messageKeyOrMessage.startsWith(prefix),
      );

      const errorMessage = isTranslationKey
        ? t(messageKeyOrMessage, params) // MessageSource.getMessage(key, args, locale)と同等
        : messageKeyOrMessage; // 直接メッセージ

      setErrors((prev) => ({
        ...prev,
        [field]: errorMessage,
      }));
    },
    [t],
  ); // 日付バリデーション（MessageSourceベース）
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

      // HTTPステータスコードに基づく適切なエラー分類
      if (error.message?.includes('Network') || error.name === 'NetworkError') {
        messageKey = 'validation.api.networkError';
      } else if (error.status === 400 || error.message?.includes('400')) {
        // 構文エラー: リクエスト形式の問題
        messageKey = 'validation.api.invalidRequest';
      } else if (error.status === 404 || error.message?.includes('404')) {
        // リソース未発見: 存在しないエンドポイント
        messageKey = 'validation.api.notFound';
      } else if (error.status === 405 || error.message?.includes('405')) {
        // メソッド未許可: 不正なHTTPメソッド
        messageKey = 'validation.api.methodNotAllowed';
      } else if (error.status === 422 || error.message?.includes('422')) {
        // ビジネスルール違反: システムの整合性に反する値
        messageKey = 'validation.api.businessRuleViolation';
      } else if (error.status === 500 || error.message?.includes('500')) {
        // サーバーエラー: システム障害を示す（詳細は隠す）
        messageKey = 'validation.api.serverError';
      } else {
        // その他の予期しないエラー
        messageKey = 'validation.api.searchFailed';
      }

      const message = t(messageKey);
      setError(field, message); // 翻訳されたメッセージを使用
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
