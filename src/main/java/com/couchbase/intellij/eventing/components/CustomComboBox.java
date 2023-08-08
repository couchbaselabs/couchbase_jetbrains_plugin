package com.couchbase.intellij.eventing.components;

import javax.swing.*;
import java.awt.*;

public class CustomComboBox extends JComboBox<String> {
    public CustomComboBox() {
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                        isSelected,
                        cellHasFocus);
                if (index == 0) {
                    label.setForeground(Color.GRAY);
                }
                return label;
            }
        });
    }

    @Override
    public void setPopupVisible(boolean v) {
        if (v && getSelectedIndex() == 0) {
            return;
        }
        super.setPopupVisible(v);
    }
}
