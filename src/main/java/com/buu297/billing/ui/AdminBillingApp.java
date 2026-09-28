package com.buu297.billing.ui;

import com.buu297.billing.config.AppConfig;
import com.buu297.billing.config.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

public class AdminBillingApp extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(cardLayout);

    private final DashboardPanel dashboardPanel;
    private final BillingManagementPanel billingPanel;
    private final PaymentLedgerPanel paymentPanel;
    private final ReportGeneratorPanel reportPanel;
    private final ConcurrencySimulatorPanel concurrencyPanel;

    private JButton activeNavButton = null;

    public AdminBillingApp() {
        setTitle("Student Billing Management System — Admin Console & Report Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        // Panels initialization
        this.dashboardPanel = new DashboardPanel(
                () -> navigateTo("BILLING"),
                () -> navigateTo("PAYMENTS"),
                () -> navigateTo("CONCURRENCY")
        );
        this.billingPanel = new BillingManagementPanel();
        this.paymentPanel = new PaymentLedgerPanel();
        this.reportPanel = new ReportGeneratorPanel();
        this.concurrencyPanel = new ConcurrencySimulatorPanel();

        contentArea.add(dashboardPanel, "DASHBOARD");
        contentArea.add(billingPanel, "BILLING");
        contentArea.add(paymentPanel, "PAYMENTS");
        contentArea.add(reportPanel, "REPORTS");
        contentArea.add(concurrencyPanel, "CONCURRENCY");

        JPanel root = new JPanel(new BorderLayout());
        root.add(createSidebar(), BorderLayout.WEST);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(createHeaderBar(), BorderLayout.NORTH);
        mainContainer.add(contentArea, BorderLayout.CENTER);

        root.add(mainContainer, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, 800));
        sidebar.setBackground(Theme.BG_SIDEBAR);

        // Logo / Brand
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 4));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(24, 20, 24, 20));

        JLabel title = new JLabel("STUDENT BILLING");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Management Console v1.0");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(148, 163, 184));

        brand.add(title);
        brand.add(sub);
        sidebar.add(brand, BorderLayout.NORTH);

        // Nav Links
        JPanel navMenu = new JPanel(new GridLayout(6, 1, 0, 6));
        navMenu.setOpaque(false);
        navMenu.setBorder(new EmptyBorder(10, 12, 10, 12));

        JButton btnDash = createNavButton("📊  Dashboard", "DASHBOARD");
        JButton btnBilling = createNavButton("📋  Billing & Invoices", "BILLING");
        JButton btnPayment = createNavButton("💳  Payment Ledger", "PAYMENTS");
        JButton btnReport = createNavButton("📈  Report Generator", "REPORTS");
        JButton btnConcurrency = createNavButton("⚡  Concurrency Lab", "CONCURRENCY");

        navMenu.add(btnDash);
        navMenu.add(btnBilling);
        navMenu.add(btnPayment);
        navMenu.add(btnReport);
        navMenu.add(btnConcurrency);

        sidebar.add(navMenu, BorderLayout.CENTER);

        // Footer info
        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 2));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel author = new JLabel("Author: buu297");
        author.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        author.setForeground(new Color(148, 163, 184));

        JLabel stack = new JLabel("Swing • MySQL • Web");
        stack.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        stack.setForeground(new Color(100, 116, 139));

        footer.add(author);
        footer.add(stack);
        sidebar.add(footer, BorderLayout.SOUTH);

        setActiveNav(btnDash);
        return sidebar;
    }

    private JButton createNavButton(String text, String targetCard) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(new Color(226, 232, 240));
        btn.setBackground(Theme.BG_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            setActiveNav(btn);
            navigateTo(targetCard);
        });
        return btn;
    }

    private void setActiveNav(JButton btn) {
        if (activeNavButton != null) {
            activeNavButton.setBackground(Theme.BG_SIDEBAR);
            activeNavButton.setForeground(new Color(226, 232, 240));
        }
        activeNavButton = btn;
        activeNavButton.setBackground(Theme.PRIMARY);
        activeNavButton.setForeground(Color.WHITE);
    }

    public void navigateTo(String cardName) {
        cardLayout.show(contentArea, cardName);
        if ("DASHBOARD".equals(cardName)) {
            dashboardPanel.refreshData();
        } else if ("BILLING".equals(cardName)) {
            billingPanel.loadFilteredData();
        } else if ("PAYMENTS".equals(cardName)) {
            paymentPanel.loadData();
        } else if ("REPORTS".equals(cardName)) {
            reportPanel.generateReport();
        }
    }

    private JPanel createHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        bar.setPreferredSize(new Dimension(800, 52));
        bar.setBorder(new EmptyBorder(8, 20, 8, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        String dbDesc = DatabaseManager.getInstance().getDatabaseStatusDescription();
        JLabel dbBadge = new JLabel("● " + dbDesc);
        dbBadge.setFont(Theme.FONT_SUBTITLE);
        dbBadge.setForeground(DatabaseManager.getInstance().isUsingMySQL() ? Theme.SUCCESS : Theme.PRIMARY);
        left.add(dbBadge);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        int port = AppConfig.getInt("server.port", 8080);
        JButton btnOpenWeb = Theme.createPrimaryButton("🌐 Open Student Web Dashboard");
        btnOpenWeb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenWeb.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please open http://localhost:" + port + " in your browser.");
            }
        });

        right.add(btnOpenWeb);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
