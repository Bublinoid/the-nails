package ru.bublinoid.thenails.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "email")
@Data
public class EmailConfig {
    private String username;
    private String password;
    private Smtp smtp;

    @Data
    public static class Smtp {
        private boolean auth;
        private Starttls starttls;
        private String host;
        private int port;
        private Ssl ssl;
    }

    @Data
    public static class Starttls {
        private boolean enable;
        private boolean required;
    }

    @Data
    public static class Ssl {
        private String trust;
        private String protocols;
    }
}
