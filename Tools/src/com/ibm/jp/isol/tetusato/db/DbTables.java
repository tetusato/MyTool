package com.ibm.jp.isol.tetusato.db;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.jp.isol.tetusato.db.printer.CsvPrinter;
import com.ibm.jp.isol.tetusato.db.printer.FilePrinter;
import com.ibm.jp.isol.tetusato.db.printer.StdoutPrinter;

public class DbTables {

    public static enum ColType {
        // @formatter:off
        INTEGER, SMALLINT, FLOAT, DOUBLE, CHAR, VARCHAR, LOGVAR, DECIMAL, GRAPHIC, VARGRAPH, LONGVARGRAPH,
        DATE, TIME, TIMESTMP, TIMESTZ,
        BLOB, CLOB, DBCLOB,
        ROWID, DISTINCT, XML, BIGINT, BINARY, VARBIN, DECFLOAT;
        // @formatter:on
        public static boolean isLobType(ColType type) {
            switch (type) {
            case BLOB:
            case CLOB:
            case DBCLOB:
                return true;
            default:
                break;
            }
            return false;
        }
    }

    public class Column {
        private String name;
        private String typeName;
        private int length;
        private int scale;
        private ColType colType;

        public Column(String name, String colType, int length, int scale) {
            this.name = name.trim();
            this.typeName = colType.trim();
            this.colType = ColType.valueOf(typeName);
            this.length = length;
            this.scale = scale;
        }

        public String getColName() {
            return typeName;
        }

        public ColType getColType() {
            return colType;
        }

        public int getLength() {
            return length;
        }

        public String getName() {
            return name;
        }

        public int getScale() {
            return scale;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setColType(ColType colType) {
            this.colType = colType;
            this.typeName = colType.name();
        }

        public void setLength(int length) {
            this.length = length;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setScale(int scale) {
            this.scale = scale;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
            this.colType = ColType.valueOf(typeName);
        }
    }

    private static boolean debugging = Boolean.getBoolean("debugging");

    private String nullString = "<nil>";

    private Arguments arguments;

    public void dumpTables() throws SQLException {
        Date date = new Date(); // ファイル名に使用するタイムスタンプ用
        try (Connection conn = connect()) {
            if (Objects.isNull(arguments.getTablenames())) {
                // コマンドラインでテーブル名の指定がなかったので全テーブルをセットする
                arguments.setTablenames(listTables(conn));
            }
            Map<String, List<Column>> tableColumns = tableColumns(conn, arguments.getTablenames());
            Map<String, List<String>> tableKeys = tableKeys(conn);
            dump(tableColumns);
            for (Entry<String, List<Column>> entry : tableColumns.entrySet()) {
                String tableName = entry.getKey();
                String fileName = arguments.getFormattedFileName(date, tableName);
                System.out.println(String.format("#### %s ####", fileName));
                String select = String.join("", "SELECT ",
                        entry.getValue().stream().map(c -> c.getName()).collect(Collectors.joining(",")), " FROM ",
                        arguments.getSchemaUpperCase(), ".", tableName.toUpperCase(),
                        createOrder(tableName, tableKeys));
                System.err.println("SQL:" + select);
                PreparedStatement ps = conn.prepareStatement(select);
                ResultSet rs = ps.executeQuery();
                try (CsvPrinter printer = selectPrinter(fileName)) {
                    writeBom(printer);
                    printer.printCsv(arguments.isBrindNull(), entry.getValue().stream().map(c -> c.getName().toUpperCase()).toArray());
                    int rows = 0;
                    while (rs.next()) {
                        rows++;
                        printer.printCsv(arguments.isBrindNull(), entry.getValue().stream()
                                .map(c -> retrieve(rs, c, arguments.isEnableLobDump())).toArray());
                    }
                    System.out.println(tableName + ":rows=" + rows);
                }
            }
        }
    }

    private void writeBom(Printer printer) {
        if (!arguments.isEnableBom()) {
            return;
        }
        String charsetName = arguments.getCharsetName().toUpperCase();
        switch (charsetName) {
        case "UTF-16":
            // UTF-16 は勝手に big endian の OutputStreamWriter によりBOM が付加される想定
            break;
        case "UTF-32":
            printer.write((byte) 0x00);
            printer.write((byte) 0x00);
            printer.write((byte) 0xFE);
            printer.write((byte) 0xFF);
            break;
        case "UTF8":
        case "UTF-8":
            printer.write((byte) 0xEF);
            printer.write((byte) 0xBB);
            printer.write((byte) 0xBF);
            break;
        case "UTF-7":
            // sorry, unsupoorted charset name in language java
            break;
        }
    }

    private String createOrder(String tableName, Map<String, List<String>> tableKeys) {
        List<String> list = tableKeys.get(tableName.toUpperCase());
        if (Objects.isNull(list) || list.isEmpty()) {
            return "";
        }
        return " ORDER BY " + String.join(", ", list.toArray(new String[list.size()]));
    }

    private CsvPrinter selectPrinter(String fileName) {
        CsvPrinter printer;
        try {
            printer = new CsvPrinter(arguments.isPrintStdout() ? new StdoutPrinter() : createFilePrinter(fileName));
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            System.err.println("Failed to open " + fileName);
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.err.println("Use StaoutPrinter insted of FilePrinter.");
            printer = new CsvPrinter(new StdoutPrinter());
        }
        return printer;
    }

    private CsvPrinter createFilePrinter(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
        File dir = new File(arguments.getDirName());
        if (arguments.hasSubdir()) {
            dir = new File(dir, arguments.getFormattedSubdir());
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, fileName);
        CsvPrinter printer = new CsvPrinter(new FilePrinter(file, arguments.getCharsetName()));
        return printer;
    }

    private static void dump(Map<String, List<Column>> tableColumns) {
        if (!debugging)
            return;
        for (Entry<String, List<Column>> entry : tableColumns.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().forEach(v -> System.out
                    .println(String.format("\t%s,%s,%d,%d", v.getName(), v.getColType(), v.getLength(), v.getScale())));
        }

    }

    private static SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private String retrieve(ResultSet rs, Column column, boolean enableLobDump) {
        String name = column.getName().toUpperCase();
        String value;
        Optional<byte[]> ofNullable;
        try {
            switch (column.getColType()) {
            case DBCLOB:
                value = "<UNSUPPORT COLUMN TYPE:DBCLOB>";
                break;
            case BLOB:
                ofNullable = Optional.ofNullable(toBytes(rs.getBlob(name)));
                value = ofNullable.isPresent() ? (enableLobDump ? Base64.getEncoder().encodeToString(ofNullable.get())
                        : "<BLOB@" + toHex(Base64.getEncoder().encodeToString(ofNullable.get()).hashCode()) + ">")
                        : nullString;
                break;
            case CLOB:
                ofNullable = Optional.ofNullable(toBytes(rs.getClob(name)));
                value = ofNullable.isPresent() ? (enableLobDump ? Base64.getEncoder().encodeToString(ofNullable.get())
                        : "<CLOB@" + toHex(Base64.getEncoder().encodeToString(ofNullable.get()).hashCode()) + ">")
                        : nullString;
                break;
            case TIMESTMP:
                Optional<Timestamp> timestamp = Optional.ofNullable(rs.getTimestamp(name));
                value = timestamp.isPresent() ? timestampFormatter.format(timestamp.get()) : nullString;
                break;
            default:
                value = Optional.ofNullable(rs.getString(name)).orElse(nullString);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            value = "#" + e.getClass().getSimpleName() + ":" + e.getMessage() + "#(" + column.getName() + " "
                    + column.getColName() + ")";
        }
        return value;
    }

    private byte[] toBytes(Blob blob) {
        if (blob == null) {
            return null;
        }
        InputStream input;
        try {
            input = blob.getBinaryStream();
        } catch (SQLException e) {
            e.printStackTrace();
            return ("#" + e.getClass().getSimpleName() + ":" + e.getMessage() + "#").getBytes();
        }
        return toBytes(input);
    }

    private byte[] toBytes(Clob clob) {
        if (clob == null) {
            return null;
        }
        InputStream input;
        try {
            input = clob.getAsciiStream();
        } catch (SQLException e3) {
            e3.printStackTrace();
            return ("#" + e3.getClass().getSimpleName() + ":" + e3.getMessage() + "#").getBytes();
        }
        return toBytes(input);
    }

    private byte[] toBytes(InputStream input) {
        if (input == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int length = 0;
            try {
                while ((length = input.read(data, 0, data.length)) > 0) {
                    output.write(data, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    output.write(e.getMessage().getBytes());
                } catch (IOException e1) {
                    return ("#" + e1.getClass().getSimpleName() + ":" + e1.getMessage() + "#").getBytes();
                }
            }
            byte[] result = output.toByteArray();
            return result;
        } catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    private String toHex(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        return hex.length() < 8 ? ("00000000" + hex).substring(hex.length()) : hex;
    }

    private String url;

    public DbTables(Arguments arguments) {
        this.arguments = arguments;
        this.nullString = arguments.isBrindNull() ? null : "<nil>";
        url = Stream.of("jdbc:db2://", arguments.getHost(), ":", arguments.getPort(), "/", arguments.getDbName())
                .collect(Collectors.joining());
    }

    public List<String> collectColumnNames(ResultSet rs) throws SQLException {
        List<String> collumnNames = new ArrayList<String>();
        int columns = rs.getMetaData().getColumnCount();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 0; i < columns; i++) {
            collumnNames.add(metaData.getColumnName(i + 1));
        }
        return collumnNames;
    }

    public Connection connect() throws SQLException {
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Connection conn = DriverManager.getConnection(url, arguments.getUser(), arguments.getPassword());
        return conn;
    }

    public List<String> listTables(Connection conn) throws SQLException {
        List<String> tableNames = new ArrayList<String>();
        try (PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM SYSIBM.SYSTABLES WHERE CREATOR = ? ORDER BY NAME")) {
            ps.setString(1, arguments.getSchemaUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableNames.add(rs.getString("NAME"));
                }
            }
        }
        return tableNames;
    }

    private Map<String, List<Column>> tableColumns(Connection conn, List<String> listTables) throws SQLException {
        Map<String, List<Column>> map = new TreeMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM SYSIBM.SYSCOLUMNS WHERE TBCREATOR = ? AND TBNAME = ? ORDER BY COLNO")) {
            for (String tableName : listTables) {
                ps.setString(1, arguments.getSchemaUpperCase());
                ps.setString(2, tableName.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.computeIfAbsent(tableName, t -> new ArrayList<>()).add(new Column(rs.getString("NAME"),
                                rs.getString("COLTYPE"), rs.getInt("LENGTH"), rs.getInt("SCALE")));
                    }
                }
            }
        }
        return map;
    }

    private Map<String, List<String>> tableKeys(Connection conn) throws SQLException {
        Map<String, List<String>> map = new TreeMap<>();
        try (PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM SYSCAT.INDEXES WHERE UNIQUERULE = 'P' AND TABSCHEMA = ?")) {
            ps.setString(1, arguments.getSchemaUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<String> keys = Stream.of(rs.getString("COLNAMES").split("\\+")).filter(v -> !v.isEmpty())
                            .collect(Collectors.toList());
                    if (keys.isEmpty()) {
                        continue;
                    }
                    map.put(rs.getString("TABNAME").toUpperCase(), keys);
                }
            }
        }
        return map;
    }
}