package com.onter.ontercore.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NotificationConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_NOTIFICATIONS = BUILDER
            .comment("Enable server notifications")
            .define("enableNotifications", true);

    public static final ModConfigSpec.IntValue NOTIFICATION_INTERVAL = BUILDER
            .comment("Notification interval in ticks (20 ticks = 1 second)")
            .defineInRange("notificationInterval", 6000, 100, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> NOTIFICATION_MESSAGE = BUILDER
            .comment("Default notification message")
            .define("notificationMessage", "Welcome to the server!");

    static final ModConfigSpec SPEC = BUILDER.build();
}
