# Intern-Bookmark-scala

## 準備

### sbt
[sbtのダウンロードページ](http://www.scala-sbt.org/download.html) などを参考にインストールしましょう。Macの場合は以下のようにすればOK。

```sh
$ brew install sbt
```

### データベース
MySQLを使いますのでインストールします。

```sh
$ brew install mysql
$ ln -fs $(brew --prefix mysql)/homebrew.mxcl.mysql.plist ~/Library/LaunchAgents/
$ launchctl load ~/Library/LaunchAgents/homebrew.mxcl.mysql.plist
```

データベースとテーブル定義を読み込ませます。

```sh
$ mysqladmin create internbookmark
$ mysqladmin create internbookmark_test
$ cd /path/to/Intern-Bookmark-scala
$ cat db/schema.sql | mysql -uroot internbookmark
$ cat db/schema.sql | mysql -uroot internbookmark_test
```

## 実行

```sh
$ sbt
> run list
> run add http://www.hatena.ne.jp 便利なページ
> run delete http://www.hatena.ne.jp
```

mainクラスが複数あるので、実行時にどれを起動するか選択する必要があります。

## テスト

```sh
$ sbt
> test
```

## モジュール

- internbookmark
  - cli
    - BookmarkCLI CLIコマンドの実装
  - model データモデル
    - Bookmark
    - Entry URLと対応
    - User
  - repository データモデルをDBやファイルシステムなどに記録して操作するためのサービス
    - Bookmarks Bookmarkのリポジトリ
    - Entries Entryのリポジトリ
    - Users Userのリポジトリ
    - Identifier Idを作ってくれる
    - TitleExtractor urlに対応するWebサイトのタイトル情報を取得してくれるサービスのtrait
    - TitleExtractorDispatch Dispatchを使ったTitleExtractorの実装
  - service アプリケーションのコアロジック
    - Json データモデルのJson表現を管理
    - BookmarkCLIApp ブックマークの主たる機能を表現したアプリケーションクラス
    - Error アプリケーション層で使うエラー定数
