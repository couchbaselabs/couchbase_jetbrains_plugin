package com.couchbase.intellij.tree;


import com.couchbase.intellij.database.DataLoader;
import com.couchbase.intellij.persistence.ClusterAlreadyExistsException;
import com.couchbase.intellij.persistence.DuplicatedClusterNameAndUserException;
import com.couchbase.intellij.persistence.SavedCluster;
import com.couchbase.intellij.tools.dialog.CollapsiblePanel;
import com.couchbase.intellij.tools.doctor.SDKDoctorTableCellRenderer;
import com.couchbase.intellij.tools.doctor.SdkDoctorRunner;
import com.couchbase.intellij.tree.node.ConnectionNodeDescriptor;
import com.couchbase.intellij.tree.overview.NewConnectionBanner;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import utils.TemplateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NewConnectionDialog extends DialogWrapper {

    private final Tree tree;
    private final SavedCluster savedCluster;
    JBTextField defaultBucketTextField;
    private JLabel errorLabel;
    private JLabel messageLabel;
    private JPanel thirdPanel;
    private JBTextField connectionNameTextField;
    private JBTextField hostTextField;
    private JCheckBox enableSSLCheckBox;
    private JBTextField usernameTextField;
    private JPasswordField passwordField;
    private JButton testConnectionButton;
    private JButton saveButton;
    private JBScrollPane consoleScrollPane;
    private JBTable eventLogTable;
    private Project project;
    private JBTextField apiKeyField;
    private JPasswordField apiSecretField;
    private JPanel wrapperPanel;

    private  DefaultMutableTreeNode clickedNode;

    public NewConnectionDialog(Project project, Tree tree, SavedCluster savedCluster, DefaultMutableTreeNode clickedNode) {
        super(false);
        this.tree = tree;
        this.project = project;
        this.savedCluster = savedCluster;
        this.clickedNode = clickedNode;
        createUIComponents();

        init();
        setTitle("New Couchbase Connection");
        setResizable(true);
        getPeer().getWindow().setMinimumSize(new Dimension(800, 600));
    }

    public static String getQueryParams(String url) {
        int index = url.indexOf('?');

        if (index == -1) {
            return "";
        }
        return url.substring(index);
    }

    public static String getBaseUrl(String url) {
        int index = url.indexOf('?');

        if (index == -1) {
            return url;
        }

        return url.substring(0, index);
    }

    @Override
    protected JComponent createCenterPanel() {
        return wrapperPanel;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }

    @Override
    protected JComponent createSouthPanel() {

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((ActionEvent e) -> close(DialogWrapper.CANCEL_EXIT_CODE));
        buttonPanel.add(cancelButton);

        // Test Connection Button
        testConnectionButton = new JButton("Test Connection");
        testConnectionButton.addActionListener((ActionEvent e) -> {
            handleTestConnection();
        });
        buttonPanel.add(testConnectionButton);

        saveButton = new JButton("Save");
        saveButton.addActionListener((ActionEvent e) -> {
            handleSaveConnection();
        });

        buttonPanel.add(saveButton);

        JPanel notificationPanel = new JPanel(new BorderLayout());
        errorLabel = new JLabel();
        errorLabel.setForeground(Color.decode("#FF4444"));
        messageLabel = new JLabel();
        messageLabel.setBorder(JBUI.Borders.emptyTop(5));
        notificationPanel.add(messageLabel, BorderLayout.NORTH);
        notificationPanel.add(errorLabel, BorderLayout.SOUTH);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(notificationPanel, BorderLayout.NORTH);
        wrapperPanel.add(buttonPanel, BorderLayout.CENTER);

        return wrapperPanel;
    }

    private void handleSaveConnection() {
        saveButton.setEnabled(false);
        checkSSL();
        List<String> errors = validateForm();
        if (errors.isEmpty()) {
            hideErrorLabel();
            messageLabel.setText("");
        } else {
            showErrorLabel("<html>" + String.join("<br>", errors) + "</html>");
            saveButton.setEnabled(true);
            return;
        }
        messageLabel.setText("Trying to Connect...");
        SwingUtilities.invokeLater(() -> {
            try {
                if (!hasCorrectBucketConnection()) {
                    messageLabel.setText("");
                    saveButton.setEnabled(true);
                    return;
                }
            } catch (Exception ex) {
                Log.error(ex);
                ex.printStackTrace();
                showErrorLabel("<html>Connection failed.<br>Please double-check your credentials" + (defaultBucketTextField.getText().trim().isEmpty() ? " or inform a Bucket on <strong>Advanced Settings</strong> -> <strong>Troubleshooting</strong> to inspect your connection" : "") + "</html>");
                saveButton.setEnabled(true);
                messageLabel.setText("");
                return;
            }

            try {

                if(this.savedCluster != null) {
                    ConnectionNodeDescriptor userObject = (ConnectionNodeDescriptor) clickedNode.getUserObject();
                    TreeActionHandler.deleteConnection(clickedNode, userObject, tree);
                }
                SavedCluster sc = DataLoader.saveDatabaseCredentials(connectionNameTextField.getText(), getBaseUrl(hostTextField.getText()), getQueryParams(hostTextField.getText()), enableSSLCheckBox.isSelected(), usernameTextField.getText(), String.valueOf(passwordField.getPassword()), defaultBucketTextField.getText().trim().isEmpty() ? null : defaultBucketTextField.getText(), apiKeyField.getText(), String.valueOf(apiSecretField.getPassword()));
                messageLabel.setText("Connection was successful");
                TreeActionHandler.connectToCluster(project, sc, tree, null);
                close(DialogWrapper.CANCEL_EXIT_CODE);
            } catch (DuplicatedClusterNameAndUserException cae) {
                messageLabel.setText("");
                showErrorLabel("The combination of the name of the cluster and user already exists.");
            } catch (ClusterAlreadyExistsException caee) {
                messageLabel.setText("");
                showErrorLabel("The Couchbase cluster URL and username already exists.");
            } catch (Exception ex) {
                Log.error(ex);
                ex.printStackTrace();
                messageLabel.setText("");
                showErrorLabel("Could not save the database credentials");
            }
            saveButton.setEnabled(true);

        });
    }

    private void handleTestConnection() {
        checkSSL();
        List<String> errors = validateForm();
        if (errors.isEmpty()) {
            hideErrorLabel();
            messageLabel.setText("");
        } else {
            showErrorLabel("<html>" + String.join("<br>", errors) + "</html>");
            return;
        }
        testConnectionButton.setEnabled(false);
        messageLabel.setText("Trying to Connect...");

        SwingUtilities.invokeLater(() -> {
            if (!defaultBucketTextField.getText().trim().isEmpty()) {
                consoleScrollPane.setVisible(true);
                thirdPanel.revalidate();
                ((DefaultTableModel) eventLogTable.getModel()).setRowCount(0);
                SdkDoctorRunner.run(hostTextField.getText(), enableSSLCheckBox.isSelected(), defaultBucketTextField.getText(), usernameTextField.getText(), String.valueOf(passwordField.getPassword()), (str) -> {
                    DefaultTableModel tableModel = (DefaultTableModel) eventLogTable.getModel();
                    tableModel.addRow(new Object[]{str});
                    eventLogTable.scrollRectToVisible(eventLogTable.getCellRect(tableModel.getRowCount() - 1, 0, true));
                });
            }

            try {
                if (!hasCorrectBucketConnection()) {
                    messageLabel.setText("");
                    testConnectionButton.setEnabled(true);
                    return;
                }
            } catch (Exception ex) {
                Log.error(ex);
                ex.printStackTrace();
                showErrorLabel("<html>Connection failed.<br>Please double-check your credentials" + (defaultBucketTextField.getText().trim().isEmpty() ? " or inform a Bucket on <strong>Advanced Settings</strong> -> <strong>Troubleshooting</strong> to inspect your connection" : "") + "</html>");
                testConnectionButton.setEnabled(true);
                messageLabel.setText("");
                return;
            }
            hideErrorLabel();
            messageLabel.setText("Connection was successful.");
            testConnectionButton.setEnabled(true);
        });
    }

    private boolean hasCorrectBucketConnection() {
        Set<String> buckets = DataLoader.listBucketNames(hostTextField.getText(), enableSSLCheckBox.isSelected(), usernameTextField.getText(), String.valueOf(passwordField.getPassword()));

        if (!defaultBucketTextField.getText().trim().isEmpty()) {
            if (!buckets.contains(defaultBucketTextField.getText())) {
                showErrorLabel("The bucket '" + defaultBucketTextField.getText() + "' is not present in this cluster.");
                return false;
            }
        }
        return true;
    }

    private List<String> validateForm() {
        List<String> errors = new ArrayList<>();
        cleanURL();

        if (connectionNameTextField.getText().trim().isEmpty()) {
            errors.add("Name can't be empty");
        }

        if (hostTextField.getText().trim().isEmpty()) {
            errors.add("Connection String can't be empty");
        }

        if (usernameTextField.getText().trim().isEmpty()) {
            errors.add("Username can't be empty");
        }

        if (String.valueOf(passwordField.getPassword()).isEmpty()) {
            errors.add("Password can't be empty");
        }
        return errors;
    }

    private void checkSSL() {
        if (hostTextField.getText().contains("cloud.couchbase.com")
                || hostTextField.getText().startsWith("https://")
                || hostTextField.getText().startsWith("couchbases://")) {
            enableSSLCheckBox.setSelected(true);
        }
    }

    private void createUIComponents() {
        JPanel databasePanel = createDatabasePanel();
        JPanel credentialsPanel = createCredentialsPanel();
        CollapsiblePanel advancedPanel = createAdvancedPanel();

        // Main Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(databasePanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing between panels
        topPanel.add(credentialsPanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing between panels

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(advancedPanel, BorderLayout.CENTER);
        mainPanel.setBorder(JBUI.Borders.emptyLeft(10));

        // Setting preferred size for leftPanel
        JPanel leftPanel = createCouchbaseBanner();
        leftPanel.setPreferredSize(new Dimension(280, leftPanel.getPreferredSize().height));
        leftPanel.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));  // this line ensures the panel doesn't grow more than 180px in width

        JPanel leftWrapper = new JPanel();
        leftWrapper.setLayout(new BoxLayout(leftWrapper, BoxLayout.PAGE_AXIS));
        leftWrapper.add(leftPanel);

        wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(leftWrapper, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(mainPanel, BorderLayout.CENTER);

        wrapperPanel.add(rightPanel, BorderLayout.CENTER);
    }

    @NotNull
    private CollapsiblePanel createAdvancedPanel() {
        JPanel advancedPanel = new JPanel(new BorderLayout());
        advancedPanel.add(getAdvancedTabs(), BorderLayout.CENTER);
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("Advanced Settings", advancedPanel);
        collapsiblePanel.setBorder(JBUI.Borders.emptyTop(10));
        return collapsiblePanel;
    }

    @NotNull
    private JPanel createCredentialsPanel() {
        JPanel secondPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.anchor = GridBagConstraints.WEST;
        gbc2.insets = JBUI.insets(5);

        TitledSeparator credentialsSeparator = new TitledSeparator("Credentials");

        JLabel usernameLabel = new JLabel("Username");
        usernameTextField = new JBTextField(30);
        JLabel passwordLabel = new JLabel("Password");
        passwordField = new JPasswordField(30);

        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.gridwidth = GridBagConstraints.REMAINDER;
        gbc2.gridy = 0;
        gbc2.gridx = 0;
        gbc2.weightx = 1;
        secondPanel.add(credentialsSeparator, gbc2);

        gbc2.fill = GridBagConstraints.NONE;
        gbc2.gridwidth = GridBagConstraints.RELATIVE;

        gbc2.gridy = 1;
        gbc2.gridx = 0;
        gbc2.weightx = 0.4;
        secondPanel.add(usernameLabel, gbc2);

        gbc2.gridy = 1;
        gbc2.gridx = 1;
        gbc2.weightx = 0.6;
        secondPanel.add(TemplateUtil.createComponentWithBalloon(usernameTextField, "The username that you will use to access the database. In Couchbase Capella, you can create new users under <Your Database> -> Settings -> Database Access"), gbc2);

        gbc2.gridy = 2;
        gbc2.gridx = 0;
        gbc2.weightx = 0.4;
        secondPanel.add(passwordLabel, gbc2);

        gbc2.gridy = 2;
        gbc2.gridx = 1;
        gbc2.weightx = 0.6;
        secondPanel.add(TemplateUtil.createComponentWithBalloon(passwordField, "The respective password for the user you have specified"), gbc2);

        if (this.savedCluster != null) {
            usernameTextField.setText(this.savedCluster.getUsername());
            passwordField.setText(DataLoader.getClusterPassword(savedCluster));
        }
        return secondPanel;
    }

    @NotNull
    private JPanel createDatabasePanel() {
        JPanel firstPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(5);

        // Add TitledBorder to the firstPanel
        TitledSeparator separator = new TitledSeparator("Database");

        JLabel connectionNameLabel = new JLabel("Name:");
        connectionNameTextField = new JBTextField(30);
        connectionNameTextField.getEmptyText().setText("Give a name to your connection");
        JLabel hostLabel = new JLabel("Connection String:");
        hostTextField = new JBTextField(30);
        hostTextField.getEmptyText().setText("Your cluster URL");
        enableSSLCheckBox = new JCheckBox("Enable SSL");

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1;
        firstPanel.add(separator, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        firstPanel.add(connectionNameLabel, gbc);

        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        firstPanel.add(TemplateUtil.createComponentWithBalloon(connectionNameTextField, "A simple name to your connection so you can quickly identify it."), gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        firstPanel.add(hostLabel, gbc);

        gbc.gridy = 2;
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        firstPanel.add(TemplateUtil.createComponentWithBalloon(hostTextField, "The Connection String is the address of your server. If your cluster is running locally, it could be something like 'couchbase://localhost'. If you are using Couchbase Capella, it should look like 'couchbases://cb.fwu-odewcpjq7f.cloud.couchbase.com'. Do not include any port numbers"), gbc);

        gbc.gridy = 3;
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        firstPanel.add(TemplateUtil.createComponentWithBalloon(enableSSLCheckBox, "Check this if TLS is enabled in your cluster. If you specify 'couchbases://' protocol in your Connection String, this option will be checked automatically"), gbc);

        if (this.savedCluster != null) {
            connectionNameTextField.setText(this.savedCluster.getName());
            hostTextField.setText(this.savedCluster.getUrl());
            enableSSLCheckBox.setSelected(this.savedCluster.isSslEnable());
            cleanURL();
            hostTextField.setText(hostTextField.getText()+ (savedCluster.getQueryParams()!=null?savedCluster.getQueryParams(): ""));
        }

        return firstPanel;
    }

    private Component getAdvancedTabs() {
        JBTabbedPane tabbedPane = new JBTabbedPane();

        // Add "Management API Keys" tab
        JPanel managementAPIKeysPanel = new JPanel(new BorderLayout());
        managementAPIKeysPanel.setBorder(JBUI.Borders.empty(10));

        JBLabel disclaimer = new JBLabel("<html><strong>Couchbase Capella Only:</strong> Inform these values if you also want to perform administrative tasks via the plugin. You can generate these credentials under <small>Your Org -> Settings -> API Keys</small></html>");
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setPreferredSize(new Dimension(300, 50));
        textPanel.add(disclaimer, BorderLayout.CENTER);

        JPanel mgmFormPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = JBUI.insets(5);

        JLabel apiKeyLabel = new JLabel("API Key:");
        apiKeyField = new JBTextField(30);
        JLabel apiSecretLabel = new JLabel("API Secret:");
        apiSecretField = new JPasswordField(30);

        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        c.gridwidth = 2;
        mgmFormPanel.add(textPanel, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 0.4;
        mgmFormPanel.add(apiKeyLabel, c);

        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0.6;
        mgmFormPanel.add(TemplateUtil.createComponentWithBalloon(apiKeyField, "Leave it blank if you don't want to inform your management keys"), c);

        c.gridy = 2;
        c.gridx = 0;
        c.weightx = 0.4;
        mgmFormPanel.add(apiSecretLabel, c);

        c.gridy = 2;
        c.gridx = 1;
        c.weightx = 0.6;
        mgmFormPanel.add(TemplateUtil.createComponentWithBalloon(apiSecretField, "Leave it blank if you don't want to inform your management keys"), c);
        managementAPIKeysPanel.add(mgmFormPanel, BorderLayout.NORTH);

        /* *************** Troubleshooting Tab **************************/
        JPanel topThirdPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        JBLabel tbLabel = new JBLabel("<html>If you can't successfully connect to your cluster, try informing an existing bucket in the field bellow and click on <strong>Test Connection</strong>. It will run the <strong>SDK Doctor</strong>, an utility that tries to identify potential issues</html>");
        tbLabel.setBorder(JBUI.Borders.empty(10, 0));
        JPanel tbWrapPanel = new JPanel(new BorderLayout());
        tbWrapPanel.setPreferredSize(new Dimension(400, 80));
        tbWrapPanel.add(tbLabel, BorderLayout.CENTER);


        JPanel troubleForm = new JPanel(new GridBagLayout());
        JLabel dfBucket = new JLabel("Bucket:");
        defaultBucketTextField = new JBTextField(30);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.4;
        troubleForm.add(dfBucket, gbc);

        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        troubleForm.add(TemplateUtil.createComponentWithBalloon(defaultBucketTextField, "Inform any existing bucket in you cluster that your credentials have access to."), gbc);

        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class; // Set the column class to String
            }
        };
        tableModel.addColumn("Output");
        eventLogTable = new JBTable(tableModel);
        eventLogTable.setDefaultRenderer(Object.class, new SDKDoctorTableCellRenderer());

        consoleScrollPane = new JBScrollPane(eventLogTable);
        consoleScrollPane.setMinimumSize(new Dimension(800, 300));
        consoleScrollPane.setVerticalScrollBarPolicy(JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        consoleScrollPane.setVisible(false);
        consoleScrollPane.setBorder(JBUI.Borders.emptyTop(10));


        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1;
        topThirdPanel.add(tbWrapPanel, gbc);
        gbc.gridy = 1;
        gbc.weightx = 1;
        topThirdPanel.add(troubleForm, gbc);
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        topThirdPanel.add(consoleScrollPane, gbc);


        //TODO: Enable this when the new Capella Rest API is live
        tabbedPane.addTab("Management API Keys", managementAPIKeysPanel);
        // Add "Connection Troubleshooting" tab
        thirdPanel = new JBPanel(new BorderLayout());
        thirdPanel.setBorder(JBUI.Borders.empty(10));
        thirdPanel.add(topThirdPanel, BorderLayout.NORTH);
        thirdPanel.add(consoleScrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Troubleshooting", thirdPanel);
        tabbedPane.setBorder(JBUI.Borders.emptyTop(10));

        return tabbedPane;
    }

    private JPanel createCouchbaseBanner() {
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Image Icon
        Icon icon = IconLoader.getIcon("/assets/icons/couchbase-logo.svg", NewConnectionDialog.class);
        JLabel imageLabel = new JLabel(icon);
        imageLabel.setBorder(JBUI.Borders.empty(35, 0));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JEditorPane label2 = new JEditorPane("text/html",
                NewConnectionBanner.getBannerContent());
        label2.setEditable(false);
        label2.setOpaque(false);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        // Open the URL based on the link that was clicked
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (IOException | URISyntaxException ex) {
                        Log.error(ex);
                    }
                }
            }
        });

        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(imageLabel);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        leftPanel.add(label2);
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private void showErrorLabel(String errorMessage) {
        errorLabel.setText(errorMessage);
    }

    private void hideErrorLabel() {
        errorLabel.setText("");
    }

    private void cleanURL() {
        String text = hostTextField.getText();
        if (text.endsWith("/")) {
            hostTextField.setText(text.substring(0, text.length() - 1));
        }

        if (text.startsWith("https://")) {
            hostTextField.setText(text.replaceFirst("https://", ""));
            enableSSLCheckBox.setSelected(true);
        } else if (text.startsWith("http://")) {
            hostTextField.setText(text.replaceFirst("http://", ""));
        } else if (text.startsWith("couchbase://")) {
            hostTextField.setText(text.replaceFirst("couchbase://", ""));
        } else if (text.startsWith("couchbases://")) {
            hostTextField.setText(text.replaceFirst("couchbases://", ""));
            enableSSLCheckBox.setSelected(true);
        }

    }
}
