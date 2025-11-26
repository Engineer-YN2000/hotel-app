package com.example.hotel.utils;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

import com.example.hotel.config.PriceProperties;

import jakarta.annotation.PostConstruct;

/**
 * 価格計算ユーティリティクラス
 *
 * ホテルごとの料金体系とその日の需要定数を考慮したダイナミックプライシングを実装
 *
 * 【重要】価格計算の責務範囲:
 * - このクラスは「1泊あたりの部屋料金」のみを計算
 * - 宿泊日数による乗算は呼び出し側（DTOやサービス層）で実行
 * - 複数泊の総額計算は RoomTypeResultDto.calculateTotalPrice() で実装
 *
 * 【注意】
 * このクラスは演習用のモッククラスであり、実際の運用環境ではプロパティファイルやデータベースから設定を取得する設計が推奨されます。
 */
@Component
public class PriceCalculator {

  private static PriceProperties properties;

  private final PriceProperties priceProperties;

  /**
   * ユーティリティクラスのため、インスタンス化を防ぐprivateコンストラクタ
   */
  private PriceCalculator(PriceProperties priceProperties) {
    this.priceProperties = priceProperties;
  }

  @PostConstruct
  private void init() {
    properties = this.priceProperties;
  }

  /**
   * 部屋の定員に基づいて価格を計算します（後方互換性のため）
   *
   * 【ダイナミックプライシングの動作】
   * このメソッドは現在日（LocalDate.now()）を使用するため、実行日によって価格が変動します。
   * これは実際のホテル業界の価格戦略を模擬したもので、以下の要素により価格が動的に決定されます：
   * - 需要変動: 日付により需要係数が20日周期で変動（季節性・イベント等をシミュレート）
   * - 時間的変動: 同じ検索条件でも日が変わると価格が変わる（現実のホテル予約と同様）
   * - 予測不可能性: ユーザーが予約タイミングを最適化する行動をシミュレート
   *
   * 【価格固定が必要な場合】
   * テストや一貫した価格表示が必要な場合は、3引数版のメソッドで固定日付を指定してください。
   *
   * @param capacity 部屋の定員
   * @return 計算された価格（1泊あたりの部屋全体料金・円）※実行日により変動
   */
  public static Integer calculatePrice(Integer capacity) {
    return calculatePrice(capacity, 1, LocalDate.now());
  }

  /**
   * ホテルIDと日付を考慮したダイナミックプライシング価格計算
   *
   * 同じホテルでは同じ料金体系を使用し、その日の需要定数で価格を調整
   *
   * 【価格計算仕様】
   * - 戻り値: 1泊あたりの部屋全体料金（宿泊日数の乗算は呼び出し側で実行）
   * - 基準価格: BASE_PRICE_PER_PERSON × 定員数 × 定員割引率
   * - 変動要素: ホテル料金係数 × その日の需要係数
   *
   * 【引数制約】
   * - hotelId: null禁止。ダイナミックプライシングに必須のホテル識別子
   * - date: null禁止。需要ベースの価格調整に必須の日付情報
   * - capacity: nullまたは0以下の場合は基準価格を返却
   *
   * @param capacity 部屋の定員（nullまたは0以下の場合は基準価格を適用）
   * @param hotelId ホテルID（料金体系の決定に使用、null禁止）
   * @param date 宿泊予定日（需要定数の決定に使用、null禁止）
   * @return 計算された価格（1泊あたりの部屋全体料金・円）
   * @throws IllegalArgumentException hotelIdまたはdateがnullの場合
   */
  /**
   * 複雑なダイナミックプライシング計算アルゴリズム
   *
   * 【アルゴリズム概要】
   * 複数の変動要素を組み合わせた価格計算を行い、リアルタイムで適正価格を算出します。
   * ホテル業界で一般的な「定員割引」「ホテルグレード」「需要変動」を数式化した実装。
   *
   * 【計算ステップ】
   * 1. 引数バリデーション（null安全性の確保）
   * 2. ホテル係数の計算（ホテルIDベースの一貫した価格体系）
   * 3. 定員係数の適用（宿泊人数に基づく効率性割引）
   * 4. 基本価格の算出（基準価格 × 定員係数 × 定員数 × ホテル係数）
   * 5. 需要係数の計算（日付ベースの季節変動）
   * 6. 最終価格の決定（基本価格 × 需要係数、最低価格保証付き）
   *
   * 【数学的背景】
   * - ホテル係数: ホテルID % 定数による疑似ランダムだが一貫した値
   * - 需要係数: dayOfYear % 周期による正弦波様の変動パターン
   * - 最低価格保証: Math.max()による下限設定でビジネス要件を満足
   *
   * 【パフォーマンス特性】
   * - 時間計算量: O(1) - 全て定数時間操作
   * - 空間計算量: O(1) - 固定メモリ使用量
   * - 再現性: 同じ引数に対して常に同じ結果を返す（副作用なし）
   */
  public static Integer calculatePrice(Integer capacity, Integer hotelId, LocalDate date) {
    // 【Step1】引数バリデーション - null安全性の確保
    // 【設計意図】技術的詳細を隠蔽し、プレゼンテーション層で適切なユーザーメッセージに変換する
    if (hotelId == null) {
      throw new IllegalArgumentException("PRICE_CALCULATION_ERROR");
    }
    if (date == null) {
      throw new IllegalArgumentException("PRICE_CALCULATION_ERROR");
    }

    // 【Step2】容量チェック - 異常値に対するフォールバック
    if (capacity == null || capacity <= 0) {
      return properties.getBasePerPerson();
    }

    // 【Step3】ホテル固有係数の計算 - IDベースの一貫した価格体系
    // ホテルIDを基にした疑似ランダムだが再現可能な係数生成
    double hotelPriceMultiplier = properties.getHotelPriceBaseMultiplier()
        + ((hotelId % properties.getHotelVariationCount()) * properties.getHotelVariationStep()
            - properties.getHotelBaseOffset());

    // 【Step4】定員効率係数の適用 - 宿泊人数による割引システム
    // 配列範囲外アクセス防止 + 5人以上は最大割引率適用
    double capacityMultiplier = capacity <= properties.getCapacityMultipliers().size()
        ? properties.getCapacityMultipliers().get(capacity - 1)
        : properties.getCapacityMultipliers().get(properties.getCapacityMultipliers().size() - 1);

    // 【Step5】基本価格の算出 - 複数要素の合成計算
    int basePrice = (int) (properties.getBasePerPerson() * capacityMultiplier * capacity
        * hotelPriceMultiplier);

    // 【Step6】需要変動係数の計算 - 日付ベースの季節調整
    // 年間通じて周期的に変動する需要パターンをシミュレート
    int dayOfYear = date.getDayOfYear();
    double demandFactor = properties.getDemandBaseFactor()
        + ((dayOfYear % properties.getDemandVariationCycle())
            * properties.getDemandVariationStep());

    // 【Step7】最終価格の決定 - ビジネス要件（最低価格保証）の適用
    int finalPrice = (int) (basePrice * demandFactor);

    return Math.max(finalPrice, properties.getBasePerPerson()); // 最低価格保証
  }
}
