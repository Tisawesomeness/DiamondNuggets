package com.tisawesomeness.diamondnuggets;

import com.google.gson.*;
import org.bukkit.Material;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public class PackCreator {

    private final Path dataPath;
    private final String pluginVersion;
    private final DiamondNuggetsConfig config;

    public PackCreator(DiamondNuggets plugin) {
        dataPath = plugin.getDataFolder().toPath();
        pluginVersion = plugin.getDescription().getVersion();
        config = plugin.config;
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

            String storedCustomModelData = packProp.getProperty("custom-model-data");
            if (!String.valueOf(config.customModelData).equals(storedCustomModelData)) {
                Files.delete(packPath);
                return true;
            }

            String storedPackFormat = packProp.getProperty("pack-format");
            if (!String.valueOf(getPackFormat()).equals(storedPackFormat)) {
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
            IO.deleteDirectoryRecursive(packFolderPath);
        }
        Path minecraftFolderPath = IO.resolve(packFolderPath, "assets", "minecraft");
        Files.createDirectories(minecraftFolderPath);

        if (config.shouldUseCustomModelData()) {
            createCustomModelPack(minecraftFolderPath, itemMaterial);
        } else {
            createCITPack(minecraftFolderPath, itemMaterial);
        }

        IO.copyFromResources("diamond_nugget.png", packFolderPath.resolve("pack.png"));

        String materialName = itemMaterial.getKey().getKey();
        int packFormat = getPackFormat();

        String packMeta = IO.readFromResources("pack.mcmeta");
        JsonObject packMetaJson = new JsonParser().parse(packMeta).getAsJsonObject();
        JsonObject packMetaJsonPack = packMetaJson.getAsJsonObject("pack");
        packMetaJsonPack.addProperty("pack_format", packFormat);
        if (!config.shouldUseCustomModelData()) {
            String description = packMetaJsonPack.get("description").getAsString();
            String newDescription = description + "\n§4Optifine/CIT Resewn required";
            packMetaJsonPack.addProperty("description", newDescription);
        }
        String packMetaStr = new GsonBuilder().setPrettyPrinting().create().toJson(packMetaJson);
        Files.write(packFolderPath.resolve("pack.mcmeta"), packMetaStr.getBytes());

        IO.zip(packFolderPath, getZipPath(dataPath, itemName));

        Properties packProp = new Properties();
        packProp.setProperty("item-name", itemName);
        packProp.setProperty("item-material", materialName);
        packProp.setProperty("plugin-version", pluginVersion);
        packProp.setProperty("custom-model-data", String.valueOf(config.customModelData));
        packProp.setProperty("pack-format", String.valueOf(packFormat));
        try (OutputStream os = Files.newOutputStream(packDataPath)) {
            packProp.store(os, "Do not modify");
        }
    }

    private void createCITPack(Path minecraftFolderPath, Material itemMaterial) throws IOException {
        Path textureFolderPath = IO.resolve(minecraftFolderPath, "optifine", "cit", "tis");
        Files.createDirectories(textureFolderPath);

        Properties textureProp = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("cit/diamond_nugget.properties")) {
            if (is == null) {
                throw new IllegalStateException("diamond_nugget.properties not found in resources!");
            }
            textureProp.load(is);
        }
        String materialName = itemMaterial.getKey().getKey();
        textureProp.setProperty("items", materialName);
        Path propPath = textureFolderPath.resolve("diamond_nugget.properties");
        try (OutputStream os = Files.newOutputStream(propPath)) {
            textureProp.store(os, null);
        }

        IO.copyFromResources("diamond_nugget.png", textureFolderPath.resolve("diamond_nugget.png"));
    }

    private void createCustomModelPack(Path minecraftFolderPath, Material itemMaterial) throws IOException {
        Path textureFolderPath = IO.resolve(minecraftFolderPath, "textures", "item");
        Files.createDirectories(textureFolderPath);

        IO.copyFromResources("diamond_nugget.png", textureFolderPath.resolve("diamond_nugget.png"));

        Path modelFolderPath = IO.resolve(minecraftFolderPath, "models", "item");
        Files.createDirectories(modelFolderPath);

        IO.copyFromResources("model/diamond_nugget.json", modelFolderPath.resolve("diamond_nugget.json"));

        String itemJsonStr = IO.readFromResources("model/item.json");
        JsonObject itemJson = new JsonParser().parse(itemJsonStr).getAsJsonObject();
        String materialName = itemMaterial.getKey().getKey().toLowerCase(Locale.ROOT);
        String layer0 = "item/" + materialName;
        itemJson.getAsJsonObject("textures").addProperty("layer0", layer0);

        itemJson.getAsJsonArray("overrides")
                .get(0).getAsJsonObject()
                .getAsJsonObject("predicate").addProperty("custom_model_data", config.customModelData);

        String itemJsonStrOutput = new GsonBuilder().setPrettyPrinting().create().toJson(itemJson);
        Files.write(modelFolderPath.resolve(materialName + ".json"), itemJsonStrOutput.getBytes());
    }

    private int getPackFormat() {
        if (config.shouldUseServerPackFormat()) {
            return SpigotVersion.SERVER_VERSION.packFormat;
        }
        return config.packFormat;
    }

    private static Path getZipPath(Path dataPath, String itemName) {
        return dataPath.resolve(IO.sanitizeFilename(itemName, "pack") + ".zip");
    }

}
