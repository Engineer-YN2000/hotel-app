import React from 'react';
import './RefineForm.css';

/**
 * C-020 絞り込みフォーム (状態 1-B)
 * 都道府県内の詳細地域による絞り込みを行う
 * (現在はモック実装のため機能しない)
 */
const RefineForm = () => {
  return (
    <section className="refinement-group">
      <h3>絞り込み条件</h3>

      {/* 地域詳細 (C-021) - 選択された都道府県内の詳細地域による絞り込み */}
      <div className="form-group">
        <label>地域詳細（選択された都道府県内）</label>
        <div className="checkbox-grid">
          {/* 注意: モック実装のため現在は大阪府のエリア例を表示 */}
          <div className="checkbox-group">
            <input type="checkbox" id="area-1" defaultChecked />
            <label htmlFor="area-1">梅田・大阪駅</label>
          </div>
          <div className="checkbox-group">
            <input type="checkbox" id="area-2" />
            <label htmlFor="area-2">なんば・心斎橋</label>
          </div>
          <div className="checkbox-group">
            <input type="checkbox" id="area-3" />
            <label htmlFor="area-3">天王寺</label>
          </div>
          <div className="checkbox-group">
            <input type="checkbox" id="area-4" />
            <label htmlFor="area-4">USJ周辺</label>
          </div>
        </div>
      </div>

      {/* 宿泊金額 (C-022) [cite: 52] (外見のみ) */}
      <div className="price-filter-group">
        <label>宿泊金額（1泊1室あたり）</label>
        <div className="price-filter-controls">
          <input type="number" defaultValue="10000" />
          <span>〜</span>
          <input type="number" defaultValue="40000" />
        </div>
        {/* 2ハンドルシークバー (外見のみ) */}
        <div className="dual-slider-container">
          <div className="dual-slider-rail"></div>
          <div className="dual-slider-range"></div>
          <div className="dual-slider-handle handle-min"></div>
          <div className="dual-slider-handle handle-max"></div>
        </div>
      </div>

      <div className="button-container center">
        <button type="button" className="btn btn-primary">
          再検索・絞り込み
        </button>
      </div>
    </section>
  );
};

export default RefineForm;
