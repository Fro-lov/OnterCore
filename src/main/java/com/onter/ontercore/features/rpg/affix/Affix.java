package com.onter.ontercore.features.rpg.affix;

import java.util.function.BiConsumer;

public class Affix {
    private final String name;
    private final String description;
    private final AffixType type;
    private final float min;
    private final float max;
    private final BiConsumer<Float, AffixContext> applyFunction;

    public Affix(String name, String description, AffixType type, float min, float max, BiConsumer<Float, AffixContext> applyFunction) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.min = min;
        this.max = max;
        this.applyFunction = applyFunction;
    }

    public void apply(float value, AffixContext context) {
        applyFunction.accept(value, context);
    }

    public float rollValue() {
        return min + (float) (Math.random() * (max - min));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AffixType getType() {
        return type;
    }

    public enum AffixType {
        CRIT_DAMAGE,
        LIFE_STEAL,
        BONUS_DAMAGE,
        DEFENSE
    }
}
