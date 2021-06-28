package com.tisawesomeness.diamondnuggets;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class DiamondNuggets extends JavaPlugin {

    public final NamespacedKey toNuggetKey = new NamespacedKey(this, "nugget");
    public final NamespacedKey toDiamondKey = new NamespacedKey(this, "diamond");
    public ItemStack nugget = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        nugget = initNugget();
        if (nugget == null) {
            return;
        }

        // Don't add recipe if amount of nuggets is invalid
        int ingredientCount = getConfig().getInt("nuggets-to-diamond");
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
            return;
        }

        // Prevent renaming even if recipes are temporarily invalid
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    private ItemStack initNugget() {
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

    private void err(String msg) {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + msg);
    }

}
