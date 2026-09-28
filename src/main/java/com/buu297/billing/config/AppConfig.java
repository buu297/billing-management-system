package com.buu297.billing.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();

    static {
        // Defaults
        props.setProperty("db.url", "jdbc:mysql://localhost:3306/billing_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        props.setProperty("db.user", "root");
        props.setProperty("db.password", "");
        props.setProperty("server.port", "8080");

        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {}
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
