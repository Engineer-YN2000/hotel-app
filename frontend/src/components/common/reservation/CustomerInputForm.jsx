import React, { useReducer, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import './CustomerInputForm.css';

/**
 * C-022 顧客情報入力フォームコンポーネント
 * 宿泊者情報（氏名、連絡先、到着予定時刻）の入力フォームを提供する。
 *
 * 【国際化対応】
 * 姓名の表示順序を言語設定に応じて動的に変更する。
 * - 日本語（ja）、中国語（zh）、韓国語（ko）: 姓→名（Family Name → Given Name）
 * - 英語（en）、その他欧米言語: 名→姓（Given Name → Family Name）
 *
 * データベースには常に以下の形式で保存:
 * - reserver_first_name: Given Name（名）
 * - reserver_last_name: Family Name（姓）
 */

const initialState = {
  givenName: '', // 名（First Name）- DB: reserver_first_name
  familyName: '', // 姓（Last Name）- DB: reserver_last_name
  emailAddress: '',
  phoneNumber: '',
  arriveAt: '',
  errors: {},
};

const formReducer = (state, action) => {
  switch (action.type) {
    case 'SET_FIELD':
      return {
        ...state,
        [action.field]: action.value,
        errors: { ...state.errors, [action.field]: null },
      };
    case 'SET_ERRORS':
      return { ...state, errors: action.errors };
    case 'INIT_FORM':
      return { ...state, ...action.data, errors: {} };
    default:
      return state;
  }
};

const CustomerInputForm = ({
  onSubmit,
  onCancel,
  isSubmitting,
  isCancelling,
  initialData,
}) => {
  const { t } = useTranslation();
  const [state, dispatch] = useReducer(formReducer, initialState);

  // 初期データがある場合はフォームにセット
  // 【依存配列について】initialDataはAPIレスポンスから取得され、
  // データ変更時は必ず新しいオブジェクト参照が渡されるため、
  // 浅い比較（オブジェクト参照の変更検知）で十分。
  // 個別フィールドの依存は不要。
  useEffect(() => {
    if (initialData) {
      dispatch({
        type: 'INIT_FORM',
        data: {
          givenName: initialData.reserverFirstName || '',
          familyName: initialData.reserverLastName || '',
          emailAddress: initialData.emailAddress || '',
          phoneNumber: initialData.phoneNumber || '',
          // arriveAtはHH:mm:ss形式で返ってくるのでHH:mmに変換
          arriveAt: initialData.arriveAt
            ? initialData.arriveAt.substring(0, 5)
            : '',
        },
      });
    }
  }, [initialData]);

  // 表示順序を取得（familyFirst または givenFirst）
  const nameDisplayOrder = t('reservation.customerForm.nameDisplayOrder');
  const isFamilyFirst = nameDisplayOrder === 'familyFirst';

  const validate = () => {
    const errors = {};
    if (!state.givenName) {
      errors.givenName = t('validation.customer.required');
    }
    if (!state.familyName) {
      errors.familyName = t('validation.customer.required');
    }

    if (!state.emailAddress && !state.phoneNumber) {
      const msg = t('validation.customer.contactRequired');
      errors.emailAddress = msg;
      errors.phoneNumber = msg;
    }

    if (state.emailAddress && !/\S+@\S+\.\S+/.test(state.emailAddress)) {
      errors.emailAddress = t('validation.customer.emailInvalid');
    }
    // DB制約: ^(\+)?[0-9]+$ （先頭に+可、数字のみ、ハイフン不可）
    if (state.phoneNumber && !/^(\+)?[0-9]+$/.test(state.phoneNumber)) {
      errors.phoneNumber = t('validation.customer.phoneInvalid');
    }

    // 到着時刻は任意（未入力時はバックエンドでデフォルト值15:00が適用される）

    return errors;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errors = validate();
    if (Object.keys(errors).length > 0) {
      dispatch({ type: 'SET_ERRORS', errors });
      alert(t('validation.customer.formHasErrors'));
      return;
    }
    // バックエンドDTO形式に変換して送信
    // reserverFirstName = givenName（名）, reserverLastName = familyName（姓）
    // 空文字列はnullに変換（DBのNULL許容カラム対応）
    // arriveAtが未選択の場合はnullを送信し、バックエンドでデフォルト值が適用される
    onSubmit({
      reserverFirstName: state.givenName,
      reserverLastName: state.familyName,
      emailAddress: state.emailAddress || null,
      phoneNumber: state.phoneNumber || null,
      arriveAt: state.arriveAt || null,
    });
  };

  const handleChange = (field) => (e) => {
    dispatch({ type: 'SET_FIELD', field, value: e.target.value });
  };

  // 姓名入力フィールドのレンダリング
  const renderGivenNameField = () => (
    <div className={`form-group ${state.errors.givenName ? 'has-error' : ''}`}>
      <label>
        {t('reservation.customerForm.givenName')}{' '}
        <span className="required">*</span>
      </label>
      <input
        type="text"
        value={state.givenName}
        onChange={handleChange('givenName')}
      />
      {state.errors.givenName && (
        <span className="error-message">{state.errors.givenName}</span>
      )}
    </div>
  );

  const renderFamilyNameField = () => (
    <div className={`form-group ${state.errors.familyName ? 'has-error' : ''}`}>
      <label>
        {t('reservation.customerForm.familyName')}{' '}
        <span className="required">*</span>
      </label>
      <input
        type="text"
        value={state.familyName}
        onChange={handleChange('familyName')}
      />
      {state.errors.familyName && (
        <span className="error-message">{state.errors.familyName}</span>
      )}
    </div>
  );

  return (
    <form className="customer-input-form" onSubmit={handleSubmit}>
      <h3>{t('reservation.customerForm.title')}</h3>
      {/* 姓名フィールド：言語設定に応じて表示順序を切り替え */}
      <div className="form-row">
        {isFamilyFirst ? (
          <>
            {renderFamilyNameField()}
            {renderGivenNameField()}
          </>
        ) : (
          <>
            {renderGivenNameField()}
            {renderFamilyNameField()}
          </>
        )}
      </div>

      <div className="form-group">
        <label>{t('reservation.customerForm.emailAddress')}</label>
        <input
          type="email"
          value={state.emailAddress}
          onChange={handleChange('emailAddress')}
        />
        {state.errors.emailAddress && (
          <span className="error-message">{state.errors.emailAddress}</span>
        )}
      </div>
      <div className="form-group">
        <label>{t('reservation.customerForm.phoneNumber')}</label>
        <input
          type="tel"
          value={state.phoneNumber}
          onChange={handleChange('phoneNumber')}
        />
        {state.errors.phoneNumber && (
          <span className="error-message">{state.errors.phoneNumber}</span>
        )}
      </div>

      {/* 到着時刻入力（任意、未選択時はデフォルト15:00） */}
      <div className="form-group">
        <label>{t('reservation.customerForm.arriveAt')}</label>
        <select value={state.arriveAt} onChange={handleChange('arriveAt')}>
          <option value="">
            {t('reservation.customerForm.arriveAtPlaceholder')}
          </option>
          {[...Array(10)].map((_, i) => (
            <option key={i} value={`${15 + i}:00`}>
              {15 + i}:00
            </option>
          ))}
        </select>
        <span className="hint-message">
          {t('reservation.customerForm.arriveAtHint')}
        </span>
      </div>

      <div className="button-container">
        <button
          type="button"
          className="btn btn-secondary"
          onClick={onCancel}
          disabled={isCancelling || isSubmitting}
        >
          {isCancelling
            ? t('reservation.customerForm.cancellingButton')
            : t('reservation.customerForm.cancelButton')}
        </button>
        <button
          type="submit"
          className="btn btn-primary"
          disabled={isSubmitting || isCancelling}
        >
          {isSubmitting
            ? t('reservation.customerForm.submittingButton')
            : t('reservation.customerForm.submitButton')}
        </button>
      </div>
    </form>
  );
};

export default CustomerInputForm;
