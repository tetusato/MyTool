package com.ibm.jp.isol.tetusato;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceListGenerator {
    private Pattern testSourceName = Pattern.compile("[^\\.]+Test\\.java");
    private File sourceMain;
    private File sourceTest;

    public SourceListGenerator(File source) {
        this(source, source);
    }

    public SourceListGenerator(File source, String regex) {
        this(source, source, regex);
    }

    public SourceListGenerator(File sourceMain, File sourceTest) {
        this.sourceMain = sourceMain;
        this.sourceTest = sourceTest;
        if (!sourceMain.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not directory", sourceMain));
        }
        if (!sourceTest.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not directory", sourceTest));
        }
    }

    public SourceListGenerator(File sourceMain, File sourceTest, String regex) {
        this(sourceMain, sourceTest);
        testSourceName = Pattern.compile(regex);
    }

    public static class Pair<T> {
        private T left;
        private T right;
        public Pair(T left) {
            this(left, null);
        }
        public Pair(T left, T right) {
            this.left = left;
            this.right = right;
        }
        public T getLeft() {
            return left;
        }
        public T getRight() {
            return right;
        }
        public void setLeft(T value) {
            left = value;
        }
        public void setRight(T value) {
            right = value;
        }
    }
    
    private BiFunction<Path, String, String> removeFileExtension = (p, removeString) -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - removeString.length()); 
    
    public void listAll() throws IOException {
        Map<Path, List<Path>> mainSources = listSourceMain();
        Map<Path, List<Path>> testSources = listSourceTest();
        Map<Path, List<Pair<Path>>> mergedSources = merge(mainSources, testSources);
        for (Entry<Path, List<Pair<Path>>> entry : mergedSources.entrySet()) {
            System.out.println(entry.getKey().toString().replace('/', '.'));
            for(Pair<Path> fileNames : entry.getValue()) {
                System.out.println(String.format("\t%s,%s" + removeFileExtension.apply(fileNames.getLeft(), ".java"),
                        removeFileExtension.apply(fileNames.getRight(), ".java")));
            }
        }
        
    }
    
    private Map<Path, List<Pair<Path>>> merge(Map<Path, List<Path>> mainSources, Map<Path, List<Path>> testSources) {
        Map<Path, List<Pair<Path>>> map = new LinkedHashMap<>();
        //mainSources.entrySet().stream().collect(collector)
        return null;
    }
    
    Predicate<Path> isTest = p -> testSourceName.matcher(p.getFileName().toString()).matches(); 
    
    public Map<Path, List<Path>> listSourceMain() throws IOException {
        return listFiles(sourceMain.toPath(), isTest.negate());
    }

    public Map<Path, List<Path>> listSourceTest() throws IOException {
        return listFiles(sourceTest.toPath(), isTest);
    }

    private Map<Path, List<Path>> listFiles(Path path, Predicate<Path> matcher) throws IOException {
        try (Stream<Path> fstream = Files.find(path, Integer.MAX_VALUE, (p, attr) -> p.toString().endsWith(".java"))) {
            Map<Path, List<Path>> fileNamesPerPackage = fstream.filter(matcher).collect(Collectors.groupingBy(p->path.relativize(p).getParent(), Collectors.mapping(Path::getFileName, Collectors.toList())));//forEach(p -> System.out.println(path.relativize(p).getParent().toString()));
            return fileNamesPerPackage;
        }
    }

    public static void main(String... args) throws IOException {
//        File file = new File("./src/com/ibm/jp/isol/tetusato");
        File fileMain = new File("../FrontAPI/src/main/java");
        File fileTest = new File("../FrontAPI/src/test/java");
        SourceListGenerator generator = new SourceListGenerator(fileMain, fileTest);
        System.out.println(generator.listSourceMain());
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println(generator.listSourceTest());
    }

}
