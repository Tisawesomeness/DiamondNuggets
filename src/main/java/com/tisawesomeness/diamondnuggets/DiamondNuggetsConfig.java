package com.tisawesomeness.diamondnuggets;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.Collections;
import java.util.List;

public class DiamondNuggetsConfig {

    private static final int CUSTOM_MODEL_DATA = 5648554;

    private final DiamondNuggets plugin;

    /** Null if invalid */
    public final String itemName;

    /** Null if invalid */
    public final Material itemMaterial;

    public final boolean itemLore;
    /** Empty if lore disabled or invalid */
    public final List<String> loreLines;

    public final boolean itemEnchanted;
    /** Empty if enchants disabled or invalid */
    public final List<EnchantmentLevel> itemEnchants;

    public final boolean itemFlagged;
    /** Empty if flags disabled or invalid */
    public final List<ItemFlag> itemFlags;

    /** Valid if between 1 and 9, -1 if invalid */
    public final int nuggetsToDiamond;
    /** MISC default */
    public final String recipeBookCategory;

    public final boolean unlockOnJoin;

    public final boolean preventRenames;
    /** May be null */
    public final String renameDisabledMessage;
    public final boolean preventCrafting;
    public final boolean preventPlacement;

    /** -1 to use Optifine / CIT Resewn method instead */
    public final int customModelData;
    /** -1 to use server default */
    public final int packFormat;

    public static void saveBrandNewConfig(DiamondNuggets plugin) {
        plugin.saveDefaultConfig();
        FileConfiguration conf = plugin.getConfig();
        conf.set("custom-model-data", CUSTOM_MODEL_DATA);
        plugin.saveConfig();
    }

    public DiamondNuggetsConfig(DiamondNuggets plugin) {
        this.plugin = plugin;
        FileConfiguration conf = plugin.getConfig();

        itemName = checkItemName(conf.getString("item-name"));

        itemMaterial = getNuggetMaterial(conf.getString("item-material"));

        itemLore = conf.getBoolean("item-lore");
        loreLines = checkLoreLines(itemLore, conf.getStringList("lore-lines"));

        itemEnchanted = conf.getBoolean("item-enchanted");
        itemEnchants = checkEnchants(itemEnchanted, conf.getStringList("item-enchants"));

        itemFlagged = conf.getBoolean("item-flagged");
        itemFlags = checkFlags(itemFlagged, conf.getStringList("item-flags"));

        nuggetsToDiamond = checkNuggetsToDiamond(conf.getInt("nuggets-to-diamond"));
        recipeBookCategory = conf.getString("recipe-book-category", "MISC");

        unlockOnJoin = conf.getBoolean("unlock-on-join", true);

        preventRenames = conf.getBoolean("prevent-renames", true);
        renameDisabledMessage = conf.getString("rename-disabled-message");
        preventCrafting = conf.getBoolean("prevent-crafting");
        preventPlacement = conf.getBoolean("prevent-placement");

        customModelData = conf.getInt("custom-model-data", -1);
        packFormat = conf.getInt("pack-format");
    }

    private String checkItemName(String name) {
        if (name == null) {
            plugin.err("Item name was missing from the config!");
            return null;
        }
        if (name.isEmpty()) {
            plugin.err("Item name cannot be empty!");
            return null;
        }
        if (name.length() > 50) {
            plugin.err("Item name must be 50 characters or less but was " + name.length() + " characters!");
            return null;
        }
        return name;
    }

    private Material getNuggetMaterial(String name) {
        if (name == null) {
            plugin.err("The item material was missing from the config!");
            return null;
        }
        Material nuggetMat = Material.matchMaterial(name);
        if (nuggetMat == null) {
            plugin.err(name + " is not a valid material!");
            return null;
        }
        // Air cannot be crafted
        if (nuggetMat.isAir()) {
            plugin.err("The item material cannot be air!");
            return null;
        }
        if (nuggetMat.getMaxStackSize() < 9) {
            plugin.err("The item material must have a max stack size of 9 or more!");
        }
        return nuggetMat;
    }

    private List<String> checkLoreLines(boolean itemLore, List<String> loreLines) {
        if (!itemLore) {
            return Collections.emptyList();
        }
        if (loreLines == null) {
            plugin.err("Lore lines were missing from the config!");
            return Collections.emptyList();
        }
        if (loreLines.isEmpty()) {
            plugin.err("Lore lines cannot be empty!");
            return Collections.emptyList();
        }
        return loreLines;
    }

    private List<EnchantmentLevel> checkEnchants(boolean itemEnchanted, List<String> itemEnchants) {
        if (!itemEnchanted) {
            return Collections.emptyList();
        }
        if (itemEnchants == null) {
            plugin.err("Item enchants were missing from the config!");
            return Collections.emptyList();
        }
        List<EnchantmentLevel> enchants = itemEnchants.stream()
                .map(this::parseEnchantString)
                .toList();
        if (enchants.contains(null)) {
            return Collections.emptyList();
        }
        return enchants;
    }
    public EnchantmentLevel parseEnchantString(String str) {
        String[] parts = str.split(":");
        if (parts.length != 2) {
            plugin.err(String.format("Enchantment \"%s\" is not in the format \"enchantment:level\"", str));
            return null;
        }
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(parts[0]));
        if (enchantment == null) {
            plugin.err(String.format("Enchantment \"%s\" is not a valid enchantment", parts[0]));
            return null;
        }
        int level;
        try {
            level = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            plugin.err(String.format("Enchantment \"%s\" has an invalid level \"%s\"", parts[0], parts[1]));
            return null;
        }
        if (level < 1) {
            plugin.err(String.format("Enchantment \"%s\" has an invalid level \"%s\"", parts[0], parts[1]));
            return null;
        }
        return new EnchantmentLevel(enchantment, level);
    }

    private List<ItemFlag> checkFlags(boolean itemFlagged, List<String> itemFlags) {
        if (!itemFlagged) {
            return Collections.emptyList();
        }
        if (itemFlags == null) {
            plugin.err("Item flags were missing from the config!");
            return Collections.emptyList();
        }
        List<ItemFlag> flags = itemFlags.stream()
                .map(this::parseItemFlag)
                .toList();
        if (flags.contains(null)) {
            return Collections.emptyList();
        }
        return flags;
    }
    private ItemFlag parseItemFlag(String str) {
        try {
            return ItemFlag.valueOf(str);
        } catch (IllegalArgumentException e) {
            plugin.err(String.format("\"%s\" is not a valid item flag", str));
            return null;
        }
    }

    private int checkNuggetsToDiamond(int n) {
        if (n == 0) {
            plugin.err("Amount of nuggets to craft a diamond was missing from config!");
            return -1;
        }
        if (n < 1 || 9 < n) {
            plugin.err("Amount of nuggets to craft a diamond must be between 1-9 but was " + n + "!");
            return -1;
        }
        return n;
    }

    /** Whether the plugin can run with this config */
    public boolean isValid() {
        return itemName != null && itemMaterial != null;
    }
    public boolean isNuggetsToDiamondValid() {
        return 1 <= nuggetsToDiamond && nuggetsToDiamond <= 9;
    }
    public boolean shouldUseCustomModelData() {
        return customModelData != -1;
    }
    public boolean shouldUseServerPackFormat() {
        return packFormat == -1;
    }

}
