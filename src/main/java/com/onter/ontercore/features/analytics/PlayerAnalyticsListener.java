package com.onter.ontercore.features.analytics;

import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.core.state.ServerStateManager;
import com.onter.ontercore.features.telegram.TelegramNotifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerAnalyticsListener {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Check Maintenance Mode
        if (ServerStateManager.isMaintenanceActive()) {
            int bypassLevel = OnterConfig.INSTANCE.maintenancePermissionLevel.get();
            if (!com.onter.ontercore.core.permission.ModPermissions.canBypassMaintenance(player, bypassLevel)) {
                String kickMsg = OnterConfig.INSTANCE.maintenanceKickMessage.get().replace("&", "§");
                player.connection.disconnect(Component.literal(kickMsg));
                return;
            }
        }

        String ip = player.getIpAddress();
        if (ip == null || ip.isEmpty()) {
            ip = "unknown";
        }

        // Record in Database
        PlayerDossierService.recordLogin(player, ip);

        // Notify Telegram
        MinecraftServer server = player.getServer();
        int currentOnline = server != null ? server.getPlayerList().getPlayerCount() : 1;
        int maxOnline = server != null ? server.getPlayerList().getMaxPlayers() : 100;
        TelegramNotifier.notifyPlayerJoin(player.getGameProfile().getName(), ip, currentOnline, maxOnline);

        // Security Alert if admin joins
        if (player.hasPermissions(2)) {
            TelegramNotifier.notifySecurityAlert(player.getGameProfile().getName(), "Вход администратора в игру");
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long duration = PlayerDossierService.recordLogout(player);

        MinecraftServer server = player.getServer();
        int currentOnline = server != null ? Math.max(0, server.getPlayerList().getPlayerCount() - 1) : 0;
        int maxOnline = server != null ? server.getPlayerList().getMaxPlayers() : 100;

        TelegramNotifier.notifyPlayerLeave(player.getGameProfile().getName(), duration, currentOnline, maxOnline);
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        try {
            if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
                String command = "/" + event.getParseResults().getReader().getString();
                
                PlayerDossierService.recordCommand(player, command);

                // Admin Security alert for sensitive commands
                String lower = command.toLowerCase();
                if (lower.startsWith("/op ") || lower.startsWith("/deop ") || lower.startsWith("/stop") || lower.startsWith("/ban ")) {
                    TelegramNotifier.notifySecurityAlert(player.getGameProfile().getName(), "Выполнение команды: <code>" + command + "</code>");
                }
            }
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String deathMessage = event.getSource().getLocalizedDeathMessage(player).getString();
            String killer = event.getSource().getEntity() != null ? event.getSource().getEntity().getName().getString() : null;

            PlayerDossierService.recordDeath(player, deathMessage, killer);
        }
    }
}
