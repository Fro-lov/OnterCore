package com.onter.ontercore.core.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSchema {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSchema.class);

    public static void initTables() {
        DatabaseManager.executeAsync(DatabaseSchema::createTablesSync);
    }

    private static void createTablesSync(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            // Players Table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS onter_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    first_join DATETIME NOT NULL,
                    last_join DATETIME NOT NULL,
                    last_leave DATETIME NULL,
                    last_ip VARCHAR(45) NULL,
                    total_playtime BIGINT DEFAULT 0,
                    last_server VARCHAR(64) NULL,
                    last_dimension VARCHAR(128) NULL,
                    last_x DOUBLE DEFAULT 0,
                    last_y DOUBLE DEFAULT 0,
                    last_z DOUBLE DEFAULT 0,
                    last_health FLOAT DEFAULT 20.0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Sessions Table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS onter_sessions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    username VARCHAR(32) NOT NULL,
                    server_name VARCHAR(64) NOT NULL,
                    ip_address VARCHAR(45) NOT NULL,
                    join_time DATETIME NOT NULL,
                    leave_time DATETIME NULL,
                    duration_seconds BIGINT DEFAULT 0,
                    INDEX idx_session_player (player_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Command Logs Table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS onter_command_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    username VARCHAR(32) NOT NULL,
                    server_name VARCHAR(64) NOT NULL,
                    command TEXT NOT NULL,
                    dimension VARCHAR(128) NOT NULL,
                    x DOUBLE DEFAULT 0,
                    y DOUBLE DEFAULT 0,
                    z DOUBLE DEFAULT 0,
                    created_at DATETIME NOT NULL,
                    INDEX idx_cmd_player (player_uuid),
                    INDEX idx_cmd_created (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Death Logs Table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS onter_death_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    username VARCHAR(32) NOT NULL,
                    server_name VARCHAR(64) NOT NULL,
                    death_message TEXT NOT NULL,
                    killer VARCHAR(64) NULL,
                    dimension VARCHAR(128) NOT NULL,
                    x DOUBLE DEFAULT 0,
                    y DOUBLE DEFAULT 0,
                    z DOUBLE DEFAULT 0,
                    created_at DATETIME NOT NULL,
                    INDEX idx_death_player (player_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Web Action Queue Table (Site -> Server commands & teleports)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS onter_web_queue (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    server_name VARCHAR(64) NOT NULL,
                    player_uuid VARCHAR(36) NULL,
                    action_type VARCHAR(32) NOT NULL,
                    payload TEXT NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                    response TEXT NULL,
                    created_at DATETIME NOT NULL,
                    executed_at DATETIME NULL,
                    INDEX idx_queue_server_status (server_name, status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Player Inventories & Live State Table (Server -> Site JSON snapshot)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS onter_player_inventories (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    server_name VARCHAR(64) NOT NULL,
                    health FLOAT DEFAULT 20.0,
                    max_health FLOAT DEFAULT 20.0,
                    food_level INT DEFAULT 20,
                    saturation FLOAT DEFAULT 5.0,
                    experience_level INT DEFAULT 0,
                    experience_progress FLOAT DEFAULT 0.0,
                    dimension VARCHAR(128) NOT NULL,
                    x DOUBLE DEFAULT 0,
                    y DOUBLE DEFAULT 0,
                    z DOUBLE DEFAULT 0,
                    inventory_json LONGTEXT NOT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_inv_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            LOGGER.info("[OnterCore] MySQL Database tables verified/created successfully.");
        } catch (Exception e) {
            LOGGER.error("[OnterCore] Error creating database schema tables: {}", e.getMessage(), e);
        }
    }
}
