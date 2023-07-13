
package com.couchbase.intellij.tools.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;

import org.jetbrains.annotations.NotNull;

import com.couchbase.intellij.database.ActiveCluster;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

public class ImportDialog extends DialogWrapper {
    // Declare UI components here
    private TextFieldWithBrowseButton datasetField;

    private JComboBox bucketCombo;

    private JBRadioButton defaultScopeAndCollectionRadio;
    private JBRadioButton collectionRadio;
    private JBRadioButton dynamicScopeAndCollectionRadio;

    private JComboBox scopeCombo;
    private JComboBox collectionCombo;

    private JBTextField scopeFieldField;
    private JBTextField collectionFieldField;

    private JBRadioButton generateUUIDRadio;
    private JBRadioButton useFieldValueRadio;
    private JBRadioButton customExpressionRadio;

    private JBTextField fieldNameField;

    private JBTextField expressionField;

    private JBTextField skipFirstField;
    private JBCheckBox skipFirstCheck;

    private JBTextField importUptoField;
    private JBCheckBox importUptoCheck;

    private JBTextField ignoreFieldsField;
    private JBCheckBox ignoreFieldsCheck;

    // Replace threadsField with threadsSpinner
    // private JBTextField threadsField;
    private JSpinner threadsSpinner;

    private JBCheckBox verboseCheck;

    // Declare additional components for navigation and summary
    private CardLayout cardLayout;
    private JPanel cardPanel;

    // Replace keyPreviewPanel with keyPreviewArea
    // private JPanel keyPreviewPanel;
    private JTextArea keyPreviewArea;

    private int currentPage;

    // Declare labels for each field
    private JBLabel scopeLabel;
    private JBLabel collectionLabel;
    private JBLabel scopeFieldLabel;
    private JBLabel collectionFieldLabel;
    private JBLabel keyLabel;
    private JBLabel fieldNameLabel;
    private JBLabel expressionLabel;
    private JBLabel skipFirstLabel;
    private JBLabel importUptoLabel;
    private JBLabel ignoreFieldsLabel;
    private JBLabel threadsLabel;
    private JBLabel verboseLabel;

    // Declare label for summary
    private JBLabel summaryLabel;

    // Declare actions for back and next buttons
    private Action backAction;
    private Action nextAction;

    public ImportDialog() {
        super(true);
        init();
        setTitle("Import Data");
        getWindow().setMinimumSize(new Dimension(600, 380));
        setResizable(true);
        setOKButtonText("Import");
    }

    @Override
    protected JComponent createCenterPanel() {

        currentPage = 1;
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        summaryLabel = new JBLabel();

        // Create and add UI components for each page here
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Page 1: Select dataset
        JPanel datasetPanel = new JPanel(new BorderLayout());
        datasetPanel.add(new TitledSeparator("Select Dataset"), BorderLayout.NORTH);

        JPanel datasetFormPanel = new JPanel();
        datasetFormPanel.setBorder(JBUI.Borders.empty(0, 10));
        datasetFormPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        c.weightx = 0.3;
        c.gridx = 0;
        datasetFormPanel.add(new JBLabel("Dataset:"), c);
        c.weightx = 0.7;
        c.gridx = 1;

        datasetField = new TextFieldWithBrowseButton();
        datasetField.addBrowseFolderListener("Select Dataset", "", null,
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor());
        datasetFormPanel.add(datasetField, c);

        datasetPanel.add(datasetFormPanel, BorderLayout.CENTER);

        // Page 2: Select bucket and target location
        JPanel targetPanel = new JPanel(new BorderLayout());
        targetPanel.add(new TitledSeparator("Target Location"), BorderLayout.NORTH);

        JPanel targetFormPanel = new JPanel();
        targetFormPanel.setBorder(JBUI.Borders.empty(0, 10));
        targetFormPanel.setLayout(new GridBagLayout());

        // Bucket label and combobox
        c.gridy = 0;
        c.weightx = 0.3;
        c.gridx = 0;
        targetFormPanel.add(new JBLabel("Bucket:"), c);
        c.weightx = 0.7;
        c.gridx = 1;

        Set<String> bucketSet = ActiveCluster.getInstance().get().buckets().getAllBuckets().keySet();
        String[] buckets = bucketSet.toArray(new String[0]);

        bucketCombo = new JComboBox<>(buckets);
        targetFormPanel.add(bucketCombo, c);

        // Radio buttons for scope and collection options
        c.gridy = 1;
        c.weightx = 0.3;
        c.gridx = 0;
        targetFormPanel.add(new JBLabel("Scope and Collection:"), c);
        c.weightx = 0.7;
        c.gridx = 1;

        defaultScopeAndCollectionRadio = new JBRadioButton("Default Scope and Collection");
        collectionRadio = new JBRadioButton("Collection");
        dynamicScopeAndCollectionRadio = new JBRadioButton("Dynamic Scope and Collection");

        ButtonGroup group = new ButtonGroup();
        group.add(defaultScopeAndCollectionRadio);
        group.add(collectionRadio);
        group.add(dynamicScopeAndCollectionRadio);

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        radioPanel.add(defaultScopeAndCollectionRadio);
        radioPanel.add(collectionRadio);
        radioPanel.add(dynamicScopeAndCollectionRadio);

        targetFormPanel.add(radioPanel, c);

        // Scope and collection dropdowns
        c.gridy = 2;
        c.weightx = 0.3;
        c.gridx = 0;
        scopeLabel = new JBLabel("Scope:");
        targetFormPanel.add(scopeLabel, c);
        c.weightx = 0.7;
        c.gridx = 1;

        scopeCombo = new JComboBox();
        targetFormPanel.add(scopeCombo, c);

        c.gridy = 3;
        c.weightx = 0.3;
        c.gridx = 0;
        collectionLabel = new JBLabel("Collection:");
        targetFormPanel.add(collectionLabel, c);
        c.weightx = 0.7;
        c.gridx = 1;

        collectionCombo = new JComboBox();
        targetFormPanel.add(collectionCombo, c);

        // Scope and collection fields
        c.gridy = 4;
        c.weightx = 0.3;
        c.gridx = 0;
        scopeFieldLabel = new JBLabel("Scope Field:");
        targetFormPanel.add(scopeFieldLabel, c);

        c.weightx = 0.7;
        c.gridx = 1;

        scopeFieldField = new JBTextField();
        targetFormPanel.add(scopeFieldField, c);

        c.gridy = 5;
        c.weightx = 0.3;
        c.gridx = 0;
        collectionFieldLabel = new JBLabel("Collection Field:");
        targetFormPanel.add(collectionFieldLabel, c);
        c.weightx = 0.7;
        c.gridx = 1;

        collectionFieldField = new JBTextField();
        targetFormPanel.add(collectionFieldField, c);

        // Set all labels to invisible and disabled by default
        scopeLabel.setVisible(false);
        collectionLabel.setVisible(false);
        scopeFieldLabel.setVisible(false);
        collectionFieldLabel.setVisible(false);

        scopeLabel.setEnabled(false);
        collectionLabel.setEnabled(false);
        scopeFieldLabel.setEnabled(false);
        collectionFieldLabel.setEnabled(false);

        // Set all fields to invisible and disabled by default
        scopeCombo.setVisible(false);
        collectionCombo.setVisible(false);
        scopeFieldField.setVisible(false);
        collectionFieldField.setVisible(false);

        scopeCombo.setEnabled(false);
        collectionCombo.setEnabled(false);
        scopeFieldField.setEnabled(false);
        collectionFieldField.setEnabled(false);

        targetPanel.add(targetFormPanel, BorderLayout.CENTER);

        // Page 3: Document key options
        JPanel keyPanel = new JPanel(new BorderLayout());
        keyPanel.add(new TitledSeparator("Document Key"), BorderLayout.NORTH);

        JPanel keyFormPanel = new JPanel();
        keyFormPanel.setBorder(JBUI.Borders.empty(0, 10));
        keyFormPanel.setLayout(new GridBagLayout());

        // Radio buttons for document key options
        c.gridy = 0;
        c.weightx = 0.3;
        c.gridx = 0;
        keyLabel = new JBLabel("Key Options:");
        keyFormPanel.add(keyLabel, c);
        c.weightx = 0.7;
        c.gridx = 1;

        generateUUIDRadio = new JBRadioButton("Generate random UUID for each document");
        useFieldValueRadio = new JBRadioButton("Use the value of a field as the key");
        customExpressionRadio = new JBRadioButton("Generate key based on custom expression");

        ButtonGroup keyGroup = new ButtonGroup();
        keyGroup.add(generateUUIDRadio);
        keyGroup.add(useFieldValueRadio);
        keyGroup.add(customExpressionRadio);

        JPanel keyRadioPanel = new JPanel();
        keyRadioPanel.setLayout(new BoxLayout(keyRadioPanel, BoxLayout.Y_AXIS));
        keyRadioPanel.add(generateUUIDRadio);
        keyRadioPanel.add(useFieldValueRadio);
        keyRadioPanel.add(customExpressionRadio);

        keyFormPanel.add(keyRadioPanel, c);

        // Field name field
        c.gridy = 1;
        c.weightx = 0.3;
        c.gridx = 0;
        fieldNameLabel = new JBLabel("Field Name:");
        keyFormPanel.add(fieldNameLabel, c);
        c.weightx = 0.7;
        c.gridx = 1;

        fieldNameField = new JBTextField();
        keyFormPanel.add(fieldNameField, c);

        // Expression field
        c.gridy = 2;
        c.weightx = 0.3;
        c.gridx = 0;
        expressionLabel = new JBLabel("Expression:");
        keyFormPanel.add(expressionLabel, c);

        c.weightx = 0.7;
        c.gridx = 1;

        expressionField = new JBTextField();
        keyFormPanel.add(expressionField, c);

        // In addListeners method, add listeners for relevant fields:
        fieldNameField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updateKeyPreview();
            }
        });

        expressionField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updateKeyPreview();
            }
        });

        keyPanel.add(keyFormPanel, BorderLayout.CENTER);

        keyPreviewArea = new JTextArea();
        keyPreviewArea.setEditable(false);
        keyPreviewArea.setLineWrap(true);
        keyPreviewArea.setWrapStyleWord(true);
        keyPreviewArea.setMinimumSize(new Dimension(150, 100));
        keyPreviewArea.setPreferredSize(new Dimension(150, 100));
        keyPanel.add(keyPreviewArea, BorderLayout.SOUTH);

        // Set all labels to invisible and disabled by default
        fieldNameLabel.setVisible(false);
        expressionLabel.setVisible(false);
        fieldNameLabel.setEnabled(false);
        expressionLabel.setEnabled(false);

        // Set all fields to invisible and disabled by default
        fieldNameField.setVisible(false);
        expressionField.setVisible(false);
        fieldNameField.setEnabled(false);
        expressionField.setEnabled(false);

        // Set the preview panel to invisible and disabled by default
        keyPreviewArea.setVisible(false);
        keyPreviewArea.setEnabled(false);

        // Page 4: Advanced options
        JPanel advancedPanel = new JPanel(new BorderLayout());
        advancedPanel.add(new TitledSeparator("Advanced Options"), BorderLayout.NORTH);

        JPanel advancedFormPanel = new JPanel();
        advancedFormPanel.setBorder(JBUI.Borders.empty(0, 10));
        advancedFormPanel.setLayout(new GridBagLayout());

        // Skip first documents
        c.gridy = 0;
        c.weightx = 0.3;
        c.gridx = 0;
        skipFirstLabel = new JBLabel("Skip the first # Documents:");
        advancedFormPanel.add(skipFirstLabel, c);
        c.weightx = 0.5;
        c.gridx = 1;

        skipFirstField = new JBTextField();
        advancedFormPanel.add(skipFirstField, c);

        c.weightx = 0.2;
        c.gridx = 2;
        skipFirstCheck = new JBCheckBox();
        advancedFormPanel.add(skipFirstCheck, c);

        // Import up to documents

        c.gridy = 1;
        c.weightx = 0.3;
        c.gridx = 0;
        importUptoLabel = new JBLabel("Import up to # Documents:");
        advancedFormPanel.add(importUptoLabel, c);
        c.weightx = 0.5;
        c.gridx = 1;

        importUptoField = new JBTextField();
        advancedFormPanel.add(importUptoField, c);

        c.weightx = 0.2;
        c.gridx = 2;
        importUptoCheck = new JBCheckBox();
        advancedFormPanel.add(importUptoCheck, c);

        // Ignore fields
        c.gridy = 2;
        c.weightx = 0.3;
        c.gridx = 0;
        ignoreFieldsLabel = new JBLabel("Ignore the fields:");
        advancedFormPanel.add(ignoreFieldsLabel, c);
        c.weightx = 0.5;
        c.gridx = 1;

        ignoreFieldsField = new JBTextField();
        advancedFormPanel.add(ignoreFieldsField, c);

        c.weightx = 0.2;
        c.gridx = 2;
        ignoreFieldsCheck = new JBCheckBox();

        advancedFormPanel.add(ignoreFieldsCheck, c);

        // Threads
        c.gridy = 3;
        c.weightx = 0.3;
        c.gridx = 0;
        threadsLabel = new JBLabel("Threads:");
        advancedFormPanel.add(threadsLabel, c);

        c.weightx = 0.7;
        c.gridx = 1;

        // Replace threadsField with threadsSpinner
        threadsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, null, 1));
        advancedFormPanel.add(threadsSpinner, c);

        // Verbose log
        c.gridy = 4;
        c.weightx = 0.3;
        c.gridx = 0;
        verboseLabel = new JBLabel("Verbose Log:");
        advancedFormPanel.add(verboseLabel, c);
        c.weightx = 0.7;
        c.gridx = 1;

        verboseCheck = new JBCheckBox();
        advancedFormPanel.add(verboseCheck, c);

        advancedPanel.add(advancedFormPanel, BorderLayout.CENTER);

        // Page 5: Summary
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.add(new TitledSeparator("Summary"), BorderLayout.NORTH);

        summaryLabel = new JBLabel();
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);

        // Add pages to card panel
        cardPanel.add(datasetPanel, "1");
        cardPanel.add(targetPanel, "2");
        cardPanel.add(keyPanel, "3");
        cardPanel.add(advancedPanel, "4");
        cardPanel.add(summaryPanel, "5");

        mainPanel.add(cardPanel, BorderLayout.CENTER);

        updateSummary();
        addListeners();

        return mainPanel;

    }

    private void addListeners() {
        // Add listeners for Page 2 radio buttons
        defaultScopeAndCollectionRadio.addActionListener(e -> updateScopeAndCollectionFields());
        collectionRadio.addActionListener(e -> updateScopeAndCollectionFields());
        dynamicScopeAndCollectionRadio.addActionListener(e -> updateScopeAndCollectionFields());

        // Add listeners for Page 3 radio buttons
        generateUUIDRadio.addActionListener(e -> updateKeyFormFields());
        useFieldValueRadio.addActionListener(e -> updateKeyFormFields());
        customExpressionRadio.addActionListener(e -> updateKeyFormFields());

        DocumentAdapter updateSummaryListener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updateSummary();
            }
        };

        // Add listeners for Page 1 fields
        // Add listener for datasetField
        datasetField.getTextField().getDocument().addDocumentListener(updateSummaryListener);
        // Add listeners for Page 2 fields
        defaultScopeAndCollectionRadio.addActionListener(e -> updateSummary());
        collectionRadio.addActionListener(e -> updateSummary());
        dynamicScopeAndCollectionRadio.addActionListener(e -> updateSummary());

        scopeCombo.addActionListener(e -> updateSummary());
        collectionCombo.addActionListener(e -> updateSummary());

        scopeFieldField.getDocument().addDocumentListener(updateSummaryListener);
        collectionFieldField.getDocument().addDocumentListener(updateSummaryListener);

        // Add listeners for Page 3 fields
        generateUUIDRadio.addActionListener(e -> updateSummary());
        useFieldValueRadio.addActionListener(e -> updateSummary());
        customExpressionRadio.addActionListener(e -> updateSummary());

        fieldNameField.getDocument().addDocumentListener(updateSummaryListener);
        expressionField.getDocument().addDocumentListener(updateSummaryListener);

        // Add listeners for Page 4 fields
        skipFirstField.getDocument().addDocumentListener(updateSummaryListener);
        importUptoField.getDocument().addDocumentListener(updateSummaryListener);
        ignoreFieldsField.getDocument().addDocumentListener(updateSummaryListener);

        skipFirstCheck.addActionListener(e -> updateSummary());
        importUptoCheck.addActionListener(e -> updateSummary());
        ignoreFieldsCheck.addActionListener(e -> updateSummary());

        // Replace threadsField with threadsSpinner
        // threadsField.getDocument().addDocumentListener(updateSummaryListener);
        threadsSpinner.addChangeListener(e -> updateSummary());

        verboseCheck.addActionListener(e -> updateSummary());

    }

    private void updateScopeAndCollectionFields() {
        boolean collectionSelected = collectionRadio.isSelected();

        scopeLabel.setVisible(collectionSelected);
        scopeCombo.setVisible(collectionSelected);
        scopeLabel.setEnabled(collectionSelected);
        scopeCombo.setEnabled(collectionSelected);

        collectionLabel.setVisible(collectionSelected);
        collectionCombo.setVisible(collectionSelected);
        collectionLabel.setEnabled(collectionSelected);
        collectionCombo.setEnabled(collectionSelected);

        boolean dynamicSelected = dynamicScopeAndCollectionRadio.isSelected();

        scopeFieldLabel.setVisible(dynamicSelected);
        scopeFieldField.setVisible(dynamicSelected);
        scopeFieldLabel.setEnabled(dynamicSelected);
        scopeFieldField.setEnabled(dynamicSelected);

        collectionFieldLabel.setVisible(dynamicSelected);
        collectionFieldField.setVisible(dynamicSelected);
        collectionFieldLabel.setEnabled(dynamicSelected);
        collectionFieldField.setEnabled(dynamicSelected);

        updateSummary();
    }

    private void updateKeyFormFields() {
        boolean useFieldValueSelected = useFieldValueRadio.isSelected();

        fieldNameLabel.setVisible(useFieldValueSelected);
        fieldNameField.setVisible(useFieldValueSelected);
        fieldNameLabel.setEnabled(useFieldValueSelected);
        fieldNameField.setEnabled(useFieldValueSelected);

        boolean customExpressionSelected = customExpressionRadio.isSelected();

        expressionLabel.setVisible(customExpressionSelected);
        expressionField.setVisible(customExpressionSelected);
        expressionLabel.setEnabled(customExpressionSelected);
        expressionField.setEnabled(customExpressionSelected);

        boolean keyPreviewVisible = useFieldValueSelected || customExpressionSelected;

        // Replace keyPreviewPanel with keyPreviewArea
        // keyPreviewPanel.setVisible(keyPreviewVisible);
        // keyPreviewPanel.setEnabled(keyPreviewVisible);
        keyPreviewArea.setVisible(keyPreviewVisible);
        keyPreviewArea.setEnabled(keyPreviewVisible);

        updateSummary();
    }

    private void updateKeyPreview() {
        // Clear existing preview content
        keyPreviewArea.setText("");

        if (useFieldValueRadio.isSelected()) {
            // Generate preview based on field name
            String fieldName = fieldNameField.getText();
            // Set preview content in keyPreviewArea
            keyPreviewArea.setText("Preview: " + fieldName);
        } else if (customExpressionRadio.isSelected()) {
            // Generate preview based on custom expression
            String expression = expressionField.getText();
            // Set preview content in keyPreviewArea
            keyPreviewArea.setText("Preview: " + expression);
        }

        // keyPreviewPanel.revalidate();
        // keyPreviewPanel.repaint();
    }

    private void updateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("<html>");

        // Page 1: Dataset
        summary.append("<b>Dataset:</b> ");
        summary.append(datasetField.getText());
        summary.append("<br><br>");

        // Page 2: Bucket and target location
        summary.append("<b>Bucket:</b> ");
        summary.append(bucketCombo.getSelectedItem());
        summary.append("<br><br>");

        summary.append("<b>Scope and Collection:</b> ");
        if (defaultScopeAndCollectionRadio.isSelected()) {
            summary.append("Default Scope and Collection");
        } else if (collectionRadio.isSelected()) {
            summary.append("Collection - Scope: ");
            summary.append(scopeCombo.getSelectedItem());
            summary.append(", Collection: ");
            summary.append(collectionCombo.getSelectedItem());
        } else if (dynamicScopeAndCollectionRadio.isSelected()) {
            summary.append("Dynamic Scope and Collection - Scope Field: ");
            summary.append(scopeFieldField.getText());
            summary.append(", Collection Field: ");
            summary.append(collectionFieldField.getText());
        }
        summary.append("<br><br>");

        // Page 3: Document key
        summary.append("<b>Document Key:</b> ");
        if (generateUUIDRadio.isSelected()) {
            summary.append("Generate random UUID for each document");
        } else if (useFieldValueRadio.isSelected()) {
            summary.append("Use the value of a field as the key - Field Name: ");
            summary.append(fieldNameField.getText());
        } else if (customExpressionRadio.isSelected()) {
            summary.append("Generate key based on custom expression - Expression: ");
            summary.append(expressionField.getText());
        }
        summary.append("<br><br>");

        // Page 4: Advanced options
        summary.append("<b>Advanced Options:</b><br>");
        if (skipFirstCheck.isSelected()) {
            summary.append("- Skip the first ");
            summary.append(skipFirstField.getText());
            summary.append(" documents<br>");
        }
        if (importUptoCheck.isSelected()) {
            summary.append("- Import up to ");
            summary.append(importUptoField.getText());
            summary.append(" documents<br>");
        }
        if (ignoreFieldsCheck.isSelected()) {
            summary.append("- Ignore the fields: ");
            summary.append(ignoreFieldsField.getText());
            summary.append("<br>");
        }
        summary.append("- Threads: ");
        // Use getValue() method of JSpinner to get its value
        summary.append(threadsSpinner.getValue());
        summary.append("<br>");
        if (verboseCheck.isSelected()) {
            summary.append("- Verbose Log<br>");
        }

        // Add additional line break at the end
        summary.append("<br>");

        summaryLabel.setText(summary.toString());
    }

    @Override
    protected Action @NotNull [] createActions() {
        Action cancelAction = getCancelAction();
        backAction = new DialogWrapperAction("Back") {
            @Override
            protected void doAction(ActionEvent e) {
                previousPage();
            }
        };
        nextAction = new DialogWrapperAction("Next") {
            @Override
            protected void doAction(ActionEvent e) {
                nextPage();
            }
        };

        backAction.setEnabled(false);

        return new Action[] { cancelAction, backAction, nextAction };

    }

    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            cardLayout.show(cardPanel, Integer.toString(currentPage));
            nextAction.putValue(Action.NAME, currentPage == 5 ? "Import" : "Next");
        }
        backAction.setEnabled(currentPage != 1);
    }

    private void nextPage() {
        if (currentPage < 5) {
            currentPage++;
            cardLayout.show(cardPanel, Integer.toString(currentPage));
            nextAction.putValue(Action.NAME, currentPage == 5 ? "Import" : "Next");
        } else {
            doOKAction();
        }
        backAction.setEnabled(currentPage != 1);
    }

    @Override
    protected void doOKAction() {
        // Implement import functionality here
        super.doOKAction();
    }

}
