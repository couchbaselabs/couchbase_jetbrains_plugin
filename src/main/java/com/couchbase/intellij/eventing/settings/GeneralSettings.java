package com.couchbase.intellij.eventing.settings;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.intellij.database.ActiveCluster;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;

public class GeneralSettings {

    private JPanel generalSettingsPanel;
    private JPanel functionScopePanel;
    private JPanel listenToLocationPanel;
    private JPanel eventingStoragePanel;

    private JBLabel functionScopeLabel;
    private JBLabel functionScopeInfoLabel;
    private JBLabel helpLabel1;
    private JBLabel listenToLocationLabel;
    private JBLabel listenToLocationInfoLabel;
    private JBLabel helpLabel2;
    private JBLabel eventingStorageLabel;
    private JBLabel eventingStorageInfoLabel;
    private JBLabel helpLabel3;
    private JBLabel functionNameLabel;
    private JBLabel deploymentFeedBoundaryLabel;
    private JBLabel warningLabel;
    private JBLabel descriptionLabel;

    private JComboBox<String> functionScopeBucketComboBox;
    private JComboBox<String> functionScopeScopeComboBox;
    private JComboBox<String> listenToLocationBucketComboBox;
    private JComboBox<String> listenToLocationScopeComboBox;
    private JComboBox<String> listenToLocationCollectionComboBox;
    private JComboBox<String> eventingStorageBucketComboBox;
    private JComboBox<String> eventingStorageScopeComboBox;
    private JComboBox<String> eventingStorageCollectionComboBox;
    private JComboBox<String> deploymentFeedBoundaryComboBox;

    public GeneralSettings() {

        // Connect to the Couchbase cluster
        Cluster cluster = Cluster.connect("couchbase://localhost", "kaustav", "password");

        generalSettingsPanel = new JPanel();
        generalSettingsPanel.setLayout(new GridBagLayout());

        GridBagConstraints generalSettingsPanelConstraints = new GridBagConstraints();
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 0;
        generalSettingsPanelConstraints.weightx = 1.0;
        // generalSettingsPanelConstraints.weighty = 1.0;
        generalSettingsPanelConstraints.insets = new Insets(5, 5, 5, 5);
        generalSettingsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        generalSettingsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;

        // Function Scope label
        functionScopeLabel = new JBLabel("Function scope");
        functionScopeLabel.setFont(functionScopeLabel.getFont().deriveFont(Font.BOLD));

        functionScopeInfoLabel = new JBLabel("bucket.scope");
        functionScopeInfoLabel.setForeground(JBColor.GRAY);

        // Add both labels to a flow layout
        functionScopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        functionScopePanel.add(functionScopeLabel);
        functionScopePanel.add(functionScopeInfoLabel);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 0;
        generalSettingsPanel.add(functionScopePanel, generalSettingsPanelConstraints);

        functionScopeBucketComboBox = new JComboBox<>();
        // functionScopeBucketComboBox.addActionListener(e -> updateScopes());
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 1;
        generalSettingsPanel.add(functionScopeBucketComboBox, generalSettingsPanelConstraints);

        functionScopeScopeComboBox = new JComboBox<>();
        functionScopeScopeComboBox.setEnabled(false);
        generalSettingsPanelConstraints.gridx = 1;
        generalSettingsPanelConstraints.gridy = 1;
        generalSettingsPanel.add(functionScopeScopeComboBox, generalSettingsPanelConstraints);
        // Function Scope error label
        helpLabel1 = new JBLabel(
                "Please specify a scope to which the function belongs. User should have Eventing Manage Scope Functions permission on this scope");
        helpLabel1.setForeground(JBColor.GRAY);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 2;
        generalSettingsPanelConstraints.gridwidth = 3;
        generalSettingsPanel.add(helpLabel1, generalSettingsPanelConstraints);

        // Listen to location label
        listenToLocationLabel = new JBLabel("Listen to location");

        listenToLocationLabel.setFont(listenToLocationLabel.getFont().deriveFont(Font.BOLD));
        // generalSettingsPanelConstraints.gridx = 0;
        // generalSettingsPanelConstraints.gridy = 3;
        // generalSettingsPanel.add(listenToLocationLabel,
        // generalSettingsPanelConstraints);

        listenToLocationInfoLabel = new JBLabel("bucket.scope.collection");
        listenToLocationInfoLabel.setForeground(JBColor.GRAY);
        // generalSettingsPanelConstraints.gridx = 1;
        // generalSettingsPanelConstraints.gridy = 3;
        // generalSettingsPanel.add(listenToLocationInfoLabel,
        // generalSettingsPanelConstraints);

        // Add both labels to a flow layout
        listenToLocationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listenToLocationPanel.add(listenToLocationLabel);
        listenToLocationPanel.add(listenToLocationInfoLabel);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 3;
        generalSettingsPanel.add(listenToLocationPanel, generalSettingsPanelConstraints);

        listenToLocationBucketComboBox = new JComboBox<>();
        // listenToLocationBucketComboBox.addActionListener(e -> updateScopes());
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 4;
        generalSettingsPanelConstraints.gridwidth = 1;
        generalSettingsPanel.add(listenToLocationBucketComboBox, generalSettingsPanelConstraints);

        listenToLocationScopeComboBox = new JComboBox<>();
        listenToLocationScopeComboBox.setEnabled(false);
        generalSettingsPanelConstraints.gridx = 1;
        generalSettingsPanelConstraints.gridy = 4;
        generalSettingsPanel.add(listenToLocationScopeComboBox, generalSettingsPanelConstraints);

        listenToLocationCollectionComboBox = new JComboBox<>();
        listenToLocationCollectionComboBox.setEnabled(false);
        generalSettingsPanelConstraints.gridx = 2;
        generalSettingsPanelConstraints.gridy = 4;
        generalSettingsPanel.add(listenToLocationCollectionComboBox, generalSettingsPanelConstraints);

        // Listen to location error label
        helpLabel2 = new JBLabel(
                "Please specify a source location for your function. User should have DCP Data Read permission on this keyspace");
        helpLabel2.setForeground(JBColor.GRAY);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 5;
        generalSettingsPanelConstraints.gridwidth = 3;

        generalSettingsPanel.add(helpLabel2, generalSettingsPanelConstraints);

        // Eventing Storage label
        eventingStorageLabel = new JBLabel("Eventing Storage");
        eventingStorageLabel.setFont(eventingStorageLabel.getFont().deriveFont(Font.BOLD));
        // generalSettingsPanelConstraints.gridx = 0;
        // generalSettingsPanelConstraints.gridy = 6;
        // generalSettingsPanel.add(eventingStorageLabel,
        // generalSettingsPanelConstraints);

        eventingStorageInfoLabel = new JBLabel("bucket.scope.collection");
        eventingStorageInfoLabel.setForeground(JBColor.GRAY);
        // generalSettingsPanelConstraints.gridx = 1;
        // generalSettingsPanelConstraints.gridy = 6;
        // generalSettingsPanelConstraints.gridwidth = 2;
        // generalSettingsPanel.add(eventingStorageInfoLabel,
        // generalSettingsPanelConstraints);

        // Add both labels to a flow layout
        eventingStoragePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        eventingStoragePanel.add(eventingStorageLabel);
        eventingStoragePanel.add(eventingStorageInfoLabel);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 6;
        generalSettingsPanel.add(eventingStoragePanel, generalSettingsPanelConstraints);

        eventingStorageBucketComboBox = new JComboBox<>();
        // eventingStorageBucketComboBox.addActionListener(e -> updateScopes());
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 7;
        generalSettingsPanelConstraints.gridwidth = 1;
        generalSettingsPanel.add(eventingStorageBucketComboBox, generalSettingsPanelConstraints);

        eventingStorageScopeComboBox = new JComboBox<>();
        eventingStorageScopeComboBox.setEnabled(false);
        generalSettingsPanelConstraints.gridx = 1;
        generalSettingsPanelConstraints.gridy = 7;
        generalSettingsPanel.add(eventingStorageScopeComboBox, generalSettingsPanelConstraints);

        eventingStorageCollectionComboBox = new JComboBox<>();
        eventingStorageCollectionComboBox.setEnabled(false);
        generalSettingsPanelConstraints.gridx = 2;
        generalSettingsPanelConstraints.gridy = 7;

        generalSettingsPanel.add(eventingStorageCollectionComboBox, generalSettingsPanelConstraints);

        // Eventing Storage help label
        helpLabel3 = new JBLabel(
                "Please specify a location to store Eventing data. User should have read/write permission on this keyspace");
        helpLabel3.setForeground(JBColor.GRAY);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 8;
        generalSettingsPanelConstraints.gridwidth = 3;
        generalSettingsPanel.add(helpLabel3, generalSettingsPanelConstraints);

        // Function Name label and text field
        functionNameLabel = new JBLabel("Function Name");
        functionNameLabel.setFont(functionNameLabel.getFont().deriveFont(Font.BOLD));
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 9;
        generalSettingsPanel.add(functionNameLabel, generalSettingsPanelConstraints);

        JBTextField functionNameField = new JBTextField(20);
        functionNameField.setToolTipText("Enter the name of the function.");
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 10;
        generalSettingsPanel.add(functionNameField, generalSettingsPanelConstraints);

        // Deployment Feed Boundary label and combo box
        deploymentFeedBoundaryLabel = new JBLabel("Deployment Feed Boundary");

        deploymentFeedBoundaryLabel.setFont(deploymentFeedBoundaryLabel.getFont().deriveFont(Font.BOLD));
        deploymentFeedBoundaryLabel.setToolTipText(
                "The preferred Deployment time Feed Boundary for the function.");
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 11;
        generalSettingsPanel.add(deploymentFeedBoundaryLabel, generalSettingsPanelConstraints);

        deploymentFeedBoundaryComboBox = new JComboBox<>();
        deploymentFeedBoundaryComboBox.addItem("Everything");
        deploymentFeedBoundaryComboBox.addItem("From now");
        deploymentFeedBoundaryComboBox.setToolTipText(
                "The preferred Deployment time Feed Boundary for the function.");

        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 12;
        generalSettingsPanelConstraints.gridwidth = 3;
        generalSettingsPanel.add(deploymentFeedBoundaryComboBox, generalSettingsPanelConstraints);

        warningLabel = new JBLabel("");
        warningLabel.setForeground(JBColor.ORANGE);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 13;
        generalSettingsPanel.add(warningLabel, generalSettingsPanelConstraints);

        deploymentFeedBoundaryComboBox.addActionListener(e -> {
            if (deploymentFeedBoundaryComboBox.getSelectedItem().equals("From now")) {
                warningLabel.setText(
                        "Warning: Deploying with 'From now' will ignore all past mutations.");
            } else {
                warningLabel.setText("");
            }
        });

        // Description label and text area
        descriptionLabel = new JBLabel("Description");

        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.BOLD));
        descriptionLabel.setToolTipText("Enter a description for the function (optional).");
        descriptionLabel.setDisplayedMnemonic('D');
        descriptionLabel.setDisplayedMnemonicIndex(0);
        descriptionLabel.setHorizontalAlignment(JBTextField.LEFT);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 14;
        generalSettingsPanel.add(descriptionLabel, generalSettingsPanelConstraints);

        JBTextArea descriptionTextArea = new JBTextArea(20, 20);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setToolTipText("Enter a description for the function (optional).");
        descriptionTextArea.setPreferredSize(new Dimension(200, 200));
        descriptionLabel.setLabelFor(descriptionTextArea);
        JBScrollPane descriptionScrollPane = new JBScrollPane(descriptionTextArea);
        generalSettingsPanelConstraints.gridx = 0;
        generalSettingsPanelConstraints.gridy = 15;
        generalSettingsPanelConstraints.fill = GridBagConstraints.BOTH;
        generalSettingsPanelConstraints.weighty = 1.0; // Set weighty to a non-zero value
        generalSettingsPanel.add(descriptionScrollPane, generalSettingsPanelConstraints);

        // Adding all the event listeners
        // Get the list of buckets in the cluster
        Map<String, BucketSettings> buckets = cluster.buckets().getAllBuckets();

        // Populate the bucket combo boxes
        functionScopeBucketComboBox.addItem("*");
        listenToLocationBucketComboBox.addItem("*");
        eventingStorageBucketComboBox.addItem("*");
        for (String bucketName : buckets.keySet()) {
            functionScopeBucketComboBox.addItem(bucketName);
            listenToLocationBucketComboBox.addItem(bucketName);
            eventingStorageBucketComboBox.addItem(bucketName);
        }

        // Add action listeners to the bucket combo boxes
        functionScopeBucketComboBox.addActionListener(e -> {
            // Get the selected bucket
            String selectedBucket = (String) functionScopeBucketComboBox.getSelectedItem();

            // Clear and repopulate the functionScopeScopeComboBox
            functionScopeScopeComboBox.removeAllItems();
            if (selectedBucket != null && !selectedBucket.equals("*")) {
                // Get the list of scopes for the selected bucket
                List<ScopeSpec> scopes = cluster.bucket(selectedBucket).collections().getAllScopes();

                functionScopeScopeComboBox.addItem("*");
                for (ScopeSpec scope : scopes) {
                    functionScopeScopeComboBox.addItem(scope.name());
                }

                // Enable the functionScopeScopeComboBox
                functionScopeScopeComboBox.setEnabled(true);
            } else {
                // Disable the functionScopeScopeComboBox
                functionScopeScopeComboBox.setEnabled(false);
            }
        });
        listenToLocationBucketComboBox.addActionListener(e -> {
            // Get the selected bucket
            String selectedBucket = (String) listenToLocationBucketComboBox.getSelectedItem();

            // Clear and repopulate the listenToLocationScopeComboBox
            listenToLocationScopeComboBox.removeAllItems();
            if (selectedBucket != null && !selectedBucket.equals("*")) {
                // Get the list of scopes for the selected bucket
                List<ScopeSpec> scopes = cluster.bucket(selectedBucket).collections().getAllScopes();

                listenToLocationScopeComboBox.addItem("*");
                for (ScopeSpec scope : scopes) {
                    listenToLocationScopeComboBox.addItem(scope.name());
                }

                // Enable the listenToLocationScopeComboBox
                listenToLocationScopeComboBox.setEnabled(true);
            } else {
                // Disable the listenToLocationScopeComboBox
                listenToLocationScopeComboBox.setEnabled(false);
            }
        });
        eventingStorageBucketComboBox.addActionListener(e -> {
            // Get the selected bucket
            String selectedBucket = (String) eventingStorageBucketComboBox.getSelectedItem();

            // Clear and repopulate the eventingStorageScopeComboBox
            eventingStorageScopeComboBox.removeAllItems();
            if (selectedBucket != null && !selectedBucket.equals("*")) {
                // Get the list of scopes for the selected bucket
                List<ScopeSpec> scopes = cluster.bucket(selectedBucket).collections().getAllScopes();

                eventingStorageScopeComboBox.addItem("*");
                for (ScopeSpec scope : scopes) {
                    eventingStorageScopeComboBox.addItem(scope.name());
                }

                // Enable the eventingStorageScopeComboBox
                eventingStorageScopeComboBox.setEnabled(true);
            } else {
                // Disable the eventingStorageScopeComboBox
                eventingStorageScopeComboBox.setEnabled(false);
            }
        });

        listenToLocationScopeComboBox.addActionListener(e -> {
            // Get the selected bucket and scope
            String selectedBucket = (String) listenToLocationBucketComboBox.getSelectedItem();
            String selectedScope = (String) listenToLocationScopeComboBox.getSelectedItem();

            // Clear and repopulate the listenToLocationCollectionComboBox
            listenToLocationCollectionComboBox.removeAllItems();
            if (selectedBucket != null && !selectedBucket.equals("*") && selectedScope != null
                    && !selectedScope.equals("*")) {
                // Get the list of collections for the selected scope
                Cluster activeCluster = ActiveCluster.getInstance().get();
                if (activeCluster != null) {
                    Bucket bucket = activeCluster.bucket(selectedBucket);
                    if (bucket != null) {
                        CollectionManager collectionManager = bucket.collections();
                        if (collectionManager != null) {
                            List<ScopeSpec> scopes = collectionManager.getAllScopes();
                            if (scopes != null) {
                                List<CollectionSpec> collections = scopes.stream()
                                        .filter(scope -> scope.name().equals(selectedScope))
                                        .flatMap(scope -> scope.collections().stream())
                                        .collect(Collectors.toList());

                                listenToLocationCollectionComboBox.addItem("*");
                                for (CollectionSpec collection : collections) {
                                    listenToLocationCollectionComboBox.addItem(collection.name());
                                }
                            }
                        }
                    }
                }

                // Enable the listenToLocationCollectionComboBox
                listenToLocationCollectionComboBox.setEnabled(true);
            } else {
                // Disable the listenToLocationCollectionComboBox
                listenToLocationCollectionComboBox.setEnabled(false);
            }
        });
        eventingStorageScopeComboBox.addActionListener(e -> {
            // Get the selected bucket and scope
            String selectedBucket = (String) eventingStorageBucketComboBox.getSelectedItem();
            String selectedScope = (String) eventingStorageScopeComboBox.getSelectedItem();

            // Clear and repopulate the eventingStorageCollectionComboBox
            eventingStorageCollectionComboBox.removeAllItems();
            if (selectedBucket != null && !selectedBucket.equals("*") && selectedScope != null
                    && !selectedScope.equals("*")) {
                // Get the list of collections for the selected scope
                Cluster activeCluster = ActiveCluster.getInstance().get();
                if (activeCluster != null) {
                    Bucket bucket = activeCluster.bucket(selectedBucket);
                    if (bucket != null) {
                        CollectionManager collectionManager = bucket.collections();
                        if (collectionManager != null) {
                            List<ScopeSpec> scopes = collectionManager.getAllScopes();
                            if (scopes != null) {
                                List<CollectionSpec> collections = scopes.stream()
                                        .filter(scope -> scope.name().equals(selectedScope))
                                        .flatMap(scope -> scope.collections().stream())
                                        .collect(Collectors.toList());

                                eventingStorageCollectionComboBox.addItem("*");
                                for (CollectionSpec collection : collections) {
                                    eventingStorageCollectionComboBox.addItem(collection.name());
                                }
                            }
                        }
                    }
                }

                // Enable the eventingStorageCollectionComboBox
                eventingStorageCollectionComboBox.setEnabled(true);
            } else {
                // Disable the eventingStorageCollectionComboBox
                eventingStorageCollectionComboBox.setEnabled(false);
            }
        });

        listenToLocationCollectionComboBox.addActionListener(e -> {
            // Get the selected bucket, scope, and collection
            String selectedBucket = (String) listenToLocationBucketComboBox.getSelectedItem();
            String selectedScope = (String) listenToLocationScopeComboBox.getSelectedItem();
            String selectedCollection = (String) listenToLocationCollectionComboBox.getSelectedItem();

            // Perform any necessary actions when the selected collection changes
            // ...

        });
        eventingStorageCollectionComboBox.addActionListener(e -> {
            // Get the selected bucket, scope, and collection
            String selectedBucket = (String) eventingStorageBucketComboBox.getSelectedItem();
            String selectedScope = (String) eventingStorageScopeComboBox.getSelectedItem();
            String selectedCollection = (String) eventingStorageCollectionComboBox.getSelectedItem();

            // Perform any necessary actions when the selected collection changes
            // ...
        });

    }

    public JPanel getPanel() {
        return generalSettingsPanel;
    }
}