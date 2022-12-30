package com.tisawesomeness.diamondnuggets;

import com.tisawesomeness.diamondnuggets.listen.*;

import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DiamondNuggets extends JavaPlugin {

    private static final String[][] SHAPES = {
            {"#"},
            {"##"},
            {"# ", "##"},
            {"##", "##"},
            {" # ", "###", " # "},
            {"###", "###"},
            {" # ", "###", "###"},
            {"###", "# #", "###"},
            {"###", "###", "###"}
    };

    public final NamespacedKey toNuggetKey = new NamespacedKey(this, "nugget");
    public final NamespacedKey toDiamondKey = new NamespacedKey(this, "diamond");
    public ItemStack nugget = null;
    public DiamondNuggetsConfig config = null;

    @Override
    public void onEnable() {
        File configFile = new File(getDataFolder(), "config.yml");
        boolean exists = configFile.exists();
        if (exists) {
            getConfig().options().copyDefaults(true);
            try {
                ConfigUpdater.update(this, "config.yml", configFile);
            } catch (IOException e) {
                err(e);
            }
            reloadConfig();
        } else {
            DiamondNuggetsConfig.saveBrandNewConfig(this);
        }

        config = new DiamondNuggetsConfig(this);
        if (!config.isValid()) {
            return;
        }

        nugget = Nugget.createNugget(config);

        if (!config.itemEnchanted && !config.shouldUseCustomModelData()) {
            // Can't generate a pack using the CIT method if the item has no enchants to separate it from other items
            err("Cannot generate CIT resource pack if item has no enchants.\n" +
                    "Please set `itemEnchanted` to true or use `customModelData`.");
        } else {
            try {
                PackCreator packCreator = new PackCreator(this);
                if (packCreator.createPackIfNeeded(config.itemName, config.itemMaterial)) {
                    log("Resource pack created");
                } else {
                    log("Resource pack already exists");
                }
            } catch (IOException e) {
                err(e);
            }
        }

        // Don't add recipe if amount of nuggets is invalid
        if (config.isNuggetsToDiamondValid()) {
            addToNuggetRecipe(config.nuggetsToDiamond);
            addToDiamondRecipe(config.nuggetsToDiamond);

            // In case plugin is reloaded while server is running, give recipes to all online players
            for (Player p : getServer().getOnlinePlayers()) {
                if (config.unlockOnJoin || shouldUnlockRecipes(p.getInventory())) {
                    unlockRecipes(p);
                }
            }
        }

        // Prevent renaming even if recipes are temporarily invalid
        PluginManager man = getServer().getPluginManager();
        man.registerEvents(new UseListener(this), this);
        man.registerEvents(new UnlockListener(this), this);

        if (config.preventCrafting) {
            man.registerEvents(new CraftListener(this), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.STONECUTTER, 0), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.CARTOGRAPHY, 0, 1), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.LOOM, 0, 1, 2), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.BREWING, 3), this);
            man.registerEvents(new InventoryDenyListener(this, InventoryType.MERCHANT, 0, 1), this);
            man.registerEvents(new BrewListener(this), this);
        }
        if (config.preventRenames) {
            man.registerEvents(new InventoryDenyListener(this, InventoryType.ANVIL, true, 0, 1), this);
        }
        man.registerEvents(new InventoryDenyListener(this, InventoryType.GRINDSTONE, 0, 1), this);
    }

    // Known bug: recipe book doesn't autofill custom items
    // ingredientCount assumed 1-9
    private void addToDiamondRecipe(int ingredientCount) {
        // Shapeless recipe doesn't work with nbt :(
        ShapedRecipe toDiamondRecipe = new ShapedRecipe(toDiamondKey, new ItemStack(Material.DIAMOND));
        toDiamondRecipe.shape(getToDiamondRecipeShape(ingredientCount));
        toDiamondRecipe.setIngredient('#', new RecipeChoice.ExactChoice(nugget));
        setRecipeBookCategory(toDiamondRecipe);
        getServer().addRecipe(toDiamondRecipe);
    }
    private static String[] getToDiamondRecipeShape(int ingredientCount) {
        return SHAPES[ingredientCount - 1];
    }

    // ingredientCount assumed 1-64
    private void addToNuggetRecipe(int ingredientCount) {
        ItemStack nuggets = nugget.clone();
        nuggets.setAmount(ingredientCount);
        ShapelessRecipe toNuggetRecipe = new ShapelessRecipe(toNuggetKey, nuggets);
        toNuggetRecipe.addIngredient(1, Material.DIAMOND);
        setRecipeBookCategory(toNuggetRecipe);
        getServer().addRecipe(toNuggetRecipe);
    }

    // Either shaped or shapeless recipe
    private void setRecipeBookCategory(Recipe recipe) {
        if (!SpigotVersion.SERVER_VERSION.supportsRecipeBookCategory()) {
            return;
        }
        try {
            Class<?> categoryClass = Class.forName("org.bukkit.inventory.recipe.CraftingBookCategory");
            Object category = findRecipeBookCategory(categoryClass);
            if (category == null) {
                err("Could not find recipe book category " + config.recipeBookCategory);
                return;
            }
            recipe.getClass().getMethod("setCategory", categoryClass).invoke(recipe, category);
        } catch (ReflectiveOperationException e) {
            err(e);
        }
    }
    private Object findRecipeBookCategory(Class<?> categoryClass) {
        for (Object obj : categoryClass.getEnumConstants()) {
            if (obj.toString().equalsIgnoreCase(config.recipeBookCategory)) {
                return obj;
            }
        }
        return null;
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
