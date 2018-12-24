package com.ibm.jp.isol.tetusato.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class Table {

    private static final String CREATE_TABLE = "CREATE TABLE %s.%s ( %s )";

    private String name;
    private List<Column> columns;
    private List<Column> keys;

    public Table(String name, List<Column> columns) {
        this.columns = columns;
        keys = columns.stream().filter(col -> col.getKeyseq() > 0)
                .sorted((col1, col2) -> Integer.compare(col1.getKeyseq(), col2.getKeyseq()))
                .collect(Collectors.toList());
        this.name = name;
    }

    public void createTable(Connection conn, String schema) throws SQLException {
        String columnDefs = columns.stream().map(Column::fieldDef).collect(Collectors.joining(","));
        String keyDefs = keys.stream().map(Column::getName).collect(Collectors.joining(",", "(", ")"));
        String columnKeyDefs = columnDefs + (keyDefs.length() == 2 ? "" : " PRIMARY KEY " + keyDefs);
        String createSql = String.format(CREATE_TABLE, schema, name, columnKeyDefs);
        System.out.println(createSql);
        // @formatter:off
        try (PreparedStatement statement = conn.prepareStatement(createSql)) {
            statement.execute();
        }
        conn.commit();
        // @formatter:on
    }
}
