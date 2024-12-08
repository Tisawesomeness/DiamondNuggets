package com.tisawesomeness.diamondnuggets;

import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public interface CustomModelData {

    void setFor(ItemMeta item);

    record Int(int data) implements CustomModelData {
        @Override
        public void setFor(ItemMeta item) {
            item.setCustomModelData(data);
        }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }

    record Str(String data) implements CustomModelData {
        @Override
        public void setFor(ItemMeta item) {
            try {
                Class<?> customModelDataClass = Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
                // CustomModelDataComponent cmd = item.getCustomModelDataComponent();
                Object cmd = ItemMeta.class
                        .getDeclaredMethod("getCustomModelDataComponent")
                        .invoke(item);
                // cmd.setStrings(List.of(data));
                customModelDataClass
                        .getDeclaredMethod("setStrings", List.class)
                        .invoke(cmd, List.of(data));
                // item.setCustomModelDataComponent(cmd);
                ItemMeta.class
                        .getDeclaredMethod("setCustomModelDataComponent", customModelDataClass)
                        .invoke(item, cmd);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return data;
        }
    }

}
