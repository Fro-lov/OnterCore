package com.onter.ontercore.features.rpg.affix;

import net.minecraft.world.entity.player.Player;

public class AffixContext {
    private final Player player;
    private final float baseValue;

    public AffixContext(Player player, float baseValue) {
        this.player = player;
        this.baseValue = baseValue;
    }

    public Player getPlayer() {
        return player;
    }

    public float getBaseValue() {
        return baseValue;
    }
}
