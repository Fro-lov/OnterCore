package com.onter.ontercore.features.rpg.key;

import net.minecraft.world.item.Rarity;

public enum KeyTier {
    COMMON(Rarity.COMMON, 0xFFFFFF),
    UNCOMMON(Rarity.UNCOMMON, 0xFFFF00),
    RARE(Rarity.RARE, 0x00FFFF),
    EPIC(Rarity.EPIC, 0xFF00FF),
    LEGENDARY(Rarity.EPIC, 0xFFAA00);

    private final Rarity rarity;
    private final int color;

    KeyTier(Rarity rarity, int color) {
        this.rarity = rarity;
        this.color = color;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getColor() {
        return color;
    }
}
