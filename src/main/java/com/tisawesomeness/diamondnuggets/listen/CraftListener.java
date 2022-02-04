package com.tisawesomeness.diamondnuggets.listen;

import com.tisawesomeness.diamondnuggets.DiamondNuggets;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingInventory;

public class CraftListener implements Listener {

    private final DiamondNuggets plugin;
    public CraftListener(DiamondNuggets plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        HumanEntity he = e.getView().getPlayer();
        if (he instanceof Player player) {
            if (isInvalidNuggetCraft(e.getRecipe(), e.getInventory().getMatrix())) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }
    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (isInvalidNuggetCraft(e.getRecipe(), e.getInventory().getMatrix())) {
            e.setCancelled(true);
        }
    }

    private boolean isInvalidNuggetCraft(Recipe recipe, ItemStack[] matrix) {
        if (recipe == null) {
            return false;
        }
        if (recipe instanceof Keyed keyed) {
            if (keyed.getKey().equals(plugin.toDiamondKey)) {
                return false;
            }
        }
        for (ItemStack item : matrix) {
            if (item == null) {
                continue;
            }
            if (item.isSimilar(plugin.nugget)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPrepareItemSmith(PrepareSmithingEvent e) {
        HumanEntity he = e.getView().getPlayer();
        if (he instanceof Player player) {
            if (isInvalidNuggetSmith(e.getInventory())) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }
    @EventHandler
    public void onSmith(SmithItemEvent e) {
        if (isInvalidNuggetSmith(e.getInventory())) {
            e.setCancelled(true);
        }
    }

    private boolean isInvalidNuggetSmith(SmithingInventory inv) {
        ItemStack i1 = inv.getItem(0);
        if (i1 != null && i1.isSimilar(plugin.nugget)) {
            return true;
        }
        ItemStack i2 = inv.getItem(1);
        return i2 != null && i2.isSimilar(plugin.nugget);
    }

}
