package com.ibm.jp.isol.tetusato.db.copy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ibm.jp.isol.tetusato.db.Column;
import com.ibm.jp.isol.tetusato.db.copy.CopierProperties.ConnInfo;

/**
 * 二つのDBのテーブルのデータをコピーするツールのメインクラス
 */
public class Copier {

    /**
     * 1UOW 対象の最大レコード数
     */
    private static final int MAX_TRAN_COUNT = 1000;

    /**
     * プロパティーファイルから読み込んだ設定値
     */
    private CopierProperties props;

    /**
     * プロパティーファイルの target と ignore から導き出した実対象テーブル名一覧
     */
    private List<String> actualTarget;

    /**
     * 処理モードの列挙子
     */
    private enum Mode {
        CLEAR, COPY, DROP, INVALID, LIST, VERIFY
    }

    /**
     * コンストラクター
     *
     * @param props
     *            実行の際に参照する各種情報を保持するプロパティーオブジェクト
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    public Copier(CopierProperties props) throws SQLException {
        this.props = props;
    }

    /**
     * メイン処理。
     *
     * <p>
     * コマンド行引数から実行に必要な情報の構築、このクラスのインスタンスの作成、コマンド行引数に指定されたモードに応じた処理の起動を担う。
     * </p>
     *
     * <p>
     * コマンド行引数の第一引数はプロパティーファイルのパス、第二引数、第三引数に処理モードを指定する。 第二引数は任意のモードを指定可能。
     * 第三引数は第二引数が clear の場合に、 copy を指定することができる。
     * これによりコピー先のクリアとコピーを一度の起動で順次実行可能となる。
     * </p>
     *
     * @param args
     *            コマンド行引数
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    public static void main(String... args) throws SQLException {
        Arguments arguments = validateArgs(args);
        if (arguments.modes.contains(Mode.INVALID)) {
            System.exit(1);
        }
        Copier copier = new Copier(loadProperties(args));
        for (Mode mode : arguments.modes) {
            switch (mode) {
            case COPY:
                copier.resolveCopyTarget();
                copier.copyTables();
                break;
            case CLEAR:
                copier.resolveCopyTarget();
                copier.clearTables(copier.props.to);
                break;
            case DROP:
                copier.dropTables(copier.props.drop);
                break;
            case LIST:
                copier.listTableNames(copier.props.from).forEach(System.out::println);
                break;
            case VERIFY:
                copier.resolveCopyTarget();
                System.err.println("List for verifying the actual target tables.");
                copier.actualTarget.forEach(System.out::println);
                break;
            default:
                break;
            }
        }
    }

    /**
     * 引数を保持するクラス
     */
    private static class Arguments {
        /** 処理モード */
        private List<Mode> modes = new ArrayList<>();
    }

    /**
     * コピー先のテーブルの全レコードを削除する。
     * <p>
     * 指定された接続情報で接続したDBの、 {@link #actualTarget} に格納されたテーブル名の全てのテーブルからレコードを削除する。
     * </p>
     *
     * @param connInfo
     *            レコードを削除する先のDBへの接続情報
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private void clearTables(ConnInfo connInfo) throws SQLException {
        final String prefixClearing = "CLEARING... " + connInfo.schema + ".";
        final String baseSqlTemplate = "DELETE FROM (SELECT * FROM " + connInfo.schema + ".%s FETCH FIRST "
                + MAX_TRAN_COUNT + " ROWS ONLY)";
        try (Connection conn = connect(connInfo, false)) {
            for (String tableName : actualTarget) {
                System.err.print(prefixClearing + tableName);
                long total = 0;
                try (PreparedStatement stmt = conn
                        .prepareStatement(String.format(baseSqlTemplate,
                                tableName))) {
                    int count = 0;
                    do {
                        stmt.execute();
                        conn.commit();
                        count = stmt.getUpdateCount();
                        total += count;
                    } while (count == MAX_TRAN_COUNT);
                }
                System.err.println(String.format(": %d rows deleted.", total));
            }
            System.err.println("CLEAR TABLES successfully ended.");
        }
    }

    /**
     * テーブルを削除(DROP)する。
     * <p>
     * 削除対象のDBへはプロパティーファイルの "drop." で始まるキーの接続情報で接続する。 削除対象のテーブルは "drop.schema"
     * に指定されたスキーマのものを対象とし、具体的なテーブルは "tables.drop" に指定された値を対象とする。
     * </p>
     *
     * @param connInfo
     *            テーブルを削除する対象のDBへの接続情報
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private void dropTables(ConnInfo connInfo) throws SQLException {
        final String prefixDroping = "DROPING... " + connInfo.schema + ".";
        final String baseSql = "DROP TABLE " + connInfo.schema + ".";

        List<String> existTableNames = listTableNames(connInfo);
        existTableNames.retainAll(props.dropTables);
        try (Connection conn = connect(connInfo, false)) {
            long total = 0;
            for (String tableName : existTableNames) {
                System.err.println(prefixDroping + tableName);
                try (PreparedStatement stmt = conn
                        .prepareStatement(baseSql + tableName)) {
                    stmt.execute();
                    total++;
                }
            }
            conn.commit();
            System.err.println("DROP TABLES successfully ended.");
            System.err.println(String.format("  dropped count = %d", total));
        }
    }

    /**
     * コマンド行引数の妥当性検証と整理。
     *
     * @param args
     *            コマンド行引数
     * @return
     */
    private static Arguments validateArgs(String[] args) {
        if (Arrays.asList(args).contains("--help")) {
            usage();
        }
        Arguments arguments = new Arguments();
        List<Mode> modes = arguments.modes;
        switch (args.length) {
        case 0:
            usage();
            break;
        case 1:
            modes.add(Mode.COPY);
            break;
        default:
            for (int i = 1; i < args.length; i++) {
                try {
                    modes.add(Mode.valueOf(args[i].toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.err.println(String.format("%d 番目のモード指定 %s に誤りがあります。", i, args[i]));
                    modes.add(Mode.INVALID);
                }
            }
            break;
        }
        if (modes.size() == 2) {
            if (!(modes.get(0) == Mode.CLEAR && modes.get(1) == Mode.COPY)) {
                System.err.println(String.format("サポートされないモードの組み合わせが指定されました。(%s, %s)", modes.get(0), modes.get(1)));
                modes.add(Mode.INVALID);
            }
        } else if (modes.size() > 2) {
            System.err.println(String.format("モード指定が2つまでの許可された組み合わせに合致しません。(%s)",
                    modes.stream().map(Mode::name).collect(Collectors.joining(", "))));
        }
        return arguments;
    }

    /**
     * 使い方コメントの出力。
     */
    private static void usage() {
        // @formatter:off
        System.err.println("Table data copy tool");
        System.err.println("Usage: invoke main method of " + Copier.class.getName() + " with valid parameters.");
        System.err.println("    " + Copier.class.getName() + " <property file path> [mode1[ mode2]]");
        System.err.println();
        System.err.println("    mode: case in sensitive");
        System.err.println("        clear         : delete rows from \"to\" DB tables");
        System.err.println("        copy          : copy tables data from \"from\" DB to \"to\" DB : default mode");
        System.err.println("        drop          : drop tables from \"drop\" DB");
        System.err.println("        list          : list actual tables of \"from\" DB");
        System.err.println("        verify        : verify tables as actual copy target");
        System.err.println("                      : this mode lists as actual copy table names after removing ignore tables from target tables");
        System.err.println();
        System.err.println("     You can specify mode1 to any of above.");
        System.err.println("     You can specify mode2 to \"copy\" mode with \"clear\" mode as mode1.");
        System.err.println();
        System.err.println("    property file keys");
        System.err.println();
        System.err.println("      from.xxx are for copy, list, verify");
        System.err.println("        from.url      : url for connecting to source DB");
        System.err.println("        from.user     : user name of from.url");
        System.err.println("        from.password : passworrd of from.user");
        System.err.println("        from.schema   : schema of source tables");
        System.err.println();
        System.err.println("      to.xxx are for clear, copy, verify");
        System.err.println("        to.url        : url for connecting to distination DB");
        System.err.println("        to.user       : user name of to.url");
        System.err.println("        to.password   : passworrd of to.user");
        System.err.println("        to.schema     : schema of distination tables");
        System.err.println();
        System.err.println("      drop.xxx are for drop mode only");
        System.err.println("        drop.url      : url for connecting to drop table DB");
        System.err.println("        drop.user     : user name of drop.url");
        System.err.println("        drop.password : passworrd of drop.user");
        System.err.println("        drop.schema   : schema of drop tables");
        System.err.println();
        System.err.println("      tables.xxx are table names list or file path of table names.");
        System.err.println("           format1 : comma separated table name list (e.g. EMP,DEPT)");
        System.err.println("           format1 : file path name with prefix \"file:\" (e.g. file:locaToDash33333.properties)");
        System.err.println("        tables.target : table names of copy target in \"from\" DB");
        System.err.println("                      : all tables of schema in \"from\" DB are target when this key is not specified.");
        System.err.println("        tables.ignore : table names to remove from tables.target");
        System.err.println("                      : no table of schema in \"from\" DB are ignored when this key is not specified.");
        System.err.println("        tables.drop   : table name of drop target in \"drop\" DB");
        System.err.println("                      : no table of schema in \"drop\" DB are dropped when this key is not specified.");
        System.err.println();
        System.err.println("      SEE sample file copier.sample.properties");
        System.err.println();
        System.err.println(" * This tool depends on db2 jdbc driver, it must be able to load from classpath.");
        System.err.println();
        // @formatter:on

        System.exit(0);
    }

    /**
     * コピー元とコピー先の実在するテーブルにあるテーブル名、コピー対象に指定されたテーブル名、無視対象に指定されたテーブル名から
     * 実際のコピー対象テーブル名を求める。 求めた値は {@link #actualTarget} に格納する。
     *
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private void resolveCopyTarget() throws SQLException {
        // コピー元を一旦は対象テーブルとする
        actualTarget = listTableNames(props.from);
        List<String> distTableNames = listTableNames(props.to);
        // コピー先にあるもののみ残す
        actualTarget.retainAll(distTableNames);
        // 無視対象を削除する
        actualTarget.removeAll(props.ignoreTables);
        if (props.targetTables.isEmpty()) {
            return;
        }
        // コピー対象にあるもののみを残す
        actualTarget.retainAll(props.targetTables);
    }

    /**
     * コマンド行引数の最初の引数をファイル名としてプロパティーファイルを読み込む。
     *
     * @param args
     *            コマンド行引数
     * @return 読み込んだ結果を保持する {@link CopierProperties} オブジェクト
     */
    private static CopierProperties loadProperties(String[] args) {
        CopierProperties props = CopierProperties.load(args[0]);
        return props;
    }

    /**
     * 引数が表すDBに接続し、引数に示されたスキーマのテーブル名一覧を返す。
     *
     * 返す一覧は自然言語順の昇順にソートする。
     *
     * @param connInfo
     *            テーブル名一覧を作成する対象の接続情報
     * @return 作成したテーブル名の一覧
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private List<String> listTableNames(CopierProperties.ConnInfo connInfo) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = connect(connInfo, true)) {
            try (PreparedStatement stmt = conn
                    .prepareStatement("SELECT NAME FROM SYSIBM.SYSTABLES WHERE TYPE='T' AND CREATOR='"
                            + connInfo.schema + "' FOR READ ONLY")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        tableNames.add(rs.getString(1));
                    }
                }
            }
        }
        Collections.sort(tableNames);
        return tableNames;
    }

    /**
     * {@link #actualTarget} に格納された全てのテーブル名のテーブルのデータをコピーする
     *
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private void copyTables() throws SQLException {
        final String copyingPrefix = "COPYING... " + props.from.schema + ".";
        final String toPrefix = " TO " + props.to.schema + ".";
        try (Connection connFrom = connect(props.from, true); Connection connTo = connect(props.to, false)) {
            connTo.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            Map<String, List<Column>> tableColumns = tableColumns(connFrom, props.from.schema, actualTarget);
            for (String tableName : actualTarget) {
                System.err.print(copyingPrefix + tableName + toPrefix + tableName);
                copyTable(connFrom, connTo, tableName, tableColumns.get(tableName));
            }
            System.err.println("COPY TABLES successfully ended.");
        }
    }

    /**
     * 引数に指定されたテーブル名のテーブルの全てのデータをコピーする。
     *
     * @param connFrom
     *            コピー元のDB接続
     * @param connTo
     *            コピー先のDB接続
     * @param tableName
     *            コピー対象のテーブル名
     * @param columns
     *            コピー対象テーブルのカラム一覧
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private void copyTable(Connection connFrom, Connection connTo, String tableName, List<Column> columns)
            throws SQLException {
        final String baseSqlFrom = "SELECT * FROM " + props.from.schema + ".";
        final String insertStatement = generateInsertStatement(props.to.schema, tableName, columns);
        try (PreparedStatement stmtFrom = connFrom
                .prepareStatement(baseSqlFrom + tableName);
                PreparedStatement stmtTo = connTo.prepareStatement(insertStatement)) {
            copyRows(stmtFrom, stmtTo, columns);
        }
    }

    /**
     * コピー先のテーブルへのインサート文を作成する。 作成したインサート文は値を埋め込むためのプレイスホルダーをカラム数分含む。
     *
     * @param schema
     *            コピー先のテーブルのスキーマ
     * @param tableName
     *            コピー先のテーブルのテーブル名
     * @param columns
     *            コピー先のテーブルのカラム一覧
     * @return 作成したインサート文。
     */
    private String generateInsertStatement(String schema, String tableName, List<Column> columns) {
        StringBuilder statement = new StringBuilder("INSERT INTO ");
        statement.append(schema).append('.').append(tableName).append(" ");
        statement.append(columns.stream().map(col -> col.getName()).collect(Collectors.joining(",\n", "(", ")")));
        statement.append(" VALUES ");
        statement.append(
                IntStream.range(0, columns.size()).mapToObj(r -> "?").collect(Collectors.joining(",", "(", ")")));
        return statement.toString();
    }

    /**
     * テーブルの全ての行をコピーする。
     *
     * @param stmtFrom
     *            コピー元から読み込むための {@link PreparedStatement}
     * @param stmtTo
     *            コピー先にインサートするための {@link PreparedStatement}
     * @param columns
     *            コピー対象テーブルのカラム一覧
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private void copyRows(PreparedStatement stmtFrom, PreparedStatement stmtTo, List<Column> columns)
            throws SQLException {
        int inserted = 0;
        try (ResultSet rsFrom = stmtFrom.executeQuery()) {
            while (rsFrom.next()) {
                copyColumns(rsFrom, stmtTo, columns);
                stmtTo.addBatch();
                inserted++;
                if (inserted % MAX_TRAN_COUNT == 0) {
                    int[] countEachStatement = stmtTo.executeBatch();
                    if (IntStream.of(countEachStatement).sum() != countEachStatement.length) {
                        System.err.println(String.format("Count Unmatch Error: expected=%d, actual=%d",
                                countEachStatement.length, countEachStatement));
                    }
                    stmtTo.getConnection().commit();
                    stmtTo.clearBatch();
                }
            }
        } catch (SQLException e) {
            stmtTo.getConnection().rollback();
            throw e;
        }

        if (inserted % MAX_TRAN_COUNT != 0) {
            int[] countEachStatement = stmtTo.executeBatch();
            if (IntStream.of(countEachStatement).sum() != countEachStatement.length) {
                System.err.println(String.format("Count Unmatch Error: expected=%d, actual=%d",
                        countEachStatement.length, countEachStatement));
            }
            stmtTo.getConnection().commit();
            stmtTo.clearBatch();
        }
        System.err.println(String.format(": %d rows inserted.", inserted));
    }

    /**
     * 一行の各カラムの値をコピー元の {@link ResultSet} から コピー先の {@link PreparedStatement}
     * に設定する。
     *
     * @param rsFrom
     *            コピー元の {@link ResultSet}
     * @param stmtTo
     *            コピー先の {@link PreparedStatement}
     * @param columns
     *            コピー対象のテーブルのカラム一覧
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private void copyColumns(ResultSet rsFrom, PreparedStatement stmtTo, List<Column> columns) throws SQLException {
        for (int i = 0; i < columns.size(); i++) {
            Column col = columns.get(i);
            Object value = rsFrom.getObject(col.getName());
            int stmtIndex = i + 1;
            if (rsFrom.wasNull()) {
                stmtTo.setNull(stmtIndex, col.getColType().getSqlColType());
            } else {
                stmtTo.setObject(stmtIndex, value, col.getColType().getSqlColType());
            }
        }
    }

    /**
     * 市営された接続情報を使用してDBへ接続する。
     *
     * @param connInfo
     *            接続先の情報
     * @param autoCommit
     *            自動コミットを有効にするかどうか (true: 有効, false: 無効)
     * @return 接続結果の {@link Connection} オブジェクト
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private Connection connect(ConnInfo connInfo, boolean autoCommit) throws SQLException {
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Connection conn = DriverManager.getConnection(connInfo.url, connInfo.user, connInfo.password);
        conn.setAutoCommit(autoCommit);
        return conn;
    }

    /**
     * 指定された接続のDBから、指定されたスキーマ、テーブル一覧を使って、テーブル名別のカラム一覧を作成して返す。
     *
     * @param conn
     *            カラム一覧を作成する対象のテーブルのDBへの接続
     * @param schema
     *            作成対象のテーブルのスキーマ
     * @param listTables
     *            作成対象のテーブル名の一覧
     * @return 作成したテーブル名別のカラム一覧
     * @throws SQLException
     *             SQLエラーが発生した場合
     */
    private Map<String, List<Column>> tableColumns(Connection conn, String schema, List<String> listTables)
            throws SQLException {
        Map<String, List<Column>> map = new TreeMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM SYSIBM.SYSCOLUMNS WHERE TBCREATOR = ? AND TBNAME = ? ORDER BY COLNO")) {
            for (String tableName : listTables) {
                ps.setString(1, schema);
                ps.setString(2, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.computeIfAbsent(tableName, t -> new ArrayList<>()).add(new Column(rs.getString("NAME"),
                                rs.getString("COLTYPE"), rs.getInt("LENGTH"), rs.getInt("SCALE"),
                                rs.getObject("KEYSEQ") == null ? 0 : rs.getInt("KEYSEQ"), rs.getBoolean("NULLS")));
                    }
                }
            }
        }
        map.values().forEach(columns -> columns.sort((a, b) -> a.getName().compareTo(b.getName())));
        return map;
    }
}
