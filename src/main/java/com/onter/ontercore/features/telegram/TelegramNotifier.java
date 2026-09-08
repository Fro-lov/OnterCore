package com.onter.ontercore.features.telegram;

import com.onter.ontercore.config.OnterConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TelegramNotifier {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy");

    public static void notifyPlayerJoin(String username, String ip, int currentOnline, int maxOnline) {
        if (!OnterConfig.INSTANCE.telegramNotifyJoin.get()) return;

        String server = OnterConfig.INSTANCE.serverName.get();
        String message = String.format(
                "🟢 <b>[Вход в игру]</b>\n" +
                "👤 <b>Игрок:</b> <code>%s</code>\n" +
                "🌐 <b>IP:</b> <code>%s</code>\n" +
                "🖥 <b>Сервер:</b> <code>%s</code>\n" +
                "👥 <b>Онлайн:</b> %d/%d\n" +
                "🕒 <i>%s</i>",
                escapeHtml(username),
                escapeHtml(ip),
                escapeHtml(server),
                currentOnline,
                maxOnline,
                LocalDateTime.now().format(TIME_FORMATTER)
        );

        TelegramClient.sendMessageAsync(message);
    }

    public static void notifyPlayerLeave(String username, long sessionDurationSeconds, int currentOnline, int maxOnline) {
        if (!OnterConfig.INSTANCE.telegramNotifyLeave.get()) return;

        String server = OnterConfig.INSTANCE.serverName.get();
        long minutes = sessionDurationSeconds / 60;
        long seconds = sessionDurationSeconds % 60;
        String durationStr = String.format("%d мин %d сек", minutes, seconds);

        String message = String.format(
                "🔴 <b>[Выход из игры]</b>\n" +
                "👤 <b>Игрок:</b> <code>%s</code>\n" +
                "⏱ <b>Время сессии:</b> %s\n" +
                "🖥 <b>Сервер:</b> <code>%s</code>\n" +
                "👥 <b>Онлайн:</b> %d/%d\n" +
                "🕒 <i>%s</i>",
                escapeHtml(username),
                durationStr,
                escapeHtml(server),
                currentOnline,
                maxOnline,
                LocalDateTime.now().format(TIME_FORMATTER)
        );

        TelegramClient.sendMessageAsync(message);
    }

    public static void notifyServerLifecycle(String status) {
        if (!OnterConfig.INSTANCE.telegramNotifyLifecycle.get()) return;

        String server = OnterConfig.INSTANCE.serverName.get();
        String icon = status.equalsIgnoreCase("STARTED") ? "🚀" : "🛑";
        String statusText = status.equalsIgnoreCase("STARTED") ? "Сервер успешно запущен!" : "Сервер останавливается...";

        String message = String.format(
                "%s <b>[Статус сервера]</b>\n" +
                "🖥 <b>Сервер:</b> <code>%s</code>\n" +
                "ℹ️ <b>Статус:</b> %s\n" +
                "🕒 <i>%s</i>",
                icon,
                escapeHtml(server),
                statusText,
                LocalDateTime.now().format(TIME_FORMATTER)
        );

        TelegramClient.sendMessageAsync(message);
    }

    public static void notifySecurityAlert(String username, String details) {
        if (!OnterConfig.INSTANCE.telegramNotifySecurity.get()) return;

        String server = OnterConfig.INSTANCE.serverName.get();
        String message = String.format(
                "⚠️ <b>[Алерт Безопасности]</b>\n" +
                "👤 <b>Пользователь:</b> <code>%s</code>\n" +
                "🖥 <b>Сервер:</b> <code>%s</code>\n" +
                "📝 <b>Действие:</b> %s\n" +
                "🕒 <i>%s</i>",
                escapeHtml(username),
                escapeHtml(server),
                escapeHtml(details),
                LocalDateTime.now().format(TIME_FORMATTER)
        );

        TelegramClient.sendMessageAsync(message);
    }

    public static void notifyLowTps(double tps, double mspt) {
        if (!OnterConfig.INSTANCE.telegramNotifyLowTps.get()) return;

        String server = OnterConfig.INSTANCE.serverName.get();
        String message = String.format(
                "📉 <b>[Предупреждение TPS]</b>\n" +
                "🖥 <b>Сервер:</b> <code>%s</code>\n" +
                "⚡️ <b>Текущий TPS:</b> <code>%.2f</code> (Порог: <code>%.1f</code>)\n" +
                "⏱ <b>MSPT:</b> <code>%.2f ms</code>\n" +
                "🕒 <i>%s</i>",
                escapeHtml(server),
                tps,
                OnterConfig.INSTANCE.telegramLowTpsThreshold.get(),
                mspt,
                LocalDateTime.now().format(TIME_FORMATTER)
        );

        TelegramClient.sendMessageAsync(message);
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
