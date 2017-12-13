package com.ibm.jp.isol.tetusato.db.printer;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.jp.isol.tetusato.db.Printer;

public class CsvPrinter implements Printer {

    private static final String SINGLE_DOUBLE_QUOTE = "\"";
    private static final String DOUBLE_DOUBLE_QUOTE = SINGLE_DOUBLE_QUOTE + SINGLE_DOUBLE_QUOTE;

    private Printer printer;

    public CsvPrinter(Printer printer) {
        this.printer = printer;
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
        printer.println(format, args);
        return null;
    }

    @Override
    public Printer print(String s) {
        printer.println(s);
        return this;
    }

    @Override
    public Printer print(String format, Object... args) {
        printer.print(format, args);
        return this;
    }

    @Override
    public Printer write(byte value) {
        return printer.write(value);
    }

    public Printer printCsv(boolean brindNull, Object... args) {
        String data = Stream.of(args).map(v -> Optional.ofNullable(v).orElse("<nil>").toString())
                .map(escapeDoubleQuote).map(escapeCR).map(escapeLF).map(escapeHTAB).map(CsvPrinter::quote)
                .collect(Collectors.joining(","));
        if (brindNull) {
            data = data.replaceAll("\"<nil>\"", "");
        }
        printer.println(data);
        return this;
    }

    private static String quote(String value) {
        return SINGLE_DOUBLE_QUOTE + value + SINGLE_DOUBLE_QUOTE;
    }

    private Function<String, Function<String, Function<String, String>>> escapeString = from -> to -> value -> escape(
            from, to, value);
    private Function<String, String> escapeDoubleQuote = escapeString.apply(SINGLE_DOUBLE_QUOTE)
            .apply(DOUBLE_DOUBLE_QUOTE);
    private Function<String, String> escapeCR = escapeString.apply("\r").apply("\\r");
    private Function<String, String> escapeLF = escapeString.apply("\n").apply("\\n");
    private Function<String, String> escapeHTAB = escapeString.apply("\t").apply("\\t");

    private static String escape(String from, String to, String value) {
        return value.replace(from, to);
    }

    @Override
    public void close() {
        try {
            printer.close();
        } catch (Exception e) {
            System.err.println("failed to close.");
            e.printStackTrace();
        }
    }

}
