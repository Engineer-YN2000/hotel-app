/**
 * 検索コンポーネント用メッセージ
 * 使用コンポーネント: RefineForm, SearchResults
 */
export const searchMessages = {
  ja: {
    labels: {
      refineConditions: '絞り込み条件',
      areaDetails: '地域詳細（選択された都道府県内）',
      accommodationPrice: '宿泊金額（１泊１室あたり）',
      searchResults: '検索結果',
      searchResultsWithCount: '検索結果（{{count}}件）',
      roomType: '部屋タイプ',
      priceFor2Nights: '価格（２泊分/１室）',
      priceForNights: '価格（{{nights}}泊分/１室）',
      availability: '空室状況',
      roomCount: '室数',
      capacity: '定員: {{count}}名',
      referencePerNight: '参考: ¥{{price}}/泊',
      remainingRooms: '残り{{count}}部屋！',
      roomsAvailable: '空室あり',
      priceCalculating: '計算中...',
      totalAmount: '合計金額',
      selectedCapacity: '選択室の定員: {{count}}名',
      priceNote: '（２泊 / 諸税込み）',
    },

    buttons: {
      refineSearch: '再検索・絞り込み',
      reserveSelectedRooms: '選択した部屋を予約',
    },

    messages: {
      noAreasAvailable: '選択された都道府県に詳細地域が登録されていません',
    },

    validation: {
      form: {
        roomSelectionRequired: '予約するお部屋を選択してください。',
        capacityError:
          '宿泊人数（{{guestCount}}名）に対して、お部屋の定員（合計{{totalCapacity}}名）が不足しています。',
      },
    },
  },

  en: {
    labels: {
      refineConditions: 'Refine Conditions',
      areaDetails: 'Area Details (Within Selected Prefecture)',
      accommodationPrice: 'Accommodation Price (Per Night Per Room)',
      searchResults: 'Search Results',
      searchResultsWithCount: 'Search Results ({{count}} found)',
      roomType: 'Room Type',
      priceFor2Nights: 'Price (2 Nights/1 Room)',
      priceForNights: 'Price ({{nights}} Nights/1 Room)',
      availability: 'Availability',
      roomCount: 'Room Count',
      capacity: 'Capacity: {{count}} guests',
      referencePerNight: 'Reference: ¥{{price}}/night',
      remainingRooms: 'Only {{count}} rooms left!',
      roomsAvailable: 'Rooms Available',
      priceCalculating: 'Calculating...',
      totalAmount: 'Total Amount',
      selectedCapacity: 'Selected capacity: {{count}} guests',
      priceNote: '(2 Nights / Tax Included)',
    },

    buttons: {
      refineSearch: 'Refine Search',
      reserveSelectedRooms: 'Reserve Selected Rooms',
    },

    messages: {
      noAreasAvailable:
        'No detailed areas are registered for the selected prefecture',
    },

    validation: {
      form: {
        roomSelectionRequired: 'Please select rooms to reserve.',
        capacityError:
          'The number of guests ({{guestCount}} guests) exceeds the room capacity (total {{totalCapacity}} guests).',
      },
    },
  },
};
