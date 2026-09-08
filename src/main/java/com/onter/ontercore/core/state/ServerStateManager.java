package com.onter.ontercore.core.state;

import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.features.telegram.TelegramNotifier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ServerStateManager {
    private static boolean maintenanceActive = false;
    private static double currentTps = 20.0;
    private static double currentMspt = 0.0;
    private static long lastTpsAlertTime = 0;
    private static final long TPS_ALERT_COOLDOWN_MS = 60000; // Alert at most once a minute

    private static long lastTickStartNanos = 0;
    private static final long[] tickDurationsNanos = new long[100];
    private static int tickSampleCount = 0;
    private static int tickIndex = 0;

    public static void init() {
        maintenanceActive = OnterConfig.INSTANCE.maintenanceEnabled.get();
    }

    public static boolean isMaintenanceActive() {
        return maintenanceActive;
    }

    public static void setMaintenanceActive(boolean active) {
        maintenanceActive = active;
    }

    public static double getCurrentTps() {
        return currentTps;
    }

    public static double getCurrentMspt() {
        return currentMspt;
    }

    @SubscribeEvent
    public void onServerTickPre(ServerTickEvent.Pre event) {
        lastTickStartNanos = System.nanoTime();
    }

    @SubscribeEvent
    public void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null || lastTickStartNanos == 0) return;

        long durationNanos = System.nanoTime() - lastTickStartNanos;
        tickDurationsNanos[tickIndex] = durationNanos;
        tickIndex = (tickIndex + 1) % tickDurationsNanos.length;
        if (tickSampleCount < tickDurationsNanos.length) {
            tickSampleCount++;
        }

        // Calculate average MSPT over samples
        long totalNanos = 0;
        for (int i = 0; i < tickSampleCount; i++) {
            totalNanos += tickDurationsNanos[i];
        }

        double avgNanos = (double) totalNanos / tickSampleCount;
        currentMspt = avgNanos / 1_000_000.0;
        currentTps = Math.min(20.0, 1000.0 / Math.max(currentMspt, 50.0));

        // Check for low TPS alerts
        if (OnterConfig.INSTANCE.telegramNotifyLowTps.get() && currentTps < OnterConfig.INSTANCE.telegramLowTpsThreshold.get()) {
            long now = System.currentTimeMillis();
            if (now - lastTpsAlertTime > TPS_ALERT_COOLDOWN_MS) {
                lastTpsAlertTime = now;
                TelegramNotifier.notifyLowTps(currentTps, currentMspt);
            }
        }
    }
}

