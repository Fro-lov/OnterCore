package com.onter.ontercore.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onter.ontercore.features.rpg.affix.Affix;
import com.onter.ontercore.ontercore;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


import java.util.ArrayList;
import java.util.List;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = 
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ontercore.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<AffixData>>> AFFIXES =
            DATA_COMPONENTS.register("affixes",
                    () -> DataComponentType.<List<AffixData>>builder()
                            .persistent(Codec.list(AffixData.CODEC))
                            .networkSynchronized(ByteBufCodecs.collection(ArrayList::new, AffixData.STREAM_CODEC))
                            .build());

    public record AffixData(String affixName, float value) {
        public static final Codec<AffixData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter(AffixData::affixName),
                Codec.FLOAT.fieldOf("value").forGetter(AffixData::value)
            ).apply(instance, AffixData::new)
        );

        public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, AffixData> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
            AffixData::affixName,
            net.minecraft.network.codec.ByteBufCodecs.FLOAT,
            AffixData::value,
            AffixData::new
        );
    }
}
