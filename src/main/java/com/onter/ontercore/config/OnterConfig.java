package com.onter.ontercore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class OnterConfig {
    public static final ModConfigSpec SPEC;
    public static final OnterConfig INSTANCE;

    // --- DATABASE ---
    public final ModConfigSpec.BooleanValue dbEnabled;
    public final ModConfigSpec.ConfigValue<String> dbHost;
    public final ModConfigSpec.IntValue dbPort;
    public final ModConfigSpec.ConfigValue<String> dbName;
    public final ModConfigSpec.ConfigValue<String> dbUsername;
    public final ModConfigSpec.ConfigValue<String> dbPassword;
    public final ModConfigSpec.IntValue dbPoolSize;
    public final ModConfigSpec.ConfigValue<String> serverName;

    // --- ANALYTICS & DOSSIER ---
    public final ModConfigSpec.BooleanValue analyticsEnabled;
    public final ModConfigSpec.BooleanValue logLogins;
    public final ModConfigSpec.BooleanValue logCommands;
    public final ModConfigSpec.BooleanValue logChat;
    public final ModConfigSpec.BooleanValue logDeaths;
    public final ModConfigSpec.ConfigValue<List<? extends String>> commandExclusions;

    // --- TELEGRAM ---
    public final ModConfigSpec.BooleanValue telegramEnabled;
    public final ModConfigSpec.ConfigValue<String> telegramBotToken;
    public final ModConfigSpec.ConfigValue<String> telegramChatId;
    public final ModConfigSpec.BooleanValue telegramNotifyJoin;
    public final ModConfigSpec.BooleanValue telegramNotifyLeave;
    public final ModConfigSpec.BooleanValue telegramNotifyLifecycle;
    public final ModConfigSpec.BooleanValue telegramNotifySecurity;
    public final ModConfigSpec.BooleanValue telegramNotifyLowTps;
    public final ModConfigSpec.DoubleValue telegramLowTpsThreshold;

    // --- BROADCASTER ---
    public final ModConfigSpec.BooleanValue broadcasterEnabled;
    public final ModConfigSpec.IntValue broadcasterIntervalSeconds;
    public final ModConfigSpec.BooleanValue broadcasterRandomOrder;
    public final ModConfigSpec.ConfigValue<String> broadcasterPrefix;
    public final ModConfigSpec.ConfigValue<List<? extends String>> broadcasterMessages;

    // --- MAINTENANCE ---
    public final ModConfigSpec.BooleanValue maintenanceEnabled;
    public final ModConfigSpec.ConfigValue<String> maintenanceKickMessage;
    public final ModConfigSpec.IntValue maintenancePermissionLevel;

    static {
        Pair<OnterConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(OnterConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    public OnterConfig(ModConfigSpec.Builder builder) {
        // --- DATABASE ---
        builder.comment("========================================",
                        " Database Configuration (MySQL / MariaDB)",
                        " Shared database with website and backend",
                        "========================================").push("database");
        
        dbEnabled = builder.comment("Enable MySQL database storage").define("enabled", true);
        dbHost = builder.comment("Database host").define("host", "127.0.0.1");
        dbPort = builder.comment("Database port").defineInRange("port", 3306, 1, 65535);
        dbName = builder.comment("Database name").define("database", "minecraft_network");
        dbUsername = builder.comment("Database username").define("username", "onter");
        dbPassword = builder.comment("Database password").define("password", "change_me");
        dbPoolSize = builder.comment("HikariCP maximum connection pool size").defineInRange("pool_size", 10, 1, 50);
        serverName = builder.comment("Identifier of this server across the network (e.g. 'rpg-1', 'lobby', 'tech')")
                .define("server_name", "rpg-1");
        builder.pop();

        // --- ANALYTICS & DOSSIER ---
        builder.comment("========================================",
                        " Player Analytics & Dossier Tracker",
                        " Logs sessions, IPs, coordinates, and commands",
                        "========================================").push("analytics");
        
        analyticsEnabled = builder.comment("Master toggle for player tracking & analytics").define("enabled", true);
        logLogins = builder.comment("Track logins, sessions, IPs, and playtime").define("log_logins", true);
        logCommands = builder.comment("Audit executed commands").define("log_commands", true);
        logChat = builder.comment("Audit player chat messages").define("log_chat", false);
        logDeaths = builder.comment("Track player deaths and death locations").define("log_deaths", true);
        commandExclusions = builder.comment("Commands that will NOT be logged (prevents logging passwords from auth mods)")
                .defineListAllowEmpty("command_exclusions", 
                        Arrays.asList("/login", "/register", "/auth", "/l", "/reg", "/changepassword", "/cp"), 
                        s -> s instanceof String);
        builder.pop();

        // --- TELEGRAM ---
        builder.comment("========================================",
                        " Telegram Bot Notifications (Async, Zero-Tick Lag)",
                        " Real-time alerts for joins, leaves, security, and TPS",
                        "========================================").push("telegram");
        
        telegramEnabled = builder.comment("Enable Telegram alerts").define("enabled", false);
        telegramBotToken = builder.comment("Telegram Bot Token from @BotFather").define("bot_token", "");
        telegramChatId = builder.comment("Telegram Target Chat ID (User or Group ID)").define("chat_id", "");
        telegramNotifyJoin = builder.comment("Send notification on player join").define("notify_join", true);
        telegramNotifyLeave = builder.comment("Send notification on player leave").define("notify_leave", true);
        telegramNotifyLifecycle = builder.comment("Send notification on server start/stop").define("notify_server_lifecycle", true);
        telegramNotifySecurity = builder.comment("Send alerts on admin logins or sensitive commands").define("notify_security", true);
        telegramNotifyLowTps = builder.comment("Send warning when server TPS drops below threshold").define("notify_low_tps", true);
        telegramLowTpsThreshold = builder.comment("TPS threshold below which a warning is sent").defineInRange("low_tps_threshold", 14.0, 1.0, 19.9);
        builder.pop();

        // --- BROADCASTER ---
        builder.comment("========================================",
                        " In-Game Periodic Broadcaster",
                        " Automated announcements & tips in chat",
                        "========================================").push("broadcaster");
        
        broadcasterEnabled = builder.comment("Enable periodic announcements").define("enabled", true);
        broadcasterIntervalSeconds = builder.comment("Interval between announcements in seconds (300s = 5m)")
                .defineInRange("interval_seconds", 300, 10, 86400);
        broadcasterRandomOrder = builder.comment("Broadcast messages in random order instead of sequential")
                .define("random_order", false);
        broadcasterPrefix = builder.comment("Chat prefix before announcement message (& color codes supported)")
                .define("prefix", "&8[&6Инфо&8]&r ");
        broadcasterMessages = builder.comment("List of announcement messages (& color codes supported)")
                .defineListAllowEmpty("messages", Arrays.asList(
                        "&aДобро пожаловать на наш сервер! Приятной игры!",
                        "&eНаш официальный Discord: &bhttps://discord.gg/example",
                        "&6Заметили баг или нарушение? Сообщите администрации через команду &e/report",
                        "&dГолосуйте за сервер каждый день и получайте ценные награды!"
                ), s -> s instanceof String);
        builder.pop();

        // --- MAINTENANCE ---
        builder.comment("========================================",
                        " Maintenance Mode (Технические работы)",
                        " Blocks non-staff connections with a custom message",
                        "========================================").push("maintenance");
        
        maintenanceEnabled = builder.comment("Enable maintenance mode").define("enabled", false);
        maintenanceKickMessage = builder.comment("Kick message displayed when maintenance is active")
                .define("kick_message", "&cСервер находится на техническом обслуживании.\n&eПожалуйста, попробуйте подключиться позже!");
        maintenancePermissionLevel = builder.comment("Minimum OP level required to bypass maintenance (default: 2)")
                .defineInRange("bypass_permission_level", 2, 0, 4);
        builder.pop();
    }
}
