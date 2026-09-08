package com.onter.ontercore.features.notifications;

import com.onter.ontercore.config.NotificationConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerNotificationTicker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerNotificationTicker.class);
    private static int tickCounter = 0;
    private static boolean enabled = false;

    public static void enable() {
        enabled = true;
        LOGGER.info("Server notification ticker enabled");
    }

    public static void disable() {
        enabled = false;
        LOGGER.info("Server notification ticker disabled");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!enabled || !NotificationConfig.ENABLE_NOTIFICATIONS.getAsBoolean()) {
            return;
        }

        tickCounter++;
        int interval = NotificationConfig.NOTIFICATION_INTERVAL.getAsInt();

        if (tickCounter >= interval) {
            tickCounter = 0;
            sendNotification(event.getServer());
        }
    }

    private void sendNotification(MinecraftServer server) {
        if (server == null) {
            return;
        }

        String message = NotificationConfig.NOTIFICATION_MESSAGE.get();
        Component notification = Component.literal(message);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(notification);
        }

        LOGGER.info("Sent notification to all players: {}", message);
    }
}
