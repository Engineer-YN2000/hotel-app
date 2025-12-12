/**
 * フィードバックコンポーネント用メッセージ
 * 使用コンポーネント: ServerError, NoResults, SessionExpiredError
 */
export const feedbackMessages = {
  ja: {
    messages: {
      noResults: {
        title: 'ご指定の日程・地域では空室がございませんでした。',
        subtitle: '条件を変更して再検索してください。',
      },
      error: {
        serverErrorTitle: 'サーバーエラー',
        apologyMessage: '大変申し訳ございません。',
        unexpectedError: 'サーバー側で予期せぬエラーが発生しました。',
        retryOrBackToTop:
          '時間をおいて再度お試しいただくか、トップページへお戻りください。',
        serverErrorWithStatus: 'サーバーエラー: {{status}}',
      },
      sessionExpired: {
        title: '予約有効時間切れ',
        timeoutMessage: 'ご予約手続きの有効時間（10分）が経過しました。',
        retryMessage: 'お手数ですが、最初からやり直してください。',
        cancelFailed:
          '予約のキャンセル処理に失敗しました。予約は一定時間後、自動的にキャンセルされます。',
      },
    },
  },

  en: {
    messages: {
      noResults: {
        title: 'No rooms available for the specified dates and location.',
        subtitle: 'Please change your criteria and search again.',
      },
      error: {
        serverErrorTitle: 'Server Error',
        apologyMessage: 'We sincerely apologize.',
        unexpectedError: 'An unexpected error occurred on the server side.',
        retryOrBackToTop: 'Please try again later or return to the top page.',
        serverErrorWithStatus: 'Server error: {{status}}',
      },
      sessionExpired: {
        title: 'Reservation Session Expired',
        timeoutMessage: 'The reservation session (10 minutes) has expired.',
        retryMessage: 'Please start over from the beginning.',
        cancelFailed:
          'Failed to cancel the reservation. The reservation will be automatically cancelled after a certain period.',
      },
    },
  },
};
