package com.tisawesomeness.diamondnuggets.listen;

import com.tisawesomeness.diamondnuggets.DiamondNuggets;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewingStandFuelEvent;

public class BrewListener implements Listener {

    private final DiamondNuggets plugin;
    public BrewListener(DiamondNuggets plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBrewFuel(BrewingStandFuelEvent e) {
        if (e.getFuel().isSimilar(plugin.nugget)) {
            e.setCancelled(true);
        }
    }

}
