package com.couchbase.intellij.tools.dialog;


import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import utils.ColorHelper;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JComboCheckBox extends JComboBox {
    private final Color border = Color.decode("#6f6f6f");
    private final MouseAdapter mouseAdapter;
    private JLabel displayLabel;
    private Consumer<List<String>> listener;

    public JComboCheckBox() {
        displayLabel = new JBLabel("") {
            @Override
            public Color getForeground() {
                return JBColor.foreground();
            }
        };
        displayLabel.setFont(displayLabel.getFont().deriveFont(Font.PLAIN, 12f));

        setRenderer(new DefaultListCellRenderer() {
            final Color selectionBackground = UIUtil.getListSelectionBackground(true);
            private final Color foregroundColor = Gray._160;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (index == -1) {
                    return displayLabel;
                }

                JCheckBox checkBox = (JCheckBox) value;
                checkBox.setBackground(isSelected ? selectionBackground : list.getBackground());
                checkBox.setForeground(isSelected ? JBColor.white : foregroundColor);

                if (isSelected) {
                    checkBox.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(selectionBackground, 1, 5, true), BorderFactory.createEmptyBorder(2, 5, 2, 5)));
                } else {
                    checkBox.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

                }
                return checkBox;
            }
        });


        mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                JList<?> list = ((ComboPopup) getAccessibleContext().getAccessibleChild(0)).getList();
                int location = list.locationToIndex(e.getPoint());
                if (location == -1) return;
                JCheckBox checkbox = (JCheckBox) list.getModel().getElementAt(location);
                checkbox.setSelected(!checkbox.isSelected());
                displayLabel.setText(String.join(", ", getSelectedItems()));
                displayLabel.setFont(displayLabel.getFont().deriveFont(Font.PLAIN, 12f));
                displayLabel.revalidate();
                if (listener != null) {
                    listener.accept(getSelectedItems());
                }
                hidePopup();
            }
        };

        setUI(new BasicComboBoxUI() {
            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox);
                popup.getList().addMouseListener(mouseAdapter);
                return popup;
            }

            @Override
            protected JButton createArrowButton() {
                Color background = UIManager.getColor("ComboBox.background");
                Color defaultShadow = UIManager.getColor("Button.shadow");
                Color defaultDarkShadow = UIManager.getColor("Button.darkShadow");
                Color defaultHighlight = UIManager.getColor("Button.highlight");

                BasicArrowButton button = new CustomArrowButton(BasicArrowButton.SOUTH, background, defaultShadow, defaultDarkShadow, defaultHighlight);
                button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                return button;
            }

        });


        Color borderColor = UIManager.getColor("TextField.borderColor");
        if (ColorHelper.isDarkTheme()) {
            borderColor = border;
        }
        setBorder(BorderFactory.createCompoundBorder(JBUI.Borders.empty(3), BorderFactory.createCompoundBorder(new RoundedLineBorder(borderColor, 1, 5, true), BorderFactory.createEmptyBorder(2, -1, 2, 5))));
    }

    public void removeAllItemsAndCreateNewComboBox() {
        JComboCheckBox newComboBox = new JComboCheckBox();
        newComboBox.setItemListener(listener);
        setModel(newComboBox.getModel());
        this.displayLabel = newComboBox.displayLabel;
        setRenderer(newComboBox.getRenderer());
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (isFocusOwner()) {

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UIUtil.getFocusedBorderColor());
            float borderWidth = 3f;
            float cornerRadius = 5f;
            float borderX = borderWidth / 2;
            @SuppressWarnings("SuspiciousNameCombination") float borderY = borderX;
            float borderW = getWidth() - borderWidth;
            float borderH = getHeight() - borderWidth;
            RoundRectangle2D borderShape = new RoundRectangle2D.Float(borderX, borderY, borderW, borderH, cornerRadius, cornerRadius);
            g2.setStroke(new BasicStroke(borderWidth));
            g2.draw(borderShape);
            g2.dispose();
        } else {
            g2.setStroke(new BasicStroke(1f));
            super.paintBorder(g);
        }
    }


    public void setItemListener(Consumer<List<String>> listener) {
        this.listener = listener;
    }

    public void setHint(String message) {
        displayLabel.setText(message);
        this.repaint();
    }

    public void addItem(String text) {
        JCheckBox checkBox = new JCheckBox(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (getModel().isRollover()) {
                    g2.setPaint(getBackground().darker());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                super.paintComponent(g2);
                g2.dispose();
            }
        };

        checkBox.setOpaque(true);
        checkBox.setBorder(JBUI.Borders.empty());
        super.addItem(checkBox);
    }

    public List<String> getSelectedItems() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            JCheckBox checkBox = (JCheckBox) getItemAt(i);
            if (checkBox.isSelected()) {
                list.add(checkBox.getText());
            }
        }
        return list;
    }


}
