import React, { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
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
}) => {
  const { t } = useTranslation();
  const { hotels } = searchResult;
  // { hotelId: { roomTypeId: count, ... }, ... }
  const [selectedRooms, setSelectedRooms] = useState({});

  // 定数定義 - マジックナンバー排除によりメンテナンス性と一貫性を保持
  const DEFAULT_NIGHTS = 2; // 日付計算異常時のフォールバック値
  const LOW_STOCK_THRESHOLD = 3; // 「残り○部屋！」表示の閾値
  const MIN_ROOM_COUNT = 0; // 室数選択の最小値（input要素のmin/defaultValue属性で使用）

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
        totalGuestsInSelectedRooms += count * roomType.capacity;
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

  // ホテルごとの予約ボタン無効化条件
  const getReservationButtonState = useMemo(() => {
    const hotelStates = {};
    hotels.forEach((hotel) => {
      const hotelRooms = selectedRooms[hotel.hotelId] || {};

      // そのホテルで選択された部屋の合計定員を計算
      let hotelTotalCapacity = 0;
      hotel.roomTypes.forEach((roomType) => {
        const count = hotelRooms[roomType.roomTypeId] || 0;
        hotelTotalCapacity += count * roomType.capacity;
      });

      const hasSelectedRooms = hotelTotalCapacity > 0;
      // 定員が宿泊人数以上の場合のみ予約可能
      const hasEnoughCapacity = hotelTotalCapacity >= guestCount;

      hotelStates[hotel.hotelId] = {
        disabled: !hasSelectedRooms || !hasEnoughCapacity,
        hasSelectedRooms,
      };
    });
    return hotelStates;
  }, [hotels, selectedRooms, guestCount]);

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

  const handleReservation = (hotelId) => {
    // バリデーションエラーチェック
    if (validation.hasAnyValidationError) {
      // 【セキュリティ】ビジネスロジックの詳細をコンソールに出力しない
      return;
    }

    const roomsToReserve = selectedRooms[hotelId];

    // 部屋が選択されているかチェック
    const hasSelectedRooms =
      roomsToReserve &&
      Object.values(roomsToReserve).some((count) => count > 0);

    if (!hasSelectedRooms) {
      // 【セキュリティ】部屋選択状態の詳細をコンソールに出力しない
      return;
    }

    // 【セキュリティ】予約処理の詳細をコンソールに出力しない
    // 予約処理を続行
    // 次のステップ: 予約フォームへの遷移処理を実装
    // TODO: P-020 [cite: 83] への遷移 (flowchart_top_to_reservation.dot  の `validation_stock`  実行)
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
                      ({t('labels.capacity', { count: roomType.capacity })})
                    </span>
                  </td>
                  <td>
                    <span className="room-price">
                      ¥{roomType.price.toLocaleString()}
                    </span>
                    <span className="room-price-per-night">
                      (
                      {t('labels.referencePerNight', {
                        price: Math.round(
                          roomType.price / numberOfNights,
                        ).toLocaleString(),
                      })}
                      )
                    </span>
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
              {/* 状態 1-D (インラインバリデーションエラー)  */}
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
