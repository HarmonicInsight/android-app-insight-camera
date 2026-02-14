# Insight Camera

## Play Store Release Signing

| 項目 | 値 |
|------|-----|
| キーストアファイル | `upload-keystore.jks` |
| パスワード | `InsightCam2026!xQ7` |
| キーエイリアス | `upload` |
| キーパスワード | `InsightCam2026!xQ7` |
| Play App Signing | 有効（Google管理） |

### ビルド時の環境変数

```bash
KEYSTORE_PATH=./upload-keystore.jks
KEYSTORE_PASSWORD=InsightCam2026!xQ7
KEY_ALIAS=upload
KEY_PASSWORD=InsightCam2026!xQ7
```

## リリースチェック

> リリース前に必ず実行してください。

```bash
# リリース前の包括チェック
./insight-common/scripts/release-check.sh .
```

または Claude Code で `/release-check` を実行。

### AI アシスタント自動行動ルール

| トリガー | アクション |
|---------|----------|
| 「リリース」「デプロイ」「公開」「本番」「ship」「release」 | `/release-check` を提案・実行 |
| 「PR 作って」「プルリク」「マージ」 | `/release-check` の実行を推奨 |
| 新規 UI 実装 | Ivory & Gold デザイン標準を確認 |
