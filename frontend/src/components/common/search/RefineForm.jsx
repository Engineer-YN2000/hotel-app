import React, { useReducer, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import './RefineForm.css';

/**
 * 地域絞り込み状態のアクションタイプ定義
 * 状態遷移を明示的に管理するための定数
 */
const AREA_ACTIONS = {
  FETCH_START: 'FETCH_START', // フェッチ開始
  FETCH_SUCCESS: 'FETCH_SUCCESS', // フェッチ成功
  FETCH_ERROR: 'FETCH_ERROR', // フェッチ失敗
  RESET: 'RESET', // 状態リセット
  SELECT_AREA: 'SELECT_AREA', // 地域選択
  DESELECT_AREA: 'DESELECT_AREA', // 地域選択解除
};

/**
 * 地域絞り込み状態の初期値
 */
const initialAreaState = {
  areaDetails: [], // 取得した地域リスト
  selectedAreas: [], // 選択された地域ID
  isLoading: false, // ローディング状態
};

/**
 * 地域絞り込み状態のReducer
 *
 * 【設計原則】
 * - 3つの関連する状態（areaDetails, selectedAreas, isLoading）を一元管理
 * - 状態遷移を明示的なアクションで制御し、予測可能性を向上
 * - イミュータブルな更新パターンを保証
 *
 * @param {Object} state - 現在の状態
 * @param {Object} action - ディスパッチされたアクション
 * @returns {Object} 新しい状態
 */
const areaReducer = (state, action) => {
  switch (action.type) {
    case AREA_ACTIONS.FETCH_START:
      return {
        ...state,
        isLoading: true,
      };

    case AREA_ACTIONS.FETCH_SUCCESS:
      return {
        ...state,
        isLoading: false,
        areaDetails: action.payload.areas,
        // デフォルトで全地域を選択状態にする
        selectedAreas: action.payload.areas.map((area) => area.areaId),
      };

    case AREA_ACTIONS.FETCH_ERROR:
    case AREA_ACTIONS.RESET:
      return {
        ...state,
        isLoading: false,
        areaDetails: [],
        selectedAreas: [],
      };

    case AREA_ACTIONS.SELECT_AREA:
      return {
        ...state,
        selectedAreas: [...state.selectedAreas, action.payload.areaId],
      };

    case AREA_ACTIONS.DESELECT_AREA:
      return {
        ...state,
        selectedAreas: state.selectedAreas.filter(
          (id) => id !== action.payload.areaId,
        ),
      };

    default:
      return state;
  }
};

/**
 * C-020 絞り込みフォーム (状態 1-B)
 * 都道府県内の詳細地域による絞り込みを行う
 *
 * 【状態管理】
 * useReducerにより関連する3つの状態を一元管理:
 * - areaDetails: 地域リスト
 * - selectedAreas: 選択された地域
 * - isLoading: ローディング状態
 */
const RefineForm = ({ selectedPrefectureId, onRefineSearch }) => {
  const { t } = useTranslation();
  const [state, dispatch] = useReducer(areaReducer, initialAreaState);
  const { areaDetails, selectedAreas, isLoading } = state;

  // 都道府県IDが変更されたら詳細地域を取得
  useEffect(() => {
    const fetchAreaDetails = async () => {
      if (!selectedPrefectureId) {
        dispatch({ type: AREA_ACTIONS.RESET });
        return;
      }

      dispatch({ type: AREA_ACTIONS.FETCH_START });
      try {
        const response = await fetch(
          `/api/area-details?prefectureId=${selectedPrefectureId}`,
        );
        if (response.ok) {
          const areas = await response.json();
          dispatch({
            type: AREA_ACTIONS.FETCH_SUCCESS,
            payload: { areas },
          });
        } else {
          // 【セキュリティ】APIエラーの詳細をコンソールに出力しない
          dispatch({ type: AREA_ACTIONS.FETCH_ERROR });
        }
      } catch (error) {
        // 【セキュリティ】エラー詳細をコンソールに出力しない
        dispatch({ type: AREA_ACTIONS.FETCH_ERROR });
      }
    };

    fetchAreaDetails();
  }, [selectedPrefectureId]);

  // チェックボックスの変更処理
  const handleAreaChange = (areaId, isChecked) => {
    if (isChecked) {
      dispatch({ type: AREA_ACTIONS.SELECT_AREA, payload: { areaId } });
    } else {
      dispatch({ type: AREA_ACTIONS.DESELECT_AREA, payload: { areaId } });
    }
  };

  // 絞り込み検索実行
  const handleRefineSearch = () => {
    if (onRefineSearch) {
      onRefineSearch(selectedAreas);
    }
  };

  return (
    <section className="refinement-group">
      <h3>{t('labels.refineConditions')}</h3>

      {/* 地域詳細 (C-021) - 選択された都道府県内の詳細地域による絞り込み */}
      <div className="form-group">
        <label>{t('labels.areaDetails')}</label>
        {isLoading ? (
          <div className="loading-message">{t('messages.loading')}</div>
        ) : areaDetails.length > 0 ? (
          <div className="checkbox-grid">
            {areaDetails.map((area) => (
              <div key={area.areaId} className="checkbox-group">
                <input
                  type="checkbox"
                  id={`area-${area.areaId}`}
                  checked={selectedAreas.includes(area.areaId)}
                  onChange={(e) =>
                    handleAreaChange(area.areaId, e.target.checked)
                  }
                />
                <label htmlFor={`area-${area.areaId}`}>{area.areaName}</label>
              </div>
            ))}
          </div>
        ) : (
          <div className="no-areas-message">
            {t('messages.noAreasAvailable')}
          </div>
        )}
      </div>

      <div className="button-container center">
        <button
          type="button"
          className="btn btn-primary"
          onClick={handleRefineSearch}
          disabled={selectedAreas.length === 0}
        >
          {t('buttons.refineSearch')}
        </button>
      </div>
    </section>
  );
};

export default RefineForm;
