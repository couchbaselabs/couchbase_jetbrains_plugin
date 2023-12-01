package com.couchbase.intellij.tree.cblite.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jetbrains.annotations.NotNull;

import com.couchbase.intellij.tools.dialog.JComboCheckBox;
import com.couchbase.intellij.tree.cblite.ActiveCBLDatabase;
import com.couchbase.intellij.workbench.Log;
import com.couchbase.lite.Collection;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.Scope;
import com.couchbase.lite.SelectResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import utils.TemplateUtil;
import utils.TimeUtils;

public class CBLExportDialog extends DialogWrapper {

    private static final String ALL_SCOPES = "All Scopes";
    private JComboCheckBox scopeComboBox;
    private JComboCheckBox collectionComboBox;
    private JLabel errorLabel;
    private TextFieldWithBrowseButton destinationField;
    private ComboBox<String> outputFormatComboBox;
    private JTextField scopeKeyField;
    private JTextField collectionKeyField;
    private JTextField documentKeyField;

    public CBLExportDialog(Project project, Tree tree) {
        super(project);
        init();
        setTitle("Couchbase Lite Export");
        getWindow().setMinimumSize(new Dimension(600, 380));
        setResizable(true);
        setOKButtonText("Export");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 0.7;
        c.fill = GridBagConstraints.BOTH;
        c.insets = JBUI.insets(5);
        formPanel.add(new TitledSeparator("Target"), c);

        JLabel scopeLabel = new JLabel("Scope:");
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0.3;
        c.gridwidth = 1;
        formPanel.add(scopeLabel, c);

        scopeComboBox = new JComboCheckBox();
        collectionComboBox = new JComboCheckBox();

        Database database = ActiveCBLDatabase.getInstance().getDatabase();

        try {
            scopeComboBox.addItem(ALL_SCOPES);
            for (Scope scopeName : database.getScopes()) {
                scopeComboBox.addItem(scopeName.getName());
            }
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }

        scopeComboBox.setItemListener(e -> {
            // First, generate the new items
            List<String> newItems = new ArrayList<>();
            if (scopeComboBox.getSelectedItems().isEmpty()) {
                scopeComboBox.setHint("Select one or more scopes");
                collectionComboBox.setHint("Select one or more collections");
                collectionComboBox.setEnabled(false);
            } else {
                collectionComboBox.setEnabled(true);
                collectionComboBox.removeAllItems();

                for (String selectedScope : scopeComboBox.getSelectedItems()) {
                    if (ALL_SCOPES.equals(selectedScope)) {
                        newItems.add("All Collections of " + ALL_SCOPES);
                    } else {
                        try {
                            newItems.add("All Collections of " + selectedScope);
                            for (Collection collectionName : Objects.requireNonNull(database.getScope(selectedScope)).getCollections()) {
                                newItems.add(selectedScope + "." + collectionName.getName());
                            }
                        } catch (Exception exception) {
                            errorLabel.setText(exception.getMessage());
                        }
                    }
                }

                // We need to arrange all the "All Collections of All Scopes" items to be at the
                // top and "All Collections of <scope>" items to be next
                newItems.sort((o1, o2) -> {
                    if (o1.startsWith("All Collections of ") && o2.startsWith("All Collections of ")) {
                        return o1.compareTo(o2);
                    } else if (o1.startsWith("All Collections of ")) {
                        return -1;
                    } else if (o2.startsWith("All Collections of ")) {
                        return 1;
                    } else {
                        return o1.compareTo(o2);
                    }
                });

                // Finally, add new items and update oldItems
                for (String item : newItems) {
                    collectionComboBox.addItem(item);
                }

            }

        });

        scopeComboBox.setHint("Select one or more scopes");
        scopeComboBox.setSelectedItem(null);
        c.gridx = 1;
        c.weightx = 0.7;
        formPanel.add(scopeComboBox, c);

        JLabel collectionLabel = new JLabel("Collection:");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        formPanel.add(collectionLabel, c);

        collectionComboBox.setEnabled(false);
        collectionComboBox.setHint("Select one or more collections");
        c.gridx = 1;
        c.weightx = 0.7;
        formPanel.add(collectionComboBox, c);

        JLabel scopeKeyLabel = new JLabel("Scope Key:");
        JPanel scopeKeyPanel = TemplateUtil.getLabelWithHelp(scopeKeyLabel, "<html>This filed will be used to store the name of the scope the document came from. It will be created on each JSON document.</html>");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        formPanel.add(scopeKeyPanel, c);

        scopeKeyField = new JTextField();
        scopeKeyField.setText("cbms");
        c.gridx = 1;
        c.weightx = 0.7;
        formPanel.add(scopeKeyField, c);

        JLabel collectionKeyLabel = new JLabel("Collection Key:");
        JPanel collectionKeyPanel = TemplateUtil.getLabelWithHelp(collectionKeyLabel, "<html>This filed will be used to store the name of the collection the document came from. It will be created on each JSON document.</html>");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        formPanel.add(collectionKeyPanel, c);

        collectionKeyField = new JTextField();
        collectionKeyField.setText("cbmc");
        c.gridx = 1;
        c.weightx = 0.7;
        formPanel.add(collectionKeyField, c);

        JLabel documentKeyLabel = new JLabel("Document Key:");
        JPanel documentKeyPanel = TemplateUtil.getLabelWithHelp(documentKeyLabel, "<html>In Couchbase, the document's key is not " + "part of the body of the document. But when you are exporting the dataset, it is recommended to also include " + "the original keys. This property defines the name of the attribute in the final exported file that will contain the document's key.</html>");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        formPanel.add(documentKeyPanel, c);

        documentKeyField = new JTextField();
        documentKeyField.setText("cbmid");
        c.gridx = 1;
        c.weightx = 0.7;
        formPanel.add(documentKeyField, c);

        JLabel outputFormatLabel = new JLabel("Output Format:");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        formPanel.add(outputFormatLabel, c);

        outputFormatComboBox = new ComboBox<>(new String[]{"JSON Lines", "JSON List"});
        c.gridx = 1;
        c.weightx = 0.7;
        formPanel.add(outputFormatComboBox, c);

        JLabel destinationLabel = new JLabel("Destination Folder:");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        formPanel.add(destinationLabel, c);

        destinationField = new TextFieldWithBrowseButton();
        destinationField.addBrowseFolderListener("Select Destination Folder", "Select destination folder", null, FileChooserDescriptorFactory.createSingleFolderDescriptor());
        destinationField.setText(System.getProperty("user.home"));
        c.gridx = 1;
        c.weightx = 0.7;
        formPanel.add(destinationField, c);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel errorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.decode("#FF4444"));
        errorPanel.add(errorLabel);
        mainPanel.add(errorPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    @Override
    protected void doOKAction() {
        List<String> selectedCollections = collectionComboBox.getSelectedItems();
        Database database = ActiveCBLDatabase.getInstance().getDatabase();

        String scopeKey = scopeKeyField.getText();
        String collectionKey = collectionKeyField.getText();
        String documentKey = documentKeyField.getText();

        String destinationFilePath = destinationField.getText() + File.separator + ActiveCBLDatabase.getInstance().getDatabase().getName() + "_cblexport_" + TimeUtils.getCurrentDateTime() + ".json";

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Exporting collections from '" + database.getName() + "' to '" + destinationFilePath + "'", false) {
            public void run(@NotNull ProgressIndicator indicator) {
                try (FileWriter writer = new FileWriter(destinationFilePath)) {
                    String outputFormat = (String) outputFormatComboBox.getSelectedItem();
                    boolean isListFormat = "JSON List".equals(outputFormat);

                    if (isListFormat) {
                        writer.write("[\n");
                    }

                    // Create a set to keep track of exported collections
                    Set<String> exportedCollections = new HashSet<>();

                    for (String collectionPath : selectedCollections) {
                        String[] parts = collectionPath.split("\\.");
                        String scopeName = parts[0];
                        String collectionName = parts.length > 1 ? parts[1] : null;

                        if (collectionPath.equals("All Collections of All Scopes")) {
                            for (Scope scope : database.getScopes()) {
                                writeCollections(writer, database, scope.getName(), collectionName, scopeKey, collectionKey, documentKey, isListFormat, exportedCollections);
                            }
                        } else if (collectionPath.startsWith("All Collections of ")) {
                            String scope = collectionPath.substring("All Collections of ".length());
                            writeCollections(writer, database, scope, collectionName, scopeKey, collectionKey, documentKey, isListFormat, exportedCollections);
                        } else {
                            writeCollections(writer, database, scopeName, collectionName, scopeKey, collectionKey, documentKey, isListFormat, exportedCollections);
                        }
                    }

                    if (isListFormat) {
                        writer.write("]\n");
                    }

                    ApplicationManager.getApplication().invokeLater(() -> {
                        Log.info("Export completed successfully");
                        Messages.showInfoMessage("Export completed successfully", "Export");
                    });

                } catch (IOException | CouchbaseLiteException e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Log.error("Export failed", e);
                        Messages.showErrorDialog("Export failed: " + e.getMessage(), "Export");
                    });
                }
            }
        });

        super.doOKAction();
    }

    private void writeCollections(FileWriter writer, Database database, String scopeName, String collectionName, String scopeKey, String collectionKey, String documentKey, boolean isListFormat, Set<String> exportedCollections) throws IOException, CouchbaseLiteException {
        if (collectionName == null || "All Collections of ".concat(scopeName).equals(collectionName)) {
            for (Collection collection : Objects.requireNonNull(database.getScope(scopeName)).getCollections()) {
                if (!exportedCollections.contains(collection.getName())) {
                    writeDocuments(writer, collection, scopeName, collection.getName(), scopeKey, collectionKey, documentKey, isListFormat);
                    exportedCollections.add(collection.getName());
                }
            }
        } else {
            Collection collection = Objects.requireNonNull(database.getScope(scopeName)).getCollection(collectionName);
            if (!exportedCollections.contains(Objects.requireNonNull(collection).getName())) {
                writeDocuments(writer, collection, scopeName, collectionName, scopeKey, collectionKey, documentKey, isListFormat);
                exportedCollections.add(collection.getName());
            }
        }
    }

    private void writeDocuments(FileWriter writer, Collection collection, String scopeName, String collectionName, String scopeKey, String collectionKey, String documentKey, boolean isListFormat) throws IOException, CouchbaseLiteException {
        Query query = QueryBuilder.select(SelectResult.all()).from(DataSource.collection(collection));

        try (ResultSet resultSet = query.execute()) {
            for (Result result : resultSet) {
                String docId = result.getString("id");
                Dictionary content = result.getDictionary(collectionName);

                // Convert the content to a JSON string
                String json = Objects.requireNonNull(content).toJSON();

                // Convert the JSON string to a map
                Map<String, Object> map = new Gson().fromJson(json, new TypeToken<Map<String, Object>>() {
                }.getType());

                // Overwrite the scope, collection, and document key fields
                map.put(scopeKey, scopeName);
                map.put(collectionKey, collectionName);
                map.put(documentKey, docId);

                // Convert the map back to a JSON string
                json = new Gson().toJson(map);

                writer.write(json);

                writer.write(isListFormat ? ",\n" : "\n");
            }
        }
    }

}
