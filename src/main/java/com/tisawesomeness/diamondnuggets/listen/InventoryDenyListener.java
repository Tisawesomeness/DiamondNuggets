package com.tisawesomeness.diamondnuggets.listen;

import com.tisawesomeness.diamondnuggets.DiamondNuggets;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;

public class InventoryDenyListener implements Listener {

    private static final EnumSet<InventoryAction> PLACE_ACTIONS = EnumSet.of(
            InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ALL,
            InventoryAction.SWAP_WITH_CURSOR
    );

    private final DiamondNuggets plugin;
    private final InventoryType type;
    private final boolean sendMessage;
    private final int[] inputSlots;
    public InventoryDenyListener(DiamondNuggets plugin, InventoryType type, int... inputSlots) {
        this(plugin, type, false, inputSlots);
    }
    public InventoryDenyListener(DiamondNuggets plugin, InventoryType type, boolean sendMessage, int... inputSlots) {
        this.plugin = plugin;
        this.type = type;
        this.sendMessage = sendMessage;
        this.inputSlots = inputSlots;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (isTryingToUse(e)) {
            e.setCancelled(true);
            if (sendMessage && plugin.config.renameDisabledMessage != null) {
                e.getWhoClicked().sendMessage(ChatColor.RED + plugin.config.renameDisabledMessage);
            }
        }
    }

    private boolean isTryingToUse(InventoryClickEvent e) {
        if (e.getInventory().getType() != type) {
            return false;
        }

        if (!plugin.config.preventPlacement) {
            return e.getSlotType() == InventoryType.SlotType.RESULT && isTryingToClickResult(e);
        }

        ItemStack placedItem = null;
        if (e.getSlotType() == InventoryType.SlotType.CRAFTING || e.getSlotType() == InventoryType.SlotType.FUEL) {
            placedItem = getPlacedItem(e);
        } else if (e.getSlotType() == InventoryType.SlotType.RESULT) {
            return isTryingToClickResult(e);
            // Shift click requires special case
        } else if (wasShiftedToUpper(e)) {
            placedItem = e.getCurrentItem();
        }

        return placedItem != null && placedItem.isSimilar(plugin.nugget);
    }
    // returns null if no item was placed
    private ItemStack getPlacedItem(InventoryClickEvent e) {
        if (PLACE_ACTIONS.contains(e.getAction())) {
            return e.getCursor();
        }
        if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
            return e.getView().getBottomInventory().getItem(e.getHotbarButton());
        }
        if (wasShiftedToUpper(e)) {
            return e.getCurrentItem();
        }
        return null;
    }
    private boolean isTryingToClickResult(InventoryClickEvent e) {
        ItemStack placedItem;
        for (int slot : inputSlots) {
            placedItem = e.getInventory().getItem(slot);
            if (placedItem != null && placedItem.isSimilar(plugin.nugget)) {
                return true;
            }
        }
        return false;
    }
    private boolean wasShiftedToUpper(InventoryClickEvent e) {
        return e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                e.getClickedInventory() != null &&
                e.getClickedInventory().getType() == InventoryType.PLAYER &&
                anySlotHasSpace(e);
    }
    private boolean anySlotHasSpace(InventoryClickEvent e) {
        for (int slot : inputSlots) {
            ItemStack item = e.getView().getTopInventory().getItem(slot);
            if (item == null || itemHasSpaceForCurrent(e, item)) {
                return true;
            }
        }
        return false;
    }
    private static boolean itemHasSpaceForCurrent(InventoryClickEvent e, ItemStack item) {
        return item.getAmount() < item.getMaxStackSize() &&
                e.getCurrentItem() != null &&
                e.getCurrentItem().isSimilar(item);
    }

}
