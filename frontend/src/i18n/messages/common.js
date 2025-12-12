/**
 * 共通メッセージ（アプリ全体・汎用）
 */
export const commonMessages = {
  ja: {
    // アプリケーション全般（app.properties相当）
    app: {
      title: 'ホテル予約サイト',
    },

    // 汎用バリデーションメッセージ
    validation: {
      general: {
        unexpectedError: '予期せぬエラーが発生しました',
        tryAgain: '再度お試しください',
        contactSupport: 'エラーが続く場合はサポートまでお問い合わせください',
      },
      api: {
        serverError:
          'サーバーエラーが発生しました。しばらく時間をおいて再度お試しください',
        networkError:
          'ネットワークエラーが発生しました。接続状況をご確認ください',
        invalidRequest:
          'リクエスト形式に問題があります。画面を再読み込みして再度お試しください',
        notFound:
          'リクエストされた機能が見つかりません。画面を再読み込みして再度お試しください',
        methodNotAllowed:
          '許可されていない操作です。画面を再読み込みして再度お試しください',
        businessRuleViolation:
          '入力値がシステムの制約に違反しています。正しい値を入力してください',
        searchFailed: '検索に失敗しました。条件を変更して再度お試しください',
      },
    },

    // 共通ボタンテキスト
    buttons: {
      retry: '再試行',
      back: '戻る',
      reset: 'リセット',
      backToTop: 'トップへ戻る',
    },

    // 共通メッセージ
    messages: {
      loading: '読み込み中...',
      loadingDetailed: {
        initializing: '初期化中...',
        searching: '検索中...',
        loading: '読み込み中...',
      },
    },
  },

  en: {
    app: {
      title: 'Hotel Booking Site',
    },

    validation: {
      general: {
        unexpectedError: 'An unexpected error occurred',
        tryAgain: 'Please try again',
        contactSupport: 'If the error persists, please contact support',
      },
      api: {
        serverError: 'A server error occurred. Please try again later',
        networkError: 'A network error occurred. Please check your connection',
        invalidRequest:
          'There is an issue with the request format. Please reload the page and try again',
        notFound:
          'The requested feature was not found. Please reload the page and try again',
        methodNotAllowed:
          'This operation is not allowed. Please reload the page and try again',
        businessRuleViolation:
          'The input values violate system constraints. Please enter correct values',
        searchFailed: 'Search failed. Please try again with different criteria',
      },
    },

    buttons: {
      retry: 'Retry',
      back: 'Back',
      reset: 'Reset',
      backToTop: 'Back to Top',
    },

    messages: {
      loading: 'Loading...',
      loadingDetailed: {
        initializing: 'Initializing...',
        searching: 'Searching...',
        loading: 'Loading...',
      },
    },
  },
};
