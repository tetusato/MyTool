# Copier : テーブルデータ・コピーツール
## 概要
1. 二つのDB、もしくは同一のDB内の異なるスキーマの同一のテーブル名のデータを一方から他方へコピーする
   - 異なるテーブル名へはコピーできない
   - テーブル定義が異なる場合はコピーできない
   - コピー先のテーブルは予め作成する
1. コピー元、およびコピー先のDBやスキーマの指定はプロパティーファイルに記述する
1. スキーマはコピー元、コピー先それぞれに1つずつ指定できる
1. 自動生成値がカラムに設定されるテーブルのコピーはサポートしていない

## 書式

実行可能jarファイルであるCopier.jarを使用する場合(*)。

java  -jar  Copier.jar  _プロパティーファイルのパス_　 [処理モード1[ 　処理モード2]]

(*) Copier.jar と同じディレクトリーに db2jcc4.jar が必要

## 処理モード
コピーツールの処理モードを指定する。

| Mode | 説明 |
| ------ | ---------------------------------------------------------------------- |
| clear | プロパティーファイルのtoに指定されたDBのテーブルのレコードを削除する。 |
| | to.schemaに指定されたスキーマのテーブルを対象テーブルとする。 |
| | 実際のクリア対象は、tables.targetに指定されたテーブルのうち、 |
| | tables.ignoreに指定さ れていないテーブルで、実際にto側のDBに存在する |
| | テーブル。 |
| | tables.targetが指定されていない場合は、fromに指定されたDB、スキーマの |
| | 全てのテーブルを対象に指定したとみなして処理する。 |
| | copy途中でエラーが発生し、同じテーブルを対象にcopyを再実行する場合に、 |
| | 重複キーを避けるために、事前にクリアするなどの目的で実行する。 |
| | |
| copy | プロパティーファイルのfromに指定されたDBのテーブルのレコードを、toに指 |
| | 定されたDBの同名のテーブルにコピーする。 |
| | コピー対象のテーブルの求め方はclearの場合と同様。 |
| | |
| drop | プロパティーファイルのdropに指定されたDBのテーブルを削除する。 |
| | 削除対象のテーブルはtables.dropに指定されたテーブルのうち実際に存在す |
| | るテーブル。db2lookなどで生成したDDLを実行して作成したが、実際には不要 |
| | なテーブルがあった時などに、そのテーブルを削除する想定。 |
| | |
| list | fromに指定されたDB、スキーマのテーブル名一覧を標準出力に出力する。 |
| | ファイルへリダイレクトして記録することでtables.target に指定するテーブ |
| | ル名一覧と して使用することを想定。 |
| | |
| verify | fromとtoに存在するテーブルのうち、tables.target、tables.ignoreを使って |
| | 求めた実際のコピー先テーブル名の一覧を標準出力に出力する。 |
| | copy実行前に対象テーブルを確認する場合などに実行する。 | 



## プロパティーファイル
コピー元やコピー先についての接続情報、コピー対象や除外対象のテーブルなどの指定を行う。

| キー | 必須 | 説明 | サンプル |
| ------------- |:-----:| ------------------------------------ | --------------------------------- |
| from.url | Y(*1) | コピー元のDBのURL | jdbc:db2://localhost:50000/sample |
| from.user | Y(*1) | コピー元のDBに接続するためのユーザー | db2inst1 |
| from.password | Y(*1) | from.user のパスワード | passw0rd |
| from.schema | Y(*1) | コピー元のテーブルのスキーマ | scott |
| to.url | Y(*2) | コピー先のDBのURL | jdbc:db2://localhost:50000/sample |
| to.user | Y(*2) | コピー先のDBに接続するためのユーザー | db2inst1 |
| to.password | Y(*2) | to.user のパスワード | passw0rd |
| to.schema | Y(*2) | コピー先のテーブルのスキーマ | steve |
| tables.target | | コピー対象のテーブル名一覧。 | カンマ区切りリスト EMP,DEPT |
| | | カンマ区切りのリスト、もしくは file: | ファイル名 file:mytarget.txt |
| | | で書き始めるファイルパスで指定。 | |
| | | ファイル名を指定する場合は、(プロパ | |
| | | ティーファイルからの相対ではなく) 実 | |
| | | 行時のディレクトリーからの相対パス、 | |
| | | または絶対パスで記述する必要がある。 | |
| | | 省略時はfrom.xxxに指定されたDB、スキ | |
| | | ーマの全てのテーブルが対象となる。 | |
| tables.ignore | | tables.target に指定されたテーブル名 | |
| | | のうち、コピー対象から除外するテーブ | |
| | | ル名を指定する。 | |
| | | 指定方法はtables.targetと同様。 | |
| | | | |
| drop.url | Y(*3) | ドロップするテーブルがあるDBのURL | jdbc:db2://localhost:50000/sample |
| drop.user | Y(*3) | ドロップするテーブルがあるDBに接続す | db2inst1 |
| | | るためのユーザー | |
| drop.password | Y(*3) | drop.user のパスワード | passw0rd |
| drop.schema | Y(*3) | ドロップするテーブルのスキーマ | steve |
| tables.drop | Y(*3) | ドロップするテーブル名一覧。 | カンマ区切りリスト EMP,DEPT |
| | | 指定方法はtables.targetと同様。 | ファイル名 file:mytarget.txt |

(*1) copy list verify の時必須

(*2) clear copy verify の時必須

(*3) drop の時必須