package com.ibm.jp.isol.tetusato.db.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import com.ibm.jp.isol.tetusato.db.Printer;

public class FilePrinter implements Printer {

    private File file;
    private PrintWriter printer;
    private OutputStream outputStream;

    public FilePrinter(File file, String charsetName) throws FileNotFoundException, UnsupportedEncodingException {
        this.file = file;
        outputStream = new FileOutputStream(this.file);
        printer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, charsetName)));
    }

    @Override
    public void close() {
        printer.close();
    }

    @Override
    public Printer println() {
        printer.println();
        return null;
    }

    @Override
    public Printer println(String x) {
        printer.println(x);
        return null;
    }

    @Override
    public Printer println(String format, Object... args) {
        printer.printf(format + "%n", args);
        return null;
    }

    @Override
    public Printer print(String s) {
        printer.print(s);
        return null;
    }

    @Override
    public Printer print(String format, Object... args) {
        printer.printf(format, args);
        return null;
    }

    @Override
    public Printer write(byte value) {
        try {
            outputStream.write(value);
        } catch (IOException e) {
            System.err.println("Failed to write value=" + value);
            e.printStackTrace();
        }
        return null;
    }

}
