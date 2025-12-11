/**
 * 予約ページ用メッセージ
 * 使用コンポーネント: ReservationInputPage, ReservationConfirmPage, ReservationCompletePage, ReservationSummary, CustomerInputForm
 */
export const reservationMessages = {
  ja: {
    reservation: {
      // P-020 予約詳細入力ページ
      inputPage: {
        title: '予約詳細入力',
        loading: '読み込み中...',
        notFound: '予約が見つかりません。',
        processingComplete: '顧客情報登録完了',
        navigateToConfirm: 'P-030（確認画面）へ遷移します（未実装）',
        expiredMessage: '有効期限切れです。トップページへ戻ります。',
        errorOccurred: 'エラーが発生しました。',
        networkError: '通信エラーが発生しました。',
        confirmCancel: 'この予約をキャンセルしてトップページへ戻りますか？',
        cancelError: '予約のキャンセルに失敗しました。',
      },
      // P-030 予約確認ページ
      confirmPage: {
        title: '予約内容確認',
        loading: '読み込み中...',
        notFound: '予約が見つかりません。',
        instruction:
          '以下の内容で予約を確定します。よろしければ「予約を確定する」ボタンを押してください。',
        customerInfoTitle: '宿泊者情報',
        guestName: '氏名',
        honorific: '様',
        phoneNumber: '電話番号',
        emailAddress: 'Eメール',
        arriveAt: '到着予定時刻',
        notRegistered: '（未登録）',
        backButton: '修正する',
        confirmButton: '予約を確定する',
        confirmingButton: '確定中...',
        confirmError: '予約の確定に失敗しました。再度お試しください。',
        cancelButton: 'キャンセル',
        cancellingButton: 'キャンセル中...',
        confirmCancel: 'この予約をキャンセルしてトップページへ戻りますか？',
        cancelError: '予約のキャンセルに失敗しました。',
      },
      // P-040 予約完了ページ
      completePage: {
        title: '予約完了',
        thankYou: 'ご予約ありがとうございました',
        confirmedMessage: '以下の通り、ご予約を承りました。',
        reservationId: '予約ID',
      },
      // C-021 予約サマリー
      summary: {
        title: 'ご予約内容',
        hotelName: 'ホテル名:',
        schedule: '日程:',
        roomAndCount: '部屋・室数:',
        roomDetail: '{{roomTypeName}} (定員{{roomCapacity}}名) x {{count}}室',
        totalFee: '合計金額:',
      },
      // C-022 顧客情報入力フォーム
      customerForm: {
        title: '宿泊者情報',
        // 姓名フィールド（日本語: 姓→名の順）
        familyName: '宿泊者姓',
        givenName: '宿泊者名',
        // 表示順序設定: 'familyFirst' = 姓→名, 'givenFirst' = 名→姓
        nameDisplayOrder: 'familyFirst',
        emailAddress: 'Eメールアドレス',
        phoneNumber: '電話番号',
        arriveAt: '到着予定時刻',
        arriveAtPlaceholder: '選択してください',
        arriveAtHint: '※ 未選択の場合は15:00が適用されます',
        cancelButton: 'キャンセル',
        cancellingButton: 'キャンセル中...',
        submitButton: '確認画面へ',
        submittingButton: '処理中...',
      },
    },
    // バリデーションメッセージ
    validation: {
      customer: {
        required: '入力してください',
        contactRequired:
          '電話番号またはEメールアドレスのいずれかを入力してください',
        emailInvalid: 'Eメールアドレスの形式が正しくありません',
        phoneInvalid: '電話番号の形式が正しくありません',
        formHasErrors: '入力内容にエラーがあります',
      },
    },
  },

  en: {
    reservation: {
      // P-020 Reservation Input Page
      inputPage: {
        title: 'Reservation Details Input',
        loading: 'Loading...',
        notFound: 'Reservation not found.',
        processingComplete: 'Customer info registered',
        navigateToConfirm:
          'Navigate to P-030 (Confirmation page) (Not implemented)',
        expiredMessage: 'Reservation expired. Returning to top page.',
        errorOccurred: 'An error occurred.',
        networkError: 'Network error occurred.',
        confirmCancel: 'Cancel this reservation and return to the top page?',
        cancelError: 'Failed to cancel the reservation.',
      },
      // P-030 Reservation Confirmation Page
      confirmPage: {
        title: 'Confirm Reservation',
        loading: 'Loading...',
        notFound: 'Reservation not found.',
        instruction:
          'Please review your reservation details below. Click "Confirm Reservation" to complete your booking.',
        customerInfoTitle: 'Guest Information',
        guestName: 'Name',
        honorific: '',
        phoneNumber: 'Phone Number',
        emailAddress: 'Email',
        arriveAt: 'Estimated Arrival Time',
        notRegistered: '(Not registered)',
        backButton: 'Edit',
        confirmButton: 'Confirm Reservation',
        confirmingButton: 'Confirming...',
        confirmError: 'Failed to confirm reservation. Please try again.',
        cancelButton: 'Cancel',
        cancellingButton: 'Cancelling...',
        confirmCancel: 'Cancel this reservation and return to the top page?',
        cancelError: 'Failed to cancel the reservation.',
      },
      // P-040 Reservation Complete Page
      completePage: {
        title: 'Reservation Complete',
        thankYou: 'Thank you for your reservation',
        confirmedMessage: 'Your reservation has been confirmed as follows.',
        reservationId: 'Reservation ID',
      },
      // C-021 Reservation Summary
      summary: {
        title: 'Reservation Details',
        hotelName: 'Hotel:',
        schedule: 'Dates:',
        roomAndCount: 'Room & Count:',
        roomDetail:
          '{{roomTypeName}} (Capacity: {{roomCapacity}}) x {{count}} room(s)',
        totalFee: 'Total:',
      },
      // C-022 Customer Info Form
      customerForm: {
        title: 'Guest Information',
        // Name fields (English: First Name → Last Name order)
        givenName: 'First Name',
        familyName: 'Last Name',
        // Display order: 'familyFirst' = Family→Given, 'givenFirst' = Given→Family
        nameDisplayOrder: 'givenFirst',
        emailAddress: 'Email Address',
        phoneNumber: 'Phone Number',
        arriveAt: 'Estimated Arrival Time',
        arriveAtPlaceholder: 'Please select',
        arriveAtHint: '* If not selected, 15:00 will be applied',
        cancelButton: 'Cancel',
        cancellingButton: 'Cancelling...',
        submitButton: 'Proceed to Confirmation',
        submittingButton: 'Processing...',
      },
    },
    // Validation messages
    validation: {
      customer: {
        required: 'This field is required',
        contactRequired: 'Please enter either phone number or email address',
        emailInvalid: 'Invalid email address format',
        phoneInvalid: 'Invalid phone number format',
        formHasErrors: 'There are errors in the form',
      },
    },
  },
};
