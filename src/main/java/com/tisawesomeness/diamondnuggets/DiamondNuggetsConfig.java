package com.tisawesomeness.diamondnuggets;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class DiamondNuggetsConfig {

    private final DiamondNuggets plugin;

    /** Null if invalid */
    public final String itemName;
    /** Null if invalid */
    public final Material itemMaterial;
    /** Valid if between 1 and 9, -1 if invalid */
    public final int nuggetsToDiamond;
    public final boolean unlockOnJoin;
    public final boolean preventRenames;
    /** May be null */
    public final String renameDisabledMessage;
    public final boolean preventCrafting;
    public final boolean preventPlacement;
    /** -1 to use server default */
    public final int packFormat;

    public DiamondNuggetsConfig(DiamondNuggets plugin) {
        this.plugin = plugin;
        FileConfiguration conf = plugin.getConfig();
        itemName = checkItemName(conf.getString("item-name"));
        itemMaterial = getNuggetMaterial(conf.getString("item-material"));
        nuggetsToDiamond = checkNuggetsToDiamond(conf.getInt("nuggets-to-diamond"));
        unlockOnJoin = conf.getBoolean("unlock-on-join", true);
        preventRenames = conf.getBoolean("prevent-renames", true);
        renameDisabledMessage = conf.getString("rename-disabled-message");
        preventCrafting = conf.getBoolean("prevent-crafting");
        preventPlacement = conf.getBoolean("prevent-placement");
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
    public boolean shouldUseServerPackFormat() {
        return packFormat == -1;
    }

}
