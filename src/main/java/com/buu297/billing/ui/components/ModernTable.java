package com.buu297.billing.ui.components;

import com.buu297.billing.model.BillingStatus;
import com.buu297.billing.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;

public class ModernTable extends JTable {

    public ModernTable(TableModel model) {
        super(model);
        setFont(Theme.FONT_BODY);
        setRowHeight(36);
        setGridColor(Theme.BORDER);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setSelectionBackground(Theme.PRIMARY_LIGHT);
        setSelectionForeground(Theme.PRIMARY_DARK);

        JTableHeader header = getTableHeader();
        header.setFont(Theme.FONT_BODY_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(Theme.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.BORDER));
        header.setPreferredSize(new Dimension(header.getWidth(), 36));

        // Default cell padding
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                return c;
            }
        });

        // Custom status badge renderer
        setDefaultRenderer(BillingStatus.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(Theme.FONT_BODY_BOLD);
                lbl.setOpaque(true);

                if (value instanceof BillingStatus) {
                    BillingStatus s = (BillingStatus) value;
                    lbl.setText(s.getDisplayName());
                    switch (s) {
                        case PAID:
                            lbl.setBackground(Theme.SUCCESS_BG);
                            lbl.setForeground(Theme.SUCCESS);
                            break;
                        case PARTIAL:
                            lbl.setBackground(Theme.WARNING_BG);
                            lbl.setForeground(Theme.WARNING);
                            break;
                        case OVERDUE:
                            lbl.setBackground(Theme.DANGER_BG);
                            lbl.setForeground(Theme.DANGER);
                            break;
                        case UNPAID:
                        default:
                            lbl.setBackground(new Color(241, 245, 249));
                            lbl.setForeground(new Color(71, 85, 105));
                            break;
                    }
                }
                return lbl;
            }
        });
    }
}
