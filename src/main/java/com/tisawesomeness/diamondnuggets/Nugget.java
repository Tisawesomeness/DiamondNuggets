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

        for (ItemFlag flag : config.itemFlags) {
            meta.addItemFlags(flag);
        }
        meta.setUnbreakable(true); // unbreakable flag prevents cheating by replicating the item in survival

        if (config.shouldUseCustomModelData()) {
            config.customModelData.setFor(meta);
        }

        nugget.setItemMeta(meta);
        return nugget;
    }

}
