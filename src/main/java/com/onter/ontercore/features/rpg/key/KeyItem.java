package com.onter.ontercore.features.rpg.key;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class KeyItem extends Item {
    private final KeyTier tier;

    public KeyItem(KeyTier tier, Properties properties) {
        super(properties.rarity(tier.getRarity()));
        this.tier = tier;
    }

    public KeyTier getTier() {
        return tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
