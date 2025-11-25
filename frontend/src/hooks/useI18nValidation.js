import { useTranslation } from 'react-i18next';
import { useState, useCallback } from 'react';
import {
  GUEST_COUNT_CONSTRAINTS,
  HTTP_STATUS,
  TRANSLATION_KEY_PREFIXES,
} from '../shared-constants';

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
  );

  /**
   * 複合日付バリデーション - 多層防御とUX配慮の実装
   *
   * 【バリデーション階層】
   * 1. 入力完了性チェック（空値許可でUX配慮）
   * 2. 推奨日付範囲チェック（ビジネス推奨範囲）
   * 3. 基本ルールチェック（過去日禁止、日付順序）
   *
   * 【Date オブジェクトの注意点】
   * - new Date(dateString): ローカルタイムゾーンで解釈
   * - setHours(0,0,0,0): 時刻部分を0時0分0秒0ミリ秒に正規化
   * - 日付比較では時刻部分の違いが結果に影響するため正規化が必須
   *
   * 【優先順位設計】
   * 推奨日付チェック > 基本ルールチェック の順序で、
   * より具体的なエラーメッセージを優先表示
   *
   * 【パフォーマンス考慮】
   * useCallback + 依存配列 [t] により、翻訳関数変更時のみ再生成
   */
  const validateDates = useCallback(
    (checkIn, checkOut, initialCheckInDate, initialCheckOutDate) => {
      // 【第1層】入力完了性チェック - UX配慮で入力途中は許可
      if (!checkIn || !checkOut) {
        return ''; // 空値はエラーなし（ユーザーが入力中の可能性）
      }

      // 【Date正規化】時刻部分を0時に統一して日付のみで比較
      const checkInDate = new Date(checkIn);
      const checkOutDate = new Date(checkOut);
      const today = new Date();
      today.setHours(0, 0, 0, 0); // 時刻正規化（必須）

      // 【第2層】推奨日付範囲チェック - ビジネス要件による制約
      if (initialCheckInDate) {
        const initialDate = new Date(initialCheckInDate);
        if (checkInDate < initialDate) {
          return t('validation.date.checkInPastDate');
        }
      }

      if (initialCheckOutDate) {
        const initialOutDate = new Date(initialCheckOutDate);
        if (checkOutDate < initialOutDate) {
          return t('validation.date.checkOutPastDate');
        }
      }

      // 【第3層】基本ルールチェック - 汎用的な日付制約
      if (checkInDate < today) {
        return t('validation.date.checkInPastDate');
      }
      if (checkOutDate <= checkInDate) {
        return t('validation.date.checkOutBeforeCheckIn');
      }
      return ''; // バリデーション成功
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
      if (
        isNaN(guestCount) ||
        guestCount < GUEST_COUNT_CONSTRAINTS.MIN
      ) {
        newErrors.guestCount = t('validation.form.guestCountMin');
      } else if (guestCount > GUEST_COUNT_CONSTRAINTS.MAX) {
        newErrors.guestCount = t('validation.form.guestCountMax');
      }

      setErrors(newErrors);
      return Object.keys(newErrors).length === 0;
    },
    [t],
  );

  /**
   * HTTPエラーハンドリング - セキュリティ考慮の分類処理
   *
   * 【設計原則】
   * HTTPステータスコードを適切に分類し、クライアントエラーとサーバーエラーを区別。
   * セキュリティを考慮した情報の開示レベルをコントロール。
   *
   * 【分類基準】
   * - Client Error (4xx): ユーザーが修正可能なエラー → 具体的なメッセージ表示
   * - Server Error (5xx): システム管理者が対応すべきエラー → 汎用メッセージで詳細隠蔽
   * - Network Error: インフラ障害 → サーバーエラー扱い
   *
   * 【セキュリティ考慮事項】
   * 1. サーバーエラーの詳細情報漏洩防止
   * 2. システム内部構造の推測防止
   * 3. 攻撃者への有用な情報提供回避
   *
   * 【実装パターン】
   * - error.status（数値）とerror.message（文字列）の両方でチェック
   * - Optional Chaining (?.) による安全なプロパティアクセス
   * - messageKey/errorType の組み合わせで後続処理を制御
   */
  const handleApiError = useCallback(
    (error, field = 'api') => {
      let messageKey;
      let errorType;

      // 【エラー分類ロジック】HTTPステータス＋メッセージ内容による判定
      if (error.message?.includes('Network') || error.name === 'NetworkError') {
        messageKey = 'validation.api.networkError';
        errorType = 'server'; // ネットワークエラーはインフラ問題としてサーバーエラー扱い
      } else if (
        error.status === HTTP_STATUS.BAD_REQUEST ||
        error.message?.includes('400')
      ) {
        // 【4xx系】構文エラー: JSONフォーマット不正、必須パラメータ欠如など
        messageKey = 'validation.api.invalidRequest';
        errorType = 'client';
      } else if (
        error.status === HTTP_STATUS.NOT_FOUND ||
        error.message?.includes('404')
      ) {
        // 【4xx系】リソース未発見: 存在しないエンドポイント、削除済みリソースなど
        messageKey = 'validation.api.notFound';
        errorType = 'client';
      } else if (
        error.status === HTTP_STATUS.METHOD_NOT_ALLOWED ||
        error.message?.includes('405')
      ) {
        // 【4xx系】メソッド未許可: POST要求にGETでアクセスなど
        messageKey = 'validation.api.methodNotAllowed';
        errorType = 'client';
      } else if (
        error.status === HTTP_STATUS.UNPROCESSABLE_ENTITY ||
        error.message?.includes('422')
      ) {
        // 【4xx系】ビジネスルール違反: データ形式は正しいが業務制約に違反
        messageKey = 'validation.api.businessRuleViolation';
        errorType = 'client';
      } else if (
        error.status === HTTP_STATUS.INTERNAL_SERVER_ERROR ||
        error.message?.includes('500')
      ) {
        // 【5xx系】サーバーエラー: DB障害、外部API障害など（詳細隠蔽）
        messageKey = 'validation.api.serverError';
        errorType = 'server';
      } else {
        // その他の予期しないエラー
        // 【セキュリティ】詳細なエラー情報はブラウザーコンソールに出力しない
        // 攻撃者がスタックトレース、URL、ユーザーエージェント等の情報を取得することを防止

        // 不明なエラーは安全のためサーバーエラー扱い（ユーザーには一般的なメッセージを表示）
        messageKey = 'validation.api.searchFailed';
        errorType = 'server';
      }

      const message = t(messageKey);
      setError(field, message); // 翻訳されたメッセージを使用

      // エラータイプも返して呼び出し側で判定できるようにする
      return { message, errorType };
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
