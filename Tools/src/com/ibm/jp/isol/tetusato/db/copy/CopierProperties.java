package com.ibm.jp.isol.tetusato.db.copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class CopierProperties {

    enum Key {
        // @formatter:off
        FROM_USER("from.user"),
        FROM_PASSWORD("from.password"),
        FROM_URL("from.url"),
        FROM_SCHEMA("from.schema"),
        TO_USER("to.user"),
        TO_PASSWORD("to.password"),
        TO_URL("to.url"),
        TO_SCHEMA("to.schema"),
        DROP_USER("drop.user"),
        DROP_PASSWORD("drop.password"),
        DROP_URL("drop.url"),
        DROP_SCHEMA("drop.schema"),
        TARGET_TABLES("tables.target"),
        IGNORE_TABLES("tables.ignore"),
        DROP_TABLES("tables.drop"),
        INVALID_KEY("not_valid_key")
        ;
        // @formatter:on

        private static Map<String, Key> propertyKeys = new HashMap<>();
        static {
            for (Key key : Key.values()) {
                propertyKeys.put(key.key, key);
            }
        }
        private String key;

        private Key(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Key asKey(String searchKey) {
            return propertyKeys.getOrDefault(searchKey, INVALID_KEY);
        }
    }

    static class ConnInfo {
        String user;
        String password;
        String url;
        String schema;

        @Override
        public String toString() {
            return "user=" + user + ", password=" + password + ", url=" + url + ", schema=" + schema;
        }
    }

    ConnInfo from = new ConnInfo();
    ConnInfo to = new ConnInfo();
    ConnInfo drop = new ConnInfo();
    List<String> targetTables = new ArrayList<>();
    List<String> ignoreTables = new ArrayList<>();
    List<String> dropTables = new ArrayList<>();

    private static final CopierProperties EMPTY_OBJ = new CopierProperties();

    private CopierProperties() {

    }

    public static CopierProperties load(String file) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(file)));
            return CopierProperties.create(props);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(String.format("File access error: ", file));
            return EMPTY_OBJ;
        }
    }

    private static String cast(Object object) {
        return String.class.isAssignableFrom(object.getClass()) ? String.class.cast(object) : object.toString();
    }

    public static CopierProperties create(Properties props) throws IOException {
        Set<Object> keySet = props.keySet();
        CopierProperties copierProps = new CopierProperties();
        for (Object key : keySet) {
            String strKey = cast(key);
            String value = props.getProperty(strKey);
            switch (Key.asKey(strKey)) {
            case INVALID_KEY:
                break;
            case DROP_PASSWORD:
                copierProps.drop.password = value;
                break;
            case DROP_SCHEMA:
                copierProps.drop.schema = value.toUpperCase();
                break;
            case DROP_URL:
                copierProps.drop.url = value;
                break;
            case DROP_USER:
                copierProps.drop.user = value;
                break;
            case FROM_PASSWORD:
                copierProps.from.password = value;
                break;
            case FROM_SCHEMA:
                copierProps.from.schema = value.toUpperCase();
                ;
                break;
            case FROM_URL:
                copierProps.from.url = value;
                break;
            case FROM_USER:
                copierProps.from.user = value;
                break;
            case DROP_TABLES:
                if (value.trim().startsWith("file:")) {
                    copierProps.dropTables.addAll(loadTableList(value.trim().substring(5).trim()));
                } else {
                    copierProps.dropTables.addAll(Arrays.asList(value.split(" *, *")));
                }
                copierProps.dropTables.replaceAll(String::toUpperCase);
                break;
            case IGNORE_TABLES:
                if (value.trim().startsWith("file:")) {
                    copierProps.ignoreTables.addAll(loadTableList(value.trim().substring(5).trim()));
                } else {
                    copierProps.ignoreTables.addAll(Arrays.asList(value.split(" *, *")));
                }
                copierProps.ignoreTables.replaceAll(String::toUpperCase);
                break;
            case TARGET_TABLES:
                if (value.trim().startsWith("file:")) {
                    copierProps.targetTables.addAll(loadTableList(value.trim().substring(5).trim()));
                } else {
                    copierProps.targetTables.addAll(Arrays.asList(value.split(" *, *")));
                }
                copierProps.targetTables.replaceAll(String::toUpperCase);
                break;
            case TO_PASSWORD:
                copierProps.to.password = value;
                break;
            case TO_SCHEMA:
                copierProps.to.schema = value.toUpperCase();
                ;
                break;
            case TO_URL:
                copierProps.to.url = value;
                break;
            case TO_USER:
                copierProps.to.user = value;
                break;
            }
        }
        return copierProps;
    }

    private static Collection<? extends String> loadTableList(String file) throws IOException {
        return Files.readAllLines(new File(file).toPath());
    }
}
