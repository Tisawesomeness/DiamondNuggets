import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class DiamondNuggets extends JavaPlugin {

    private static DiamondNuggets plugin;
    /**
     * @return This plugin instance
     */
    public static DiamondNuggets self() {
        return plugin;
    }

    public final NamespacedKey toNuggetKey = new NamespacedKey(this, "nugget");
    public final NamespacedKey toDiamondKey = new NamespacedKey(this, "diamond");
    public final ItemStack nugget = initNugget();

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        addToNuggetRecipe();
        addToDiamondRecipe();

        // In case plugin is reloaded while server is running, give recipes to all online players
        for (Player p : getServer().getOnlinePlayers()) {
            if (getConfig().getBoolean("unlock-on-join") || shouldUnlockRecipes(p.getInventory())) {
                unlockRecipes(p);
            }
        }

        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
    }

    private ItemStack initNugget() {
        ItemStack nugget = new ItemStack(Material.GOLD_NUGGET);
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
    private void addToDiamondRecipe() {
        ShapedRecipe toDiamondRecipe = new ShapedRecipe(toDiamondKey, new ItemStack(Material.DIAMOND));
        toDiamondRecipe.shape("###", "###", "###");
        toDiamondRecipe.setIngredient('#', new RecipeChoice.ExactChoice(nugget));
        getServer().addRecipe(toDiamondRecipe);
    }
    private void addToNuggetRecipe() {
        ItemStack nuggets = nugget.clone();
        nuggets.setAmount(9);
        ShapelessRecipe toNuggetRecipe = new ShapelessRecipe(toNuggetKey, nuggets);
        toNuggetRecipe.addIngredient(1, Material.DIAMOND);
        getServer().addRecipe(toNuggetRecipe);
    }

    @Override
    public void onDisable() {
        getServer().removeRecipe(toNuggetKey);
        getServer().removeRecipe(toDiamondKey);
    }

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

}
