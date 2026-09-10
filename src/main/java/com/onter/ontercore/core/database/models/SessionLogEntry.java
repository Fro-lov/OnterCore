package com.onter.ontercore.core.database.models;

import java.sql.Timestamp;

public class SessionLogEntry {
    private final String serverName;
    private final String ipAddress;
    private final Timestamp joinTime;
    private final Timestamp leaveTime;
    private final long durationSeconds;

    public SessionLogEntry(String serverName, String ipAddress, Timestamp joinTime, Timestamp leaveTime, long durationSeconds) {
        this.serverName = serverName;
        this.ipAddress = ipAddress;
        this.joinTime = joinTime;
        this.leaveTime = leaveTime;
        this.durationSeconds = durationSeconds;
    }

    public String getServerName() { return serverName != null ? serverName : "unknown"; }
    public String getIpAddress() { return ipAddress != null ? ipAddress : "unknown"; }
    public Timestamp getJoinTime() { return joinTime; }
    public Timestamp getLeaveTime() { return leaveTime; }
    public long getDurationSeconds() { return durationSeconds; }
}
