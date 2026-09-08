package com.onter.ontercore.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.core.database.DatabaseManager;
import com.onter.ontercore.core.database.models.CommandLogEntry;
import com.onter.ontercore.core.database.models.PlayerDossier;
import com.onter.ontercore.core.state.ServerStateManager;
import com.onter.ontercore.features.analytics.PlayerDossierService;
import com.onter.ontercore.features.broadcaster.AnnouncementTicker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Optional;

public class OnterCommand {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var onterNode = Commands.literal("onter")
                .requires(source -> com.onter.ontercore.core.permission.ModPermissions.check(source, com.onter.ontercore.core.permission.ModPermissions.COMMAND_BASE, 2))
                .executes(OnterCommand::showHelp)
                .then(Commands.literal("info")
                        .requires(source -> com.onter.ontercore.core.permission.ModPermissions.check(source, com.onter.ontercore.core.permission.ModPermissions.COMMAND_INFO, 2))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(OnterCommand::executeInfo)))
                .then(Commands.literal("status")
                        .requires(source -> com.onter.ontercore.core.permission.ModPermissions.check(source, com.onter.ontercore.core.permission.ModPermissions.COMMAND_STATUS, 2))
                        .executes(OnterCommand::executeStatus))
                .then(Commands.literal("maintenance")
                        .requires(source -> com.onter.ontercore.core.permission.ModPermissions.check(source, com.onter.ontercore.core.permission.ModPermissions.COMMAND_MAINTENANCE, 2))
                        .then(Commands.literal("on").executes(ctx -> setMaintenance(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setMaintenance(ctx, false)))
                        .then(Commands.literal("toggle").executes(OnterCommand::toggleMaintenance))
                        .then(Commands.literal("status").executes(OnterCommand::showMaintenanceStatus)))
                .then(Commands.literal("broadcast")
                        .requires(source -> com.onter.ontercore.core.permission.ModPermissions.check(source, com.onter.ontercore.core.permission.ModPermissions.COMMAND_BROADCAST, 2))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(OnterCommand::executeBroadcast)))
                .then(Commands.literal("reload")
                        .requires(source -> com.onter.ontercore.core.permission.ModPermissions.check(source, com.onter.ontercore.core.permission.ModPermissions.COMMAND_RELOAD, 2))
                        .executes(OnterCommand::executeReload));

        dispatcher.register(onterNode);
        dispatcher.register(Commands.literal("oc")
                .requires(source -> com.onter.ontercore.core.permission.ModPermissions.check(source, com.onter.ontercore.core.permission.ModPermissions.COMMAND_BASE, 2))
                .redirect(onterNode.build()));
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6=== [ OnterCore Команды Управления ] ==="), false);
        source.sendSuccess(() -> Component.literal("§e/onter info <ник/uuid> §7- Полное досье и аналитика игрока"), false);
        source.sendSuccess(() -> Component.literal("§e/onter status §7- Состояние сервера, TPS, MSPT, ОЗУ, БД"), false);
        source.sendSuccess(() -> Component.literal("§e/onter maintenance <on|off|toggle> §7- Режим техработ"), false);
        source.sendSuccess(() -> Component.literal("§e/onter broadcast <сообщение> §7- Отправить анонс в чат"), false);
        source.sendSuccess(() -> Component.literal("§e/onter reload §7- Перезагрузить конфигурацию мода"), false);
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

            // Online status & UUID
            String onlineStatus = d.isCurrentlyOnline() ? "§aВ сети" : "§cНе в сети";
            source.sendSuccess(() -> Component.literal("§7UUID: §f" + d.getUuid().toString()), false);
            source.sendSuccess(() -> Component.literal("§7Статус: " + onlineStatus + " §7| Имя сервера: §b" + (d.getLastServer() != null ? d.getLastServer() : "N/A")), false);

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

            // Recent commands
            if (!d.getRecentCommands().isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7Последние команды:"), false);
                for (CommandLogEntry cmd : d.getRecentCommands()) {
                    String cmdTime = DATE_FORMAT.format(cmd.getCreatedAt());
                    source.sendSuccess(() -> Component.literal(" §8- §e" + cmd.getCommand() + " §8(§7" + cmdTime + "§8)"), false);
                }
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

        source.sendSuccess(() -> Component.literal("§6=== [ Состояние Сервера OnterCore ] ==="), false);
        source.sendSuccess(() -> Component.literal("§7Сервер: §b" + OnterConfig.INSTANCE.serverName.get() + " §7| Аптайм: §f" + uptimeMinutes + " мин"), false);
        source.sendSuccess(() -> Component.literal(String.format("§7TPS: §e%.2f §7| MSPT: §e%.2f ms", tps, mspt)), false);
        source.sendSuccess(() -> Component.literal("§7ОЗУ: §a" + usedMem + "MB §7/ §f" + maxMem + "MB"), false);
        source.sendSuccess(() -> Component.literal("§7База данных (MySQL): " + dbStatus), false);
        source.sendSuccess(() -> Component.literal("§7Режим техработ: " + maintStatus), false);
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

    private static int executeBroadcast(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String message = StringArgumentType.getString(context, "message");
        String prefix = OnterConfig.INSTANCE.broadcasterPrefix.get();
        String fullFormatted = (prefix + message).replace("&", "§");
        Component comp = Component.literal(fullFormatted);

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(comp);
        }

        source.sendSuccess(() -> Component.literal("§a[OnterCore] Анонс успешно отправлен всем игрокам."), false);
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
