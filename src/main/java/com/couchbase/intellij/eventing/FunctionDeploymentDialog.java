package com.couchbase.intellij.eventing;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jetbrains.annotations.Nullable;

import com.couchbase.intellij.eventing.settings.AdvancedSettings;
import com.couchbase.intellij.eventing.settings.BindingSettings;
import com.couchbase.intellij.eventing.settings.GeneralSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

public class FunctionDeploymentDialog extends DialogWrapper {
        public Set<String> buckets;
        private JPanel mainPanel;

        private JPanel generalSettingsPanel;
        private JPanel advancedSettingsPanel;
        private JPanel bindingsPanel;

        private boolean advancedSettingsPanelExpanded = false;

        public FunctionDeploymentDialog(@Nullable Project project) {
                super(project);
                this.buckets = null;
                setTitle("Deploy as a Couchbase Function");
                setSize(1000, 1000);
                setResizable(true);
                init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
                mainPanel = new JPanel(new GridBagLayout());
                GridBagConstraints mainPanelConstraints = new GridBagConstraints();
                mainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;

                GeneralSettings generalSettings = new GeneralSettings();
                generalSettingsPanel = generalSettings.getPanel();

                mainPanelConstraints.gridx = 0;
                mainPanelConstraints.gridy = 0;
                mainPanel.add(generalSettingsPanel, mainPanelConstraints);

                // Settings label and expandable mainPanel
                // Add a toggle button for the settings mainPanel
                JLabel settingsToggleButton = new JLabel("▶");
                settingsToggleButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                                // Toggle the expanded state of the settings mainPanel
                                advancedSettingsPanelExpanded = !advancedSettingsPanelExpanded;

                                // Show or hide the settings mainPanel based on its expanded state
                                if (advancedSettingsPanelExpanded) {
                                        settingsToggleButton.setText("▼");
                                        advancedSettingsPanel.setVisible(true);
                                } else {
                                        settingsToggleButton.setText("▶");
                                        advancedSettingsPanel.setVisible(false);
                                }
                        }
                });

                // TODO: Add label and expandable mainPanel for settings
                JLabel settingsLabel = new JLabel("Settings");
                settingsLabel.setFont(settingsLabel.getFont().deriveFont(Font.BOLD));
                settingsLabel.setToolTipText("Click to expand/collapse the Settings section.");

                // Add both the toggle button and the label to a FlowLayout
                JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                settingsPanel.add(settingsToggleButton);
                settingsPanel.add(settingsLabel);
                mainPanelConstraints.gridx = 0;
                mainPanelConstraints.gridy = 17;
                mainPanel.add(settingsPanel, mainPanelConstraints);

                AdvancedSettings advancedSettings = new AdvancedSettings();
                advancedSettingsPanel = advancedSettings.getPanel();
                advancedSettingsPanel.setVisible(false);

                mainPanelConstraints.gridx = 0;
                mainPanelConstraints.gridy = 18;
                mainPanel.add(advancedSettingsPanel, mainPanelConstraints);

                // Binding Type label and expandable mainPanel
                // TODO: Add label and expandable mainPanel for binding type
                BindingSettings bindingSettings = new BindingSettings();
                bindingsPanel = bindingSettings.getPanel();

                mainPanelConstraints.gridx = 0;
                mainPanelConstraints.gridy = 19;
                mainPanel.add(bindingsPanel, mainPanelConstraints);

                return new JScrollPane(mainPanel);

        }

}
