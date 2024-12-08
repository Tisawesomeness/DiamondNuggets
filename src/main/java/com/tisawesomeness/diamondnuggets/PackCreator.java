package com.tisawesomeness.diamondnuggets;

import com.google.gson.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

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

            String storedPluginVersion = packProp.getProperty("plugin-version");
            if (!pluginVersion.equals(storedPluginVersion)) {
                Files.delete(packPath);
                return true;
            }

            String storedItemMaterial = packProp.getProperty("item-material");
            if (storedItemMaterial == null || Material.matchMaterial(storedItemMaterial) != itemMaterial) {
                Files.delete(packPath);
                return true;
            }

            if (!config.shouldUseCustomModelData()) {
                String storedItemEnchants = packProp.getProperty("item-enchants");
                if (!buildEnchantsString().equals(storedItemEnchants)) {
                    Files.delete(packPath);
                    return true;
                }
            }

            String storedCustomModelData = packProp.getProperty("custom-model-data");
            if (!config.customModelData.toString().equals(storedCustomModelData)) {
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
        Path assetsFolderPath = IO.resolve(packFolderPath, "assets");
        Files.createDirectories(assetsFolderPath);

        if (config.shouldUseCustomModelData()) {
            CustomModelData customModelData = config.customModelData;
            if (SpigotVersion.SERVER_VERSION.supportsCustomModelDataComponent()) {
                createCustomModelComponentPack(assetsFolderPath, itemMaterial, customModelData);
            } else {
                int data = ((CustomModelData.Int) customModelData).data();
                createCustomModelPack(assetsFolderPath, itemMaterial, data);
            }
        } else {
            createCITPack(assetsFolderPath, itemMaterial);
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
        packProp.setProperty("plugin-version", pluginVersion);
        packProp.setProperty("item-name", itemName);
        packProp.setProperty("item-material", materialName);
        packProp.setProperty("item-enchants", buildEnchantsString());
        packProp.setProperty("custom-model-data", config.customModelData.toString());
        packProp.setProperty("pack-format", String.valueOf(packFormat));
        try (OutputStream os = Files.newOutputStream(packDataPath)) {
            packProp.store(os, "Do not modify");
        }
    }

    private void createCITPack(Path assetsFolderPath, Material itemMaterial) throws IOException {
        Path textureFolderPath = IO.resolve(assetsFolderPath, "minecraft", "optifine", "cit", "tis");
        Files.createDirectories(textureFolderPath);

        Properties textureProp = IO.readPropertiesFromResources("cit/diamond_nugget.properties");
        String materialName = itemMaterial.getKey().getKey();
        textureProp.setProperty("items", materialName);
        textureProp.setProperty("enchantments", buildEnchantsString());
        Path propPath = textureFolderPath.resolve("diamond_nugget.properties");
        try (OutputStream os = Files.newOutputStream(propPath)) {
            textureProp.store(os, null);
        }

        IO.copyFromResources("diamond_nugget.png", textureFolderPath.resolve("diamond_nugget.png"));
    }

    private void createCustomModelPack(Path assetsFolderPath, Material itemMaterial, int customModelData) throws IOException {
        Path minecraftFolderPath = IO.resolve(assetsFolderPath, "minecraft");
        Path textureFolderPath = IO.resolve(minecraftFolderPath, "textures", "item");
        Files.createDirectories(textureFolderPath);

        IO.copyFromResources("diamond_nugget.png", textureFolderPath.resolve("diamond_nugget.png"));

        Path modelFolderPath = IO.resolve(minecraftFolderPath, "models", "item");
        Files.createDirectories(modelFolderPath);

        IO.copyFromResources("model/diamond_nugget.json", modelFolderPath.resolve("diamond_nugget.json"));

        String materialName = itemMaterial.getKey().getKey().toLowerCase(Locale.ROOT);
        JsonObject itemJson = generateModelJson(materialName, customModelData);
        String itemJsonStrOutput = new GsonBuilder().setPrettyPrinting().create().toJson(itemJson);
        Files.write(modelFolderPath.resolve(materialName + ".json"), itemJsonStrOutput.getBytes());
    }
    private JsonObject generateModelJson(String materialName, int customModelData) throws IOException {
        String itemJsonStr = IO.readFromResources("model/item.json");
        JsonObject itemJson = new JsonParser().parse(itemJsonStr).getAsJsonObject();
        String layer0 = "item/" + materialName;
        itemJson.getAsJsonObject("textures").addProperty("layer0", layer0);

        itemJson.getAsJsonArray("overrides")
                .get(0).getAsJsonObject()
                .getAsJsonObject("predicate").addProperty("custom_model_data", customModelData);
        return itemJson;
    }

    private void createCustomModelComponentPack(Path assetsFolderPath, Material itemMaterial, CustomModelData customModelData) throws IOException {
        Path itemsFolderPath = IO.resolve(assetsFolderPath, "minecraft", "items");
        Files.createDirectories(itemsFolderPath);

        String materialName = itemMaterial.getKey().getKey().toLowerCase(Locale.ROOT);
        JsonObject itemJson = generateItemJson(materialName, customModelData);
        String itemJsonStrOutput = new GsonBuilder().setPrettyPrinting().create().toJson(itemJson);
        Files.write(itemsFolderPath.resolve(materialName + ".json"), itemJsonStrOutput.getBytes());

        Path tisFolderPath = IO.resolve(assetsFolderPath, "tis");
        Path modelsItemFolderPath = IO.resolve(tisFolderPath, "models", "item");
        Files.createDirectories(modelsItemFolderPath);

        IO.copyFromResources("model_component/diamond_nugget.json", modelsItemFolderPath.resolve("diamond_nugget.json"));

        Path texturesItemFolderPath = IO.resolve(tisFolderPath, "textures", "item");
        Files.createDirectories(texturesItemFolderPath);

        IO.copyFromResources("diamond_nugget.png", texturesItemFolderPath.resolve("diamond_nugget.png"));
    }
    private JsonObject generateItemJson(String materialName, CustomModelData customModelData) throws IOException {
        JsonObject itemJson;
        if (customModelData instanceof CustomModelData.Int) {
            int data = ((CustomModelData.Int) customModelData).data();
            String itemJsonStr = IO.readFromResources("model_component/item_int.json");
            itemJson = new JsonParser().parse(itemJsonStr).getAsJsonObject();
            itemJson.getAsJsonObject("model")
                    .getAsJsonArray("entries")
                    .get(0).getAsJsonObject().addProperty("threshold", data);
        } else {
            String data = ((CustomModelData.Str) customModelData).data();
            String itemJsonStr = IO.readFromResources("model_component/item_str.json");
            itemJson = new JsonParser().parse(itemJsonStr).getAsJsonObject();
            itemJson.getAsJsonObject("model")
                    .getAsJsonArray("cases")
                    .get(0).getAsJsonObject().addProperty("when", data);
        }

        String model = "item/" + materialName;
        itemJson.getAsJsonObject("model")
                .getAsJsonObject("fallback").addProperty("model", model);
        return itemJson;
    }

    private int getPackFormat() {
        if (config.shouldUseServerPackFormat()) {
            return SpigotVersion.SERVER_VERSION.packFormat;
        }
        return config.packFormat;
    }

    private String buildEnchantsString() {
        return config.itemEnchants.stream()
                .map(EnchantmentLevel::enchantment)
                .map(Enchantment::getKey)
                .map(NamespacedKey::getKey)
                .collect(Collectors.joining(" "));
    }

    private static Path getZipPath(Path dataPath, String itemName) {
        String fileName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', itemName));
        return dataPath.resolve(IO.sanitizeFilename(fileName, "pack") + ".zip");
    }

}
