# テーブルダンプツール

***

## 概要

JDBC で接続したデータベースのテーブルを読んで指定された出力先へ csv ファイルとして出力する。
JDBC で接続することができれば IBM Cloud の IBM Db2 Warehouse on Cloud のデータでもローカル環境へ比較的容易にデータを保存でき、他の環境へ移植するデータとして利用可能。
CLOB と BLOB のカラムは Base64 でエンコードした状態で出力されるので、デコードして適切な拡張子をつけたファイルにすることで内容を復元することができる。
出力先には標準出力かファイルシステム上のディレクトリーを指定できる。

ディレクトリーを指定した場合はファイル名のフォーマットにより決まった名前でテーブルごとにファイルが作成される。そのためにはフォーマットの指定には必ずテーブル名を含める必要があり、含めていない場合は上書きされるなどの不具合が生じる可能性がある。

タイムスタンプをファイル名に含める場合、このツールを実行した単位で同じタイムスタンプが設定される。そのため複数のテーブルのダンプを取得する場合は、１度のコマンド実行で取得することで同時に行ったことが明確になる。ファイルシステム上のタイムスタンプとのズレが生じるが、ダンプの内容が期待と異なる場合、調査する時間帯をファイル名とファイルシステムのタイムスタンプの時間の間に絞ることもできる。

***

## 使い方

### 準備

1. 作業用ディレクトリーを適当なところに作成する
1. DumpTool.jar と dumptool.properties を作業用ディレクトリにコピーする
1. db2jcc4.jar を DumpTool.jar と同じディクレクトリにコピーする
1. dumptool.properties をテキストエディタで編集する
  1. host, user, password を書き換える (他はデフォルトのままでもとりあえず大丈夫)
  1. アクセスするテーブルのスキーマがユーザーと異なる場合は schema を設定する
  1. SSLポートへ接続する場合は sslConnection に true を設定する


### 実行

__書式1__ : 出力先ディレクトリへ保存する、出力先ディレクトリはサブディレクトリ指定しない

   java -jar DumpTool.jar <プロパティーファイル名> <出力先ディレクトリ名> [対象テーブル名1[ 対象テーブル名2[ ...]]]

  具体例 : テストケース A-001-01 実行前後の EMP テーブルの内容を A-001-01 の下に before と after のディレクトリをつけて保管する

   テスト実行前

   ```
   java -jar DumpTool.jar mysettings.properties A-001-01/before EMP
   ```

   テスト実行後


   ```
   java -jar DumpTool.jar mysettings.properties A-001-01/after EMP
   ```

   この場合サブフォルダー名を作成するファイル名に含めることはできない。

__書式2__ : 出力先ディレクトリへ保存する、出力先ディレクトリはサブディレクトリ指定する


 java -jar DumpTool.jar <プロパティーファイル名> <出力先ディレクトリ名>#<サブディレクトリ名> [対象テーブル名1[ 対象テーブル名2[ ...]]]


 具体例 : テストケース A-001-01 実行前後の EMP テーブルの内容を A-001-01 の下に before と after のフォルダーをつけて保管する

   テスト実行前

   ```
   java -jar DumpTool.jar mysettings.properties A-001-01#before EMP
   ```

   テスト実行後

   ```
   java -jar DumpTool.jar mysettings.properties A-001-01#after EMP
   ```

   この場合サブフォルダー名を作成するファイル名に含められる。

 __書式3__ : とりあえず画面で確認する

 java -Dtool.stdout=true -jar DumpTool.jar <プロパティーファイル名> [<dummyの文字列> [対象テーブル名1[ 対象テーブル名2[ ...]]]]

 特定のテーブルを指定するには、dummy文字列が必要。 全テーブルを画面に出すというのであれば dummy文字列 は省略できる。

 具体例1: EMP テーブルの内容が画面で見る

   ```
   java -Dtool.stdout=true -jar DumpTool.jar foo EMP
   ```

 具体例2: 全テーブルを画面に出して一つのファイルにリダイレクトする

   ```
   java -Dtool.stdout=true -jar DumpTool.jar >  alltables.csv
   ```

この場合、ファイル内にはテーブル名の行があるので、完全な CSV ではない。

__書式4__ : Db2 Warehouse on Cloud のテーブル用ロードデータとして取得したいので NULL は引用符なしの空状態で出力したい

   java -DbrindNull=true -jar DumpTool.jar <プロパティーファイル名> <出力先ディレクトリ名> [対象テーブル名1[ 対象テーブル名2[ ...]]]

　具体例:

   ```
   java -DbrindNull=true -jar DumpTool.jar mysettings.properties work EMP
   ```

__書式5__ : Db2 の import 文用ロードデータとして取得したいので NULL は引用符なしの空状態で、数値項目は二重引用符なしで出力したい

   java -Dtool.db2support=true -DbrindNull=true -jar DumpTool.jar <プロパティーファイル名> <出力先ディレクトリ名> [対象テーブル名1[ 対象テーブル名2[ ...]]]

　具体例:

   ```
   java -Dtool.db2support=true -DbrindNull=true -jar DumpTool.jar mysettings.properties work EMP
   ```
