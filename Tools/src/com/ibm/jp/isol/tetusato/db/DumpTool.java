package com.ibm.jp.isol.tetusato.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * コマンドライン実行用のメインクラス。 コマンドライン引数とプロパティーからの各種設定値の取得を行い、取得した値を引き渡して実処理を起動する。
 */
public class DumpTool {

    private static final String SYS_TOOL_STDOUT = "tool.stdout";
    private static final String BRIND_NULL = "brindNull";

    public static void main(String... args) throws SQLException {
        Arguments arguments = createArguments(args);
        System.err.println("dump from " + arguments.getHost() + ":" + arguments.getPort());
        System.err.println("  user=" + arguments.getUserUpperCase() + ", schema=" + arguments.getSchemaUpperCase());
        System.err.println(arguments);
        DbTables dbtables = new DbTables(arguments);
        dbtables.dumpTables();
    }

    private static Arguments createArguments(String[] args) {
        Arguments arguments = new Arguments();
        arguments.setPrintStdout(Boolean.getBoolean(SYS_TOOL_STDOUT));
        arguments.setBrindNull(Boolean.getBoolean(BRIND_NULL));
        if (args.length < 1 && arguments.isPrintStdout() || args.length < 2 && !arguments.isPrintStdout()) {
            System.err.println("Command line arguments missing.");
            printUsage();
            System.exit(-1);
        }
        String propertyFileName = args[0];
        File propertyFile = new File(propertyFileName);
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(propertyFile));
        } catch (FileNotFoundException e) {
            System.err
                    .println("proeprty file not found:" + propertyFile.getAbsolutePath() + "(" + e.getMessage() + ")");
            System.exit(-1);
        } catch (IOException e) {
            System.err.println("io error: " + e.getMessage());
            System.exit(-2);
        }
        arguments.setHost(properties.getProperty("host"));
        arguments.setUser(properties.getProperty("user"));
        arguments.setPassword(properties.getProperty("password"));
        if (Objects.isNull(arguments.getHost())) {
            System.err.println("host is not specified.");
            printUsage();
            System.exit(-1);
        }
        if (Objects.isNull(arguments.getUser())) {
            System.err.println("user is not specified.");
            printUsage();
            System.exit(-1);
        }
        if (Objects.isNull(arguments.getPassword())) {
            System.err.println("password is not specified.");
            printUsage();
            System.exit(-1);
        }
        arguments.setSchema(properties.getProperty("schema", arguments.getUser()));
        arguments.setPort(properties.getProperty("port", arguments.getPort()));
        arguments.setDbName(properties.getProperty("dbname", arguments.getDbName()));
        arguments.setFilename1(properties.getProperty("filename1", arguments.getFileName1()));
        arguments.setFilename2(properties.getProperty("filename2", arguments.getFileName2()));
        if (properties.containsKey("enableLobDump")) {
            arguments.setEnableLobDump(Boolean.valueOf(properties.getProperty("enableLobDump")));
        }
        arguments.setCharsetName(properties.getProperty("charsetName", arguments.getCharsetName()));
        if (properties.containsKey("enableBom")) {
            arguments.setEnableBom(Boolean.valueOf(properties.getProperty("enableBom")));
        }
        // 標準出力に出力するときはどうでもいいので以下のチェックは迂回する
        if (!arguments.isPrintStdout()) {
            int separatorPos = args[1].lastIndexOf('#');
            if (separatorPos > 1) {
                arguments.setDirname(args[1].substring(0, separatorPos));
                arguments.setSubdir(args[1].substring(separatorPos + 1));
                File dir = new File(arguments.getDirName());
                if (dir.exists() && !dir.isDirectory()) {
                    System.err.println("dirname is exist and is not directory:" + dir.getAbsolutePath());
                    printUsage();
                    System.exit(-1);
                }
                File subdir = new File(dir, arguments.getFormattedSubdir());
                if (subdir.exists() && !subdir.isDirectory()) {
                    System.err.println("subdir is exist and is not directory:" + subdir.getAbsolutePath());
                    printUsage();
                    System.exit(-1);
                }
                try {
                    if (subdir.getCanonicalPath().equals(dir.getCanonicalPath())) {
                        System.err.println("dirname and subdir is same dir:" + dir.getCanonicalPath());
                        printUsage();
                        System.exit(-1);
                    }
                } catch (IOException e) {
                    System.err.println("Unrecognizable exception has been occurred:" + e.getMessage());
                    printUsage();
                    System.exit(-1);
                }
            } else {
                arguments.setDirname(args[1]);
                File dir = new File(arguments.getDirName());
                if (dir.exists() && !dir.isDirectory()) {
                    System.err.println("dirname is exist and is not directory:" + dir.getAbsolutePath());
                    printUsage();
                    System.exit(-1);
                }
            }
        }
        if (args.length > 2) {
            arguments.setTablenames(Arrays.asList(Arrays.copyOfRange(args, 2, args.length)));
        }
        return arguments;
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("USAGE:");
        System.out.println(String.format("[-D%s=true] [-D%s=true] %s %s", BRIND_NULL, SYS_TOOL_STDOUT, DumpTool.class.getSimpleName(),
                "propertyFilename dirname[#subdir] [tablename[ tablename...]]"));
        System.out.println();
        System.out.println("  Dump to stdout when the system property " + SYS_TOOL_STDOUT + " is true.");
        System.out.println("  Dump null as no quoted empty column data between column separator chars when the system property " + BRIND_NULL + " is true.");
        System.out.println();
        System.out.println("  property file key and value example: host,user and password is required.");
        System.out.println("      host=dashdb-xxxx-xxxx.bludmix.net");
        System.out.println("      port=50000  : default");
        System.out.println("      dbname=BLUDB  : default");
        System.out.println("      user=dashXxxx");
        System.out.println("      schema=dashXxxx");
        System.out.println("        default settings: use user name");
        System.out.println("      password=b99999ee33");
        System.out.println("      filename1=pattern of filename when sub dir is not specified");
        System.out.println("      filename2=pattern of filename when sub dir is specified");
        System.out.println("        @TL@ : long timestamp : yyyyMMddHHmmssSSS");
        System.out.println("        @TS@ : short timestamp : yyyyMMddHHmmss");
        System.out.println("        @TB@ : tablename");
        System.out.println("        @SD@ : sub dir name");
        System.out.println("        @@ : charcter of @");
        System.out.println("        default setting:");
        System.out.println("          filename1=@TB@.csv");
        System.out.println("          filename2=@TB@.csv");
        System.out.println("        eg. format to avoid confliction of filenmaes");
        System.out.println("          filename1=@TL@@@@TB@.csv");
        System.out.println("          filename2=@TL@@@@SD@@@@TB@.csv");
        System.out.println();
        System.out.println("  dirname: distination dir name");
        System.out.println("      this tool create the dir if it is not exit.");
        System.out.println("  subdir: sub dir name of dirname (# is separator)");
        System.out.println("      this tool create the dir if it is not exit.");
        System.out.println();
        System.out.println(
                "  you can specify dummy name to dirname and subdir when " + SYS_TOOL_STDOUT + "=true is specified.");
        System.out.println();
        System.out.println("  tablename: table name which you'd like to dump");
        System.out.println("      All tables will be dumped when no tablenames are specified at all.");
        System.out.println("      this tool select all rows of specified table with column header.");
        System.out.println("      filename format:  timestamp[@subdir]@tablename.csv");
        System.out.println("        timestamp : yyyyMMddHHmmssSSS");
        System.out.println("        subdir : subdir name in command line arguments when you specify it.");
        System.out.println("        eg. 20170201163247912@W01-0001-01@COIN_USER.csv");
    }
}
