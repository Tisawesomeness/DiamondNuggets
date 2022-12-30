package com.tisawesomeness.diamondnuggets;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Nugget {

    // Config must be valid
    public static ItemStack createNugget(DiamondNuggetsConfig config) {
        ItemStack nugget = new ItemStack(config.itemMaterial);
        ItemMeta meta = nugget.getItemMeta();
        assert meta != null;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.itemName));

        if (config.itemLore) {
            List<String> lore = config.loreLines.stream()
                    .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                    .toList();
            meta.setLore(lore);
        }

        for (EnchantmentLevel enchant : config.itemEnchants) {
            meta.addEnchant(enchant.enchantment(), enchant.level(), true);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true); // unbreakable flag prevents cheating with enchants

        if (config.shouldUseCustomModelData()) {
            meta.setCustomModelData(config.customModelData);
        }

        nugget.setItemMeta(meta);
        return nugget;
    }

}
