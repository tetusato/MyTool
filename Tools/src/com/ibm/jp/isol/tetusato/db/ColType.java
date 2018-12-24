package com.ibm.jp.isol.tetusato.db;

import java.sql.Types;

import com.ibm.db2.jcc.DB2Types;

public enum ColType {
    // @formatter:off
    INTEGER(Types.INTEGER), SMALLINT(Types.SMALLINT),
    FLOAT(Types.FLOAT), DOUBLE(Types.DOUBLE),
    DECIMAL(Types.DECIMAL),
    BIGINT(Types.BIGINT), DECFLOAT(DB2Types.DECFLOAT),
    CHAR(Types.CHAR), VARCHAR(Types.VARCHAR), LONGVAR(Types.LONGVARCHAR),
    GRAPHIC(Types.CHAR), VARGRAPH(Types.VARCHAR), LONGVARG(Types.LONGVARCHAR),
    DATE(Types.DATE), TIME(Types.TIME), TIMESTMP(Types.TIMESTAMP), TIMESTZ(Types.TIME_WITH_TIMEZONE),
    BLOB(Types.BLOB), CLOB(Types.CLOB), DBCLOB(Types.CLOB),
    ROWID(Types.ROWID), DISTINCT(Types.DISTINCT), XML(Types.SQLXML),
    BINARY(Types.BINARY), VARBIN(Types.VARBINARY);
    // @formatter:on

    private int sqlColType;

    private ColType(int sqlColType) {
        this.sqlColType = sqlColType;
    }

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

    public static boolean isEnableLength(ColType type) {
        switch (type) {
        case INTEGER:
        case SMALLINT:
        case BIGINT:
        case DATE:
        case TIME:
        case XML:
            return false;
        default:
            break;
        }
        return true;
    }

    public static boolean isEnableScale(ColType type) {
        switch (type) {
        case DECIMAL:
            return true;
        default:
            break;
        }
        return false;
    }

    public static boolean isNumberType(ColType type) {
        switch (type) {
        case INTEGER:
        case SMALLINT:
        case FLOAT:
        case DOUBLE:
        case DECIMAL:
        case BIGINT:
        case BINARY:
        case VARBIN:
        case DECFLOAT:
        case ROWID:
        case DISTINCT:
            return true;
        default:
            break;
        }
        return false;
    }

    public int getSqlColType() {
        return sqlColType;
    }
}
