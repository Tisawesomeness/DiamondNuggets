package com.tisawesomeness.diamondnuggets;

import com.tisawesomeness.diamondnuggets.listen.*;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DiamondNuggets extends JavaPlugin {

    public final NamespacedKey toNuggetKey = new NamespacedKey(this, "nugget");
    public final NamespacedKey toDiamondKey = new NamespacedKey(this, "diamond");
    public ItemStack nugget = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String itemName = getConfig().getString("item-name");
        if (itemName == null || itemName.isEmpty()) {
            err("Item name cannot be empty!");
            return;
        }
        if (itemName.length() > 50) {
            err("Item name must be 50 characters or less but was " + itemName.length() + " characters!");
            return;
        }

        Material nuggetMat = getNuggetMaterial();
        if (nuggetMat == null) {
            return;
        }
        nugget = initNugget(nuggetMat);

        try {
            PackCreator packCreator = new PackCreator(getDataFolder().toPath(), getDescription().getVersion());
            if (packCreator.createPackIfNeeded(itemName, nuggetMat)) {
                log("Resource pack created");
            } else {
                log("Resource pack already exists");
            }
        } catch (IOException e) {
            err(e);
        }

        // Don't add recipe if amount of nuggets is invalid
        // TODO Find workaround for 1.18 breaking shapeless exact recipe choice
//        int ingredientCount = getConfig().getInt("nuggets-to-diamond");
        int ingredientCount = 9;
        if (1 <= ingredientCount && ingredientCount <= 9) {

            addToNuggetRecipe(ingredientCount);
            addToDiamondRecipe(ingredientCount);

            // In case plugin is reloaded while server is running, give recipes to all online players
            for (Player p : getServer().getOnlinePlayers()) {
                if (getConfig().getBoolean("unlock-on-join") || shouldUnlockRecipes(p.getInventory())) {
                    unlockRecipes(p);
                }
            }

        } else {
            err("Amount of nuggets to craft a diamond must be between 1-9 but was " + ingredientCount + "!");
        }

        // Prevent renaming even if recipes are temporarily invalid
        PluginManager man = getServer().getPluginManager();
        man.registerEvents(new UseListener(this), this);
        man.registerEvents(new UnlockListener(this), this);

        if (getConfig().getBoolean("prevent-crafting", true)) {
            man.registerEvents(new CraftListener(this), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.STONECUTTER, 0), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.CARTOGRAPHY, 0, 1), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.LOOM, 0, 1, 2), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.BREWING, 3), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.MERCHANT, 0, 1), this);
            man.registerEvents(new BrewListener(this), this);
        }
        if (getConfig().getBoolean("prevent-renames", true)) {
            man.registerEvents(new InventoryDenyListener(this, InventoryType.ANVIL, true, 0, 1), this);
        }
        man.registerEvents(new InventoryDenyListener(this, InventoryType.GRINDSTONE, 0, 1), this);
    }

    private Material getNuggetMaterial() {
        String nuggetStr = getConfig().getString("item-material");
        if (nuggetStr == null) {
            err("The item material was missing from the config!");
            return null;
        }
        Material nuggetMat = Material.matchMaterial(nuggetStr);
        if (nuggetMat == null) {
            err(nuggetStr + " is not a valid material!");
            return null;
        }
        // Air cannot be crafted
        if (nuggetMat.isAir()) {
            err("The item material cannot be air!");
            return null;
        }
        if (nuggetMat.getMaxStackSize() < 9) {
            err("The item material must have a max stack size of 9 or more!");
        }
        return nuggetMat;
    }
    private ItemStack initNugget(Material nuggetMat) {
        ItemStack nugget = new ItemStack(nuggetMat);
        nugget.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, Enchantment.LOOT_BONUS_BLOCKS.getMaxLevel());
        ItemMeta meta = nugget.getItemMeta();
        assert meta != null;
        meta.setDisplayName(getConfig().getString("item-name"));
        meta.setUnbreakable(true); // unbreakable flag prevents cheating with enchants
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        nugget.setItemMeta(meta);
        return nugget;
    }

    // Known bug: recipe book doesn't autofill custom items
    // ingredientCount assumed 1-9
    private void addToDiamondRecipe(int ingredientCount) {
        getServer().addRecipe(getToDiamondRecipe(ingredientCount));
    }
    private Recipe getToDiamondRecipe(int ingredientCount) {
        if (ingredientCount == 9) {
            ShapedRecipe toDiamondRecipe = new ShapedRecipe(toDiamondKey, new ItemStack(Material.DIAMOND));
            toDiamondRecipe.shape("###", "###", "###");
            toDiamondRecipe.setIngredient('#', new RecipeChoice.ExactChoice(nugget));
            return toDiamondRecipe;
        }
        ShapelessRecipe toDiamondRecipe = new ShapelessRecipe(toDiamondKey, new ItemStack(Material.DIAMOND));
        RecipeChoice choice = new RecipeChoice.ExactChoice(nugget);
        for (int i = 0; i < ingredientCount; i++) {
            toDiamondRecipe.addIngredient(choice);
        }
        return toDiamondRecipe;
    }

    // ingredientCount assumed 1-64
    private void addToNuggetRecipe(int ingredientCount) {
        ItemStack nuggets = nugget.clone();
        nuggets.setAmount(ingredientCount);
        ShapelessRecipe toNuggetRecipe = new ShapelessRecipe(toNuggetKey, nuggets);
        toNuggetRecipe.addIngredient(1, Material.DIAMOND);
        getServer().addRecipe(toNuggetRecipe);
    }

    @Override
    public void onDisable() {}

    /**
     * @param inv The player's inventory
     * @return True if the player has a diamond or diamond nugget and should unlock recipes
     */
    public boolean shouldUnlockRecipes(Inventory inv) {
        return inv.contains(Material.DIAMOND) || inv.containsAtLeast(nugget, 1);
    }
    /**
     * Unlocks the diamond and diamond nugget recipes.
     * @param player The player to unlock recipes for
     */
    public void unlockRecipes(HumanEntity player) {
        player.discoverRecipe(toNuggetKey);
        player.discoverRecipe(toDiamondKey);
    }

    public void log(String msg) {
        getLogger().info(msg);
    }
    public void err(String msg) {
        getLogger().warning(msg);
    }
    public void err(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        for (String line : sw.toString().split("\n")) {
            getLogger().severe(line);
        }
    }

}
