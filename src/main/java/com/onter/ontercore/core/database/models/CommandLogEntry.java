package com.onter.ontercore.core.database.models;

import java.sql.Timestamp;

public class CommandLogEntry {
    private final String serverName;
    private final String command;
    private final String dimension;
    private final double x, y, z;
    private final Timestamp createdAt;

    public CommandLogEntry(String serverName, String command, String dimension, double x, double y, double z, Timestamp createdAt) {
        this.serverName = serverName;
        this.command = command;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAt = createdAt;
    }

    public String getServerName() { return serverName != null ? serverName : "unknown"; }
    public String getCommand() { return command; }
    public String getDimension() { return dimension; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public Timestamp getCreatedAt() { return createdAt; }
}
