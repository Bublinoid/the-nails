package ru.bublinoid.thenails.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Telegram bot.
 */
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Data
public class BotConfig {
    private String token;
    private String username;
}
