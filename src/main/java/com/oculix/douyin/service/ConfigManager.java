package com.oculix.douyin.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.oculix.douyin.model.Config;
import com.oculix.douyin.util.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration manager for loading and saving bot settings.
 */
public class ConfigManager {
    private static final String DEFAULT_CONFIG_PATH = "./config/config.json";
    private static ConfigManager instance;
    private Config config;
    private Path configPath;
    private final Gson gson;

    private ConfigManager() {
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Load configuration from file or classpath.
     */
    public boolean loadConfig() {
        // Try to load from file first
        configPath = Paths.get(DEFAULT_CONFIG_PATH);
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                config = gson.fromJson(content, Config.class);
                AppLogger.info("Configuration loaded from: " + configPath.toAbsolutePath());
                return true;
            } catch (IOException e) {
                AppLogger.error("Failed to read config file: " + e.getMessage());
            } catch (JsonSyntaxException e) {
                AppLogger.error("Invalid config file format: " + e.getMessage());
            }
        }

        // Fallback to classpath resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (is != null) {
                config = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), Config.class);
                AppLogger.info("Configuration loaded from classpath");
                return true;
            }
        } catch (IOException e) {
            AppLogger.error("Failed to read classpath config: " + e.getMessage());
        }

        AppLogger.warn("No configuration found, using defaults");
        config = createDefaultConfig();
        return false;
    }

    /**
     * Save configuration to file.
     */
    public boolean saveConfig() {
        if (configPath == null) {
            configPath = Paths.get(DEFAULT_CONFIG_PATH);
        }
        try {
            Path parent = configPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            String content = gson.toJson(config);
            Files.writeString(configPath, content, StandardCharsets.UTF_8);
            AppLogger.success("Configuration saved to: " + configPath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            AppLogger.error("Failed to save config: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current configuration.
     */
    public Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    /**
     * Update configuration.
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Reload configuration from file.
     */
    public boolean reloadConfig() {
        return loadConfig();
    }

    /**
     * Get config file path.
     */
    public String getConfigPath() {
        return configPath != null ? configPath.toAbsolutePath().toString() : DEFAULT_CONFIG_PATH;
    }

    private Config createDefaultConfig() {
        Config cfg = new Config();
        
        
        
        
        
        
        
        
        
        
        
        
        Config.MonitorConfig monitor = new Config.MonitorConfig();
        
        
        
        
        Config.CommentConfig comment = new Config.CommentConfig();
        comment.setFramework("这篇内容真不错！想了解更多相关技巧，可以关注我~");
        cfg.setComment(comment);
        
        Config.AntiDetectionConfig antiDetection = new Config.AntiDetectionConfig();
        
        
        
        
        Config.AuditConfig audit = new Config.AuditConfig();
        
        
        return cfg;
    }

    /**
     * Auto-detect Chrome profile path by checking common locations.
     * Returns the first valid Chrome profile found, or null if none found.
     */
    public String autoDetectChromeProfile() {
        String username = System.getProperty("user.name");
        String[] commonPaths = {
            "C:/Users/" + username + "/AppData/Local/Google/Chrome/User Data/Default",
            "C:/Users/" + username + "/AppData/Local/Google/Chrome/User Data/Profile 1",
            "C:/Users/" + username + "/AppData/Local/Google/Chrome/User Data/Profile 2",
            "C:/Users/" + username + "/AppData/Local/Google/Chrome/User Data/Profile 3",
            "C:/Users/" + username + "/AppData/Local/Google/Chrome/User Data/Profile 4",
            "C:/Users/" + username + "/AppData/Local/Google/Chrome/User Data/Profile 5"
        };
        for (String path : commonPaths) {
            java.nio.file.Path p = java.nio.file.Paths.get(path);
            if (java.nio.file.Files.exists(p) && java.nio.file.Files.isDirectory(p)) {
                AppLogger.info("Auto-detected Chrome profile: " + path);
                return path;
            }
        }
        AppLogger.warn("No Chrome profile found at common locations");
        return null;
    }
}
