package com.onter.ontercore.core.database;

import com.onter.ontercore.config.OnterConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;
    private static ExecutorService asyncExecutor;
    private static boolean initialized = false;

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection conn) throws Exception;
    }

    @FunctionalInterface
    public interface SqlFunction<R> {
        R apply(Connection conn) throws Exception;
    }

    public static synchronized void init() {
        if (!OnterConfig.INSTANCE.dbEnabled.get()) {
            LOGGER.info("[OnterCore] MySQL Database is disabled in configuration.");
            return;
        }

        if (initialized) {
            shutdown();
        }

        asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "OnterCore-DB-Worker");
            thread.setDaemon(true);
            return thread;
        });

        try {
            String host = OnterConfig.INSTANCE.dbHost.get();
            int port = OnterConfig.INSTANCE.dbPort.get();
            String db = OnterConfig.INSTANCE.dbName.get();
            String user = OnterConfig.INSTANCE.dbUsername.get();
            String pass = OnterConfig.INSTANCE.dbPassword.get();
            int poolSize = OnterConfig.INSTANCE.dbPoolSize.get();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8", host, port, db));
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(Math.min(2, poolSize));
            config.setConnectionTimeout(5000);
            config.setValidationTimeout(3000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("OnterCoreHikariPool");

            // Recommended MySQL performance optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);
            initialized = true;
            LOGGER.info("[OnterCore] MySQL Database connected successfully to {}:{}/{}", host, port, db);

            // Execute schema migration / table creation
            DatabaseSchema.initTables();
        } catch (Exception e) {
            LOGGER.error("[OnterCore] Failed to initialize MySQL Database connection: {}", e.getMessage(), e);
            initialized = false;
        }
    }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("[OnterCore] MySQL Database connection pool closed.");
        }
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
        }
        initialized = false;
    }

    public static boolean isConnected() {
        return initialized && dataSource != null && !dataSource.isClosed();
    }

    public static Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Database is not connected or initialized");
        }
        return dataSource.getConnection();
    }

    public static CompletableFuture<Void> executeAsync(SqlConsumer task) {
        if (!isConnected()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                task.accept(connection);
            } catch (Exception e) {
                LOGGER.error("[OnterCore] Error executing async database task: {}", e.getMessage(), e);
            }
        }, asyncExecutor);
    }

    public static <T> CompletableFuture<T> queryAsync(SqlFunction<T> task) {
        if (!isConnected()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                return task.apply(connection);
            } catch (Exception e) {
                LOGGER.error("[OnterCore] Error executing async database query: {}", e.getMessage(), e);
                return null;
            }
        }, asyncExecutor);
    }
}
