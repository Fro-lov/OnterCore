package com.onter.ontercore.config;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfigSpecHolder {
    public static void registerConfigs(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, NotificationConfig.SPEC);
    }
}
