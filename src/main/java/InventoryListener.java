import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;

public class InventoryListener implements Listener {

    private static final EnumSet<InventoryAction> PLACE_ACTIONS = EnumSet.of(
            InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ALL,
            InventoryAction.SWAP_WITH_CURSOR
    );
    private static final DiamondNuggets MAIN = DiamondNuggets.self();

    // Events cover all detectable cases where an item is added to a player's inventory
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (MAIN.getConfig().getBoolean("unlock-on-join") ||
                MAIN.shouldUnlockRecipes(e.getPlayer().getInventory())) {
            MAIN.unlockRecipes(e.getPlayer());
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (MAIN.getConfig().getBoolean("prevent-renames") && shouldCancel(e)) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(ChatColor.RED + MAIN.getConfig().getString("rename-disabled-message"));
        } else {
            onInventoryChange(e.getWhoClicked());
        }
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        onInventoryChange(e.getWhoClicked());
    }
    private void onInventoryChange(HumanEntity player) {
        if (MAIN.shouldUnlockRecipes(player.getInventory())) {
            MAIN.unlockRecipes(player);
        }
    }
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        ItemStack item = e.getItem().getItemStack();
        if (e.getEntityType() == EntityType.PLAYER &&
                (item.getType() == Material.DIAMOND || item.isSimilar(MAIN.nugget))) {
            MAIN.unlockRecipes((HumanEntity) e.getEntity());
        }
    }

    private static boolean shouldCancel(InventoryClickEvent e) {
        ItemStack placedItem = null;
        if (e.getInventory().getType() == InventoryType.ANVIL && e.getSlotType() == InventoryType.SlotType.CRAFTING) {
            placedItem = getPlacedItem(e);
        }
        // Shift click requires special case
        if (wasShiftedToAnvil(e)) {
            placedItem = e.getCurrentItem();
        }
        return placedItem != null && placedItem.isSimilar(MAIN.nugget);
    }
    // returns null if no item was placed
    private static ItemStack getPlacedItem(InventoryClickEvent e) {
        if (PLACE_ACTIONS.contains(e.getAction())) {
            return e.getCursor();
        }
        if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
            return e.getView().getBottomInventory().getItem(e.getHotbarButton());
        }
        return null;
    }
    private static boolean wasShiftedToAnvil(InventoryClickEvent e) {
        return e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                e.getClickedInventory() != null &&
                e.getClickedInventory().getType() == InventoryType.PLAYER &&
                e.getView().getTopInventory().getType() == InventoryType.ANVIL &&
                (slotHasSpace(e, 0) || slotHasSpace(e, 1));
    }
    private static boolean slotHasSpace(InventoryClickEvent e, int slot) {
        ItemStack item = e.getView().getTopInventory().getItem(slot);
        return item == null || itemHasSpaceForCurrent(e, item);
    }
    private static boolean itemHasSpaceForCurrent(InventoryClickEvent e, ItemStack item) {
        return item.getAmount() < item.getMaxStackSize() &&
                e.getCurrentItem() != null &&
                e.getCurrentItem().isSimilar(item);
    }

}
