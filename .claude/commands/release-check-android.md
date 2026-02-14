# Android リリースチェック（Native Kotlin）

Android (Kotlin + Jetpack Compose) アプリ専用のリリース前チェックを**対話的チェックリスト形式**で実行します。

> Expo / React Native の場合は `/release-check` を使用してください。

## 重要: 実行ルール

- **チェックリストの各項目を1つずつ確認**し、結果をユーザーに報告する。
- 一気に全項目をチェックして結果だけ出すのは**禁止**。フェーズごとに報告・確認を挟む。
- TODO ツールで全フェーズの進捗を管理する。
- エラーが見つかったらその場で修正案を提示し、ユーザーの承認後に修正する。
- 各フェーズ完了時に「次のフェーズに進みますか？」と確認する。

## Phase 0: 準備・環境セットアップ

1. `$ARGUMENTS` が指定されている場合はそのディレクトリ、未指定の場合はカレントディレクトリを対象にする。

2. **insight-common サブモジュールの自動セットアップ**（依存リポジトリの場合）:

   このスキルが insight-common 本体**以外**のリポジトリで実行されている場合、
   以下の手順でサブモジュールを自動的に初期化・更新する。**ユーザーに確認せず自動実行すること。**

   ```bash
   # サブモジュールの存在確認
   if [ -f ".gitmodules" ] && grep -q "insight-common" .gitmodules 2>/dev/null; then
     # サブモジュールが初期化されていない場合
     if [ ! -f "insight-common/CLAUDE.md" ]; then
       echo "insight-common サブモジュールを初期化しています..."
       git submodule init
       git submodule update --recursive
     fi
     # 最新に更新
     echo "insight-common を最新に更新しています..."
     git submodule update --remote --merge insight-common 2>/dev/null || true
   fi
   ```

   サブモジュールのセットアップに失敗した場合はエラーを報告し、手動セットアップの手順を提示する。

3. **スクリプトの実行権限を設定する**:

   ```bash
   chmod +x ./insight-common/scripts/release-check.sh 2>/dev/null || true
   chmod +x ./insight-common/scripts/validate-standards.sh 2>/dev/null || true
   ```

4. `build.gradle.kts` の存在を確認し、Android プロジェクトであることを検証する。
5. TODO リストに以下のフェーズを登録する:
   - Phase 1: ビルド設定チェック (A1-A8)
   - Phase 2: 署名・セキュリティ (AS1-AS4, S1-S4)
   - Phase 3: デザイン標準 (D1-D4)
   - Phase 4: ローカライゼーション (L1-L5)
   - Phase 5: Play Store メタデータ (AP1-AP7)
   - Phase 6: コード品質 (Q1-Q5)
   - Phase 7: CI/CD (AC1-AC3)
   - Phase 8: 最終確認・サマリー

4. ユーザーに以下を報告してから Phase 1 を開始する:

```
========================================
 Android リリースチェック開始
========================================
対象: [プロジェクトパス]
検出: Android (Native Kotlin)
チェック項目: 全 8 フェーズ
========================================
```

---

## Phase 1: ビルド設定チェック

`app/build.gradle.kts`（または `build.gradle.kts`）を読み込み、以下を確認する。

| # | チェック項目 | 確認内容 | 期待値 |
|---|------------|---------|--------|
| A1 | versionCode | `versionCode` の値を確認 | 前回リリースからインクリメント |
| A2 | versionName | `versionName` の値を確認 | セマンティックバージョニング |
| A3 | compileSdk | `compileSdk` の値 | `35` |
| A4 | targetSdk | `targetSdk` の値 | `35` |
| A5 | minSdk | `minSdk` の値 | `26` |
| A6 | isMinifyEnabled | release ブロック内 | `true` |
| A7 | isShrinkResources | release ブロック内 | `true` |
| A8 | ProGuard ルール | `proguard-rules.pro` の存在 | ファイルが存在する |

**報告フォーマット:**

```
## Phase 1: ビルド設定 結果

| # | チェック項目 | 結果 | 現在の値 |
|---|------------|:----:|---------|
| A1 | versionCode | ✅ | 12 |
| A2 | versionName | ✅ | 1.2.0 |
| A3 | compileSdk | ✅ | 35 |
| A4 | targetSdk | ✅ | 35 |
| A5 | minSdk | ✅ | 26 |
| A6 | isMinifyEnabled (release) | ❌ | false → true に変更が必要 |
| A7 | isShrinkResources (release) | ✅ | true |
| A8 | ProGuard ルール | ✅ | proguard-rules.pro 存在 |
```

❌ がある場合:
- 具体的な修正コードを提示する
- ユーザーの承認後に修正する
- 修正後に該当項目を再チェックする

---

## Phase 2: 署名・セキュリティ

| # | チェック項目 | 確認方法 |
|---|------------|---------|
| AS1 | release の signingConfig が設定されている | `build.gradle.kts` で `signingConfigs` ブロックと `signingConfig = signingConfigs.getByName("release")` を確認 |
| AS2 | keystore ファイルの存在（ローカル） | プロジェクト内で `*.jks` / `*.keystore` を検索 |
| AS3 | keystore がリポジトリに含まれていない | `.gitignore` で `*.jks` / `*.keystore` が除外されているか確認 |
| AS4 | keystore.properties / 環境変数で参照 | `build.gradle.kts` で `keystore.properties` または `System.getenv()` を使用しているか確認 |
| S1 | .env が .gitignore に含まれる | `.gitignore` を確認 |
| S2 | API キーがソースコードに埋め込まれていない | `grep -rn "sk-\|AIza\|AKIA\|api_key.*=.*\"" --include="*.kt"` |
| S3 | google-services.json がリポジトリに含まれていない | `.gitignore` と `git ls-files` で確認 |
| S4 | keystore ファイルがリポジトリに含まれていない | `git ls-files` で `*.jks` / `*.keystore` を確認 |

**報告フォーマット:**

```
## Phase 2: 署名・セキュリティ 結果

| # | チェック項目 | 結果 | 詳細 |
|---|------------|:----:|------|
| AS1 | release signingConfig | ✅ | signingConfigs.release 設定済み |
| AS2 | keystore 存在 | ⚠️ | ローカル確認が必要（CI では secrets 経由） |
| AS3 | keystore が .gitignore | ✅ | *.jks, *.keystore 除外済み |
| AS4 | keystore.properties 参照 | ✅ | keystore.properties から読み込み |
| S1 | .env 除外 | ✅ | .gitignore に含まれる |
| S2 | API キー埋め込みなし | ✅ | ハードコード検出なし |
| S3 | google-services.json | ✅ | .gitignore に含まれる |
| S4 | keystore 未追跡 | ✅ | git ls-files に含まれない |
```

---

## Phase 3: デザイン標準 (Ivory & Gold)

以下のファイルを確認する:
- `app/src/main/kotlin/**/ui/theme/Color.kt`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`

| # | チェック項目 | 確認内容 |
|---|------------|---------|
| D1 | Gold (#B8942F) がプライマリカラー | `Color.kt` / `colors.xml` で Gold が Primary として定義 |
| D2 | Ivory (#FAF8F5) が背景色 | `Color.kt` で Ivory が Background として定義 |
| D3 | Blue (#2563EB) がプライマリ未使用 | `#2563EB` がプライマリとして使われていないこと |
| D4 | ハードコードされた色値なし | Compose コードで直接 `Color(0xFF...)` を使っていないこと（theme 経由であること） |
| D5 | Theme 関数名が `Insight<AppName>Theme` | `Theme.kt` で正しい命名 |
| D6 | Material3 + Material You 使用 | `Theme.kt` で `dynamicColor` 対応 |
| D7 | InsightTypography 定義 | `Type.kt` で標準タイポグラフィ定義 |
| D8 | themes.xml で透明ステータスバー | `android:statusBarColor` が `@android:color/transparent` |

**報告フォーマット:** Phase 1 と同様のテーブル形式。

---

## Phase 4: ローカライゼーション

| # | チェック項目 | 確認方法 |
|---|------------|---------|
| L1 | `values/strings.xml` 存在（日本語） | ファイル存在確認 |
| L2 | `values-en/strings.xml` 存在（英語） | ファイル存在確認 |
| L3 | 日英のキーが完全一致 | 両ファイルの `<string name="...">` を比較 |
| L4 | Compose で文字列ハードコードなし | `*.kt` ファイルで `Text("日本語")` のようなハードコードを検索 |
| L5 | 日付・数値フォーマットがロケール対応 | 手動確認推奨 |

**L3 の確認方法:**

```
日本語のみに存在するキー: [一覧]
英語のみに存在するキー: [一覧]
→ 両方に存在しないキーがあれば ❌
```

---

## Phase 5: Play Store メタデータ

`fastlane/metadata/android/` ディレクトリの構成を確認する。

| # | チェック項目 | ファイル | 文字数制限 |
|---|------------|---------|:--------:|
| AP1 | 日本語タイトル | `ja-JP/title.txt` | 30 |
| AP2 | 英語タイトル | `en-US/title.txt` | 30 |
| AP3 | 日本語短い説明 | `ja-JP/short_description.txt` | 80 |
| AP3 | 英語短い説明 | `en-US/short_description.txt` | 80 |
| AP4 | 日本語詳細説明 | `ja-JP/full_description.txt` | 4000 |
| AP4 | 英語詳細説明 | `en-US/full_description.txt` | 4000 |
| AP5 | 日本語リリースノート | `ja-JP/changelogs/default.txt` | 500 |
| AP5 | 英語リリースノート | `en-US/changelogs/default.txt` | 500 |
| AP7 | スクリーンショット（日英） | `ja-JP/images/` / `en-US/images/` | — |

**ファイルが存在しない場合:**

ディレクトリ構造とテンプレートを提示し、作成を提案する:

```
fastlane/metadata/android/
├── ja-JP/
│   ├── title.txt              # アプリ名（30文字以内）
│   ├── short_description.txt  # 短い説明（80文字以内）
│   ├── full_description.txt   # 詳細説明（4000文字以内）
│   ├── changelogs/
│   │   └── default.txt        # リリースノート（500文字以内）
│   └── images/
│       └── phoneScreenshots/  # スクリーンショット
├── en-US/
│   ├── title.txt
│   ├── short_description.txt
│   ├── full_description.txt
│   ├── changelogs/
│   │   └── default.txt
│   └── images/
│       └── phoneScreenshots/
```

文字数が超過している場合は、**現在の文字数と上限**を表示し、短縮案を提示する。

---

## Phase 6: コード品質

| # | チェック項目 | 確認方法 |
|---|------------|---------|
| Q1 | TODO/FIXME/HACK コメント | `*.kt` / `*.xml` / `*.kts` ファイルで `TODO\|FIXME\|HACK` を検索 |
| Q2 | デバッグ出力 (`Log.d` / `println`) | `*.kt` ファイルで `Log\.\|println\|System\.out` を検索（ただしリリースビルドで除外される Timber 等は OK） |
| Q3 | ハードコードされた API キー | `*.kt` / `*.xml` ファイルで秘密鍵パターンを検索 |
| Q4 | 未使用 import | IDE 警告レベル — 手動確認推奨 |
| Q5 | Lint エラー | `./gradlew lint` の実行推奨（手動確認） |

**Q1 の結果報告:**

```
TODO/FIXME/HACK 検出結果:
- app/src/main/kotlin/.../MyScreen.kt:42 — TODO: エラーハンドリング追加
- app/src/main/kotlin/.../Repository.kt:15 — FIXME: キャッシュ実装

→ 2件検出。リリース前に解決するか、意図的に残す場合はその理由を確認してください。
```

---

## Phase 7: CI/CD

| # | チェック項目 | 確認方法 |
|---|------------|---------|
| AC1 | `.github/workflows/build.yml` 存在 | ファイル存在確認 |
| AC2 | ワークフローの内容が標準準拠 | JDK 17 (Temurin) + `gradle/actions/setup-gradle@v4` 使用 |
| AC3 | google-services.json の CI プレースホルダー | Firebase 使用時のみ — ワークフロー内で secrets から生成しているか |

**標準 CI ワークフローとの差分がある場合:**

`standards/ANDROID.md` §14 の CI/CD 標準を参照し、差分を報告する。

---

## Phase 8: 最終確認・サマリー

全フェーズの結果を統合して最終サマリーを表示する。

```
============================================================
 Android リリースチェック 最終サマリー
============================================================

対象: [プロジェクトパス]
プラットフォーム: Android (Native Kotlin)
実行日時: [日時]

Phase 1: ビルド設定          ✅ 完了 (8/8 OK)
Phase 2: 署名・セキュリティ   ✅ 完了 (8/8 OK)
Phase 3: デザイン標準         ✅ 完了 (8/8 OK)
Phase 4: ローカライゼーション  ✅ 完了 (5/5 OK)
Phase 5: Play Store メタデータ ✅ 完了 (8/8 OK)
Phase 6: コード品質           ⚠️ 警告あり (4/5 OK, 警告 1)
Phase 7: CI/CD               ✅ 完了 (3/3 OK)

合計: ✅ OK 44件 / ❌ エラー 0件 / ⚠️ 警告 1件

⚠️ 警告項目:
1. Q1: TODO コメントが 2件残存（意図的であれば OK）

手動確認が必要な項目:
1. [ ] Release APK/AAB のビルド・動作確認
2. [ ] スクリーンショットの準備（日英）
3. [ ] リリースノートの内容承認
4. [ ] keystore の存在確認（ローカル / CI secrets）

→ 全エラーが解消されていればリリース可能です。
============================================================
```

---

## 参照ドキュメント

- `standards/ANDROID.md` — Android 開発標準（§15 チェックリスト）
- `standards/RELEASE_CHECKLIST.md` — 全プラットフォーム共通リリースチェックリスト（§2 Android 固有）
- `standards/LOCALIZATION.md` — ストアメタデータのローカライゼーション（§6）
- `CLAUDE.md` §12 — 開発完了チェックリスト
