package com.onter.ontercore.features.web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.core.database.DatabaseManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebQueueService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebQueueService.class);
    private static int tickCounter = 0;
    private static final AtomicBoolean isPolling = new AtomicBoolean(false);

    public record QueueItem(long id, String serverName, String playerUuid, String actionType, String payload) {}

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!OnterConfig.INSTANCE.webQueueEnabled.get() || !DatabaseManager.isConnected()) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (server == null) return;

        tickCounter++;
        int interval = Math.max(5, OnterConfig.INSTANCE.webQueuePollIntervalTicks.get());

        if (tickCounter >= interval) {
            tickCounter = 0;
            pollAndExecute(server);
        }
    }

    private void pollAndExecute(MinecraftServer server) {
        if (!isPolling.compareAndSet(false, true)) {
            return; // Previous poll is still in progress
        }

        String currentServer = OnterConfig.INSTANCE.serverName.get();

        DatabaseManager.queryAsync(connection -> {
            List<QueueItem> items = new ArrayList<>();
            String selectSql = """
                SELECT id, server_name, player_uuid, action_type, payload
                FROM onter_web_queue
                WHERE server_name IN (?, 'ALL') AND status = 'PENDING'
                ORDER BY id ASC LIMIT 20;
            """;
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setString(1, currentServer);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new QueueItem(
                                rs.getLong("id"),
                                rs.getString("server_name"),
                                rs.getString("player_uuid"),
                                rs.getString("action_type"),
                                rs.getString("payload")
                        ));
                    }
                }
            }

            // Mark fetched items as PROCESSING
            if (!items.isEmpty()) {
                String markSql = "UPDATE onter_web_queue SET status = 'PROCESSING' WHERE id = ?;";
                try (PreparedStatement ps = connection.prepareStatement(markSql)) {
                    for (QueueItem item : items) {
                        ps.setLong(1, item.id());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            return items;
        }).thenAccept(items -> {
            try {
                if (items != null && !items.isEmpty()) {
                    server.execute(() -> {
                        for (QueueItem item : items) {
                            processItemOnServerThread(server, item);
                        }
                    });
                }
            } finally {
                isPolling.set(false);
            }
        }).exceptionally(ex -> {
            isPolling.set(false);
            LOGGER.error("[OnterCore] Error polling web action queue: {}", ex.getMessage());
            return null;
        });
    }

    private void processItemOnServerThread(MinecraftServer server, QueueItem item) {
        try {
            String actionType = item.actionType().toUpperCase();
            String payload = item.payload();
            String playerUuid = item.playerUuid();
            ServerPlayer targetPlayer = null;

            if (playerUuid != null && !playerUuid.trim().isEmpty()) {
                try {
                    UUID uuid = UUID.fromString(playerUuid.trim());
                    targetPlayer = server.getPlayerList().getPlayer(uuid);
                } catch (Exception ignored) {}
            }

            String resultText = "Executed successfully";
            boolean success = true;

            switch (actionType) {
                case "COMMAND", "RUN_COMMAND" -> {
                    String commandToRun = payload;
                    // If payload is JSON {"command": "..."}
                    if (payload.trim().startsWith("{")) {
                        try {
                            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                            if (json.has("command")) {
                                commandToRun = json.get("command").getAsString();
                            }
                        } catch (Exception ignored) {}
                    }

                    if (commandToRun.startsWith("/")) {
                        commandToRun = commandToRun.substring(1);
                    }

                    // Replace placeholders
                    if (targetPlayer != null) {
                        commandToRun = commandToRun.replace("%player%", targetPlayer.getGameProfile().getName());
                        commandToRun = commandToRun.replace("%uuid%", targetPlayer.getUUID().toString());
                    }
                    commandToRun = commandToRun.replace("%server%", OnterConfig.INSTANCE.serverName.get());

                    CommandSourceStack source = server.createCommandSourceStack();
                    server.getCommands().performPrefixedCommand(source, commandToRun);
                    resultText = "Command executed: " + commandToRun;
                }

                case "TELEPORT" -> {
                    if (targetPlayer == null) {
                        success = false;
                        resultText = "Player is offline on this server";
                    } else {
                        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                        double x = json.get("x").getAsDouble();
                        double y = json.get("y").getAsDouble();
                        double z = json.get("z").getAsDouble();
                        String dimStr = json.has("dimension") ? json.get("dimension").getAsString() : "minecraft:overworld";

                        ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(
                                Registries.DIMENSION, ResourceLocation.parse(dimStr));
                        ServerLevel targetLevel = server.getLevel(dimKey);
                        if (targetLevel == null) targetLevel = server.overworld();

                        targetPlayer.teleportTo(targetLevel, x, y, z, targetPlayer.getYRot(), targetPlayer.getXRot());
                        resultText = String.format("Teleported %s to %.1f, %.1f, %.1f (%s)",
                                targetPlayer.getGameProfile().getName(), x, y, z, dimStr);
                    }
                }

                case "MESSAGE", "SEND_MESSAGE" -> {
                    String msgText = payload;
                    if (payload.trim().startsWith("{")) {
                        try {
                            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                            if (json.has("message")) msgText = json.get("message").getAsString();
                        } catch (Exception ignored) {}
                    }
                    String formatted = msgText.replace("&", "§");

                    if (targetPlayer != null) {
                        targetPlayer.sendSystemMessage(Component.literal(formatted));
                        resultText = "Message sent to " + targetPlayer.getGameProfile().getName();
                    } else {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(Component.literal(formatted));
                        }
                        resultText = "Broadcast message sent to all online players";
                    }
                }

                case "KICK" -> {
                    if (targetPlayer == null) {
                        success = false;
                        resultText = "Player is offline";
                    } else {
                        String kickReason = "Kicked from web console";
                        if (payload.trim().startsWith("{")) {
                            try {
                                JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                                if (json.has("reason")) kickReason = json.get("reason").getAsString();
                            } catch (Exception ignored) {}
                        } else if (!payload.isEmpty()) {
                            kickReason = payload;
                        }
                        targetPlayer.connection.disconnect(Component.literal(kickReason.replace("&", "§")));
                        resultText = "Kicked " + targetPlayer.getGameProfile().getName() + " (" + kickReason + ")";
                    }
                }

                default -> {
                    success = false;
                    resultText = "Unknown action type: " + actionType;
                }
            }

            updateItemStatus(item.id(), success ? "SUCCESS" : "FAILED", resultText);
        } catch (Exception e) {
            LOGGER.error("[OnterCore] Error processing web queue item #{}: {}", item.id(), e.getMessage(), e);
            updateItemStatus(item.id(), "FAILED", "Exception: " + e.getMessage());
        }
    }

    private void updateItemStatus(long id, String status, String response) {
        DatabaseManager.executeAsync(connection -> {
            String sql = "UPDATE onter_web_queue SET status = ?, response = ?, executed_at = ? WHERE id = ?;";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, response);
                ps.setTimestamp(3, Timestamp.from(Instant.now()));
                ps.setLong(4, id);
                ps.executeUpdate();
            }
        });
    }
}
