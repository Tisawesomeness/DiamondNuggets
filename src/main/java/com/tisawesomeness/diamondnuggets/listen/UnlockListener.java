package com.tisawesomeness.diamondnuggets.listen;

import com.tisawesomeness.diamondnuggets.DiamondNuggets;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class UnlockListener implements Listener {

    private final DiamondNuggets plugin;
    public UnlockListener(DiamondNuggets plugin) {
        this.plugin = plugin;
    }

    // Events cover all detectable cases where an item is added to a player's inventory
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.config.unlockOnJoin || plugin.shouldUnlockRecipes(e.getPlayer().getInventory())) {
            plugin.unlockRecipes(e.getPlayer());
        }
    }
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            onInventoryChange(p);
        }
    }
    @EventHandler
    public void onInventoryInteract(InventoryInteractEvent e) {
        onInventoryChange(e.getWhoClicked());
    }

    private void onInventoryChange(HumanEntity player) {
        if (plugin.shouldUnlockRecipes(player.getInventory())) {
            plugin.unlockRecipes(player);
        }
    }

}
