import { useState, useCallback } from 'react';
import { getDateMessage, getFormMessage, getApiMessage } from '../utils/validationMessages';

/**
 * バリデーション機能を提供するカスタムフック
 * メッセージの統一管理と再利用性を向上
 */
export const useValidation = () => {
  const [errors, setErrors] = useState({});

  // エラーをクリア
  const clearErrors = useCallback(() => {
    setErrors({});
  }, []);

  // 特定のフィールドのエラーをクリア
  const clearError = useCallback((field) => {
    setErrors(prev => {
      const newErrors = { ...prev };
      delete newErrors[field];
      return newErrors;
    });
  }, []);

  // エラーを設定
  const setError = useCallback((field, message) => {
    setErrors(prev => ({
      ...prev,
      [field]: message
    }));
  }, []);

  // 日付バリデーション
  const validateDates = useCallback((checkIn, checkOut, initialCheckInDate, initialCheckOutDate) => {
    if (!checkIn || !checkOut) {
      return ''; // 空の場合はエラーなし（入力中の可能性）
    }

    const checkInDate = new Date(checkIn);
    const checkOutDate = new Date(checkOut);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 初期推奨日以前の選択をチェック（HTML5 min属性との二重防御）
    if (initialCheckInDate) {
      const initialDate = new Date(initialCheckInDate);
      if (checkInDate < initialDate) {
        return getDateMessage('checkInPastDate');
      }
    }

    // 初期推奨チェックアウト日以前の選択をチェック
    if (initialCheckOutDate) {
      const initialOutDate = new Date(initialCheckOutDate);
      if (checkOutDate < initialOutDate) {
        return getDateMessage('checkOutPastDate');
      }
    }

    if (checkInDate < today) {
      return getDateMessage('checkInPastDate');
    }
    if (checkOutDate <= checkInDate) {
      return getDateMessage('checkOutBeforeCheckIn');
    }
    return '';
  }, []);

  // フォームバリデーション
  const validateForm = useCallback((formData) => {
    const newErrors = {};

    // 都道府県の必須チェック
    if (!formData.prefecture || formData.prefecture === '') {
      newErrors.prefecture = getFormMessage('prefectureRequired');
    }

    // 宿泊人数のチェック
    const guestCount = parseInt(formData.guestCount, 10);
    if (isNaN(guestCount) || guestCount < 1) {
      newErrors.guestCount = getFormMessage('guestCountMin');
    } else if (guestCount > 99) {
      newErrors.guestCount = getFormMessage('guestCountMax');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, []);

  // APIエラーハンドリング
  const handleApiError = useCallback((error, field = 'api') => {
    let message;

    if (error.message?.includes('Network')) {
      message = getApiMessage('networkError');
    } else if (error.message?.includes('500')) {
      message = getApiMessage('serverError');
    } else {
      message = getApiMessage('searchFailed');
    }

    setError(field, message);
    return message;
  }, [setError]);

  return {
    errors,
    clearErrors,
    clearError,
    setError,
    validateDates,
    validateForm,
    handleApiError,
    hasErrors: Object.keys(errors).length > 0,
    getError: (field) => errors[field] || '',
  };
};
