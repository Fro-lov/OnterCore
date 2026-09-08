package com.onter.ontercore.features.broadcaster;

import com.onter.ontercore.config.OnterConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Random;

public class AnnouncementTicker {
    private static int tickCounter = 0;
    private static int currentIndex = 0;
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!OnterConfig.INSTANCE.broadcasterEnabled.get()) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (server == null) return;

        tickCounter++;
        int intervalTicks = Math.max(200, OnterConfig.INSTANCE.broadcasterIntervalSeconds.get() * 20);

        if (tickCounter >= intervalTicks) {
            tickCounter = 0;
            broadcastNextMessage(server);
        }
    }

    public static void broadcastNextMessage(MinecraftServer server) {
        List<? extends String> messages = OnterConfig.INSTANCE.broadcasterMessages.get();
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String message;
        if (OnterConfig.INSTANCE.broadcasterRandomOrder.get()) {
            message = messages.get(RANDOM.nextInt(messages.size()));
        } else {
            if (currentIndex >= messages.size()) {
                currentIndex = 0;
            }
            message = messages.get(currentIndex);
            currentIndex = (currentIndex + 1) % messages.size();
        }

        String prefix = OnterConfig.INSTANCE.broadcasterPrefix.get();
        String fullFormatted = (prefix + message).replace("&", "§");
        Component component = Component.literal(fullFormatted);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }

    public static void resetCounter() {
        tickCounter = 0;
        currentIndex = 0;
    }
}
