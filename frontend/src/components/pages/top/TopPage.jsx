import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  RefineForm,
  SearchResults,
  NoResults,
  ServerError,
  LanguageSwitcher,
} from '../../common';
import { useI18nValidation } from '../../../hooks/useI18nValidation';
import './TopPage.css';

/**
 * P-010 ホテル検索ページ
 */
const TopPage = () => {
  const { t } = useTranslation(); // MessageSourceと同等の機能

  // 状態定義 (1-A) [cite: 26-34]
  const [showRefineForm, setShowRefineForm] = useState(false); // C-020
  const [showResults, setShowResults] = useState(false); // C-030
  const [showNoResults, setShowNoResults] = useState(false); // C-040

  const [isServerError, setIsServerError] = useState(false); // P-900

  const [isLoading, setIsLoading] = useState(false);
  const [searchResult, setSearchResult] = useState(null);
  const [originalSearchResult, setOriginalSearchResult] = useState(null); // 元の検索結果を保持
  const [guestCount, setGuestCount] = useState(1); // 1-Dの検証用に保持（フォームのdefaultValueと統一）
  const [selectedPrefectureId, setSelectedPrefectureId] = useState(null); // 絞り込み用
  const [updatedPrices, setUpdatedPrices] = useState(null); // 価格再計算結果を保持
  const [isPriceLoading, setIsPriceLoading] = useState(false); // 価格再計算中フラグ

  // 日付バリデーション用の状態
  const [checkInDate, setCheckInDate] = useState('');
  const [checkOutDate, setCheckOutDate] = useState('');
  const [initialCheckInDate, setInitialCheckInDate] = useState(''); // 初期チェックイン日を保持
  const [initialCheckOutDate, setInitialCheckOutDate] = useState(''); // 初期チェックアウト日を保持

  // バリデーションフック（React i18next版）を使用
  const { validateDates, handleApiError, getError, setError, clearError } =
    useI18nValidation();

  // 都道府県リスト状態
  const [prefectures, setPrefectures] = useState([]);

  // 日付バリデーション用のuseEffect - 競合状態を防ぐため
  useEffect(() => {
    // 初期値が設定される前はバリデーションをスキップ
    if (!initialCheckInDate || !initialCheckOutDate) {
      return;
    }

    // 両方の日付が設定されている場合のみバリデーション実行
    if (checkInDate && checkOutDate) {
      const error = validateDates(
        checkInDate,
        checkOutDate,
        initialCheckInDate,
        initialCheckOutDate,
      );
      if (error) {
        setError('dateValidation', error);
      } else {
        clearError('dateValidation');
      }
    }
  }, [
    checkInDate,
    checkOutDate,
    initialCheckInDate,
    initialCheckOutDate,
    validateDates,
    setError,
    clearError,
  ]);

  // 初期化時にgetInitialDataから各種UI値を取得＆設定
  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const response = await fetch('/api/initial-data');
        if (response.ok) {
          const data = await response.json();

          // 都道府県リストを設定
          if (data.prefectures) {
            setPrefectures(data.prefectures);
          }

          // 推奨日付を初期値として設定
          if (data.recommendedCheckin) {
            setCheckInDate(data.recommendedCheckin);
            setInitialCheckInDate(data.recommendedCheckin); // 初期チェックイン日を保存
          }
          if (data.recommendedCheckout) {
            setCheckOutDate(data.recommendedCheckout);
            setInitialCheckOutDate(data.recommendedCheckout); // 初期チェックアウト日を保存
          }

          // 初期化時は推奨日付が適切に設定されるため、バリデーションエラーはクリア
          if (data.recommendedCheckin && data.recommendedCheckout) {
            clearError('dateValidation'); // 推奨日付は適切な値なのでエラーをクリア
          }
        }
      } catch (error) {
        // 【セキュリティ】詳細なエラー情報をコンソールに出力しない
        // 初期データ取得失敗時は黙ってデフォルト状態で継続
      }
    };

    fetchInitialData();
  }, [clearError]);

  /**
   * 価格再計算API呼び出し
   *
   * 【処理目的】
   * 検索結果表示中に日付が変更された場合、新しい日付に基づいて
   * 各部屋タイプの価格を再計算する。
   *
   * 【セキュリティ設計】
   * - サーバー側で価格計算を行い、係数等の内部ロジックを隠蔽
   * - クライアントには計算結果のみを提供
   */
  const recalculatePrices = async (newCheckInDate, newCheckOutDate) => {
    // 検索結果がない場合は再計算不要
    if (
      !searchResult ||
      !searchResult.hotels ||
      searchResult.hotels.length === 0
    ) {
      return;
    }

    // 日付バリデーション
    if (!newCheckInDate || !newCheckOutDate) {
      return;
    }

    // バリデーションエラーがある場合はスキップ
    const dateError = validateDates(
      newCheckInDate,
      newCheckOutDate,
      initialCheckInDate,
      initialCheckOutDate,
    );
    if (dateError) {
      return;
    }

    setIsPriceLoading(true);

    // 全ホテルの全部屋タイプをリクエスト用に変換
    const rooms = searchResult.hotels.flatMap((hotel) =>
      hotel.roomTypes.map((roomType) => ({
        roomTypeId: roomType.roomTypeId,
        hotelId: hotel.hotelId,
      })),
    );

    try {
      const response = await fetch('/api/price/calculate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          checkInDate: newCheckInDate,
          checkOutDate: newCheckOutDate,
          rooms,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        // 価格をマップ形式に変換: { "hotelId-roomTypeId": price }
        const priceMap = {};
        data.rooms.forEach((room) => {
          priceMap[`${room.hotelId}-${room.roomTypeId}`] = room.price;
        });
        setUpdatedPrices(priceMap);
      }
    } catch (error) {
      // 【セキュリティ】エラー詳細をコンソールに出力しない
      // 価格再計算失敗時は元の価格を維持（UX考慮）
    } finally {
      setIsPriceLoading(false);
    }
  };

  /**
   * 日付連動処理 - UX最適化の実装パターン
   *
   * 【処理目的】
   * チェックイン日変更時に、チェックアウト日を自動的に翌日に設定し、
   * ユーザーの入力負荷を軽減する。ホテル予約では一般的なUXパターン。
   *
   * 【実装ポイント】
   * 1. Date オブジェクトによる日付計算（タイムゾーン考慮）
   * 2. setDate() メソッドによる安全な日付加算（月跨ぎ自動処理）
   * 3. ISO文字列変換でHTMLのdate input形式に統一
   * 4. バリデーション処理の分離（useEffect側で集約管理）
   *
   * 【注意事項】
   * - new Date(dateString)はローカルタイムゾーンで解釈
   * - toISOString()はUTC基準だが、split('T')[0]で日付部分のみ取得
   * - 月末日の翌日は自動的に翌月1日になる（setDateの仕様）
   */
  const handleCheckInChange = (e) => {
    const newCheckInDate = e.target.value;
    setCheckInDate(newCheckInDate);

    // 【日付連動ロジック】チェックイン日+1日をチェックアウト日に自動設定
    if (newCheckInDate) {
      const checkIn = new Date(newCheckInDate);
      const nextDay = new Date(checkIn);
      nextDay.setDate(checkIn.getDate() + 1); // 月跨ぎも自動処理
      const newCheckOutDate = nextDay.toISOString().split('T')[0];
      setCheckOutDate(newCheckOutDate);
      // 【価格再計算】検索結果表示中は新しい日付で価格を再計算
      recalculatePrices(newCheckInDate, newCheckOutDate);
    }
    // 【設計判断】バリデーションはuseEffectで集約管理（関心の分離）
  };

  // チェックアウト日変更時の処理
  const handleCheckOutChange = (e) => {
    const newCheckOutDate = e.target.value;
    setCheckOutDate(newCheckOutDate);
    // 【価格再計算】検索結果表示中は新しい日付で価格を再計算
    recalculatePrices(checkInDate, newCheckOutDate);
    // バリデーションはuseEffectで自動実行されるため、ここでは削除
  };

  // 人数入力変更時の処理
  const handleGuestCountChange = (e) => {
    const value = e.target.value;
    const num = parseInt(value, 10);
    // 1以上99以下のみ反映
    if (!isNaN(num) && num >= 1 && num <= 99) {
      setGuestCount(num);
    } else if (value === '') {
      setGuestCount(''); // 空欄時は空文字
    }
  };

  // リセット処理
  const handleReset = () => {
    setIsServerError(false);
    setShowResults(false);
    setShowNoResults(false);
    setShowRefineForm(false);
    setSearchResult(null);
    setOriginalSearchResult(null);
    setIsLoading(false);
    setSelectedPrefectureId(null);
  };

  /**
   * クライアントサイド絞り込み検索 - パフォーマンス最適化パターン
   *
   * 【設計方針】
   * サーバーへの再リクエストを避け、既取得の検索結果をフィルタリングすることで、
   * レスポンス速度向上とサーバー負荷軽減を実現。
   *
   * 【アルゴリズム】
   * 1. originalSearchResult（初回検索結果）を不変データとして保持
   * 2. 地域IDの配列に基づいて Array.filter() でホテルを絞り込み
   * 3. スプレッド演算子でオブジェクトを複製し、hotelsプロパティのみ更新
   * 4. 結果数に応じて表示状態を切り替え（結果あり/なし）
   *
   * 【パフォーマンス特性】
   * - 時間計算量: O(n) （nはホテル数）
   * - 空間計算量: O(n) （フィルタ結果の新配列生成）
   * - ネットワーク: 0（サーバー通信なし）
   *
   * 【エッジケース】
   * - selectedAreaIds.length === 0: 全地域表示（絞り込み解除）
   * - filteredHotels.length === 0: NoResults コンポーネント表示
   */
  const handleRefineSearch = (selectedAreaIds) => {
    // 【前提条件】元の検索結果の存在確認
    if (originalSearchResult && originalSearchResult.hotels) {
      // 【フィルタリングロジック】地域ID配列による包含判定
      const filteredHotels = originalSearchResult.hotels.filter(
        (hotel) =>
          selectedAreaIds.length === 0 || // 空配列 = 絞り込み解除
          selectedAreaIds.includes(hotel.areaId), // 包含チェック
      );

      // 【イミュータブル更新】元オブジェクトを破壊せずに新しい結果を作成
      const refinedResult = {
        ...originalSearchResult,
        hotels: filteredHotels,
      };

      setSearchResult(refinedResult);

      // 【UI状態制御】結果数に基づく表示コンポーネントの切り替え
      if (filteredHotels.length > 0) {
        setShowResults(true);
        setShowNoResults(false);
      } else {
        setShowResults(false);
        setShowNoResults(true);
      }
    }
  };

  const handleSearch = async (e) => {
    e.preventDefault();

    // 【セキュリティ強化】HTML5 required属性の回避を防ぐためのJavaScriptバリデーション
    // 最初にすべてのエラーをクリア
    clearError('dateValidation');
    clearError('search');
    clearError('prefecture');
    clearError('guestCount');

    let hasValidationError = false;

    // 日付バリデーションチェック
    const dateError = validateDates(
      checkInDate,
      checkOutDate,
      initialCheckInDate,
      initialCheckOutDate,
    );
    if (dateError) {
      setError('dateValidation', dateError);
      hasValidationError = true;
    }

    // 都道府県選択の必須チェック（HTML required属性の回避対策）
    const prefectureValue = e.target.elements['prefecture'].value;
    if (!prefectureValue || prefectureValue === '') {
      setError('prefecture', t('validation.prefecture.required'));
      hasValidationError = true;
    }

    // 人数の範囲チェック（HTML min属性の回避対策）
    const guestCountValue = e.target.elements['guests'].value;
    const guestCountNum = parseInt(guestCountValue, 10);
    if (
      !guestCountValue ||
      isNaN(guestCountNum) ||
      guestCountNum < 1 ||
      guestCountNum > 99
    ) {
      setError('guestCount', t('validation.guestCount.range'));
      hasValidationError = true;
    }

    // バリデーションエラーがある場合は処理中止
    if (hasValidationError) {
      return;
    }

    setIsLoading(true);
    setShowResults(false);
    setShowNoResults(false);
    setShowRefineForm(false); // 検索中は一旦隠す
    setIsServerError(false);
    setOriginalSearchResult(null); // 前回の元検索結果をクリア
    setUpdatedPrices(null); // 前回の再計算価格をクリア
    clearError('search'); // 検索開始時に前回のAPIエラーをクリア

    // flowchart_top_page.dot  の「DB検索」 (API呼び出し)

    // フォームから値を取得
    const criteria = {
      checkInDate: checkInDate,
      checkOutDate: checkOutDate,
      prefectureId: e.target.elements['prefecture'].value,
      guestCount: e.target.elements['guests'].value,
    };

    setSelectedPrefectureId(Number(criteria.prefectureId) || null); // 絞り込み用（無効な値はnullに）

    // URLSearchParams を使ってクエリ文字列を構築
    const params = new URLSearchParams(criteria);

    try {
      const response = await fetch(`/api/search?${params.toString()}`);
      if (!response.ok) {
        // 500エラー (P-900) [cite: 129, 130]
        throw new Error(
          t('messages.error.serverErrorWithStatus', {
            status: response.status,
          }),
        );
      }

      const result = await response.json();

      if (result && result.hotels && result.hotels.length > 0) {
        // 状態 1-B (検索結果あり)
        setSearchResult(result);
        setOriginalSearchResult(result); // 元の検索結果を保存
        setShowResults(true);
        setShowRefineForm(true); // 1-B  では絞り込みも表示
        setShowNoResults(false);
      } else {
        // 状態 1-C (検索結果なし)
        setSearchResult(null);
        setOriginalSearchResult(null);
        setShowResults(false);
        setShowRefineForm(false); // 1-C  では絞り込みは非表示
        setShowNoResults(true);
      }
    } catch (error) {
      // 【セキュリティ】APIエラーの詳細をコンソールに出力しない

      // エラーハンドリング：4xxクライアントエラーと 5xxサーバーエラーを区別
      const errorResult = handleApiError(error, 'search');

      // handleApiErrorの戻り値のerrorTypeに基づいて処理を分岐
      if (errorResult.errorType === 'server') {
        setIsServerError(true); // 5xxエラーとネットワークエラーのみ P-900 表示
      } else {
        // 4xxクライアントエラー：現在のフォームでエラーメッセージを表示
        // handleApiErrorで既にエラーメッセージが設定されている
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (isServerError) {
    return <ServerError onRetry={handleReset} />;
  }

  return (
    <div className="container">
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <h1>{t('app.title')}</h1>
        <LanguageSwitcher />
      </header>

      <main>
        {/* P-010 ホテル検索 (1-A) [cite: 25-37] */}
        <section className="search-section">
          <h2>{t('labels.search')}</h2>

          {/* C-010: 検索フォーム (HTML標準の date を使用) */}
          <form className="search-form" onSubmit={handleSearch}>
            {/* 日付バリデーションエラー表示 */}
            {getError('dateValidation') && (
              <div
                className="date-error-message"
                style={{ gridColumn: '1 / -1' }}
              >
                {getError('dateValidation')}
              </div>
            )}

            {/* APIエラーメッセージ表示（4xxクライアントエラー用） */}
            {getError('search') && (
              <div
                className="api-error-message"
                style={{ gridColumn: '1 / -1' }}
              >
                {getError('search')}
              </div>
            )}

            {/* C-011: チェックイン [cite: 29] */}
            <div className="form-group">
              <label htmlFor="checkin-date">{t('labels.checkInDate')}</label>
              <input
                type="date"
                id="checkin-date"
                name="checkin-date"
                value={checkInDate}
                min={
                  initialCheckInDate || new Date().toISOString().split('T')[0]
                }
                onChange={handleCheckInChange}
                required
              />
            </div>

            {/* C-012: チェックアウト [cite: 30] */}
            <div className="form-group">
              <label htmlFor="checkout-date">{t('labels.checkOutDate')}</label>
              <input
                type="date"
                id="checkout-date"
                name="checkout-date"
                value={checkOutDate}
                min={
                  initialCheckOutDate || new Date().toISOString().split('T')[0]
                }
                onChange={handleCheckOutChange}
                required
              />
            </div>

            {/* C-013: 都道府県選択 */}
            <div className="form-group">
              <label htmlFor="prefecture">{t('labels.prefecture')}</label>
              <select
                id="prefecture"
                name="prefecture"
                defaultValue=""
                required
              >
                <option value="">{t('labels.selectPrefecture')}</option>
                {prefectures.map((prefecture) => (
                  <option key={prefecture.id} value={prefecture.id}>
                    {prefecture.name}
                  </option>
                ))}
              </select>
              {/* セキュリティ: HTML required回避対策のエラー表示 */}
              {getError('prefecture') && (
                <div className="error-message">{getError('prefecture')}</div>
              )}
            </div>

            {/* C-014: 人数選択  */}
            <div className="form-group">
              <label htmlFor="guests">{t('labels.guestCount')}</label>
              <input
                type="number"
                id="guests"
                name="guests"
                value={guestCount}
                min="1"
                max="99"
                required
                onChange={handleGuestCountChange}
              />
              {/* セキュリティ: HTML min/max回避対策のエラー表示 */}
              {getError('guestCount') && (
                <div className="error-message">{getError('guestCount')}</div>
              )}
            </div>

            {/* C-015: 検索ボタン  */}
            <div className="form-actions">
              <button
                type="submit"
                className={`search-button ${
                  isLoading ||
                  getError('dateValidation') ||
                  getError('prefecture') ||
                  getError('guestCount')
                    ? 'btn-disabled'
                    : ''
                }`}
                disabled={
                  isLoading ||
                  getError('dateValidation') ||
                  getError('prefecture') ||
                  getError('guestCount')
                }
                // 【セキュリティ強化】ボタン無効化条件:
                // - dateValidation: 日付バリデーションエラー
                // - prefecture: 都道府県未選択エラー（HTML required回避対策）
                // - guestCount: 人数範囲エラー（HTML min/max回避対策）
                // - getError('search'): 無効化しない → APIエラー後の再試行を許可
              >
                {isLoading ? t('buttons.searching') : t('buttons.search')}
              </button>
            </div>
          </form>
        </section>

        {/* 絞り込みフォーム (C-020) - (1-B)  で表示 */}
        {showRefineForm && (
          <RefineForm
            selectedPrefectureId={selectedPrefectureId}
            onRefineSearch={handleRefineSearch}
          />
        )}

        {/* 検索結果 (C-030) - (1-B)  で表示 */}
        {showResults && (
          <SearchResults
            searchResult={searchResult}
            guestCount={guestCount}
            checkInDate={checkInDate}
            checkOutDate={checkOutDate}
            updatedPrices={updatedPrices}
            isPriceLoading={isPriceLoading}
          />
        )}

        {/* 結果なし (C-040) - (1-C)  で表示 */}
        {showNoResults && <NoResults />}
      </main>
    </div>
  );
};

export default TopPage;
