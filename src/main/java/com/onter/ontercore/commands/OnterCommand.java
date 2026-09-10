package com.onter.ontercore.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.core.database.DatabaseManager;
import com.onter.ontercore.core.database.models.CommandLogEntry;
import com.onter.ontercore.core.database.models.PlayerDossier;
import com.onter.ontercore.core.database.models.SessionLogEntry;
import com.onter.ontercore.core.permission.ModPermissions;
import com.onter.ontercore.core.state.ServerStateManager;
import com.onter.ontercore.features.analytics.PlayerDossierService;
import com.onter.ontercore.features.broadcaster.AnnouncementTicker;
import com.onter.ontercore.features.sync.PlayerInventorySyncService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.List;

public class OnterCommand {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var onterNode = Commands.literal("onter")
                .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_BASE, 2))
                .executes(OnterCommand::showHelp)
                // /onter info <player>
                .then(Commands.literal("info")
                        .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_INFO, 2))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(OnterCommand::executeInfo)))
                // /onter history <player> [limit]
                .then(Commands.literal("history")
                        .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_HISTORY, 2))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> executeHistory(ctx, 10))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> executeHistory(ctx, IntegerArgumentType.getInteger(ctx, "limit"))))))
                // /onter status
                .then(Commands.literal("status")
                        .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_STATUS, 2))
                        .executes(OnterCommand::executeStatus))
                // /onter maintenance
                .then(Commands.literal("maintenance")
                        .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_MAINTENANCE, 2))
                        .then(Commands.literal("on").executes(ctx -> setMaintenance(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setMaintenance(ctx, false)))
                        .then(Commands.literal("toggle").executes(OnterCommand::toggleMaintenance))
                        .then(Commands.literal("status").executes(OnterCommand::showMaintenanceStatus)))
                // /onter broadcast
                .then(Commands.literal("broadcast")
                        .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_BROADCAST, 2))
                        .then(Commands.literal("list").executes(OnterCommand::broadcastList))
                        .then(Commands.literal("time").executes(OnterCommand::broadcastTime))
                        .then(Commands.literal("next").executes(OnterCommand::broadcastNext))
                        .then(Commands.literal("toggle").executes(OnterCommand::broadcastToggle))
                        .then(Commands.literal("send")
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(OnterCommand::executeBroadcast)))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(OnterCommand::executeBroadcast)))
                // /onter sync [target]
                .then(Commands.literal("sync")
                        .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_SYNC, 2))
                        .executes(OnterCommand::executeSyncAll)
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(OnterCommand::executeSyncPlayer)))
                // /onter reload
                .then(Commands.literal("reload")
                        .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_RELOAD, 2))
                        .executes(OnterCommand::executeReload));

        dispatcher.register(onterNode);
        dispatcher.register(Commands.literal("oc")
                .requires(source -> ModPermissions.check(source, ModPermissions.COMMAND_BASE, 2))
                .redirect(onterNode.build()));
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6=== [ OnterCore Управление Сервером ] ==="), false);
        source.sendSuccess(() -> Component.literal("§e/onter info <ник/uuid> §7- Полное досье и аналитика игрока"), false);
        source.sendSuccess(() -> Component.literal("§e/onter history <ник> [лимит] §7- Глобальная история команд игрока"), false);
        source.sendSuccess(() -> Component.literal("§e/onter status §7- Состояние сервера (TPS, MSPT, RAM, БД)"), false);
        source.sendSuccess(() -> Component.literal("§e/onter maintenance <on|off|toggle> §7- Режим техработ"), false);
        source.sendSuccess(() -> Component.literal("§e/onter broadcast <list|time|next|toggle|send> §7- Управление анонсами"), false);
        source.sendSuccess(() -> Component.literal("§e/onter sync [ник] §7- Синхронизация инвентаря в MySQL"), false);
        source.sendSuccess(() -> Component.literal("§e/onter reload §7- Перезагрузка конфигурации мода"), false);
        return 1;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String target = StringArgumentType.getString(context, "target");

        source.sendSuccess(() -> Component.literal("§7[OnterCore] Поиск данных для §e" + target + "§7..."), false);

        PlayerDossierService.fetchDossier(target).thenAccept(optDossier -> {
            if (optDossier.isEmpty()) {
                source.sendFailure(Component.literal("§c[OnterCore] Игрок '" + target + "' не найден в базе данных."));
                return;
            }

            PlayerDossier d = optDossier.get();
            source.sendSuccess(() -> Component.literal("§6================= §e[ Досье: " + d.getUsername() + " ] §6================="), false);

            // Online status & Server & UUID
            String onlineStatus = d.isCurrentlyOnline() ? "§aВ сети" : "§cНе в сети";
            String serverTag = d.getLastServer() != null ? "§b[" + d.getLastServer() + "]" : "§7[N/A]";
            source.sendSuccess(() -> Component.literal("§7UUID: §f" + d.getUuid().toString()), false);
            source.sendSuccess(() -> Component.literal("§7Статус: " + onlineStatus + " §7| Последний сервер: " + serverTag), false);

            // IP Address (Clickable copy)
            MutableComponent ipComp = Component.literal("§7Последний IP: §f" + (d.getLastIp() != null ? d.getLastIp() : "N/A"))
                    .setStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§aНажмите, чтобы скопировать IP")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, d.getLastIp() != null ? d.getLastIp() : "")));
            source.sendSuccess(() -> ipComp, false);

            // Playtime & Deaths
            source.sendSuccess(() -> Component.literal("§7Общий онлайн: §a" + d.formatPlaytime() + " §7| Смертей: §c" + d.getTotalDeaths()), false);

            // First & Last Join
            String firstJoinStr = d.getFirstJoin() != null ? DATE_FORMAT.format(d.getFirstJoin()) : "N/A";
            String lastJoinStr = d.getLastJoin() != null ? DATE_FORMAT.format(d.getLastJoin()) : "N/A";
            source.sendSuccess(() -> Component.literal("§7Первый вход: §f" + firstJoinStr + " §7| Последний: §f" + lastJoinStr), false);

            // Coordinates & Dimension (with clickable teleport button)
            String coordsStr = String.format("%.1f, %.1f, %.1f", d.getLastX(), d.getLastY(), d.getLastZ());
            MutableComponent coordsComp = Component.literal("§7Координаты: §e" + coordsStr + " §7(" + d.getLastDimension() + ") ");
            
            MutableComponent tpButton = Component.literal("§a[Телепорт]")
                    .setStyle(Style.EMPTY
                            .withColor(ChatFormatting.GREEN)
                            .withBold(true)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§aТелепортироваться к последней позиции")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tp @s %.1f %.1f %.1f", d.getLastX(), d.getLastY(), d.getLastZ()))));
            coordsComp.append(tpButton);
            source.sendSuccess(() -> coordsComp, false);

            // Recent Sessions (with server tags)
            if (!d.getRecentSessions().isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7Последние сессии:"), false);
                for (SessionLogEntry s : d.getRecentSessions()) {
                    String time = s.getJoinTime() != null ? DATE_FORMAT.format(s.getJoinTime()) : "N/A";
                    long mins = s.getDurationSeconds() / 60;
                    source.sendSuccess(() -> Component.literal(String.format(" §8- §b[%s] §7%s §8(§e%d мин§8) §7IP: §f%s",
                            s.getServerName(), time, mins, s.getIpAddress())), false);
                }
            }

            // Recent commands (with server tags)
            if (!d.getRecentCommands().isEmpty()) {
                MutableComponent cmdHeader = Component.literal("§7Последние команды: ");
                MutableComponent historyBtn = Component.literal("§e[Вся история]")
                        .setStyle(Style.EMPTY
                                .withColor(ChatFormatting.YELLOW)
                                .withUnderlined(true)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§6Посмотреть расширенную историю команд")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/onter history " + d.getUsername() + " 15")));
                cmdHeader.append(historyBtn);
                source.sendSuccess(() -> cmdHeader, false);

                for (CommandLogEntry cmd : d.getRecentCommands()) {
                    String cmdTime = DATE_FORMAT.format(cmd.getCreatedAt());
                    source.sendSuccess(() -> Component.literal(String.format(" §8- §b[%s] §e%s §8(§7%s§8)",
                            cmd.getServerName(), cmd.getCommand(), cmdTime)), false);
                }
            }

            source.sendSuccess(() -> Component.literal("§6=================================================="), false);
        });

        return 1;
    }

    private static int executeHistory(CommandContext<CommandSourceStack> context, int limit) {
        CommandSourceStack source = context.getSource();
        String target = StringArgumentType.getString(context, "target");

        source.sendSuccess(() -> Component.literal("§7[OnterCore] Загрузка истории команд для §e" + target + " §8(лимит: " + limit + ")§7..."), false);

        PlayerDossierService.fetchCommandHistory(target, limit).thenAccept(logs -> {
            if (logs.isEmpty()) {
                source.sendFailure(Component.literal("§c[OnterCore] История команд для '" + target + "' пуста или не найдена."));
                return;
            }

            source.sendSuccess(() -> Component.literal("§6=== [ История Команд: " + target + " (" + logs.size() + ") ] ==="), false);
            for (CommandLogEntry cmd : logs) {
                String cmdTime = DATE_FORMAT.format(cmd.getCreatedAt());
                String location = String.format("%.0f, %.0f, %.0f (%s)", cmd.getX(), cmd.getY(), cmd.getZ(), cmd.getDimension());
                source.sendSuccess(() -> Component.literal(String.format("§8[§b%s§8] §7%s §8| §e%s §8| §7%s",
                        cmd.getServerName(), cmdTime, cmd.getCommand(), location)), false);
            }
            source.sendSuccess(() -> Component.literal("§6=================================================="), false);
        });

        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / 1024 / 1024;
        long totalMem = rt.totalMemory() / 1024 / 1024;
        long freeMem = rt.freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;

        double tps = ServerStateManager.getCurrentTps();
        double mspt = ServerStateManager.getCurrentMspt();
        long uptimeMinutes = ManagementFactory.getRuntimeMXBean().getUptime() / 60000;

        String dbStatus = DatabaseManager.isConnected() ? "§aПодключена" : "§cОтключена / Ошибка";
        String maintStatus = ServerStateManager.isMaintenanceActive() ? "§cВКЛЮЧЕН" : "§aВЫКЛЮЧЕН";
        String webQueueStatus = OnterConfig.INSTANCE.webQueueEnabled.get() ? "§aАктивна" : "§7Выключена";
        String invSyncStatus = OnterConfig.INSTANCE.inventorySyncEnabled.get() ? "§aВключена" : "§7Выключена";

        source.sendSuccess(() -> Component.literal("§6=== [ Состояние Сервера OnterCore ] ==="), false);
        source.sendSuccess(() -> Component.literal("§7Сервер сети: §b" + OnterConfig.INSTANCE.serverName.get() + " §7| Аптайм: §f" + uptimeMinutes + " мин"), false);
        source.sendSuccess(() -> Component.literal(String.format("§7TPS: §e%.2f §7| MSPT: §e%.2f ms", tps, mspt)), false);
        source.sendSuccess(() -> Component.literal("§7ОЗУ: §a" + usedMem + "MB §7/ §f" + maxMem + "MB"), false);
        source.sendSuccess(() -> Component.literal("§7База данных (MySQL): " + dbStatus), false);
        source.sendSuccess(() -> Component.literal("§7Веб-очередь (Site ➔ Server): " + webQueueStatus + " §7| Синхр. инвентарей: " + invSyncStatus), false);
        source.sendSuccess(() -> Component.literal("§7Режим техработ: " + maintStatus), false);
        return 1;
    }

    private static int broadcastList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<? extends String> messages = OnterConfig.INSTANCE.broadcasterMessages.get();

        source.sendSuccess(() -> Component.literal("§6=== [ Список авто-анонсов (" + messages.size() + ") ] ==="), false);
        for (int i = 0; i < messages.size(); i++) {
            int num = i + 1;
            String msg = messages.get(i).replace("&", "§");
            source.sendSuccess(() -> Component.literal("§e[" + num + "] §r" + msg), false);
        }
        int remaining = AnnouncementTicker.getTimeRemainingSeconds();
        source.sendSuccess(() -> Component.literal("§7Следующий авто-анонс через: §a" + remaining + " сек"), false);
        return 1;
    }

    private static int broadcastTime(CommandContext<CommandSourceStack> context) {
        int seconds = AnnouncementTicker.getTimeRemainingSeconds();
        int minutes = seconds / 60;
        int remSec = seconds % 60;
        String status = AnnouncementTicker.isBroadcastingActive() ? "§aВКЛЮЧЕН" : "§cВЫКЛЮЧЕН";

        context.getSource().sendSuccess(() -> Component.literal(String.format(
                "§7[OnterCore] Авто-бродкастер: %s §7| До следующего анонса: §e%d мин %d сек §8(%d сек)",
                status, minutes, remSec, seconds)), false);
        return 1;
    }

    private static int broadcastNext(CommandContext<CommandSourceStack> context) {
        AnnouncementTicker.broadcastNextMessage(context.getSource().getServer());
        AnnouncementTicker.resetCounter();
        context.getSource().sendSuccess(() -> Component.literal("§a[OnterCore] Следующий анонс принудительно отправлен, таймер сброшен."), true);
        return 1;
    }

    private static int broadcastToggle(CommandContext<CommandSourceStack> context) {
        boolean active = AnnouncementTicker.toggleBroadcasting();
        String status = active ? "§aВКЛЮЧЕН" : "§cВЫКЛЮЧЕН";
        context.getSource().sendSuccess(() -> Component.literal("§7[OnterCore] Авто-бродкастер теперь " + status), true);
        return 1;
    }

    private static int executeBroadcast(CommandContext<CommandSourceStack> context) {
        String message = StringArgumentType.getString(context, "message");
        AnnouncementTicker.sendCustomBroadcast(context.getSource().getServer(), message);
        context.getSource().sendSuccess(() -> Component.literal("§a[OnterCore] Пользовательский анонс отправлен всем игрокам."), true);
        return 1;
    }

    private static int executeSyncAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            PlayerInventorySyncService.syncPlayerSnapshot(player);
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§a[OnterCore] Синхронизировано инвентарей игроков: " + finalCount), true);
        return 1;
    }

    private static int executeSyncPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "target");
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(targetName);

        if (player == null) {
            source.sendFailure(Component.literal("§c[OnterCore] Игрок '" + targetName + "' не в сети на этом сервере."));
            return 0;
        }

        PlayerInventorySyncService.syncPlayerSnapshot(player);
        source.sendSuccess(() -> Component.literal("§a[OnterCore] Инвентарь игрока " + targetName + " успешно сохранен в MySQL!"), true);
        return 1;
    }

    private static int setMaintenance(CommandContext<CommandSourceStack> context, boolean active) {
        ServerStateManager.setMaintenanceActive(active);
        String statusText = active ? "§cВКЛЮЧЕН" : "§aВЫКЛЮЧЕН";
        context.getSource().sendSuccess(() -> Component.literal("§7[OnterCore] Режим техработ теперь " + statusText), true);
        return 1;
    }

    private static int toggleMaintenance(CommandContext<CommandSourceStack> context) {
        boolean newState = !ServerStateManager.isMaintenanceActive();
        ServerStateManager.setMaintenanceActive(newState);
        String statusText = newState ? "§cВКЛЮЧЕН" : "§aВЫКЛЮЧЕН";
        context.getSource().sendSuccess(() -> Component.literal("§7[OnterCore] Режим техработ теперь " + statusText), true);
        return 1;
    }

    private static int showMaintenanceStatus(CommandContext<CommandSourceStack> context) {
        String statusText = ServerStateManager.isMaintenanceActive() ? "§cВКЛЮЧЕН" : "§aВЫКЛЮЧЕН";
        context.getSource().sendSuccess(() -> Component.literal("§7[OnterCore] Режим техработ сейчас: " + statusText), false);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            AnnouncementTicker.resetCounter();
            DatabaseManager.init();
            source.sendSuccess(() -> Component.literal("§a[OnterCore] Конфигурация, модули и БД успешно перезагружены!"), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[OnterCore] Ошибка при перезагрузке: " + e.getMessage()));
        }
        return 1;
    }
}
