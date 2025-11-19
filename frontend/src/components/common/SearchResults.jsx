import React, { useState, useMemo } from 'react';
import './SearchResults.css';

/**
 * C-030 検索結果 (状態 1-B)
 * C-031 (室数選択) , C-032 (予約ボタン)
 * 状態 1-D (バリデーション)
 */
const SearchResults = ({ searchResult, guestCount }) => {
  const { hotels } = searchResult;
  // { hotelId: { roomTypeId: count, ... }, ... }
  const [selectedRooms, setSelectedRooms] = useState({});

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

    // 検索時の「宿泊予定人数(合計)」 > 選択した室数の「合計定員」か？
    if (
      guestCount > totalGuestsInSelectedRooms &&
      totalGuestsInSelectedRooms > 0
    ) {
      capacityError = true;
    }

    return {
      totalCapacity: totalGuestsInSelectedRooms,
      capacityError: capacityError,
    };
  }, [selectedRooms, guestCount, hotels]);

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
    // 1-D  エラーがあれば何もしない
    if (validation.capacityError) return;

    const roomsToReserve = selectedRooms[hotelId];
    console.log(`ホテルID ${hotelId} の予約に進みます (次のステップ)`);
    console.log('選択された室数:', roomsToReserve);
    // TODO: P-020 [cite: 83] への遷移 (flowchart_top_to_reservation.dot  の `validation_stock`  実行)
  };

  return (
    <section className="results-section">
      <h2>検索結果</h2>
      {hotels.map((hotel) => (
        <div key={hotel.hotelId} className="hotel-result-card">
          <div className="hotel-header">
            <h3>{hotel.hotelName}</h3>
          </div>
          <table className="room-type-table">
            <thead>
              <tr>
                <th>部屋タイプ</th>
                <th>価格（2泊分/1室）</th>
                <th>空室状況</th>
                <th>室数 (C-031) </th>
              </tr>
            </thead>
            <tbody>
              {hotel.roomTypes.map((roomType) => (
                <tr key={roomType.roomTypeId}>
                  <td>
                    {roomType.roomTypeName}
                    <span className="room-capacity">
                      (定員: {roomType.capacity}名)
                    </span>
                  </td>
                  <td>
                    <span className="room-price">
                      ¥{roomType.price.toLocaleString()}
                    </span>
                    <span className="room-price-per-night">
                      (参考: ¥{(roomType.price / 2).toLocaleString()}/泊)
                    </span>
                  </td>
                  <td>
                    {/* 画面仕様書  (ビジネスホテルXYZ) の「残り3部屋！」のロジック */}
                    {roomType.availableStock <= 3 ? (
                      <span className="remaining-rooms">
                        残り{roomType.availableStock}部屋！
                      </span>
                    ) : (
                      '空室あり'
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
                  宿泊人数（{guestCount}名）に対して、お部屋の定員（合計
                  {validation.totalCapacity}名）が不足しています。
                </span>
              )}
              {/* TODO: 合計金額の表示 */}
              <span className="total-price-display">合計金額: ¥0</span>
              <span className="total-price-note">（2泊 / 諸税込み）</span>
            </div>
            {/* C-032 予約ボタン  */}
            <button
              type="button"
              className={`btn btn-primary ${validation.capacityError ? 'btn-disabled' : ''}`}
              onClick={() => handleReservation(hotel.hotelId)}
              disabled={validation.capacityError} // 1-D  エラー時は無効
            >
              選択した部屋を予約
            </button>
          </div>
        </div>
      ))}
    </section>
  );
};

export default SearchResults;
