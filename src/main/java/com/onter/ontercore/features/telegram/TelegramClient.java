package com.onter.ontercore.features.telegram;

import com.onter.ontercore.config.OnterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class TelegramClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramClient.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static CompletableFuture<Boolean> sendMessageAsync(String htmlText) {
        if (!OnterConfig.INSTANCE.telegramEnabled.get()) {
            return CompletableFuture.completedFuture(false);
        }

        String token = OnterConfig.INSTANCE.telegramBotToken.get();
        String chatId = OnterConfig.INSTANCE.telegramChatId.get();

        if (token == null || token.trim().isEmpty() || chatId == null || chatId.trim().isEmpty()) {
            LOGGER.warn("[OnterCore] Telegram is enabled, but bot_token or chat_id is empty!");
            return CompletableFuture.completedFuture(false);
        }

        try {
            String encodedText = URLEncoder.encode(htmlText, StandardCharsets.UTF_8);
            String encodedChatId = URLEncoder.encode(chatId, StandardCharsets.UTF_8);
            String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=HTML&disable_web_page_preview=true",
                    token, encodedChatId, encodedText);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return true;
                        } else {
                            LOGGER.warn("[OnterCore] Telegram API returned code {}: {}", response.statusCode(), response.body());
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        LOGGER.error("[OnterCore] Failed to send Telegram notification: {}", ex.getMessage());
                        return false;
                    });
        } catch (Exception e) {
            LOGGER.error("[OnterCore] Error preparing Telegram message: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
