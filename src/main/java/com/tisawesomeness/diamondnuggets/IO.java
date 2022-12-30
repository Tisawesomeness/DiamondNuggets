package com.tisawesomeness.diamondnuggets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    // Adapted from https://stackoverflow.com/a/32052016
    public static void zip(Path sourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
        }
        Files.createFile(targetPath);
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(targetPath));
             Stream<Path> paths = Files.walk(sourcePath)) {
            paths.filter(path -> !Files.isDirectory(path))
                    .forEach(path -> addZipEntry(sourcePath, zs, path));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }
    private static void addZipEntry(Path sourcePath, ZipOutputStream zs, Path path) {
        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
        try {
            zs.putNextEntry(zipEntry);
            Files.copy(path, zs);
            zs.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
