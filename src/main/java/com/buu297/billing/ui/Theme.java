package com.buu297.billing.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Theme {
    public static final Color PRIMARY = new Color(37, 99, 235);       // Blue 600
    public static final Color PRIMARY_DARK = new Color(29, 78, 216);  // Blue 700
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255); // Blue 50
    public static final Color ACCENT = new Color(14, 165, 233);       // Sky 500

    public static final Color SUCCESS = new Color(22, 163, 74);      // Green 600
    public static final Color SUCCESS_BG = new Color(220, 252, 231);  // Green 100
    public static final Color WARNING = new Color(217, 119, 6);      // Amber 600
    public static final Color WARNING_BG = new Color(254, 243, 199);  // Amber 100
    public static final Color DANGER = new Color(225, 29, 72);        // Rose 600
    public static final Color DANGER_BG = new Color(255, 228, 230);   // Rose 100
    public static final Color INFO = new Color(79, 70, 229);          // Indigo 600
    public static final Color INFO_BG = new Color(238, 242, 255);

    public static final Color BG_DARK = new Color(15, 23, 42);        // Slate 900
    public static final Color BG_SIDEBAR = new Color(30, 41, 59);     // Slate 800
    public static final Color BG_MAIN = new Color(248, 250, 252);     // Slate 50
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BORDER = new Color(226, 232, 240);      // Slate 200

    public static final Color TEXT_MAIN = new Color(15, 23, 42);      // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);  // Slate 500
    public static final Color TEXT_WHITE = Color.WHITE;

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 22);

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        );
    }
}
