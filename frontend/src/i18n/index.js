import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

/**
 * React i18next設定ファイル
 * JavaのMessageSourceやSpring BootのThymeleafのmessages.propertiesと同様の機能を提供
 */

// 日本語リソース（messages.propertiesの代替）
const resources = {
  ja: {
    translation: {
      // アプリケーション全般（app.properties相当）
      app: {
        title: 'ホテル予約サイト',
      },

      // バリデーションメッセージ（validation.properties相当）
      validation: {
        date: {
          checkInPastDate: 'チェックイン日は本日以降の日付を選択してください',
          checkOutPastDate:
            'チェックアウト日は本日以降の日付を選択してください',
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
          capacityError: '宿泊人数（{{guestCount}}名）に対して、お部屋の定員（合計{{totalCapacity}}名）が不足しています。',
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

      // UIラベル（labels.properties相当）
      labels: {
        search: 'ホテル検索',
        checkInDate: 'チェックイン予定日',
        checkOutDate: 'チェックアウト予定日',
        prefecture: '宿泊予定の都道府県',
        guestCount: '宿泊予定人数（合計）',
        selectPrefecture: '都道府県を選択',
        hotelSearch: 'ホテル検索',
        hotelReservationSite: 'ホテル予約サイト',
        refineConditions: '絞り込み条件',
        areaDetails: '地域詳細（選択された都道府県内）',
        accommodationPrice: '宿泊金額（１泊１室あたり）',
        searchResults: '検索結果',
        roomType: '部屋タイプ',
        priceFor2Nights: '価格（２泊分/１室）',
        availability: '空室状況',
        roomCount: '室数',
        capacity: '定員: {{count}}名',
        referencePerNight: '参考: ¥{{price}}/泊',
        remainingRooms: '残り{{count}}部屋！',
        roomsAvailable: '空室あり',
        totalAmount: '合計金額',
        priceNote: '（２泊 / 諸税込み）',
      },

      // ボタンテキスト（buttons.properties相当）
      buttons: {
        search: '検索する',
        searching: '検索中...',
        retry: '再試行',
        back: '戻る',
        reset: 'リセット',
        backToTop: 'トップへ戻る',
        refineSearch: '再検索・絞り込み',
        reserveSelectedRooms: '選択した部屋を予約',
      },

      // メッセージ（messages.properties相当）
      messages: {
        noResults: {
          title: 'ご指定の日程・地域では空室がございませんでした。',
          subtitle: '条件を変更して再検索してください。',
        },
        loading: {
          initializing: '初期化中...',
          searching: '検索中...',
          loading: '読み込み中...',
        },
        error: {
          serverErrorTitle: 'サーバーエラー',
          apologyMessage: '大変申し訳ございません。',
          unexpectedError: 'サーバー側で予期せぬエラーが発生しました。',
          retryOrBackToTop: '時間をおいて再度お試しいただくか、トップページへお戻りください。',
        },
      },
    },
  },

  // 英語リソース（将来的な拡張用）
  en: {
    translation: {
      // Application general (app.properties equivalent)
      app: {
        title: 'Hotel Booking Site',
      },

      validation: {
        date: {
          checkInPastDate: 'Please select a check-in date from today onwards',
          checkOutPastDate: 'Please select a check-out date from today onwards',
          checkOutBeforeCheckIn: 'Check-out date must be after check-in date',
          invalidDateRange:
            'There is an issue with the date selection. Please select the correct dates',
        },
        form: {
          required: 'This field is required',
          prefectureRequired: 'Please select your preferred prefecture',
          guestCountMin: 'Number of guests must be 1 or more',
          guestCountMax: 'Number of guests must be 99 or less',
          capacityError: 'The room capacity (total {{totalCapacity}} guests) is insufficient for the number of guests ({{guestCount}} guests).',
        },
        api: {
          serverError: 'A server error occurred. Please try again later',
          networkError:
            'A network error occurred. Please check your connection',
          searchFailed:
            'Search failed. Please try again with different criteria',
        },
        general: {
          unexpectedError: 'An unexpected error occurred',
          tryAgain: 'Please try again',
          contactSupport: 'If the error persists, please contact support',
        },
      },
      labels: {
        search: 'Hotel Search',
        checkInDate: 'Check-in Date',
        checkOutDate: 'Check-out Date',
        prefecture: 'Prefecture',
        guestCount: 'Number of Guests',
        selectPrefecture: 'Select Prefecture',
        hotelSearch: 'Hotel Search',
        hotelReservationSite: 'Hotel Reservation Site',
        refineConditions: 'Refine Conditions',
        areaDetails: 'Area Details (Within Selected Prefecture)',
        accommodationPrice: 'Accommodation Price (Per Night Per Room)',
        searchResults: 'Search Results',
        roomType: 'Room Type',
        priceFor2Nights: 'Price (2 Nights/1 Room)',
        availability: 'Availability',
        roomCount: 'Room Count',
        capacity: 'Capacity: {{count}} guests',
        referencePerNight: 'Reference: ¥{{price}}/night',
        remainingRooms: 'Only {{count}} rooms left!',
        roomsAvailable: 'Rooms Available',
        totalAmount: 'Total Amount',
        priceNote: '(2 Nights / Tax Included)',
      },
      buttons: {
        search: 'Search',
        searching: 'Searching...',
        retry: 'Retry',
        back: 'Back',
        reset: 'Reset',
        backToTop: 'Back to Top',
        refineSearch: 'Refine Search',
        reserveSelectedRooms: 'Reserve Selected Rooms',
      },
      messages: {
        noResults: {
          title: 'No rooms available for the specified dates and location.',
          subtitle: 'Please change your criteria and search again.',
        },
        loading: {
          initializing: 'Initializing...',
          searching: 'Searching...',
          loading: 'Loading...',
        },
        error: {
          serverErrorTitle: 'Server Error',
          apologyMessage: 'We sincerely apologize.',
          unexpectedError: 'An unexpected error occurred on the server side.',
          retryOrBackToTop: 'Please try again later or return to the top page.',
        },
      },
    },
  },
};

i18n
  // ブラウザの言語を自動検出（JavaのLocaleResolver相当）
  .use(LanguageDetector)
  // React i18nextとの連携
  .use(initReactI18next)
  // 初期化
  .init({
    resources,

    // デフォルト言語（Spring Bootのspring.messages.basename相当）
    fallbackLng: 'ja',

    // デバッグモード（開発時のみ）
    debug: process.env.NODE_ENV === 'development',

    // 補間設定（MessageSourceのパラメータ置換相当）
    interpolation: {
      escapeValue: false, // Reactは既にXSS対策済み
    },

    // React Suspenseサポート
    react: {
      useSuspense: true,
    },

    // 言語検出設定
    detection: {
      // 検出順序（localStorage → navigator → fallback）
      order: ['localStorage', 'navigator', 'htmlTag'],
      // ローカルストレージのキー名
      lookupLocalStorage: 'i18nextLng',
      // キャッシュ設定
      caches: ['localStorage'],
    },
  });

export default i18n;
