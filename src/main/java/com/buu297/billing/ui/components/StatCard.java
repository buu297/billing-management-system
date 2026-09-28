package com.buu297.billing.ui.components;

import com.buu297.billing.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StatCard extends JPanel {
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public StatCard(String title, String initialValue, String subtitle, Color accentColor) {
        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.BG_CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER, 1),
                        new EmptyBorder(14, 16, 14, 16)
                )
        ));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.TEXT_MUTED);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(Theme.FONT_METRIC);
        valueLabel.setForeground(Theme.TEXT_MAIN);

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(accentColor);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        centerPanel.setOpaque(false);
        centerPanel.add(titleLabel);
        centerPanel.add(valueLabel);

        add(centerPanel, BorderLayout.CENTER);
        add(subtitleLabel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}
