package com.couchbase.intellij.eventing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
// import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
// import javax.swing.JScrollPane;
// import javax.swing.JTextArea;
// import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.couchbase.intellij.eventing.settings.AdvancedSettings;
import com.couchbase.intellij.eventing.settings.BindingSettings;
import com.couchbase.intellij.eventing.settings.GeneralSettings;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;

public class FunctionDeploymentSettings extends JFrame {
        private JPanel mainPanel;
        private JPanel leftPanel;
        private JPanel rightPanel;
        private JPanel bottomPanel;
        private JBLabel titleLabel;

        private JPanel generalSettingsPanel;
        private JPanel advancedSettingsPanel;
        private JPanel bindingsPanel;

        private JButton applyButton;
        private JButton cancelButton;

        private Map<String, Boolean> changedSettings;

        public FunctionDeploymentSettings() {
                // Set up the main frame
                setTitle("Function Configuration");
                setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                // setSize(1000, 800);
                setMinimumSize(new Dimension(1000, 800));
                setLocationRelativeTo(null);

                // Create and configure the main panel
                mainPanel = new JPanel();
                mainPanel.setLayout(new BorderLayout());
                setContentPane(mainPanel);

                // Create and configure the left panel
                leftPanel = new JPanel();
                leftPanel.setLayout(new BorderLayout());
                // leftPanel.setMaximumSize(new Dimension(200, 800));
                // leftPanel.setPreferredSize(new Dimension(200, 800));
                // leftPanel.setForeground(JBColor.BLACK);
                leftPanel.setBackground(new Color(62, 67, 76, 255)); // Jetbrains Darcula background color

                // Add components to the left panel
                titleLabel = new JBLabel("Function Configuration");
                titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
                titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                leftPanel.add(titleLabel, BorderLayout.NORTH);

                String[] settings = {
                                "General", // General settings
                                "Settings", // Advanced settings
                                "Binding Types" // Binding settings
                };
                JList<String> settingsList = new JList<>(settings);
                settingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                settingsList.setSelectedIndex(0);
                settingsList.setBackground(new Color(62, 67, 76, 255)); // Jetbrains Darcula background color

                settingsList.setCellRenderer(new DefaultListCellRenderer() {
                        @Override
                        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                        boolean isSelected, boolean cellHasFocus) {
                                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                setHorizontalAlignment(SwingConstants.LEFT);
                                setForeground(Color.WHITE);
                                if (isSelected) {
                                        setBackground(new Color(98, 181, 229)); // Jetbrains Darcula selection color
                                } else {
                                        setBackground(new Color(62, 67, 76, 255)); // Jetbrains Darcula background color
                                }
                                setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0)); // Add left padding
                                return this;
                        }
                });
                JBScrollPane leftScrollPane = new JBScrollPane(settingsList);
                leftScrollPane.setBorder(BorderFactory.createEmptyBorder());
                leftPanel.add(leftScrollPane, BorderLayout.CENTER);

                // Create and configure the right panel
                rightPanel = new JPanel(new CardLayout());
                // rightPanel.setMaximumSize(new Dimension(800, 800));
                // rightPanel.setPreferredSize(new Dimension(800, 800));

                // Add components to the right panel
                GeneralSettings generalSettings = new GeneralSettings();
                generalSettingsPanel = generalSettings.getPanel();
                JPanel generalSettingsWrapper = new JPanel(new BorderLayout());
                generalSettingsWrapper.add(generalSettingsPanel, BorderLayout.NORTH);
                generalSettingsWrapper.setBorder(new EmptyBorder(20, 20, 20, 20));
                generalSettingsWrapper.setMinimumSize(new Dimension(800, 800));
                rightPanel.add(generalSettingsWrapper, "General");

                // Settings panel
                AdvancedSettings advancedSettings = new AdvancedSettings();
                advancedSettingsPanel = advancedSettings.getPanel();
                JPanel advancedSettingsWrapper = new JPanel(new BorderLayout());
                advancedSettingsWrapper.add(advancedSettingsPanel, BorderLayout.NORTH);
                advancedSettingsWrapper.setBorder(new EmptyBorder(20, 20, 20, 20));
                advancedSettingsWrapper.setMinimumSize(new Dimension(800, 800));
                rightPanel.add(advancedSettingsWrapper, "Settings");

                // Binding Types panel
                BindingSettings bindingSettings = new BindingSettings();
                bindingsPanel = bindingSettings.getPanel();
                JPanel bindingsWrapper = new JPanel(new BorderLayout());
                bindingsWrapper.add(bindingsPanel, BorderLayout.NORTH);
                bindingsWrapper.setMinimumSize(new Dimension(800, 800));
                rightPanel.add(new JScrollPane(bindingsWrapper), "Binding Types"); // No need to wrap this panel

                settingsList.addListSelectionListener(e -> {
                        if (!e.getValueIsAdjusting()) {
                                String selectedValue = settingsList.getSelectedValue();
                                CardLayout cardLayout = (CardLayout) rightPanel.getLayout();
                                cardLayout.show(rightPanel, selectedValue);
                        }
                });

                // Create and configure the bottom panel
                bottomPanel = new JPanel();
                bottomPanel.setLayout(new BorderLayout());
                bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add a margin of 10 pixels around the panel

                // Create a new panel with a FlowLayout
                JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

                // Add components to the bottom panel
                cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(e -> {
                        // Discard changes
                        dispose();
                });
                buttonsPanel.add(cancelButton);

                applyButton = new JButton("Apply");
                applyButton.addActionListener(e -> {
                        // Save changes
                        dispose();
                });
                buttonsPanel.add(applyButton);

                // Add the buttons panel to the EAST region of the bottom panel
                bottomPanel.add(buttonsPanel, BorderLayout.EAST);

                // Add the panels to the main panel with a splitter in the middle
                JBSplitter splitter = new JBSplitter(false);
                splitter.setFirstComponent(leftPanel);
                splitter.setSecondComponent(rightPanel);
                splitter.setDividerWidth(1);
                splitter.getDivider().setBackground(new Color(62, 67, 76, 255));
                mainPanel.add(splitter, BorderLayout.CENTER);

                mainPanel.add(bottomPanel, BorderLayout.SOUTH);

                // Add a window listener to handle unsaved changes
                addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                                // Check for unsaved changes
                                if (changedSettings.values().stream().anyMatch(changed -> changed)) {
                                        int result = JOptionPane.showConfirmDialog(FunctionDeploymentSettings.this,
                                                        "You have unsaved changes. Do you want to save them before closing?",
                                                        "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
                                        if (result == JOptionPane.YES_OPTION) {
                                                // Save changes
                                                dispose();
                                        } else if (result == JOptionPane.NO_OPTION) {
                                                // Discard changes
                                                dispose();
                                        }
                                } else {
                                        dispose();
                                }
                        }
                });

                // Initialize the changed settings map
                changedSettings = new HashMap<>();
                changedSettings.put("General", false);
                changedSettings.put("Settings", false);
                changedSettings.put("Binding Types", false);

                // Add document listeners to track changes
                DocumentListener documentListener = new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                changedSettings.put("General", true);
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                changedSettings.put("General", true);
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                changedSettings.put("General", true);
                        }
                };
                // functionNameField.getDocument().addDocumentListener(documentListener);
                // descriptionTextArea.getDocument().addDocumentListener(documentListener);

                // ActionListener actionListener = e -> changedSettings.put("General",
                // true);
                // deploymentFeedBoundaryComboBox.addActionListener(actionListener);

        }

}