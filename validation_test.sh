#!/bin/bash
echo "=== バックエンドバリデーションテスト ==="
echo "注意: バックエンドサーバーが http://localhost:8080 で起動している必要があります"
echo ""

# 動的日付計算（現在日時基準）
TODAY=$(date +%Y-%m-%d)
TOMORROW=$(date -d "+1 day" +%Y-%m-%d)
DAY_AFTER_TOMORROW=$(date -d "+2 days" +%Y-%m-%d)
YESTERDAY=$(date -d "-1 day" +%Y-%m-%d)

echo "テスト実行日: $TODAY"
echo "使用する日付: チェックイン=$TOMORROW, チェックアウト=$DAY_AFTER_TOMORROW"
echo ""

# JSON整形関数（jqの代替）
format_json() {
    if command -v jq >/dev/null 2>&1; then
        jq .
    elif command -v python >/dev/null 2>&1; then
        python -m json.tool 2>/dev/null || cat
    else
        cat
    fi
}

# テスト1: 都道府県IDが未指定（null）
echo "【テスト1】都道府県ID未指定"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$DAY_AFTER_TOMORROW&guestCount=2" | format_json
echo ""

# テスト2: 都道府県IDが0（不正値）
echo "【テスト2】都道府県ID=0（不正値）"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=0&guestCount=2" | format_json
echo ""

# テスト3: 都道府県IDが負の値
echo "【テスト3】都道府県ID=-1（負の値）"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=-1&guestCount=2" | format_json
echo ""

# テスト4: 人数が0
echo "【テスト4】人数=0（不正値）"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=1&guestCount=0" | format_json
echo ""

# テスト5: 人数が100（上限超過）
echo "【テスト5】人数=100（上限超過）"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=1&guestCount=100" | format_json
echo ""

# テスト6: 人数が負の値
echo "【テスト6】人数=-5（負の値）"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=1&guestCount=-5" | format_json
echo ""

# テスト7: チェックイン日が未指定
echo "【テスト7】チェックイン日未指定"
curl -s "http://localhost:8080/api/search?checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=1&guestCount=2" | format_json
echo ""

# テスト8: チェックアウト日が未指定
echo "【テスト8】チェックアウト日未指定"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&prefectureId=1&guestCount=2" | format_json
echo ""

# テスト9: 過去のチェックイン日
echo "【テスト9】過去のチェックイン日"
curl -s "http://localhost:8080/api/search?checkInDate=$YESTERDAY&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=1&guestCount=2" | format_json
echo ""

# テスト10: チェックアウト日がチェックイン日と同日
echo "【テスト10】チェックアウト日=チェックイン日（同日）"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$TOMORROW&prefectureId=1&guestCount=2" | format_json
echo ""

# テスト11: チェックアウト日がチェックイン日より前
echo "【テスト11】チェックアウト日 < チェックイン日"
curl -s "http://localhost:8080/api/search?checkInDate=$DAY_AFTER_TOMORROW&checkOutDate=$TOMORROW&prefectureId=1&guestCount=2" | format_json
echo ""

# テスト12: 不正な文字列パラメータ
echo "【テスト12】不正な文字列パラメータ"
curl -s "http://localhost:8080/api/search?checkInDate=invalid&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=abc&guestCount=xyz" | format_json
echo ""

# テスト13: 正常なリクエスト（比較用）
echo "【テスト13】正常なリクエスト（比較用）"
curl -s "http://localhost:8080/api/search?checkInDate=$TOMORROW&checkOutDate=$DAY_AFTER_TOMORROW&prefectureId=1&guestCount=2" | format_json
echo ""

echo "=== テスト完了 ==="
echo "期待される結果:"
echo "- テスト1-12: 422 Unprocessable Entity + エラーメッセージ"
echo "- テスト13: 200 OK + 検索結果"
echo ""
echo "セキュリティチェック:"
echo "- エラーレスポンスにcriteriaフィールドが含まれていないこと"
echo "- 詳細な内部情報が漏洩していないこと"
echo ""
echo "注意: JSON整形は利用可能なツールに応じて自動選択されます"
echo "- jq (推奨): 利用可能な場合に使用"
echo "- python -m json.tool: jqが無い場合のフォールバック"
echo "- cat: どちらも無い場合の生JSON表示"
