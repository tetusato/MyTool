package com.ibm.jp.isol.tetusato.db.printer;

import java.io.PrintStream;

import com.ibm.jp.isol.tetusato.db.Printer;

public class StdPrinter implements Printer{

    private PrintStream stream;
    
    public StdPrinter(PrintStream stream) {
        this.stream = stream;
    }
    
    @Override
    public Printer println() {
        stream.println();
        return this;
    }

    @Override
    public Printer println(String x) {
        stream.println(x);
        return this;
    }

    @Override
    public Printer println(String format, Object... args) {
        stream.printf(format + "%n", args);
        return this;
    }

    @Override
    public Printer print(String s) {
        stream.print(s);
        return this;
    }

    @Override
    public Printer print(String format, Object... args) {
        stream.printf(format, args);
        return this;
    }

    @Override
    public void close() throws Exception {
        // nothing to do
    }

}
