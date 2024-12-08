package com.tisawesomeness.diamondnuggets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public final class IO {
    private IO() {}

    private static final Pattern ILLEGAL_FILE_CHARS = Pattern.compile("[^a-zA-Z0-9-_]");
    private static final Pattern ILLEGAL_FILE_NAMES = Pattern.compile(
            "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$", Pattern.CASE_INSENSITIVE);

    public static String readFromResources(String resource) throws IOException {
        try (InputStream is = IO.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException(resource + " not found in resources!");
            }
            return new String(is.readAllBytes());
        }
    }
    public static Properties readPropertiesFromResources(String resource) throws IOException {
        Properties prop = new Properties();
        try (InputStream is = IO.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException(resource + " not found in resources!");
            }
            prop.load(is);
        }
        return prop;
    }
    public static void copyFromResources(String resource, Path targetPath) throws IOException {
        try (InputStream is = IO.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException(resource + " not found in resources!");
            }
            Files.copy(is, targetPath);
        }
    }

    public static Path resolve(Path path, String... directoryNames) {
        Path temp = path;
        for (String directory : directoryNames) {
            temp = temp.resolve(directory);
        }
        return temp;
    }

    // https://softwarecave.org/2018/03/24/delete-directory-with-contents-in-java/
    public static void deleteDirectoryRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursive(entry);
                }
            }
        }
        Files.delete(path);
    }

    public static String sanitizeFilename(String name, String alternate) {
        String sanitized = ILLEGAL_FILE_CHARS.matcher(name).replaceAll("");
        // Use alternate filename in case sanitize deletes all characters or creates a windows reserved file
        if (sanitized.isEmpty() || ILLEGAL_FILE_NAMES.matcher(sanitized).matches()) {
            return alternate;
        }
        return sanitized;
    }

    public static void zip(Path sourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
        }
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try (FileSystem zipfs = FileSystems.newFileSystem(targetPath, env)) {
            copyRecursive(sourcePath, zipfs.getPath("/"));
        }
    }

    // Adapted from https://stackoverflow.com/a/60621544
    private static void copyRecursive(Path sourcePath, Path targetPath) throws IOException {
        Files.walkFileTree(sourcePath, new CopyVisitor(sourcePath, targetPath));
    }

    private static class CopyVisitor extends SimpleFileVisitor<Path> {
        private final Path source;
        private final Path target;
        public CopyVisitor(Path source, Path target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Files.createDirectories(target.resolve(source.relativize(dir).toString()));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, target.resolve(source.relativize(file).toString()));
            return FileVisitResult.CONTINUE;
        }
    }

}
