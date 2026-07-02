package com.oculix.douyin.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Application logger with both console output and file logging.
 */
public class AppLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ConcurrentLinkedQueue<LogEntry> logBuffer = new ConcurrentLinkedQueue<>();
    private static Consumer<String> uiCallback;
    private static String logFilePath;
    private static volatile boolean initialized = false;

    public enum Level {
        INFO, WARN, ERROR, SUCCESS
    }

    public static class LogEntry {
        public final LocalDateTime timestamp;
        public final Level level;
        public final String message;

        public LogEntry(Level level, String message) {
            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.message = message;
        }

        @Override
        public String toString() {
            String prefix = switch (level) {
                case INFO -> "[INFO]";
                case WARN -> "[WARN]";
                case ERROR -> "[ERROR]";
                case SUCCESS -> "[OK]";
            };
            return String.format("[%s] %s %s", timestamp.format(FORMATTER), prefix, message);
        }
    }

    public static synchronized void init(String logPath) {
        logFilePath = logPath;
        try {
            Path path = Paths.get(logPath).getParent();
            if (path != null) {
                Files.createDirectories(path);
            }
            initialized = true;
            // Flush buffered logs
            while (!logBuffer.isEmpty()) {
                writeToFile(logBuffer.poll());
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    public static void setUICallback(Consumer<String> callback) {
        uiCallback = callback;
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void warn(String message) {
        log(Level.WARN, message);
    }

    public static void error(String message) {
        log(Level.ERROR, message);
    }

    public static void success(String message) {
        log(Level.SUCCESS, message);
    }

    public static void log(Level level, String message) {
        LogEntry entry = new LogEntry(level, message);
        String formatted = entry.toString();
        
        // Console output
        System.out.println(formatted);
        
        // File output
        if (initialized && logFilePath != null) {
            writeToFile(entry);
        } else {
            logBuffer.offer(entry);
        }
        
        // UI callback
        if (uiCallback != null) {
            uiCallback.accept(formatted);
        }
    }

    private static synchronized void writeToFile(LogEntry entry) {
        if (logFilePath == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println(entry.toString());
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }
}
