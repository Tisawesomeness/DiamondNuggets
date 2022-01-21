package com.tisawesomeness.diamondnuggets.listen;

import com.tisawesomeness.diamondnuggets.DiamondNuggets;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class UseListener implements Listener {

    private final DiamondNuggets plugin;
    public UseListener(DiamondNuggets plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item != null && item.isSimilar(plugin.nugget)) {
            e.setUseItemInHand(Event.Result.DENY);
        }
    }

}
