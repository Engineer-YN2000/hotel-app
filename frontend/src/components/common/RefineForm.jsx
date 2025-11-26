import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import './RefineForm.css';

/**
 * C-020 絞り込みフォーム (状態 1-B)
 * 都道府県内の詳細地域による絞り込みを行う
 */
const RefineForm = ({ selectedPrefectureId, onRefineSearch }) => {
  const { t } = useTranslation();
  const [areaDetails, setAreaDetails] = useState([]);
  const [selectedAreas, setSelectedAreas] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  // 都道府県IDが変更されたら詳細地域を取得
  useEffect(() => {
    const fetchAreaDetails = async () => {
      if (!selectedPrefectureId) {
        setAreaDetails([]);
        return;
      }

      setIsLoading(true);
      try {
        const response = await fetch(
          `/api/area-details?prefectureId=${selectedPrefectureId}`,
        );
        if (response.ok) {
          const areas = await response.json();
          setAreaDetails(areas);
          // デフォルトで全地域を選択状態にする
          setSelectedAreas(areas.map((area) => area.areaId));
        } else {
          // 【セキュリティ】APIエラーの詳細をコンソールに出力しない
          setAreaDetails([]);
        }
      } catch (error) {
        // 【セキュリティ】エラー詳細をコンソールに出力しない
        setAreaDetails([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAreaDetails();
  }, [selectedPrefectureId]);

  // チェックボックスの変更処理
  const handleAreaChange = (areaId, isChecked) => {
    if (isChecked) {
      setSelectedAreas((prev) => [...prev, areaId]);
    } else {
      setSelectedAreas((prev) => prev.filter((id) => id !== areaId));
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
