package com.tisawesomeness.diamondnuggets.listen;

import com.tisawesomeness.diamondnuggets.DiamondNuggets;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;

public class RenameListener implements Listener {

    private static final EnumSet<InventoryAction> PLACE_ACTIONS = EnumSet.of(
            InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ALL,
            InventoryAction.SWAP_WITH_CURSOR
    );

    private final DiamondNuggets plugin;
    public RenameListener(DiamondNuggets plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (isTryingToRename(e)) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(ChatColor.RED + plugin.getConfig().getString("rename-disabled-message"));
        }
    }
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        ItemStack item = e.getItem().getItemStack();
        if (e.getEntityType() == EntityType.PLAYER &&
                (item.getType() == Material.DIAMOND || item.isSimilar(plugin.nugget))) {
            plugin.unlockRecipes((HumanEntity) e.getEntity());
        }
    }

    private boolean isTryingToRename(InventoryClickEvent e) {
        ItemStack placedItem = null;
        if (e.getInventory().getType() == InventoryType.ANVIL) {
            if (e.getSlotType() == InventoryType.SlotType.CRAFTING) {
                placedItem = getPlacedItem(e);
            } else if (e.getSlotType() == InventoryType.SlotType.RESULT) {
                AnvilInventory ai = (AnvilInventory) e.getInventory();
                placedItem = ai.getItem(0);
            }
            // Shift click requires special case
        } else if (wasShiftedToAnvil(e)) {
            placedItem = e.getCurrentItem();
        }
        return placedItem != null && placedItem.isSimilar(plugin.nugget);
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
