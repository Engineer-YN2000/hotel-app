/**
 * バリデーションエラーメッセージの定義
 * 多言語対応やメッセージ管理の統一化のため
 */

export const VALIDATION_MESSAGES = {
  // 日付関連のバリデーションメッセージ
  date: {
    checkInPastDate: 'チェックイン日は本日以降の日付を選択してください',
    checkOutPastDate: 'チェックアウト日は本日以降の日付を選択してください',
    checkOutBeforeCheckIn:
      'チェックアウト日はチェックイン日より後の日付を選択してください',
    invalidDateRange:
      '日付の選択に問題があります。正しい日付を選択してください',
  },

  // フォーム関連のバリデーションメッセージ
  form: {
    required: '必須項目です',
    prefectureRequired: '宿泊予定の都道府県を選択してください',
    guestCountMin: '宿泊人数は1名以上で入力してください',
    guestCountMax: '宿泊人数は99名以下で入力してください',
  },

  // API関連のエラーメッセージ
  api: {
    serverError:
      'サーバーエラーが発生しました。しばらく時間をおいて再度お試しください',
    networkError: 'ネットワークエラーが発生しました。接続状況をご確認ください',
    searchFailed: '検索に失敗しました。条件を変更して再度お試しください',
  },

  // 一般的なエラーメッセージ
  general: {
    unexpectedError: '予期せぬエラーが発生しました',
    tryAgain: '再度お試しください',
    contactSupport: 'エラーが続く場合はサポートまでお問い合わせください',
  },
};

// メッセージ取得のヘルパー関数
export const getMessage = (
  category,
  key,
  fallback = '不明なエラーが発生しました',
) => {
  try {
    return VALIDATION_MESSAGES[category]?.[key] || fallback;
  } catch (error) {
    console.warn(`メッセージ取得エラー: ${category}.${key}`, error);
    return fallback;
  }
};

// よく使用されるメッセージのショートカット
export const getDateMessage = (key) => getMessage('date', key);
export const getFormMessage = (key) => getMessage('form', key);
export const getApiMessage = (key) => getMessage('api', key);
export const getGeneralMessage = (key) => getMessage('general', key);
