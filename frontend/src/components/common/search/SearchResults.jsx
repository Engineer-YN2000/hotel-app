import React, { useState, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import './SearchResults.css';

/**
 * C-030 検索結果 (状態 1-B)
 * C-031 (室数選択) , C-032 (予約ボタン)
 * 状態 1-D (バリデーション)
 */
const SearchResults = ({
  searchResult,
  guestCount,
  checkInDate,
  checkOutDate,
  updatedPrices,
  isPriceLoading,
}) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { hotels } = searchResult;
  // { hotelId: { roomTypeId: count, ... }, ... }
  const [selectedRooms, setSelectedRooms] = useState({});

  // 定数定義 - マジックナンバー排除によりメンテナンス性と一貫性を保持
  const DEFAULT_NIGHTS = 2; // 日付計算異常時のフォールバック値
  const LOW_STOCK_THRESHOLD = 3; // 「残り○部屋！」表示の閾値
  const MIN_ROOM_COUNT = 0; // 室数選択の最小値（input要素のmin/defaultValue属性で使用）

  /**
   * 部屋タイプの価格を取得
   * 価格再計算結果があればそれを使用、なければ元の価格を使用
   */
  const getRoomPrice = useCallback(
    (hotelId, roomTypeId, originalPrice) => {
      if (
        updatedPrices &&
        updatedPrices[`${hotelId}-${roomTypeId}`] !== undefined
      ) {
        return updatedPrices[`${hotelId}-${roomTypeId}`];
      }
      return originalPrice;
    },
    [updatedPrices],
  );

  // 宿泊日数を計算
  const numberOfNights = useMemo(() => {
    if (!checkInDate || !checkOutDate) return DEFAULT_NIGHTS;
    const checkIn = new Date(checkInDate);
    const checkOut = new Date(checkOutDate);
    const timeDiff = checkOut.getTime() - checkIn.getTime();
    const dayDiff = Math.ceil(timeDiff / (1000 * 3600 * 24));
    return dayDiff > 0 ? dayDiff : DEFAULT_NIGHTS; // 異常時はデフォルト値を使用
  }, [checkInDate, checkOutDate]);

  /**
   * 複雑なビジネスロジック - リアルタイム定員バリデーション
   *
   * 【処理概要】
   * このuseMemoフックは、ユーザーが選択した部屋の合計定員と、
   * 検索時に指定した宿泊人数を比較し、予約可能性をリアルタイム判定します。
   *
   * 【アルゴリズム】
   * 1. 全ホテル・全部屋タイプを走査し、選択数量×定員を累積計算
   * 2. 合計定員と宿泊人数を比較してエラー種別を決定
   * 3. バリデーション結果を返却し、UIの状態制御に使用
   *
   * 【パフォーマンス考慮】
   * - useMemoによる結果キャッシュ（selectedRooms、guestCount、hotels変更時のみ再計算）
   * - O(h×r×s)の計算量（h:ホテル数, r:部屋タイプ数, s:選択された部屋）
   *
   * 【ビジネスルール】
   * - 部屋未選択: 予約ボタン無効化
   * - 定員不足: エラーメッセージ表示＋予約ボタン無効化
   * - 定員充足: 予約処理許可
   */
  const validation = useMemo(() => {
    let totalGuestsInSelectedRooms = 0;
    let capacityError = false;
    let roomSelectionError = false;

    // 【Step1】選択された全室の合計定員を計算
    // ネストしたforEachによる全組み合わせ走査
    hotels.forEach((hotel) => {
      hotel.roomTypes.forEach((roomType) => {
        const count = selectedRooms[hotel.hotelId]?.[roomType.roomTypeId] || 0;
        totalGuestsInSelectedRooms += count * roomType.roomCapacity;
      });
    });

    const hasSelectedRooms = totalGuestsInSelectedRooms > 0;

    // 【Step2】バリデーションロジックの優先順位付き判定
    // 優先度1: 部屋未選択エラー（基本的な選択要求）
    // 優先度2: 定員不足エラー（ビジネスルール違反）
    if (!hasSelectedRooms) {
      roomSelectionError = true;
    } else if (guestCount > totalGuestsInSelectedRooms) {
      capacityError = true;
    }

    return {
      totalCapacity: totalGuestsInSelectedRooms,
      capacityError: capacityError,
      roomSelectionError: roomSelectionError,
      hasSelectedRooms: hasSelectedRooms,
      hasAnyValidationError: capacityError || roomSelectionError,
    };
  }, [selectedRooms, guestCount, hotels]);

  // ホテルごとの予約ボタン無効化条件と合計金額計算
  const getReservationButtonState = useMemo(() => {
    const hotelStates = {};
    hotels.forEach((hotel) => {
      const hotelRooms = selectedRooms[hotel.hotelId] || {};

      // そのホテルで選択された部屋の合計定員と合計金額を計算
      let hotelTotalCapacity = 0;
      let hotelTotalPrice = 0;
      hotel.roomTypes.forEach((roomType) => {
        const count = hotelRooms[roomType.roomTypeId] || 0;
        hotelTotalCapacity += count * roomType.roomCapacity;
        // 価格再計算結果があればそれを使用
        const price = getRoomPrice(
          hotel.hotelId,
          roomType.roomTypeId,
          roomType.price,
        );
        hotelTotalPrice += count * price;
      });

      const hasSelectedRooms = hotelTotalCapacity > 0;
      // 定員が宿泊人数以上の場合のみ予約可能
      const hasEnoughCapacity = hotelTotalCapacity >= guestCount;

      hotelStates[hotel.hotelId] = {
        disabled: !hasSelectedRooms || !hasEnoughCapacity,
        hasSelectedRooms,
        totalPrice: hotelTotalPrice,
        totalCapacity: hotelTotalCapacity,
      };
    });
    return hotelStates;
  }, [hotels, selectedRooms, guestCount, getRoomPrice]);

  const handleRoomCountChange = (hotelId, roomTypeId, count) => {
    // 数値を最小値以上に補正
    const newCount = Math.max(
      MIN_ROOM_COUNT,
      parseInt(count, 10) || MIN_ROOM_COUNT,
    );

    setSelectedRooms((prev) => ({
      ...prev,
      [hotelId]: {
        ...prev[hotelId],
        [roomTypeId]: newCount,
      },
    }));
  };

  const handleReservation = async (hotelId) => {
    // バリデーションエラーチェック
    if (validation.hasAnyValidationError) {
      return;
    }
    const roomsToReserve = selectedRooms[hotelId];
    const hasSelectedRooms =
      roomsToReserve &&
      Object.values(roomsToReserve).some((count) => count > 0);
    if (!hasSelectedRooms) {
      return;
    }

    // 対象ホテルの部屋情報を取得
    const hotel = hotels.find((h) => h.hotelId === hotelId);
    if (!hotel) {
      navigate('/server-error');
      return;
    }

    // バックエンドDTOの形式に変換: [{ roomTypeId, roomCount }, ...]
    const roomsPayload = Object.entries(roomsToReserve)
      .filter(([, count]) => count > 0)
      .map(([roomTypeIdStr, count]) => {
        const roomTypeId = parseInt(roomTypeIdStr, 10);
        return {
          roomTypeId,
          roomCount: count,
        };
      });

    // 防御的チェック: フィルタ後に部屋が残っているか確認
    // （UIで無効化されているが、API操作による空配列送信を防止）
    if (roomsPayload.length === 0) {
      return;
    }

    // 予約API呼び出し
    try {
      const payload = {
        checkInDate,
        checkOutDate,
        rooms: roomsPayload,
      };
      const response = await fetch('/api/reservations/pending', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        throw new Error('Reservation API error');
      }
      // 正常時はP-020（予約詳細入力ページ）へ遷移
      // レスポンスには reservationId, accessToken, sessionToken が含まれる
      const data = await response.json();
      navigate(
        `/reservation/${data.reservationId}?token=${encodeURIComponent(data.accessToken)}&sessionToken=${encodeURIComponent(data.sessionToken)}`,
      );
    } catch (error) {
      // エラー時はServerErrorページへ遷移
      navigate('/server-error');
    }
  };

  return (
    <section className="results-section">
      <h2>{t('labels.searchResultsWithCount', { count: hotels.length })}</h2>
      {hotels.map((hotel) => (
        <div key={hotel.hotelId} className="hotel-result-card">
          <div className="hotel-header">
            <h3>{hotel.hotelName}</h3>
          </div>
          <table className="room-type-table">
            <thead>
              <tr>
                <th>{t('labels.roomType')}</th>
                <th>
                  {t('labels.priceForNights', { nights: numberOfNights })}
                </th>
                <th>{t('labels.availability')}</th>
                <th>{t('labels.roomCount')}</th>
              </tr>
            </thead>
            <tbody>
              {hotel.roomTypes.map((roomType) => (
                <tr key={roomType.roomTypeId}>
                  <td>
                    {roomType.roomTypeName}
                    <span className="room-capacity">
                      ({t('labels.capacity', { count: roomType.roomCapacity })})
                    </span>
                  </td>
                  <td>
                    {isPriceLoading ? (
                      <span className="price-loading">
                        {t('labels.priceCalculating')}
                      </span>
                    ) : (
                      <>
                        <span className="room-price">
                          ¥
                          {getRoomPrice(
                            hotel.hotelId,
                            roomType.roomTypeId,
                            roomType.price,
                          ).toLocaleString()}
                        </span>
                        <span className="room-price-per-night">
                          (
                          {t('labels.referencePerNight', {
                            price: Math.round(
                              getRoomPrice(
                                hotel.hotelId,
                                roomType.roomTypeId,
                                roomType.price,
                              ) / numberOfNights,
                            ).toLocaleString(),
                          })}
                          )
                        </span>
                      </>
                    )}
                  </td>
                  <td>
                    {/* 画面仕様書  (ビジネスホテルXYZ) の「残り3部屋！」のロジック */}
                    {roomType.availableStock <= LOW_STOCK_THRESHOLD ? (
                      <span className="remaining-rooms">
                        {t('labels.remainingRooms', {
                          count: roomType.availableStock,
                        })}
                      </span>
                    ) : (
                      t('labels.roomsAvailable')
                    )}
                  </td>
                  <td>
                    <input
                      type="number"
                      className="room-select-input"
                      min={MIN_ROOM_COUNT.toString()}
                      max={roomType.availableStock} // 在庫数以上は選択不可
                      defaultValue={MIN_ROOM_COUNT.toString()}
                      onChange={(e) =>
                        handleRoomCountChange(
                          hotel.hotelId,
                          roomType.roomTypeId,
                          e.target.value,
                        )
                      }
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="hotel-booking-footer">
            <div className="booking-summary">
              {/* 合計金額表示 */}
              {getReservationButtonState[hotel.hotelId]?.hasSelectedRooms && (
                <div className="total-price-section">
                  <span className="total-price-display">
                    {t('labels.totalAmount')}: ¥
                    {getReservationButtonState[
                      hotel.hotelId
                    ]?.totalPrice.toLocaleString()}
                  </span>
                  <span className="total-price-note">
                    {t('labels.selectedCapacity', {
                      count:
                        getReservationButtonState[hotel.hotelId]?.totalCapacity,
                    })}
                  </span>
                </div>
              )}
              {/* 状態 1-D (インラインバリデーションエラー)  */}
              <div className="validation-messages">
                {validation.roomSelectionError && (
                  <span className="room-selection-error-message">
                    {t('validation.form.roomSelectionRequired')}
                  </span>
                )}
                {validation.capacityError && (
                  <span className="capacity-error-message">
                    {t('validation.form.capacityError', {
                      guestCount,
                      totalCapacity: validation.totalCapacity,
                    })}
                  </span>
                )}
              </div>
            </div>
            {/* C-032 予約ボタン  */}
            <button
              type="button"
              className={`btn btn-primary ${getReservationButtonState[hotel.hotelId]?.disabled ? 'btn-disabled' : ''}`}
              onClick={() => handleReservation(hotel.hotelId)}
              disabled={getReservationButtonState[hotel.hotelId]?.disabled} // 1-D  エラー時または部屋未選択時は無効
            >
              {t('buttons.reserveSelectedRooms')}
            </button>
          </div>
        </div>
      ))}
    </section>
  );
};

export default SearchResults;
