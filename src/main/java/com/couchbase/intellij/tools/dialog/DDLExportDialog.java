package com.couchbase.intellij.tools.dialog;

import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.tools.DDLExport;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import utils.TemplateUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@SuppressWarnings("rawtypes")
public class DDLExportDialog extends DialogWrapper {
    private final String ALL_SCOPES = "All Scopes";
    private JComboCheckBox scopeCombo;
    private ComboBox bucketCombo;
    private JBLabel errorLabel;

    private JBCheckBox includeIndexes;

    private TextFieldWithBrowseButton fileDestination;


    public DDLExportDialog() {
        super(true);
        init();
        setTitle("Export DDL");
        getWindow().setMinimumSize(new Dimension(600, 200));
        setResizable(true);
        setOKButtonText("Export");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel targetPanelWrapper = new JPanel(new BorderLayout());
        targetPanelWrapper.add(createPanel(), BorderLayout.NORTH);
        mainPanel.add(targetPanelWrapper);

        JPanel errorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        errorLabel = new JBLabel("");
        errorLabel.setForeground(Color.decode("#FF4444"));
        errorPanel.add(errorLabel);
        mainPanel.add(errorPanel);

        return mainPanel;
    }


    private JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setBorder(JBUI.Borders.empty(0, 10));
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        // Bucket label and combobox
        c.gridy = 0;
        c.weightx = 0.3;
        c.gridx = 0;
        formPanel.add(new JBLabel("Bucket:"), c);
        c.weightx = 0.7;
        c.gridx = 1;

        Set<String> bucketSet = ActiveCluster.getInstance().get().buckets().getAllBuckets().keySet();
        String[] buckets = bucketSet.toArray(new String[0]);

        bucketCombo = new ComboBox<>(buckets);
        bucketCombo.addActionListener(e -> {
            if (bucketCombo.getSelectedItem() == null) {
                return;
            }
            String bucketId = (String) bucketCombo.getSelectedItem();
            List<String> scopes = ActiveCluster.getInstance().get().bucket(bucketId).collections().getAllScopes().stream().map(ScopeSpec::name).collect(Collectors.toList());
            List<String> list = new ArrayList<>();
            list.add(ALL_SCOPES);
            list.addAll(scopes);

            scopeCombo.removeAllItemsAndCreateNewComboBox();
            scopeCombo.setHint("Select one or more items");
            scopeCombo.setEnabled(true);
            list.forEach(item -> scopeCombo.addItem(item));
        });
        bucketCombo.setSelectedItem(null);
        formPanel.add(bucketCombo, c);

        // Scope label and combobox
        c.gridy = 1;
        c.weightx = 0.3;
        c.gridx = 0;
        formPanel.add(new JBLabel("Scope:"), c);
        c.weightx = 0.7;
        c.gridx = 1;


        scopeCombo = new JComboCheckBox();
        scopeCombo.setEnabled(false);

        formPanel.add(scopeCombo, c);

        c.gridy = 2;
        c.weightx = 0.3;
        c.gridx = 0;
        JLabel checkLabel = new JBLabel("Include indexes");
        checkLabel.setBorder(JBUI.Borders.empty(5, 0));
        formPanel.add(checkLabel, c);
        c.weightx = 0.7;
        c.gridx = 1;
        includeIndexes = new JBCheckBox();
        formPanel.add(includeIndexes, c);
        if (!ActiveCluster.getInstance().hasQueryService()) {
            includeIndexes.setEnabled(false);
        }

        c.gridy = 3;
        c.weightx = 0.3;
        c.gridx = 0;
        formPanel.add(TemplateUtil.getLabelWithHelp("File destination folder:", "<html>Folder where the file will be stored</html>"), c);
        c.weightx = 0.7;
        c.gridx = 1;
        fileDestination = new TextFieldWithBrowseButton();
        fileDestination.setText(System.getProperty("user.home"));
        fileDestination.addBrowseFolderListener("Select File Destination", "", null, FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor());
        formPanel.add(fileDestination, c);

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }


    @Override
    protected void doOKAction() {

        List<String> errors = new ArrayList<>();
        if (bucketCombo.getSelectedItem() == null) {
            errors.add("Please select a bucket");
        } else if (scopeCombo.getSelectedItems().isEmpty()) {
            errors.add("Select one or more scopes");
        }


        if (fileDestination.getText().trim().isEmpty()) {
            errors.add("Inform the file destination folder");
        } else if (!Files.exists(Path.of(fileDestination.getText()))) {
            errors.add("The folder specified does not exists");
        }

        if (!errors.isEmpty()) {
            String errorMsg = String.join("<br>", errors);
            errorLabel.setText("<html>" + errorMsg + "</html>");
            return;
        } else {
            DDLExport.exportScope(bucketCombo.getSelectedItem().toString(), scopeCombo.getSelectedItems(), fileDestination.getText(), includeIndexes.isSelected());
        }
        super.doOKAction();
    }
}

