package com.onter.ontercore.core.database.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDossier {
    private final UUID uuid;
    private final String username;
    private final Timestamp firstJoin;
    private final Timestamp lastJoin;
    private final Timestamp lastLeave;
    private final String lastIp;
    private final long totalPlaytimeSeconds;
    private final String lastServer;
    private final String lastDimension;
    private final double lastX;
    private final double lastY;
    private final double lastZ;
    private final float lastHealth;
    private final boolean isCurrentlyOnline;
    private final List<CommandLogEntry> recentCommands = new ArrayList<>();
    private int totalDeaths;

    public PlayerDossier(UUID uuid, String username, Timestamp firstJoin, Timestamp lastJoin, Timestamp lastLeave,
                         String lastIp, long totalPlaytimeSeconds, String lastServer, String lastDimension,
                         double lastX, double lastY, double lastZ, float lastHealth, boolean isCurrentlyOnline) {
        this.uuid = uuid;
        this.username = username;
        this.firstJoin = firstJoin;
        this.lastJoin = lastJoin;
        this.lastLeave = lastLeave;
        this.lastIp = lastIp;
        this.totalPlaytimeSeconds = totalPlaytimeSeconds;
        this.lastServer = lastServer;
        this.lastDimension = lastDimension;
        this.lastX = lastX;
        this.lastY = lastY;
        this.lastZ = lastZ;
        this.lastHealth = lastHealth;
        this.isCurrentlyOnline = isCurrentlyOnline;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public Timestamp getFirstJoin() { return firstJoin; }
    public Timestamp getLastJoin() { return lastJoin; }
    public Timestamp getLastLeave() { return lastLeave; }
    public String getLastIp() { return lastIp; }
    public long getTotalPlaytimeSeconds() { return totalPlaytimeSeconds; }
    public String getLastServer() { return lastServer; }
    public String getLastDimension() { return lastDimension; }
    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getLastZ() { return lastZ; }
    public float getLastHealth() { return lastHealth; }
    public boolean isCurrentlyOnline() { return isCurrentlyOnline; }
    public List<CommandLogEntry> getRecentCommands() { return recentCommands; }
    public int getTotalDeaths() { return totalDeaths; }
    public void setTotalDeaths(int totalDeaths) { this.totalDeaths = totalDeaths; }

    public String formatPlaytime() {
        long seconds = totalPlaytimeSeconds;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0 || days > 0) sb.append(hours).append("ч ");
        sb.append(minutes).append("м");
        return sb.toString();
    }
}
