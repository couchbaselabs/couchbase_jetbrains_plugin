
package com.couchbase.intellij.eventing.settings;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.text.NumberFormatter;

import com.couchbase.intellij.eventing.components.HelpIcon;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

public class AdvancedSettings {

        private JPanel advancedSettingsPanel;
        private JPanel systemLogLevelPanel;
        private JPanel n1qlConsistencyPanel;
        private JPanel workersPanel;
        private JPanel languageCompatibilityPanel;
        private JPanel scriptTimeoutPanel;
        private JPanel timerContextMaxSizePanel;
        private JPanel errorPanel;

        private JBLabel systemLogLevelLabel;
        private JBLabel n1qlConsistencyLabel;
        private JBLabel workersLabel;
        private JBLabel languageCompatibilityLabel;
        private JBLabel scriptTimeoutLabel;
        private JBLabel timerContextMaxSizeLabel;

        private JComboBox<String> systemLogLevelComboBox;
        private JComboBox<String> n1qlConsistencyComboBox;
        private JComboBox<String> languageCompatibilityComboBox;

        private JFormattedTextField workersField;
        private JFormattedTextField scriptTimeoutField;
        private JFormattedTextField timerContextMaxSizeField;

        public AdvancedSettings() {

                // Advanced Settings panel
                advancedSettingsPanel = new JPanel();
                advancedSettingsPanel.setLayout(new GridBagLayout());

                GridBagConstraints advancedSettingsPanelConstraints = new GridBagConstraints();
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 0;
                advancedSettingsPanelConstraints.weightx = 1;

                advancedSettingsPanelConstraints.insets = JBUI.insets(5, 5, 5, 5);
                advancedSettingsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
                advancedSettingsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;

                // Add components to the Advanced Settings panel
                systemLogLevelLabel = new JBLabel("System Log Level");
                systemLogLevelLabel.setFont(systemLogLevelLabel.getFont().deriveFont(Font.BOLD));

                // Add both the labels to a panel
                systemLogLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                systemLogLevelPanel.add(systemLogLevelLabel);
                systemLogLevelPanel
                                .add(HelpIcon.createHelpIcon("Granularity of system events being captured in the log"));
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 0;
                advancedSettingsPanelConstraints.insets = JBUI.insets(5, 2, 2, 2); // Change the top inset to be larger
                advancedSettingsPanel.add(systemLogLevelPanel, advancedSettingsPanelConstraints);

                systemLogLevelComboBox = new JComboBox<>();
                systemLogLevelComboBox.addItem("Info");
                systemLogLevelComboBox.addItem("Error");
                systemLogLevelComboBox.addItem("Warning");
                systemLogLevelComboBox.addItem("Debug");
                systemLogLevelComboBox.addItem("Trace");
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 1;
                advancedSettingsPanelConstraints.insets = JBUI.insets(2, 2, 5, 2); // Change the bottom inset to be
                                                                                   // larger
                advancedSettingsPanel.add(systemLogLevelComboBox, advancedSettingsPanelConstraints);

                n1qlConsistencyLabel = new JBLabel("N1QL Consistency");
                n1qlConsistencyLabel.setFont(n1qlConsistencyLabel.getFont().deriveFont(Font.BOLD));

                // Add both the labels to a panel
                n1qlConsistencyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                n1qlConsistencyPanel.add(n1qlConsistencyLabel);
                n1qlConsistencyPanel
                                .add(HelpIcon.createHelpIcon("Consistency level of N1QL statements in the function:"));
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 3;
                advancedSettingsPanelConstraints.insets = JBUI.insets(5, 2, 2, 2); // Change the top inset to be larger
                advancedSettingsPanel.add(n1qlConsistencyPanel, advancedSettingsPanelConstraints);

                n1qlConsistencyComboBox = new JComboBox<>();
                n1qlConsistencyComboBox.addItem("None");
                n1qlConsistencyComboBox.addItem("Request");
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 4;
                advancedSettingsPanelConstraints.insets = JBUI.insets(2, 2, 5, 2); // Change the bottom inset to be
                                                                                   // larger
                advancedSettingsPanel.add(n1qlConsistencyComboBox, advancedSettingsPanelConstraints);

                workersLabel = new JBLabel("Workers");
                workersLabel.setFont(workersLabel.getFont().deriveFont(Font.BOLD));

                // Add both the labels to a panel
                workersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                workersPanel.add(workersLabel);
                workersPanel.add(HelpIcon.createHelpIcon("Number of workers per node to process the events:"));
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 5;
                advancedSettingsPanelConstraints.insets = JBUI.insets(5, 2, 2, 2); // Change the top inset to be larger
                advancedSettingsPanel.add(workersPanel, advancedSettingsPanelConstraints);

                NumberFormat format = NumberFormat.getInstance();
                NumberFormatter formatter = new NumberFormatter(format);
                formatter.setValueClass(Integer.class);
                formatter.setMinimum(0);
                formatter.setMaximum(Integer.MAX_VALUE);
                formatter.setAllowsInvalid(false);
                formatter.setCommitsOnValidEdit(true);

                workersField = new JFormattedTextField(formatter);
                workersField.setColumns(20);
                workersField.setValue(1);
                workersField.setToolTipText("Enter the number of workers per node to process the events.");
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 6;
                advancedSettingsPanelConstraints.gridwidth = 3;
                advancedSettingsPanelConstraints.insets = JBUI.insets(2, 2, 5, 2); // Change the bottom inset to be
                                                                                   // larger
                advancedSettingsPanel.add(workersField, advancedSettingsPanelConstraints);

                // Add a focus listener to the workersField
                workersField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                                validateForm(); // Validate the form for error generation
                                // Validate the worker count
                                if (!validateWorkerCount()) {
                                        // Worker count is invalid, so set the border and label to red
                                        workersField.setBorder(BorderFactory.createLineBorder(Color.decode("#FF4444")));
                                        workersLabel.setForeground(Color.decode("#FF4444"));
                                } else {
                                        // Worker count is valid, so set the border and label back to their normal state
                                        workersField.setBorder(UIManager.getBorder("TextField.border"));
                                        workersLabel.setForeground(UIManager.getColor("Label.foreground"));
                                }
                        }
                });

                languageCompatibilityLabel = new JBLabel("Language compatibility");
                languageCompatibilityLabel.setFont(languageCompatibilityLabel.getFont().deriveFont(Font.BOLD));

                // Add both the labels to a panel
                languageCompatibilityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                languageCompatibilityPanel.add(languageCompatibilityLabel);
                languageCompatibilityPanel.add(HelpIcon.createHelpIcon("Language compatibility of the function:"));
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 7;
                advancedSettingsPanelConstraints.insets = JBUI.insets(5, 2, 2, 2); // Change the top inset to be larger
                advancedSettingsPanel.add(languageCompatibilityPanel, advancedSettingsPanelConstraints);

                languageCompatibilityComboBox = new JComboBox<>();
                languageCompatibilityComboBox.addItem("6.0.0");
                languageCompatibilityComboBox.addItem("6.5.0");
                languageCompatibilityComboBox.addItem("6.6.2");
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 8;
                advancedSettingsPanelConstraints.insets = JBUI.insets(2, 2, 5, 2); // Change the bottom inset to be
                                                                                   // larger
                advancedSettingsPanel.add(languageCompatibilityComboBox, advancedSettingsPanelConstraints);

                scriptTimeoutLabel = new JBLabel("Script Timeout");
                scriptTimeoutLabel.setFont(scriptTimeoutLabel.getFont().deriveFont(Font.BOLD));

                // Add both the labels to a panel
                scriptTimeoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                scriptTimeoutPanel.add(scriptTimeoutLabel);
                scriptTimeoutPanel.add(HelpIcon
                                .createHelpIcon("Time after which the Function's execution will be timed out:"));
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 9;
                advancedSettingsPanelConstraints.insets = JBUI.insets(5, 2, 2, 2); // Change the top inset to be larger
                advancedSettingsPanel.add(scriptTimeoutPanel, advancedSettingsPanelConstraints);

                scriptTimeoutField = new JFormattedTextField(formatter);
                scriptTimeoutField.setColumns(20);
                scriptTimeoutField.setValue(60);
                scriptTimeoutField.setToolTipText(
                                "Enter the time after which the Function's execution will be timed out.");
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 10;
                advancedSettingsPanelConstraints.gridwidth = 3;
                advancedSettingsPanelConstraints.insets = JBUI.insets(2, 2, 5, 2); // Change the bottom inset to be
                                                                                   // larger
                advancedSettingsPanel.add(scriptTimeoutField, advancedSettingsPanelConstraints);

                // Add a focus listener to the scriptTimeoutField
                scriptTimeoutField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                                validateForm(); // Validate the form for error generation
                                // Validate the script timeout
                                if (!validateScriptTimeout()) {
                                        // Script timeout is invalid, so set the border and label to red
                                        scriptTimeoutField.setBorder(
                                                        BorderFactory.createLineBorder(Color.decode("#FF4444")));
                                        scriptTimeoutLabel.setForeground(Color.decode("#FF4444"));
                                } else {
                                        // Script timeout is valid, so set the border and label back to their normal
                                        // state
                                        scriptTimeoutField.setBorder(UIManager.getBorder("TextField.border"));
                                        scriptTimeoutLabel.setForeground(UIManager.getColor("Label.foreground"));
                                }
                        }
                });

                timerContextMaxSizeLabel = new JBLabel("Timer Context Max Size");
                timerContextMaxSizeLabel.setFont(timerContextMaxSizeLabel.getFont().deriveFont(Font.BOLD));

                // Add both the labels to a panel
                timerContextMaxSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                timerContextMaxSizePanel.add(timerContextMaxSizeLabel);
                timerContextMaxSizePanel.add(HelpIcon.createHelpIcon(
                                "Timer Context Size in Bytes. If the size of the Timer Context exceeds this value, the Timer Context will be cleared"));
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 11;
                advancedSettingsPanelConstraints.insets = JBUI.insets(5, 2, 2, 2); // Change the top inset to be larger
                advancedSettingsPanel.add(timerContextMaxSizePanel, advancedSettingsPanelConstraints);

                timerContextMaxSizeField = new JFormattedTextField(formatter);
                timerContextMaxSizeField.setColumns(20);
                timerContextMaxSizeField.setValue(1024);
                timerContextMaxSizeField
                                .setToolTipText("Enter the maximum allowed value of the Timer Context Size in Bytes.");
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 12;
                advancedSettingsPanelConstraints.insets = JBUI.insets(2, 2, 5, 2); // Change the bottom inset to be
                                                                                   // larger
                advancedSettingsPanel.add(timerContextMaxSizeField, advancedSettingsPanelConstraints);

                // Add a focus listener to the timerContextMaxSizeField
                timerContextMaxSizeField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                                validateForm(); // Validate the form for error generation
                                // Validate the timer context max size
                                if (!validateTimerContextMaxSize()) {
                                        // Timer context max size is invalid, so set the border and label to red
                                        timerContextMaxSizeField.setBorder(
                                                        BorderFactory.createLineBorder(Color.decode("#FF4444")));
                                        timerContextMaxSizeLabel.setForeground(Color.decode("#FF4444"));
                                } else {
                                        // Timer context max size is valid, so set the border and label back to their
                                        // normal state
                                        timerContextMaxSizeField.setBorder(UIManager.getBorder("TextField.border"));
                                        timerContextMaxSizeLabel.setForeground(UIManager.getColor("Label.foreground"));
                                }
                        }
                });

                errorPanel = new JPanel();
                errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));

                // Add the error panel to the advanced settings panel
                advancedSettingsPanelConstraints.gridx = 0;
                advancedSettingsPanelConstraints.gridy = 13;
                advancedSettingsPanelConstraints.gridwidth = 3;
                advancedSettingsPanel.add(errorPanel, advancedSettingsPanelConstraints);

        }

        public JPanel getPanel() {
                return advancedSettingsPanel;
        }

        private List<String> validateForm() {
                List<String> errors = new ArrayList<>();

                // Validate function name
                if (!validateWorkerCount()) {
                        errors.add("Workers count should be a positive integer");
                }

                if (!validateScriptTimeout()) {
                        errors.add("Script timeout should be a positive integer");
                }

                if (!validateTimerContextMaxSize()) {
                        errors.add("Timer context max size should be a positive integer");
                }

                // Update the error panel to display the validation errors
                errorPanel.removeAll();
                for (String error : errors) {
                        JLabel errorLabel = new JLabel(error);
                        errorLabel.setForeground(Color.decode("#FF4444"));
                        errorPanel.add(errorLabel);
                }
                errorPanel.revalidate();
                errorPanel.repaint();

                return errors;
        }

        private boolean validateWorkerCount() {

                String workerCount = workersField.getText();
                // remove commas from the string
                workerCount = workerCount.replaceAll(",", "");
                if (workerCount == null || workerCount.isEmpty()) {
                        return false;
                }
                try {
                        int count = Integer.parseInt(workerCount);
                        if (count < 1) {
                                return false;
                        }
                } catch (NumberFormatException e) {
                        return false;
                }
                return true;
        }

        private boolean validateScriptTimeout() {
                String scriptTimeout = scriptTimeoutField.getText();
                // remove commas from the string
                scriptTimeout = scriptTimeout.replaceAll(",", "");
                if (scriptTimeout == null || scriptTimeout.isEmpty()) {
                        return false;
                }
                try {
                        int timeout = Integer.parseInt(scriptTimeout);
                        if (timeout < 1) {
                                return false;
                        }
                } catch (NumberFormatException e) {
                        return false;
                }
                return true;
        }

        private boolean validateTimerContextMaxSize() {
                String timerContextMaxSize = timerContextMaxSizeField.getText();
                // remove commas from the string
                timerContextMaxSize = timerContextMaxSize.replaceAll(",", "");
                if (timerContextMaxSize == null || timerContextMaxSize.isEmpty()) {
                        return false;
                }
                try {
                        int maxSize = Integer.parseInt(timerContextMaxSize);
                        if (maxSize < 1) {
                                return false;
                        }
                } catch (NumberFormatException e) {
                        return false;
                }
                return true;
        }

}
