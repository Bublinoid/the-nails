package ru.bublinoid.thenails.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import java.io.File;

/**
 * Initializes the 'logs' directory if it does not exist.
 */

@Configuration
public class LogDirectoryInitializer {

    private static final Logger log = LoggerFactory.getLogger(LogDirectoryInitializer.class);

    @PostConstruct
    public void init() {
        File logDir = new File("logs");
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                log.info("The 'logs' directory was created.");
            } else {
                log.info("Failed to create the 'logs' directory.");
            }
        } else {
            log.info("The 'logs' directory already exists.");
        }
    }
}
