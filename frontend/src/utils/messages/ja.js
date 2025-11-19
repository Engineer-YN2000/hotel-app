/**
 * 日本語メッセージファイル
 * 現在のvalidationMessages.jsの内容をベースにした多言語対応版
 */

export default {
  validation: {
    date: {
      checkInPastDate: 'チェックイン日は本日以降の日付を選択してください',
      checkOutPastDate: 'チェックアウト日は本日以降の日付を選択してください',
      checkOutBeforeCheckIn:
        'チェックアウト日はチェックイン日より後の日付を選択してください',
      invalidDateRange:
        '日付の選択に問題があります。正しい日付を選択してください',
    },
    form: {
      required: '必須項目です',
      prefectureRequired: '宿泊予定の都道府県を選択してください',
      guestCountMin: '宿泊人数は1名以上で入力してください',
      guestCountMax: '宿泊人数は99名以下で入力してください',
    },
    api: {
      serverError:
        'サーバーエラーが発生しました。しばらく時間をおいて再度お試しください',
      networkError:
        'ネットワークエラーが発生しました。接続状況をご確認ください',
      searchFailed: '検索に失敗しました。条件を変更して再度お試しください',
    },
    general: {
      unexpectedError: '予期せぬエラーが発生しました',
      tryAgain: '再度お試しください',
      contactSupport: 'エラーが続く場合はサポートまでお問い合わせください',
    },
  },

  components: {
    noResults: {
      title: 'ご指定の日程・地域では空室がございませんでした。',
      subtitle: '条件を変更して再検索してください。',
    },
    loading: {
      searching: '検索中...',
      loading: '読み込み中...',
    },
    buttons: {
      search: '検索する',
      retry: '再試行',
      back: '戻る',
    },
  },

  labels: {
    checkInDate: 'チェックイン予定日',
    checkOutDate: 'チェックアウト予定日',
    prefecture: '宿泊予定の都道府県',
    guestCount: '宿泊予定人数（合計）',
    selectPrefecture: '都道府県を選択',
  },
};
