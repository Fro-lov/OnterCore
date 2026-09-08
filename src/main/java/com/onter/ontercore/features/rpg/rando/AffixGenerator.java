package com.onter.ontercore.features.rpg.rando;

import com.onter.ontercore.features.rpg.affix.Affix;
import com.onter.ontercore.features.rpg.affix.Affix.AffixType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AffixGenerator {
    private static final Random RANDOM = new Random();
    private static final List<Affix> AVAILABLE_AFFIXES = new ArrayList<>();

    static {
        initializeAffixes();
    }

    private static void initializeAffixes() {
        AVAILABLE_AFFIXES.add(new Affix(
            "Critical Strike",
            "Increases critical damage",
            AffixType.CRIT_DAMAGE,
            5.0f,
            25.0f,
            (value, context) -> {}
        ));

        AVAILABLE_AFFIXES.add(new Affix(
            "Life Steal",
            "Heals on hit",
            AffixType.LIFE_STEAL,
            1.0f,
            10.0f,
            (value, context) -> {}
        ));

        AVAILABLE_AFFIXES.add(new Affix(
            "Bonus Damage",
            "Increases attack damage",
            AffixType.BONUS_DAMAGE,
            2.0f,
            15.0f,
            (value, context) -> {}
        ));

        AVAILABLE_AFFIXES.add(new Affix(
            "Fortified",
            "Increases defense",
            AffixType.DEFENSE,
            3.0f,
            20.0f,
            (value, context) -> {}
        ));
    }

    public static List<Affix> generateAffixes(int count) {
        List<Affix> selected = new ArrayList<>();
        List<Affix> pool = new ArrayList<>(AVAILABLE_AFFIXES);

        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            int index = RANDOM.nextInt(pool.size());
            selected.add(pool.remove(index));
        }

        return selected;
    }

    public static List<Affix> generateAffixesByType(AffixType type, int count) {
        List<Affix> matching = AVAILABLE_AFFIXES.stream()
            .filter(affix -> affix.getType() == type)
            .toList();

        List<Affix> selected = new ArrayList<>();
        List<Affix> pool = new ArrayList<>(matching);

        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            int index = RANDOM.nextInt(pool.size());
            selected.add(pool.remove(index));
        }

        return selected;
    }
}
