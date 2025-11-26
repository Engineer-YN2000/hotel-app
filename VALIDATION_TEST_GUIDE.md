# バックエンドバリデーションテストガイド

## 概要
このガイドでは、フロントエンドのUI制約を回避してバックエンドのバリデーションロジックを直接テストする方法を説明します。

## 前提条件
1. バックエンドサーバーが起動していること（`mvn spring-boot:run`）
2. サーバーが http://localhost:8080 で稼働していること
3. `curl`コマンドが利用可能であること
4. `jq`コマンドがインストールされていること（JSON整形用、オプション）

## バックエンド起動方法
```bash
cd backend
mvn spring-boot:run
```

## テストケース一覧

### 🔴 不正値テスト（422エラーが期待される）

#### 1. 都道府県ID関連
```bash
# 都道府県ID未指定
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-23&guestCount=2"

# 都道府県ID=0（不正値）
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-23&prefectureId=0&guestCount=2"

# 都道府県ID=-1（負の値）
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-23&prefectureId=-1&guestCount=2"
```

#### 2. 人数関連
```bash
# 人数=0（不正値）
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-23&prefectureId=1&guestCount=0"

# 人数=100（上限超過）
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-23&prefectureId=1&guestCount=100"

# 人数=-5（負の値）
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-23&prefectureId=1&guestCount=-5"
```

#### 3. 日付関連
```bash
# チェックイン日未指定
curl "http://localhost:8080/api/search?checkOutDate=2025-11-23&prefectureId=1&guestCount=2"

# チェックアウト日未指定
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&prefectureId=1&guestCount=2"

# 過去のチェックイン日
curl "http://localhost:8080/api/search?checkInDate=2025-11-20&checkOutDate=2025-11-23&prefectureId=1&guestCount=2"

# チェックアウト日=チェックイン日（同日）
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-22&prefectureId=1&guestCount=2"

# チェックアウト日 < チェックイン日
curl "http://localhost:8080/api/search?checkInDate=2025-11-23&checkOutDate=2025-11-22&prefectureId=1&guestCount=2"
```

#### 4. 不正な文字列
```bash
# 不正な文字列パラメータ
curl "http://localhost:8080/api/search?checkInDate=invalid&checkOutDate=2025-11-23&prefectureId=abc&guestCount=xyz"
```

### ✅ 正常値テスト（200 OKが期待される）
```bash
# 正常なリクエスト
curl "http://localhost:8080/api/search?checkInDate=2025-11-22&checkOutDate=2025-11-23&prefectureId=1&guestCount=2"
```

## 自動テストスクリプト実行
```bash
# テストスクリプト実行
./validation_test.sh
```

## 期待される結果

### 不正値の場合（422 Unprocessable Entity）
```json
{
  "hotels": null,
  "criteria": null,
  "errorMessage": "適切なエラーメッセージ"
}
```

### 正常値の場合（200 OK）
```json
{
  "hotels": [...],
  "criteria": {
    "checkInDate": "2025-11-22",
    "checkOutDate": "2025-11-23",
    "prefectureId": 1,
    "guestCount": 2
  },
  "errorMessage": null
}
```

## セキュリティ検証ポイント

### ✅ 確認すべき点
1. **criteriaフィールドの隠蔽**: エラーレスポンスに`criteria`フィールドが含まれていない
2. **エラーメッセージの適切性**: 内部情報を漏洩しない適切なメッセージ
3. **HTTPステータス**: 422 Unprocessable Entityが返される
4. **一貫性**: すべての不正値で同様のレスポンス構造

### ❌ 検出すべきセキュリティ問題
1. 詳細な内部情報の漏洩
2. スタックトレースの露出
3. データベースエラーの詳細
4. システム構成情報の露出

## トラブルシューティング

### Connection Refused
```bash
# バックエンドが起動していない
cd backend
mvn spring-boot:run
```

### jq command not found
```bash
# Windows (Chocolatey)
choco install jq

# または手動でjsonを確認
curl "..." | python -m json.tool
```

## 補足情報

### Defense in Depth確認
この テストにより以下の多層防御が確認できます：
1. **HTML5属性**: フロントエンドでの基本制約
2. **JavaScriptバリデーション**: フロントエンドでの詳細検証
3. **バックエンドバリデーション**: サーバーサイドでの最終検証

### セキュリティ強化の検証
- HTML5 `required`属性の回避対策
- JavaScriptによる堅牢なクライアントサイド検証
- サーバーサイドでの包括的バリデーション
- エラー情報の適切な隠蔽
