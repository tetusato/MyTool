# Copier : テーブルデータ・コピーツール
## 概要
1. 二つのDB、もしくは同一のDB内の異なるスキーマの同一のテーブル名のデータを一方から他方へコピーする
   - 異なるテーブル名へはコピーできない
   - テーブル定義が異なる場合はコピーできない
   - コピー先のテーブルは予め作成する
1. コピー元、およびコピー先のDBやスキーマの指定はプロパティーファイルに記述する
1. スキーマはコピー元、コピー先それぞれに1つずつ指定できる


## プロパティーファイル
コピー元やコピー先についての接続情報、コピー対象や除外対象のテーブルなどの指定を行う。

| キー | 説明 | サンプル |
| ------------- | ------------------------------------ | --------------------------------- |
| from.url | コピー元のDBのURL | jdbc:db2://localhost:50000/sample |
| from.user | コピー元のDBに接続するためのユーザー | db2inst1 |
| from.password | from.user のパスワード | passw0rd |
| from.schema | コピー元のテーブルのスキーマ | scott |
| to.url | コピー先のDBのURL | jdbc:db2://localhost:50000/sample |
| to.user | コピー先のDBに接続するためのユーザー | db2inst1 |
| to.password | to.user のパスワード | passw0rd |
| to.schema | コピー先のテーブルのスキーマ | steve |
| | | |

| キー | 必須 | 説明 | サンプル |
| ------------- |:----:| ------------------------------------ | --------------------------------- |
| from.url | O | コピー元のDBのURL | jdbc:db2://localhost:50000/sample |
| from.user | O | コピー元のDBに接続するためのユーザー | db2inst1 |
| from.password | O | from.user のパスワード | passw0rd |
| from.schema | O | コピー元のテーブルのスキーマ | scott |
| to.url | O | コピー先のDBのURL | jdbc:db2://localhost:50000/sample |
| to.user | O | コピー先のDBに接続するためのユーザー | db2inst1 |
| to.password | O | to.user のパスワード | passw0rd |
| to.schema | O | コピー先のテーブルのスキーマ | steve |
| tables.target | | コピー対象のテーブル名一覧。 | カンマ区切りリスト EMP,DEPT |
| | | カンマ区切りのリスト、もしくは file: | ファイル名 file:mytarget.txt |
| | | で書き始めるファイルパスで指定 | |
