/**
 * トップページ用メッセージ
 * 使用コンポーネント: TopPage
 */
export const topPageMessages = {
  ja: {
    labels: {
      search: 'ホテル検索',
      checkInDate: 'チェックイン予定日',
      checkOutDate: 'チェックアウト予定日',
      prefecture: '宿泊予定の都道府県',
      guestCount: '宿泊予定人数（合計）',
      selectPrefecture: '都道府県を選択',
      hotelSearch: 'ホテル検索',
      hotelReservationSite: 'ホテル予約サイト',
    },

    buttons: {
      search: '検索する',
      searching: '検索中...',
    },

    validation: {
      date: {
        checkInPastDate: 'チェックイン日は本日以降の日付を選択してください',
        checkOutPastDate: 'チェックアウト日は本日以降の日付を選択してください',
        checkOutBeforeCheckIn:
          'チェックアウト日はチェックイン日より後の日付を選択してください',
        invalidDateRange:
          '日付の選択に問題があります。正しい日付を選択してください',
        checkInRequired: 'チェックイン日を入力してください',
        checkOutRequired: 'チェックアウト日を入力してください',
      },
      prefecture: {
        required: '宿泊予定の都道府県を選択してください',
      },
      guestCount: {
        range: '宿泊人数は1～99人の範囲で入力してください',
      },
      form: {
        required: '必須項目です',
        prefectureRequired: '宿泊予定の都道府県を選択してください',
        guestCountMin: '宿泊人数は1名以上で入力してください',
        guestCountMax: '宿泊人数は99名以下で入力してください',
      },
    },
  },

  en: {
    labels: {
      search: 'Hotel Search',
      checkInDate: 'Check-in Date',
      checkOutDate: 'Check-out Date',
      prefecture: 'Prefecture',
      guestCount: 'Number of Guests',
      selectPrefecture: 'Select Prefecture',
      hotelSearch: 'Hotel Search',
      hotelReservationSite: 'Hotel Reservation Site',
    },

    buttons: {
      search: 'Search',
      searching: 'Searching...',
    },

    validation: {
      date: {
        checkInPastDate: 'Please select a check-in date from today onwards',
        checkOutPastDate: 'Please select a check-out date from today onwards',
        checkOutBeforeCheckIn: 'Check-out date must be after check-in date',
        invalidDateRange:
          'There is an issue with the date selection. Please select the correct dates',
        checkInRequired: 'Please enter your check-in date',
        checkOutRequired: 'Please enter your check-out date',
      },
      prefecture: {
        required: 'Please select your preferred prefecture',
      },
      guestCount: {
        range: 'Number of guests must be between 1 and 99',
      },
      form: {
        required: 'This field is required',
        prefectureRequired: 'Please select your preferred prefecture',
        guestCountMin: 'Number of guests must be 1 or more',
        guestCountMax: 'Number of guests must be 99 or less',
      },
    },
  },
};
