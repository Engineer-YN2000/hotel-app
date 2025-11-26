/**
 * バリデーション関連の定数
 * フロントエンド・バックエンド共通の定数定義
 *
 * 【設計原則】
 * - フロントエンド・バックエンド間の定数値の整合性を保つ
 * - 将来の要件変更時の修正箇所を最小化
 * - プロジェクト全体での一元管理を実現
 *
 * 【同期対象】
 * - バックエンド: /backend/src/main/java/com/example/hotel/top/TopPageController.java
 * - フロントエンド: useI18nValidation.js, TopPage.jsx
 */

// 宿泊人数の制約（TopPageController.javaと同期）
export const GUEST_COUNT_CONSTRAINTS = {
  MIN: 1,
  MAX: 99,
};

// 都道府県IDの制約（TopPageController.javaと同期）
export const PREFECTURE_ID_CONSTRAINTS = {
  MIN: 1, // データベースのAUTO_INCREMENTにより1(北海道)～47(沖縄県)
  MAX: 47,
};

// 推奨日付設定（TopPageController.javaと同期）
export const RECOMMENDED_DATE_OFFSETS = {
  CHECK_IN_DAYS_FROM_TODAY: 1,
  CHECK_OUT_DAYS_FROM_TODAY: 2,
};

// HTTPステータスコード定数
export const HTTP_STATUS = {
  BAD_REQUEST: 400,
  NOT_FOUND: 404,
  METHOD_NOT_ALLOWED: 405,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
};

// 翻訳キーのプレフィックス定数
export const TRANSLATION_KEY_PREFIXES = [
  'validation.',
  'messages.',
  'labels.',
  'buttons.',
  'app.',
];
