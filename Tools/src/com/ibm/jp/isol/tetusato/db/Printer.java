package com.ibm.jp.isol.tetusato.db;

public interface Printer extends AutoCloseable {
    Printer println();

    Printer println(String x);

    Printer println(String format, Object... args);

    Printer print(String s);

    Printer print(String format, Object... args);

    default Printer write(byte value) {
        return println(Byte.toString(value));
    }
}
