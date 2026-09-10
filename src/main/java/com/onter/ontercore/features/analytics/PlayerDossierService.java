package com.onter.ontercore.features.analytics;

import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.core.database.DatabaseManager;
import com.onter.ontercore.core.database.models.CommandLogEntry;
import com.onter.ontercore.core.database.models.PlayerDossier;
import com.onter.ontercore.core.database.models.SessionLogEntry;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDossierService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDossierService.class);
    private static final ConcurrentHashMap<UUID, Long> loginTimestamps = new ConcurrentHashMap<>();

    public static void recordLogin(ServerPlayer player, String ipAddress) {
        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();
        String server = OnterConfig.INSTANCE.serverName.get();
        loginTimestamps.put(uuid, System.currentTimeMillis());

        if (!OnterConfig.INSTANCE.analyticsEnabled.get() || !OnterConfig.INSTANCE.logLogins.get()) {
            return;
        }

        DatabaseManager.executeAsync(connection -> {
            Timestamp now = Timestamp.from(Instant.now());
            // Upsert onter_players
            String playerSql = """
                INSERT INTO onter_players (uuid, username, first_join, last_join, last_ip, last_server, last_dimension, last_x, last_y, last_z, last_health)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    last_join = VALUES(last_join),
                    last_ip = VALUES(last_ip),
                    last_server = VALUES(last_server),
                    last_dimension = VALUES(last_dimension),
                    last_x = VALUES(last_x),
                    last_y = VALUES(last_y),
                    last_z = VALUES(last_z),
                    last_health = VALUES(last_health);
            """;
            try (PreparedStatement ps = connection.prepareStatement(playerSql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setTimestamp(3, now);
                ps.setTimestamp(4, now);
                ps.setString(5, ipAddress);
                ps.setString(6, server);
                ps.setString(7, player.level().dimension().location().toString());
                ps.setDouble(8, player.getX());
                ps.setDouble(9, player.getY());
                ps.setDouble(10, player.getZ());
                ps.setFloat(11, player.getHealth());
                ps.executeUpdate();
            }

            // Insert onter_sessions
            String sessionSql = """
                INSERT INTO onter_sessions (player_uuid, username, server_name, ip_address, join_time)
                VALUES (?, ?, ?, ?, ?);
            """;
            try (PreparedStatement ps = connection.prepareStatement(sessionSql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setString(3, server);
                ps.setString(4, ipAddress);
                ps.setTimestamp(5, now);
                ps.executeUpdate();
            }
        });
    }

    public static long recordLogout(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Long loginTime = loginTimestamps.remove(uuid);
        long durationSeconds = 0;
        if (loginTime != null) {
            durationSeconds = Math.max(0, (System.currentTimeMillis() - loginTime) / 1000);
        }

        if (!OnterConfig.INSTANCE.analyticsEnabled.get() || !OnterConfig.INSTANCE.logLogins.get()) {
            return durationSeconds;
        }

        long finalDuration = durationSeconds;
        DatabaseManager.executeAsync(connection -> {
            Timestamp now = Timestamp.from(Instant.now());
            // Update onter_players
            String updatePlayerSql = """
                UPDATE onter_players
                SET last_leave = ?,
                    total_playtime = total_playtime + ?,
                    last_dimension = ?,
                    last_x = ?,
                    last_y = ?,
                    last_z = ?,
                    last_health = ?
                WHERE uuid = ?;
            """;
            try (PreparedStatement ps = connection.prepareStatement(updatePlayerSql)) {
                ps.setTimestamp(1, now);
                ps.setLong(2, finalDuration);
                ps.setString(3, player.level().dimension().location().toString());
                ps.setDouble(4, player.getX());
                ps.setDouble(5, player.getY());
                ps.setDouble(6, player.getZ());
                ps.setFloat(7, player.getHealth());
                ps.setString(8, uuid.toString());
                ps.executeUpdate();
            }

            // Update latest onter_session for this player
            String updateSessionSql = """
                UPDATE onter_sessions
                SET leave_time = ?, duration_seconds = ?
                WHERE player_uuid = ? AND leave_time IS NULL
                ORDER BY id DESC LIMIT 1;
            """;
            try (PreparedStatement ps = connection.prepareStatement(updateSessionSql)) {
                ps.setTimestamp(1, now);
                ps.setLong(2, finalDuration);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            }
        });

        return durationSeconds;
    }

    public static void recordCommand(ServerPlayer player, String command) {
        if (!OnterConfig.INSTANCE.analyticsEnabled.get() || !OnterConfig.INSTANCE.logCommands.get()) {
            return;
        }

        // Check exclusions (e.g. passwords in /login /register)
        for (String exclusion : OnterConfig.INSTANCE.commandExclusions.get()) {
            if (command.toLowerCase().startsWith(exclusion.toLowerCase())) {
                return;
            }
        }

        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();
        String server = OnterConfig.INSTANCE.serverName.get();
        String dimension = player.level().dimension().location().toString();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        DatabaseManager.executeAsync(connection -> {
            String sql = """
                INSERT INTO onter_command_logs (player_uuid, username, server_name, command, dimension, x, y, z, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setString(3, server);
                ps.setString(4, command);
                ps.setString(5, dimension);
                ps.setDouble(6, x);
                ps.setDouble(7, y);
                ps.setDouble(8, z);
                ps.setTimestamp(9, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
        });
    }

    public static void recordDeath(ServerPlayer player, String deathMessage, String killer) {
        if (!OnterConfig.INSTANCE.analyticsEnabled.get() || !OnterConfig.INSTANCE.logDeaths.get()) {
            return;
        }

        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();
        String server = OnterConfig.INSTANCE.serverName.get();
        String dimension = player.level().dimension().location().toString();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        DatabaseManager.executeAsync(connection -> {
            String sql = """
                INSERT INTO onter_death_logs (player_uuid, username, server_name, death_message, killer, dimension, x, y, z, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setString(3, server);
                ps.setString(4, deathMessage);
                ps.setString(5, killer);
                ps.setString(6, dimension);
                ps.setDouble(7, x);
                ps.setDouble(8, y);
                ps.setDouble(9, z);
                ps.setTimestamp(10, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
        });
    }

    public static CompletableFuture<Optional<PlayerDossier>> fetchDossier(String nameOrUuid) {
        return DatabaseManager.queryAsync(connection -> {
            String sql = """
                SELECT uuid, username, first_join, last_join, last_leave, last_ip, total_playtime,
                       last_server, last_dimension, last_x, last_y, last_z, last_health
                FROM onter_players
                WHERE uuid = ? OR LOWER(username) = LOWER(?)
                LIMIT 1;
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, nameOrUuid);
                ps.setString(2, nameOrUuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String username = rs.getString("username");
                        Timestamp firstJoin = rs.getTimestamp("first_join");
                        Timestamp lastJoin = rs.getTimestamp("last_join");
                        Timestamp lastLeave = rs.getTimestamp("last_leave");
                        String lastIp = rs.getString("last_ip");
                        long totalPlaytime = rs.getLong("total_playtime");
                        String lastServer = rs.getString("last_server");
                        String lastDimension = rs.getString("last_dimension");
                        double lastX = rs.getDouble("last_x");
                        double lastY = rs.getDouble("last_y");
                        double lastZ = rs.getDouble("last_z");
                        float lastHealth = rs.getFloat("last_health");
                        boolean isOnline = loginTimestamps.containsKey(uuid);

                        PlayerDossier dossier = new PlayerDossier(
                                uuid, username, firstJoin, lastJoin, lastLeave, lastIp,
                                totalPlaytime, lastServer, lastDimension, lastX, lastY, lastZ, lastHealth, isOnline
                        );

                        // Load recent commands
                        loadRecentCommands(connection, uuid.toString(), dossier);

                        // Load recent sessions
                        loadRecentSessions(connection, uuid.toString(), dossier);

                        // Load total deaths
                        loadTotalDeaths(connection, uuid.toString(), dossier);

                        return Optional.of(dossier);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[OnterCore] Error fetching player dossier for {}: {}", nameOrUuid, e.getMessage());
            }
            return Optional.empty();
        });
    }

    public static CompletableFuture<List<CommandLogEntry>> fetchCommandHistory(String nameOrUuid, int limit) {
        return DatabaseManager.queryAsync(connection -> {
            List<CommandLogEntry> list = new ArrayList<>();
            String sql = """
                SELECT server_name, command, dimension, x, y, z, created_at
                FROM onter_command_logs
                WHERE player_uuid = ? OR LOWER(username) = LOWER(?)
                ORDER BY id DESC LIMIT ?;
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, nameOrUuid);
                ps.setString(2, nameOrUuid);
                ps.setInt(3, Math.min(100, Math.max(1, limit)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new CommandLogEntry(
                                rs.getString("server_name"),
                                rs.getString("command"),
                                rs.getString("dimension"),
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getTimestamp("created_at")
                        ));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[OnterCore] Error fetching command history for {}: {}", nameOrUuid, e.getMessage());
            }
            return list;
        });
    }

    private static void loadRecentCommands(Connection connection, String playerUuid, PlayerDossier dossier) {
        String sql = """
            SELECT server_name, command, dimension, x, y, z, created_at
            FROM onter_command_logs
            WHERE player_uuid = ?
            ORDER BY id DESC LIMIT 5;
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dossier.getRecentCommands().add(new CommandLogEntry(
                            rs.getString("server_name"),
                            rs.getString("command"),
                            rs.getString("dimension"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (Exception ignored) {}
    }

    private static void loadRecentSessions(Connection connection, String playerUuid, PlayerDossier dossier) {
        String sql = """
            SELECT server_name, ip_address, join_time, leave_time, duration_seconds
            FROM onter_sessions
            WHERE player_uuid = ?
            ORDER BY id DESC LIMIT 3;
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dossier.getRecentSessions().add(new SessionLogEntry(
                            rs.getString("server_name"),
                            rs.getString("ip_address"),
                            rs.getTimestamp("join_time"),
                            rs.getTimestamp("leave_time"),
                            rs.getLong("duration_seconds")
                    ));
                }
            }
        } catch (Exception ignored) {}
    }

    private static void loadTotalDeaths(Connection connection, String playerUuid, PlayerDossier dossier) {
        String sql = "SELECT COUNT(*) FROM onter_death_logs WHERE player_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dossier.setTotalDeaths(rs.getInt(1));
                }
            }
        } catch (Exception ignored) {}
    }
}
