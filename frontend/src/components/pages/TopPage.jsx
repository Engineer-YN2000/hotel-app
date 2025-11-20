import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
// import CalendarModal from '../common/CalendarModal'; // (カレンダーは今回使わない)
import RefineForm from '../common/RefineForm'; // C-020
import SearchResults from '../common/SearchResults'; // C-030
import NoResults from '../common/NoResults'; // C-040
import ServerError from '../common/ServerError'; // P-900
import LanguageSwitcher from '../common/LanguageSwitcher';
import { useI18nValidation } from '../../hooks/useI18nValidation';
import './TopPage.css';

/**
 * P-010 ホテル検索ページ
 */
const TopPage = () => {
  const { t, i18n } = useTranslation(); // MessageSourceと同等の機能

  // 状態定義 (1-A) [cite: 26-34]
  const [showRefineForm, setShowRefineForm] = useState(false); // C-020
  const [showResults, setShowResults] = useState(false); // C-030
  const [showNoResults, setShowNoResults] = useState(false); // C-040

  const [isServerError, setIsServerError] = useState(false); // P-900

  const [isLoading, setIsLoading] = useState(false);
  const [searchResult, setSearchResult] = useState(null);
  const [guestCount, setGuestCount] = useState(1); // 1-D  の検証用に保持

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

  // 初期化時にgetInitialDataから各種UI値を取得＆設定
  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const response = await fetch('/api/initial-data');
        if (response.ok) {
          const data = await response.json();
          console.log('初期データ取得:', data);

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
        console.error('初期データの取得に失敗しました', error);
      }
    };

    fetchInitialData();
  }, [clearError]);

  // チェックイン日変更時の処理
  const handleCheckInChange = (e) => {
    const newCheckInDate = e.target.value;
    setCheckInDate(newCheckInDate);

    // チェックイン日が変更されたら、チェックアウト日をチェックイン日+1に設定
    if (newCheckInDate) {
      const checkInDate = new Date(newCheckInDate);
      const nextDay = new Date(checkInDate);
      nextDay.setDate(checkInDate.getDate() + 1);
      const newCheckOutDate = nextDay.toISOString().split('T')[0];
      setCheckOutDate(newCheckOutDate);

      // 自動調整後はエラーをクリア（適切な日付が自動設定されるため）
      clearError('dateValidation');
    } else {
      // チェックイン日が空の場合はバリデーション実行
      const error = validateDates(
        newCheckInDate,
        checkOutDate,
        initialCheckInDate,
        initialCheckOutDate,
      );
      if (error) setError('dateValidation', error);
      else clearError('dateValidation');
    }

    // 初期推奨日以前を選択した場合のバリデーション（HTML5 min属性との二重防御）
    if (newCheckInDate && initialCheckInDate) {
      const selectedDate = new Date(newCheckInDate);
      const initialDate = new Date(initialCheckInDate);
      if (selectedDate < initialDate) {
        const error = validateDates(
          newCheckInDate,
          checkOutDate,
          initialCheckInDate,
          initialCheckOutDate,
        );
        if (error) setError('dateValidation', error);
      }
    }
  };

  // チェックアウト日変更時の処理
  const handleCheckOutChange = (e) => {
    const newCheckOutDate = e.target.value;
    setCheckOutDate(newCheckOutDate);

    // チェックアウト日を能動的に変更した時のみバリデーションを実行
    const error = validateDates(
      checkInDate,
      newCheckOutDate,
      initialCheckInDate,
      initialCheckOutDate,
    );
    if (error) setError('dateValidation', error);
    else clearError('dateValidation');
  };

  // リセット処理
  const handleReset = () => {
    setIsServerError(false);
    setShowResults(false);
    setShowNoResults(false);
    setShowRefineForm(false);
    setSearchResult(null);
    setIsLoading(false);
  };

  const handleSearch = async (e) => {
    e.preventDefault();

    // 日付バリデーションチェック
    const error = validateDates(
      checkInDate,
      checkOutDate,
      initialCheckInDate,
      initialCheckOutDate,
    );
    if (error) {
      setError('dateValidation', error);
      return;
    }
    clearError('dateValidation');

    setIsLoading(true);
    setShowResults(false);
    setShowNoResults(false);
    setShowRefineForm(false); // 検索中は一旦隠す
    setIsServerError(false);

    // flowchart_top_page.dot  の「DB検索」 (API呼び出し)

    // フォームから値を取得
    const criteria = {
      checkInDate: checkInDate,
      checkOutDate: checkOutDate,
      prefectureId: e.target.elements['prefecture'].value,
      guestCount: e.target.elements['guests'].value,
    };

    setGuestCount(parseInt(criteria.guestCount, 10)); // 1-D  のために保持

    // URLSearchParams を使ってクエリ文字列を構築
    const params = new URLSearchParams(criteria);

    try {
      console.log('検索リクエスト送信:', `/api/search?${params.toString()}`);
      const response = await fetch(`/api/search?${params.toString()}`);

      console.log('レスポンス受信:', response.status, response.ok);
      if (!response.ok) {
        // 500エラー (P-900) [cite: 129, 130]
        throw new Error(`サーバーエラー: ${response.status}`);
      }

      const result = await response.json();
      console.log('レスポンスデータ:', result);

      // デバッグ用: APIレスポンスの詳細をチェック
      console.log('result:', result);
      console.log('result.hotels:', result?.hotels);
      console.log('result.hotels?.length:', result?.hotels?.length);

      if (result && result.hotels && result.hotels.length > 0) {
        // 状態 1-B (検索結果あり)
        console.log('検索結果あり - ホテル数:', result.hotels.length);
        setSearchResult(result);
        setShowResults(true);
        setShowRefineForm(true); // 1-B  では絞り込みも表示
        setShowNoResults(false);
      } else {
        // 状態 1-C (検索結果なし)
        console.log('検索結果なし - result:', result);
        setSearchResult(null);
        setShowResults(false);
        setShowRefineForm(false); // 1-C  では絞り込みは非表示
        setShowNoResults(true);
      }
    } catch (error) {
      console.error('検索APIの呼び出しに失敗しました', error);
      console.error('エラー詳細:', error.message);
      // 新しいエラーハンドリングシステムを使用
      handleApiError(error, 'search');
      setIsServerError(true); // エラーフラグを立てる
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
                defaultValue="27"
                required
              >
                <option value="">{t('labels.selectPrefecture')}</option>
                {prefectures.map((prefecture, index) => (
                  <option key={index + 1} value={index + 1}>
                    {prefecture}
                  </option>
                ))}
              </select>
            </div>

            {/* C-014: 人数選択  */}
            <div className="form-group">
              <label htmlFor="guests">{t('labels.guestCount')}</label>
              <input
                type="number"
                id="guests"
                name="guests"
                defaultValue="2"
                min="1"
              />
            </div>

            {/* C-015: 検索ボタン  */}
            <div className="form-actions">
              <button
                type="submit"
                className={`search-button ${isLoading || getError('dateValidation') ? 'btn-disabled' : ''}`}
                disabled={isLoading || getError('dateValidation')}
              >
                {isLoading ? t('buttons.searching') : t('buttons.search')}
              </button>
            </div>
          </form>
        </section>

        {/* 絞り込みフォーム (C-020) - (1-B)  で表示 */}
        {showRefineForm && <RefineForm />}

        {/* 検索結果 (C-030) - (1-B)  で表示 */}
        {showResults && (
          <SearchResults searchResult={searchResult} guestCount={guestCount} />
        )}

        {/* 結果なし (C-040) - (1-C)  で表示 */}
        {showNoResults && <NoResults />}
      </main>
    </div>
  );
};

export default TopPage;
