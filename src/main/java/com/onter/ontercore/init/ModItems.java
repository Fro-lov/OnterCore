package com.onter.ontercore.init;

import com.onter.ontercore.ontercore;
import com.onter.ontercore.features.rpg.key.KeyItem;
import com.onter.ontercore.features.rpg.key.KeyTier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredItem<Item> COMMON_KEY = ontercore.ITEMS.register("common_key",
        () -> new KeyItem(KeyTier.COMMON, new Item.Properties()));

    public static final DeferredItem<Item> UNCOMMON_KEY = ontercore.ITEMS.register("uncommon_key",
        () -> new KeyItem(KeyTier.UNCOMMON, new Item.Properties()));

    public static final DeferredItem<Item> RARE_KEY = ontercore.ITEMS.register("rare_key",
        () -> new KeyItem(KeyTier.RARE, new Item.Properties()));

    public static final DeferredItem<Item> EPIC_KEY = ontercore.ITEMS.register("epic_key",
        () -> new KeyItem(KeyTier.EPIC, new Item.Properties()));

    public static final DeferredItem<Item> LEGENDARY_KEY = ontercore.ITEMS.register("legendary_key",
        () -> new KeyItem(KeyTier.LEGENDARY, new Item.Properties()));
}
