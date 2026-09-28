package com.buu297.billing;

import com.buu297.billing.config.AppConfig;
import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.server.StudentApiServer;
import com.buu297.billing.ui.AdminBillingApp;

import javax.swing.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("        STUDENT BILLING MANAGEMENT SYSTEM");
        System.out.println("  Dual Interfaces: Web Student Dashboard & Java Swing Admin App");
        System.out.println("  Stack: HTML, CSS, JavaScript, Java Swing, MySQL");
        System.out.println("  Author: buu297 (github.com/buu297/billing-management-system)");
        System.out.println("==================================================================");

        // Initialize Database Manager
        DatabaseManager dbManager = DatabaseManager.getInstance();
        System.out.println("[DB STATUS] " + dbManager.getDatabaseStatusDescription());

        boolean startServer = true;
        boolean startGui = true;

        if (args.length > 0) {
            if ("--server".equalsIgnoreCase(args[0]) || "-s".equalsIgnoreCase(args[0])) {
                startGui = false;
            } else if ("--admin".equalsIgnoreCase(args[0]) || "-a".equalsIgnoreCase(args[0])) {
                startServer = false;
            }
        }

        // 1. Launch Embedded Web Server
        if (startServer) {
            int port = AppConfig.getInt("server.port", 8080);
            File webRoot = new File("web");
            if (!webRoot.exists()) {
                // Check relative to current working directory
                webRoot = new File("./web");
            }

            try {
                StudentApiServer apiServer = new StudentApiServer(port, webRoot);
                apiServer.start();
                System.out.println("[WEB DASHBOARD] http://localhost:" + port);
                System.out.println("[REST API]       http://localhost:" + port + "/api/students");
                System.out.println("[REST API]       http://localhost:" + port + "/api/bills");
                System.out.println("[REST API]       http://localhost:" + port + "/api/payments");
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to start HTTP Web Server: " + e.getMessage());
            }
        }

        // 2. Launch Java Swing Admin Console
        if (startGui) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            SwingUtilities.invokeLater(() -> {
                AdminBillingApp app = new AdminBillingApp();
                app.setVisible(true);
                System.out.println("[SWING ADMIN] Admin Billing Report Generator GUI launched successfully.");
            });
        }
    }
}
