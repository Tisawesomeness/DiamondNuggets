package com.tisawesomeness.diamondnuggets;

import com.google.gson.*;
import org.bukkit.Material;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackCreator {

    private static final Pattern ILLEGAL_FILE_CHARS = Pattern.compile("[^a-zA-Z0-9-_]");
    private static final Pattern ILLEGAL_FILE_NAMES = Pattern.compile(
            "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$", Pattern.CASE_INSENSITIVE);

    private final Path dataPath;
    private final String pluginVersion;
    public PackCreator(Path dataPath, String pluginVersion) {
        this.dataPath = dataPath;
        this.pluginVersion = pluginVersion;
    }

    public boolean createPackIfNeeded(String itemName, Material itemMaterial) throws IOException {
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }
        // This file determines whether to regenerate the pack if the item name or material has changed
        Path packDataPath = dataPath.resolve("generated_pack.dat");

        if (shouldCreatePack(packDataPath, itemName, itemMaterial)) {
            createPack(packDataPath, itemName, itemMaterial);
            return true;
        }
        return false;
    }
    private boolean shouldCreatePack(Path packDataPath, String itemName, Material itemMaterial) throws IOException {
        // File doesn't exist on first run
        if (!packDataPath.toFile().exists()) {
            return true;
        }

        try (InputStream is = Files.newInputStream(packDataPath)) {
            Properties packProp = new Properties();
            packProp.load(is);

            String storedItemName = packProp.getProperty("item-name");
            if (storedItemName == null) {
                return true;
            }
            Path packPath = getZipPath(dataPath, storedItemName);
            if (!itemName.equals(storedItemName)) {
                Files.delete(packPath);
                return true;
            }

            String storedItemMaterial = packProp.getProperty("item-material");
            if (storedItemMaterial == null || Material.matchMaterial(storedItemMaterial) != itemMaterial) {
                Files.delete(packPath);
                return true;
            }

            String storedPluginVersion = packProp.getProperty("plugin-version");
            if (!pluginVersion.equals(storedPluginVersion)) {
                Files.delete(packPath);
                return true;
            }

            String storedPackFormat = packProp.getProperty("pack-format");
            if (!String.valueOf(SpigotVersion.SERVER_VERSION.packFormat).equals(storedPackFormat)) {
                Files.delete(packPath);
                return true;
            }

            return !packPath.toFile().exists();

        } catch (NoSuchFileException ignore) {
            return true; // If the old pack isn't found, we still need to create the new one
        }
    }

    // Assumes itemName is 1-50 chars
    private void createPack(Path packDataPath, String itemName, Material itemMaterial) throws IOException {
        // Using temp folder to create pack directory structure before zipping
        Path packFolderPath = dataPath.resolve("temp");
        if (Files.exists(packFolderPath)) {
            deleteDirectoryRecursive(packFolderPath);
        }
        Path textureFolderPath = resolve(packFolderPath, "assets", "minecraft", "optifine", "cit", "tis");
        Files.createDirectories(textureFolderPath);

        String materialName = itemMaterial.getKey().getKey();

        Properties textureProp = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("diamond_nugget.properties")) {
            if (is == null) {
                throw new IllegalStateException("diamond_nugget.properties not found in resources!");
            }
            textureProp.load(is);
        }
        textureProp.setProperty("items", materialName);
        Path propPath = textureFolderPath.resolve("diamond_nugget.properties");
        try (OutputStream os = Files.newOutputStream(propPath)) {
            textureProp.store(os, null);
        }

        copyFromResources("diamond_nugget.png", textureFolderPath.resolve("diamond_nugget.png"));
        copyFromResources("diamond_nugget.png", packFolderPath.resolve("pack.png"));

        String packMeta = readFromResources("pack.mcmeta");
        JsonObject packMetaJson = new JsonParser().parse(packMeta).getAsJsonObject();
        int packFormat = SpigotVersion.SERVER_VERSION.packFormat;
        packMetaJson.getAsJsonObject("pack").addProperty("pack_format", packFormat);
        String packMetaStr = new GsonBuilder().setPrettyPrinting().create().toJson(packMetaJson);
        Files.write(packFolderPath.resolve("pack.mcmeta"), packMetaStr.getBytes());

        zip(packFolderPath, getZipPath(dataPath, itemName));

        Properties packProp = new Properties();
        packProp.setProperty("item-name", itemName);
        packProp.setProperty("item-material", materialName);
        packProp.setProperty("plugin-version", pluginVersion);
        packProp.setProperty("pack-format", String.valueOf(packFormat));
        try (OutputStream os = Files.newOutputStream(packDataPath)) {
            packProp.store(os, "Do not modify");
        }
    }

    private static Path resolve(Path path, String... directoryNames) {
        Path temp = path;
        for (String directory : directoryNames) {
            temp = temp.resolve(directory);
        }
        return temp;
    }
    // https://softwarecave.org/2018/03/24/delete-directory-with-contents-in-java/
    private static void deleteDirectoryRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursive(entry);
                }
            }
        }
        Files.delete(path);
    }
    private static void copyFromResources(String resource, Path targetPath) throws IOException {
        try (InputStream is = PackCreator.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException(resource + " not found in resources!");
            }
            Files.copy(is, targetPath);
        }
    }
    private static String readFromResources(String resource) throws IOException {
        try (InputStream is = PackCreator.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException(resource + " not found in resources!");
            }
            return new String(is.readAllBytes());
        }
    }

    private Path getZipPath(Path dataPath, String itemName) {
        return dataPath.resolve(sanitizeFilename(itemName, "pack") + ".zip");
    }
    private String sanitizeFilename(String name, String alternate) {
        String sanitized = ILLEGAL_FILE_CHARS.matcher(name).replaceAll("");
        // Use alternate filename in case sanitize deletes all characters or creates a windows reserved file
        if (sanitized.isEmpty() || ILLEGAL_FILE_NAMES.matcher(sanitized).matches()) {
            return alternate;
        }
        return sanitized;
    }
    // https://stackoverflow.com/a/32052016
    private void zip(Path sourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
        }
        Files.createFile(targetPath);
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(targetPath))) {
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> addZipEntry(sourcePath, zs, path));
        }
    }
    private void addZipEntry(Path sourcePath, ZipOutputStream zs, Path path) {
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
