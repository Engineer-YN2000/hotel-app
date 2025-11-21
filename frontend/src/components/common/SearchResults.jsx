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

  // 宿泊日数を計算
  const numberOfNights = useMemo(() => {
    if (!checkInDate || !checkOutDate) return 2; // デフォルトは2泊
    const checkIn = new Date(checkInDate);
    const checkOut = new Date(checkOutDate);
    const timeDiff = checkOut.getTime() - checkIn.getTime();
    const dayDiff = Math.ceil(timeDiff / (1000 * 3600 * 24));
    return dayDiff > 0 ? dayDiff : 2; // 異常時は2泊をデフォルトとする
  }, [checkInDate, checkOutDate]);

  // 1-D  のバリデーションロジック
  const validation = useMemo(() => {
    let totalGuestsInSelectedRooms = 0;
    let capacityError = false;

    // 選択された全室の合計定員を計算
    hotels.forEach((hotel) => {
      hotel.roomTypes.forEach((roomType) => {
        const count = selectedRooms[hotel.hotelId]?.[roomType.roomTypeId] || 0;
        totalGuestsInSelectedRooms += count * roomType.capacity;
      });
    });

    // 部屋が選択されており、かつ宿泊予定人数が選択した部屋の合計定員を超えている場合にエラー
    if (
      totalGuestsInSelectedRooms > 0 &&
      guestCount > totalGuestsInSelectedRooms
    ) {
      capacityError = true;
    }

    return {
      totalCapacity: totalGuestsInSelectedRooms,
      capacityError: capacityError,
      hasSelectedRooms: totalGuestsInSelectedRooms > 0,
    };
  }, [selectedRooms, guestCount, hotels]);

  // ホテルごとの予約ボタン無効化条件
  const getReservationButtonState = useMemo(() => {
    const hotelStates = {};
    hotels.forEach((hotel) => {
      const hotelRooms = selectedRooms[hotel.hotelId] || {};
      const hasSelectedRooms = Object.values(hotelRooms).some(
        (count) => count > 0,
      );
      hotelStates[hotel.hotelId] = {
        disabled: validation.capacityError || !hasSelectedRooms,
        hasSelectedRooms,
      };
    });
    return hotelStates;
  }, [hotels, selectedRooms, validation.capacityError]);

  const handleRoomCountChange = (hotelId, roomTypeId, count) => {
    // 数値を0以上に補正
    const newCount = Math.max(0, parseInt(count, 10) || 0);

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
    if (validation.capacityError) {
      console.warn('予約処理中止: 定員エラーが発生しています', {
        hotelId,
        guestCount,
        totalCapacity: validation.totalCapacity,
        capacityError: validation.capacityError,
      });
      return;
    }

    const roomsToReserve = selectedRooms[hotelId];

    // 部屋が選択されているかチェック
    const hasSelectedRooms =
      roomsToReserve &&
      Object.values(roomsToReserve).some((count) => count > 0);

    if (!hasSelectedRooms) {
      console.warn('予約処理中止: 部屋が選択されていません', {
        hotelId,
        selectedRooms: roomsToReserve,
      });
      return;
    }

    console.log(`ホテルID ${hotelId} の予約に進みます (次のステップ)`);
    console.log('選択された室数:', roomsToReserve);
    console.log('バリデーション状態:', {
      guestCount,
      totalCapacity: validation.totalCapacity,
      capacityError: validation.capacityError,
    });
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
                    {roomType.availableStock <= 3 ? (
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
                      min="0"
                      max={roomType.availableStock} // 在庫数以上は選択不可
                      defaultValue="0"
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
