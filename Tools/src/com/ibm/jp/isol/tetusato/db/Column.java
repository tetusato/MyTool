package com.ibm.jp.isol.tetusato.db;

public class Column {
    private String name;
    private String typeName;
    private int length;
    private int scale;
    private ColType colType;
    private int keyseq;
    private boolean nulls;

    public Column(String name, String colType, int length, int scale, int keyseq, boolean nulls) {
        this.name = name.trim();
        typeName = colType.trim();
        this.colType = ColType.valueOf(typeName);
        this.length = length;
        this.scale = scale;
        this.keyseq = keyseq;
        this.nulls = nulls;
    }

    public String fieldDef() {
        String size = ColType.isEnableLength(colType) && length > 0 ? Integer.toString(length) : "";
        size += ColType.isEnableScale(colType) && scale > 0 ? "." + Integer.toString(scale) : "";
        if (!size.isEmpty()) {
            size = "(" + size + ")";
        }
        return name + " " + typeName + " " + size + (nulls ? "" : " NOT NULL");
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

    public int getKeyseq() {
        return keyseq;
    }

    public boolean getNulls() {
        return nulls;
    }

    public void setColType(ColType colType) {
        this.colType = colType;
        typeName = colType.name();
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
        colType = ColType.valueOf(typeName);
    }

    public void setKeyseq(int keyseq) {
        this.keyseq = keyseq;
    }

    public void setNulls(boolean nulls) {
        this.nulls = nulls;
    }
}
