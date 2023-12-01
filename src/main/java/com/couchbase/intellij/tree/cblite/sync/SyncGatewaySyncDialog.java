package com.couchbase.intellij.tree.cblite.sync;

import com.couchbase.intellij.tools.dialog.JComboCheckBox;
import com.couchbase.intellij.tree.cblite.ActiveCBLDatabase;
import com.couchbase.intellij.workbench.Log;
import com.couchbase.lite.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class SyncGatewaySyncDialog extends DialogWrapper {

    public static final String PUSH_AND_PULL = "Push and Pull";
    public static final String PULL = "Pull";
    public static final String PUSH = "Push";
    private JTextField syncGatewayUrlTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordTextField;
    private ComboBox<String> replicationTypeComboBox;
    private JSpinner heartbeatSpinner;
    private JCheckBox continuousCheckBox;
    private JCheckBox autoPurgeCheckBox;
    private JLabel errorLabel;
    private JPanel collectionPickerPanel;
    private JComboCheckBox colCombo;
    private Replicator replicator;
    private Database database;
    private JComboCheckBox scopeCombo;
    private SyncGatewayTablePanel syncResult;
    private JBRadioButton defaultScopeButton;
    private JBRadioButton allScopesButton;
    private JBRadioButton selectCollectionsButton;
    private JBTabbedPane tabbedPane;
    private final Project project;
    private final Tree tree;

    private final String disconnectListenerId;

    public SyncGatewaySyncDialog(@Nullable Project project, Tree tree) {
        super(project, false);
        setModal(false);
        this.project = project;
        this.tree = tree;
        this.disconnectListenerId = "sync_test" + UUID.randomUUID();
        ActiveCBLDatabase.getInstance().addDisconnectListener(disconnectListenerId, () -> this.close(0));

        init();
        setTitle("Sync Gateway Test");
        getPeer().getWindow().setMinimumSize(new Dimension(760, 600));
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        ActiveCBLDatabase.getInstance().removeDisconnectListener(this.disconnectListenerId);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(760, 600);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {


        JPanel configPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = JBUI.insets(4);
        gbc.gridx = 0;
        gbc.gridy = 0;

        addLabeledComponent(configPanel, gbc, "Sync Gateway URL", syncGatewayUrlTextField = new JTextField());
        addLabeledComponent(configPanel, gbc, "Username", usernameTextField = new JTextField());
        addLabeledComponent(configPanel, gbc, "Password", passwordTextField = new JPasswordField());

        replicationTypeComboBox = new ComboBox<>();
        replicationTypeComboBox.addItem(PUSH_AND_PULL);
        replicationTypeComboBox.addItem(PULL);
        replicationTypeComboBox.addItem(PUSH);

        addLabeledComponent(configPanel, gbc, "Replication Type", replicationTypeComboBox);

        heartbeatSpinner = new JSpinner(new SpinnerNumberModel(60, 1, Integer.MAX_VALUE, 1));
        addLabeledComponent(configPanel, gbc, "Heartbeat", heartbeatSpinner);

        continuousCheckBox = new JCheckBox("Continuous Sync");
        continuousCheckBox.setSelected(true);
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0.6;
        configPanel.add(continuousCheckBox, gbc);


        autoPurgeCheckBox = new JCheckBox("Auto purge");
        autoPurgeCheckBox.setSelected(true);
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0.6;
        configPanel.add(autoPurgeCheckBox, gbc);


        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        configPanel.add(createCollapsibleCollectionPanel(), gbc);


        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        syncResult = new SyncGatewayTablePanel(project, tree);
        tabbedPane = new JBTabbedPane();

        errorLabel = new JLabel();
        errorLabel.setForeground(Color.decode("#FF4444"));
        JPanel firstPanel = new JPanel(new BorderLayout());
        firstPanel.add(configPanel, BorderLayout.NORTH);
        tabbedPane.addTab("Configuration", firstPanel);
        tabbedPane.addTab("Sync Log", syncResult);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(errorLabel, BorderLayout.SOUTH);

        return mainPanel;
    }


    private JPanel createCollapsibleCollectionPanel() {
        JPanel advancedPanel = new JPanel(new BorderLayout());

        TitledSeparator titledSeparator = new TitledSeparator("Target Sync Collections");
        titledSeparator.setBorder(JBUI.Borders.emptyBottom(15));
        advancedPanel.add(titledSeparator, BorderLayout.NORTH);
        advancedPanel.add(getCollectionOptions(), BorderLayout.CENTER);
        advancedPanel.setBorder(JBUI.Borders.emptyTop(5));

        return advancedPanel;
    }

    private void addLabeledComponent(JPanel mainPanel, GridBagConstraints gbc, String labelText, JComponent component) {
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.4;
        mainPanel.add(new JLabel(labelText), gbc);

        gbc.gridx++;
        gbc.weightx = 0.6;
        mainPanel.add(component, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
    }


    public JPanel getCollectionOptions() {

        try {
            colCombo = new JComboCheckBox();
            colCombo.setEnabled(false);

            scopeCombo = new JComboCheckBox();
            scopeCombo.setHint("Select one or more items");

            ActiveCBLDatabase.getInstance().getDatabase().getScopes().forEach(item -> scopeCombo.addItem(item.getName()));

            scopeCombo.setItemListener(e -> {

                colCombo.removeAllItemsAndCreateNewComboBox();
                if (e.isEmpty()) {
                    colCombo.setHint("");
                    colCombo.setEnabled(false);
                    return;
                }

                colCombo.setHint("Select one or more items");
                colCombo.setEnabled(true);

                Set<Scope> scopes;
                try {
                    scopes = ActiveCBLDatabase.getInstance().getDatabase().getScopes();

                    java.util.List<String> partial = new ArrayList<>();

                    for (Scope scope : scopes) {
                        if (e.contains(scope.getName())) {
                            for (Collection colSpec : Objects.requireNonNull(ActiveCBLDatabase.getInstance().getDatabase().getScope(scope.getName())).getCollections()) {
                                partial.add(scope.getName() + "." + colSpec.getName());
                            }
                        }
                    }
                    java.util.List<String> all = new ArrayList<>(partial);
                    all.forEach(item -> colCombo.addItem(item));

                } catch (CouchbaseLiteException ex) {
                    Log.error("Ann error occurred while trying to load scopes", ex);
                }
            });


            collectionPickerPanel = new JBPanel<>(new BorderLayout());
            JPanel radioPanel = new JBPanel<>(new GridLayout(0, 1));
            collectionPickerPanel.add(radioPanel, BorderLayout.NORTH);

            ButtonGroup group = new ButtonGroup();
            defaultScopeButton = new JBRadioButton("Use the default scope and collection");
            defaultScopeButton.setSelected(true);
            allScopesButton = new JBRadioButton("All scopes and collections");
            selectCollectionsButton = new JBRadioButton("Select the collections that will be synchronized");


            group.add(defaultScopeButton);
            group.add(allScopesButton);
            group.add(selectCollectionsButton);

            radioPanel.add(defaultScopeButton);
            radioPanel.add(allScopesButton);
            radioPanel.add(selectCollectionsButton);

            JPanel detailPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.insets = JBUI.insets(4);
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.gridwidth = 1;
            detailPanel.add(new JLabel("Select the scope(s)"), gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            detailPanel.add(scopeCombo, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 1;
            gbc.gridwidth = 1;
            detailPanel.add(new JLabel("Select the collection(s)"), gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            detailPanel.add(colCombo, gbc);

            detailPanel.setVisible(false);
            detailPanel.setBorder(JBUI.Borders.emptyTop(15));


            ActionListener showHidePanelListener = e -> {
                boolean selected = selectCollectionsButton.isSelected();
                detailPanel.setVisible(selected);
                collectionPickerPanel.revalidate();
            };

            defaultScopeButton.addActionListener(showHidePanelListener);
            allScopesButton.addActionListener(showHidePanelListener);
            selectCollectionsButton.addActionListener(showHidePanelListener);

            collectionPickerPanel.add(detailPanel, BorderLayout.CENTER);

            return collectionPickerPanel;
        } catch (CouchbaseLiteException e) {
            Log.error("An error occurred while loading the scopes and collections.");
            return new JPanel();
        }
    }

    @Override
    protected void dispose() {
        super.dispose();
    }


    @Override
    protected Action @NotNull [] createActions() {
        Action startSyncAction = new AbstractAction("Start Sync") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) e.getSource();
                if ("Start Sync".equals(button.getText())) {

                    java.util.List<String> errors = validateForm();

                    if (!errors.isEmpty()) {
                        String errorMsg = String.join("<br>", errors);
                        errorLabel.setText("<html>" + errorMsg + "</html>");
                        return;
                    } else {
                        errorLabel.setText("");
                    }

                    syncResult.clearTable();

                    try {
                        DatabaseConfiguration config = new DatabaseConfiguration();
                        config.setDirectory(ActiveCBLDatabase.getInstance().getSavedDatabase().getPath());
                        database = new Database(ActiveCBLDatabase.getInstance().getSavedDatabase().getName(), config);

                        java.util.List<Collection> collectionList = loadTargetCollections();

                        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(
                                new URLEndpoint(new URI(syncGatewayUrlTextField.getText().trim()))).addCollections(collectionList, null)
                                .setType(getReplicatorType())
                                .setContinuous(continuousCheckBox.isSelected())
                                .setAutoPurgeEnabled(autoPurgeCheckBox.isSelected())
                                .setAuthenticator(new BasicAuthenticator(usernameTextField.getText().trim(), String.valueOf(passwordTextField.getPassword()).toCharArray()));


                        replicator = new Replicator(replConfig);

                        replicator.addChangeListener(change -> {
                            CouchbaseLiteException err = change.getStatus().getError();
                            if (err != null) {
                                Log.error("Error code :: " + err.getCode(), err);
                            } else {
                                syncResult.updateSyncStatus(change.getStatus().getActivityLevel().toString());
                            }
                        });

                        replicator.addDocumentReplicationListener(replication -> {
                            boolean isPush = replication.isPush();

                            for (ReplicatedDocument document : replication.getDocuments()) {
                                String docID = document.getID();

                                boolean isDeletion = document.getFlags().contains(DocumentFlag.DELETED);
                                boolean isAccessRemoved = document.getFlags().contains(DocumentFlag.ACCESS_REMOVED);

                                CouchbaseLiteException error = document.getError();
                                if (error != null) {
                                    Log.error("Error replicating document: " + docID + ", error: " + error);
                                } else {
                                    syncResult.addEntry(new JsonEntry(docID, isPush, isDeletion, document.getCollectionScope(), document.getCollectionName(), isAccessRemoved));
                                }
                            }
                        });

                        replicator.start(true);
                        disableAllFields();

                        button.setText("Stop Sync");
                    } catch (Exception ex) {
                        Log.error(ex);
                        stopSync(button);

                    }
                } else {
                    stopSync(button);
                }
            }

            @NotNull
            private java.util.List<Collection> loadTargetCollections() throws CouchbaseLiteException {
                java.util.List<Collection> collectionList = new ArrayList<>();

                java.util.List<String> scopeNames = new ArrayList<>();
                java.util.List<String> collectionNames = new ArrayList<>();

                if (defaultScopeButton.isSelected()) {
                    collectionList.add(database.getDefaultCollection());
                    scopeNames.add("_default");
                    collectionNames.add("_default._default");

                } else if (allScopesButton.isSelected()) {

                    for (Scope scope : database.getScopes()) {
                        scopeNames.add(scope.getName());
                        Set<Collection> cols = Objects.requireNonNull(database.getScope(scope.getName())).getCollections();
                        cols.forEach(ce -> collectionNames.add(scope.getName() + "." + ce.getName()));
                        collectionList.addAll(cols);
                    }
                } else {
                    scopeNames.addAll(scopeCombo.getSelectedItems());
                    collectionNames.addAll(colCombo.getSelectedItems());

                    for (String col : colCombo.getSelectedItems()) {
                        String[] val = col.split("\\.");
                        collectionList.add(Objects.requireNonNull(database.getScope(val[0])).getCollection(val[1]));
                    }
                }

                syncResult.setScopes(scopeNames);
                syncResult.setCollections(collectionNames);
                return collectionList;
            }

            private void stopSync(JButton button) {
                syncResult.updateSyncStatus("Disconnected");

                try {
                    if (replicator != null) {
                        replicator.stop();
                    }
                    if (database != null) {
                        database.close();
                    }

                    enableAllFields();

                } catch (CouchbaseLiteException ex) {
                    throw new RuntimeException(ex);
                }

                button.setText("Start Sync");
            }

            @NotNull
            private ReplicatorType getReplicatorType() {
                ReplicatorType replicatorType;
                if (PUSH_AND_PULL.equals(Objects.requireNonNull(replicationTypeComboBox.getSelectedItem()).toString())) {
                    replicatorType = ReplicatorType.PUSH_AND_PULL;
                } else if (PUSH.equals(replicationTypeComboBox.getSelectedItem().toString())) {
                    replicatorType = ReplicatorType.PUSH;
                } else {
                    replicatorType = ReplicatorType.PULL;
                }
                return replicatorType;
            }
        };
        return new Action[]{startSyncAction, getCancelAction()};
    }

    private java.util.List<String> validateForm() {
        java.util.List<String> errors = new ArrayList<>();

        if (syncGatewayUrlTextField.getText().isBlank()) {
            errors.add("Inform the Sync Gateway URL");
        } else if (!(syncGatewayUrlTextField.getText().startsWith("ws://")
                || syncGatewayUrlTextField.getText().startsWith("wss://"))) {
            errors.add("The Sync Gateway URL must start with the protocol ws:// or wss://");
        }

        if (usernameTextField.getText().isBlank()) {
            errors.add("Inform the username");
        }

        if (usernameTextField.getText().isBlank()) {
            errors.add("Inform the password");
        }

        if (selectCollectionsButton.isSelected()) {

            if (scopeCombo.getSelectedItems().isEmpty()) {
                errors.add("Select the scope(s) that you would like to sync");
            }

            if (colCombo.getSelectedItems().isEmpty()) {
                errors.add("Select the collection(s) that you would like to sync");
            }
        }

        return errors;
    }

    private void disableAllFields() {
        syncGatewayUrlTextField.setEnabled(false);
        usernameTextField.setEnabled(false);
        passwordTextField.setEnabled(false);
        replicationTypeComboBox.setEnabled(false);
        heartbeatSpinner.setEnabled(false);
        continuousCheckBox.setEnabled(false);
        defaultScopeButton.setEnabled(false);
        allScopesButton.setEnabled(false);
        selectCollectionsButton.setEnabled(false);
        colCombo.setEnabled(false);
        scopeCombo.setEnabled(false);
        tabbedPane.setSelectedIndex(1);

    }

    private void enableAllFields() {
        syncGatewayUrlTextField.setEnabled(true);
        usernameTextField.setEnabled(true);
        passwordTextField.setEnabled(true);
        replicationTypeComboBox.setEnabled(true);
        heartbeatSpinner.setEnabled(true);
        continuousCheckBox.setEnabled(true);
        defaultScopeButton.setEnabled(true);
        allScopesButton.setEnabled(true);
        selectCollectionsButton.setEnabled(true);
        scopeCombo.setEnabled(true);
        if (selectCollectionsButton.isSelected()) {
            colCombo.setEnabled(true);
        }
        tabbedPane.setSelectedIndex(0);
    }
}