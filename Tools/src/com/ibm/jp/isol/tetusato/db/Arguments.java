package com.ibm.jp.isol.tetusato.db;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class Arguments {
    private static final String FILE_EXTENSION_CSV = ".csv";
    private static final String FILENAME_SEPARATOR = "@";
    private static final String PATTERN_SEPARATOR = "@@";
    private static final String PATTERN_SUB_DIRECTORY = "@SD@";
    private static final String PATTERN_TABLE_NAME = "@TB@";
    private static final String PATTERN_TIMESTAMP_SHORT = "@TS@";
    private static final String PATTERN_TIMESTAMP_LONG = "@TL@";
    private boolean printStdout;
    private boolean brindNull;
    private String host;
    private String port = "50000";
    private String dbName = "BLUDB";
    private String user;
    private String userUpperCase;
    private String schema;
    private String schemaUpperCase;
    private String password;
    private String dirName;
    private String formattedSubdir;
    private String subdir;
    private String fileName1 = PATTERN_TABLE_NAME + FILE_EXTENSION_CSV; // @TB@.csv;
    private String fileName2 = PATTERN_TABLE_NAME + FILE_EXTENSION_CSV; // @TB@.csv;
    private String filename1Format;
    private String filename2Format;
    private List<String> tablenames;
    private boolean enableLobDump = false;
    private String charsetName = System.getProperty("file.encoding");
    private boolean enableBom = false;

    public String getDbName() {
        return dbName;
    }

    public String getDirName() {
        return dirName;
    }

    public String getFileName1() {
        return fileName1;
    }

    public String getFileName2() {
        return fileName2;
    }

    public boolean hasSubdir() {
        return Optional.ofNullable(subdir).orElse("").length() != 0;
    }

    public String getFormattedSubdir() {
        return formattedSubdir;
    }

    public String getFormattedFileName(Date date, String tableName) {
        return hasSubdir() ? getFormattedFileName2(date, tableName) : getFormattedFileName1(date, tableName);
    }

    public String getFormattedFileName1(Date date, String tableName) {
        return String.format(filename1Format, date, tableName, subdir);
    }

    public String getFormattedFileName2(Date date, String tableName) {
        return String.format(filename2Format, date, tableName, subdir);
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getPort() {
        return port;
    }

    public String getSubdir() {
        return subdir;
    }

    public List<String> getTablenames() {
        return tablenames;
    }

    public String getUser() {
        return user;
    }

    public String getUserUpperCase() {
        return userUpperCase;
    }

    public String getSchema() {
        return schema;
    }

    public String getSchemaUpperCase() {
        return schemaUpperCase;
    }

    public boolean isPrintStdout() {
        return printStdout;
    }

    public boolean isBrindNull() {
        return brindNull;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public void setDirname(String dirname) {
        this.dirName = dirname;
    }

    public void setFilename1(String filename1) {
        this.fileName1 = filename1;
        this.filename1Format = buildFormatPattern(this.fileName1);
    }

    public void setFilename2(String filename2) {
        this.fileName2 = filename2;
        this.filename2Format = buildFormatPattern(this.fileName2);
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setPrintStdout(boolean printStdout) {
        this.printStdout = printStdout;
    }

    public void setBrindNull(boolean brindNull) {
        this.brindNull = brindNull;
    }

    public void setSubdir(String subdir) {
        this.subdir = subdir;
        if (this.subdir.contains(PATTERN_TIMESTAMP_LONG) || this.subdir.contains(PATTERN_TIMESTAMP_SHORT)) {
            this.formattedSubdir = String.format(buildDateFormatPattern(this.subdir), new Date());
        } else {
            this.formattedSubdir = this.subdir;
        }
    }

    public void setTablenames(List<String> tablenames) {
        this.tablenames = tablenames;
    }

    public void setUser(String user) {
        this.user = user;
        this.userUpperCase = user.toUpperCase();
    }

    public void setSchema(String schema) {
        this.schema = schema;
        this.schemaUpperCase = schema.toUpperCase();
    }

    public boolean isEnableLobDump() {
        return enableLobDump;
    }

    public void setEnableLobDump(boolean enableLobDump) {
        this.enableLobDump = enableLobDump;
    }

    /**
     * @return the charsetName
     */
    public String getCharsetName() {
        return charsetName;
    }

    /**
     * @param charsetName
     *            the charsetName to set
     */
    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    /**
     * @return the enableBom
     */
    public boolean isEnableBom() {
        return enableBom;
    }

    /**
     * @param enableBom
     *            the enableBom to set
     */
    public void setEnableBom(boolean enableBom) {
        this.enableBom = enableBom;
    }

    @Override
    public String toString() {
        return "Arguments [brindNull=" + brindNull + ", printStdout=" + printStdout + ", host=" + host + ", port=" + port + ", dbName=" + dbName
                + ", user=" + user + ", userUpperCase=" + userUpperCase + ", schema=" + schema + ", schemaUpperCase="
                + schemaUpperCase + ", password=" + password + ", dirname=" + dirName + ", subdir=" + subdir
                + ", filename1=" + fileName1 + ", filename2=" + fileName2 + ", filename1Format=" + filename1Format
                + ", filename2Format=" + filename2Format + ", tablenames=" + tablenames + ", enableLobDump="
                + enableLobDump + ", charsetName=" + charsetName + ", enableBom=" + enableBom + "]";
    }

    private String buildFormatPattern(String filename) {
        return buildDateFormatPattern(filename).replace(PATTERN_TABLE_NAME, "%2$s")
                .replace(PATTERN_SUB_DIRECTORY, "%3$s").replace(PATTERN_SEPARATOR, FILENAME_SEPARATOR);
    }

    private String buildDateFormatPattern(String filename) {
        return filename.replace(PATTERN_TIMESTAMP_LONG, "%1$tY%1$tm%1$td%1$tH%1$tM%1$tS%1$tL")
                .replace(PATTERN_TIMESTAMP_SHORT, "%1$tY%1$tm%1$td%1$tH%1$tM%1$tS");
    }
}
