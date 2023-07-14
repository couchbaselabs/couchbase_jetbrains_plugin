package com.couchbase.intellij.tree;

import javax.swing.*;
import java.awt.*;

import com.couchbase.intellij.tools.dialog.CollapsiblePanel;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.couchbase.intellij.database.ActiveCluster;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.intellij.ui.components.JBTextField;
import com.intellij.openapi.ui.popup.Balloon;

public class PillowFightDialog extends DialogWrapper {
    private ComboBox<String> bucketComboBox;
    private ComboBox<String> durabilityComboBox;
    private JSpinner persistToSpinner;
    private JCheckBox persistToCheckbox;
    private JSpinner batchSizeSpinner;
    private JCheckBox batchSizeCheckbox;
    private JSpinner numberItemsSpinner;
    private JCheckBox numberItemsCheckbox;
    private JBTextField keyPrefixTextField;
    private JSpinner numberThreadsSpinner;
    private  JCheckBox numberThreadsCheckbox;
    private JSpinner percentageSpinner;
    private JCheckBox percentageCheckbox;
    private ComboBox<String> noPopulationComboBox;
    private ComboBox<String> populateOnlyComboBox;
    private JSpinner minSizeSpinner;
    private JCheckBox minSizeCheckbox;
    private JSpinner maxSizeSpinner;
    private JCheckBox maxSizeCheckbox;
    private JSpinner numberCyclesSpinner;
    private JCheckBox numberCyclesCheckbox;
    private ComboBox<String> sequentialComboBox;
    private JSpinner startAtSpinner;
    private JCheckBox startAtCheckbox;
    private ComboBox<String> timingsComboBox;
    private JSpinner expirySpinner;
    private JCheckBox expiryCheckbox;
    private JSpinner replicateToSpinner;
    private JCheckBox replicateToCheckbox;
    private JSpinner lockSpinner;
    private JCheckBox lockCheckbox;
    private ComboBox<String> jsonComboBox;
    private ComboBox<String> noopComboBox;
    private ComboBox<String> subdocComboBox;
    private JSpinner pathcountSpinner;
    private JCheckBox pathcountCheckbox;
    private JLabel errorMessage;

    @Override
    protected Action @NotNull [] createActions() {
        Action okAction = getOKAction();
        okAction.putValue(Action.NAME, "Start");
        return new Action[]{okAction, getCancelAction()};
    }
    protected PillowFightDialog(Project project) {
        super(project);

        setTitle("Pillow Fight");

        bucketComboBox = new ComboBox<>();
        try {
            Set<String> bucketNamesSet = ActiveCluster.getInstance().get().buckets().getAllBuckets().keySet();
            String[] bucketNamesArray = bucketNamesSet.toArray(new String[bucketNamesSet.size()]);
            for (String s : bucketNamesArray) {
                bucketComboBox.addItem(s);
            }
        } catch (Exception e) {
            Messages.showErrorDialog("Could not connect to the cluster. Please check your network connectivity, " +
                    " if the cluster is active or if the credentials are still valid.", "Couchbase Plugin Error");
            Log.error(e);
        }

        String[] disableEnableOptions = {"Disable", "Enable"};
        String[] durabilityOptions = {"none", "majority", "majority_and_persist_to_active", "persist_to_majority"};

        durabilityComboBox = new ComboBox<>(durabilityOptions);
        persistToSpinner = createJSpinner(-1);
        batchSizeSpinner = createJSpinner(0);
        numberItemsSpinner = createJSpinner(0);
        keyPrefixTextField = new JBTextField();
        numberThreadsSpinner = createJSpinner(0);
        percentageSpinner = createJSpinner(0);
        noPopulationComboBox = new ComboBox<>(disableEnableOptions);
        populateOnlyComboBox = new ComboBox<>(disableEnableOptions);
        minSizeSpinner = createJSpinner(0);
        maxSizeSpinner = createJSpinner(0);
        numberCyclesSpinner = createJSpinner(-1);
        sequentialComboBox = new ComboBox<>(disableEnableOptions);
        startAtSpinner = createJSpinner(0);
        timingsComboBox = new ComboBox<>(disableEnableOptions);
        expirySpinner = createJSpinner(0);
        replicateToSpinner = createJSpinner(-1);
        lockSpinner = createJSpinner(0);
        jsonComboBox = new ComboBox<>(disableEnableOptions);
        noopComboBox = new ComboBox<>(disableEnableOptions);
        subdocComboBox = new ComboBox<>(disableEnableOptions);
        pathcountSpinner = createJSpinner(0);

        persistToCheckbox = createCheckbox(persistToSpinner);
        batchSizeCheckbox = createCheckbox(batchSizeSpinner);
        numberItemsCheckbox = createCheckbox(numberItemsSpinner);
        numberThreadsCheckbox = createCheckbox(numberThreadsSpinner);
        percentageCheckbox = createCheckbox(percentageSpinner);
        minSizeCheckbox = createCheckbox(minSizeSpinner);
        maxSizeCheckbox = createCheckbox(maxSizeSpinner);
        numberCyclesCheckbox = createCheckbox(numberCyclesSpinner);
        startAtCheckbox = createCheckbox(startAtSpinner);
        expiryCheckbox = createCheckbox(expirySpinner);
        replicateToCheckbox = createCheckbox(replicateToSpinner);
        lockCheckbox = createCheckbox(lockSpinner);
        pathcountCheckbox = createCheckbox(pathcountSpinner);

        init();
    }

    protected PillowFightDialog(Project project, String bucket, String durability, String persistTo, String batchSize, String numberItems, String keyPrefix, String numberThreads, String percentage, String noPopulation, String populateOnly, String minSize, String maxSize, String numberCycles, String sequential, String startAt, String timings, String expiry, String replicateTo, String lock, String json, String noop, String subdoc, String pathcount) {
        this(project);

        setValue(bucketComboBox, bucket);
        setValue(durabilityComboBox, durability);
        setValue(persistToSpinner, persistTo, persistToCheckbox);
        setValue(batchSizeSpinner, batchSize, batchSizeCheckbox);
        setValue(numberItemsSpinner, numberItems, numberItemsCheckbox);
        keyPrefixTextField.setText(keyPrefix);
        setValue(numberThreadsSpinner, numberThreads, numberThreadsCheckbox);
        setValue(percentageSpinner, percentage, percentageCheckbox);
        setValue(noPopulationComboBox, noPopulation);
        setValue(populateOnlyComboBox, populateOnly);
        setValue(minSizeSpinner, minSize, minSizeCheckbox);
        setValue(maxSizeSpinner, maxSize, maxSizeCheckbox);
        setValue(numberCyclesSpinner, numberCycles, numberCyclesCheckbox);
        setValue(sequentialComboBox, sequential);
        setValue(startAtSpinner, startAt, startAtCheckbox);
        setValue(timingsComboBox, timings);
        setValue(expirySpinner, expiry, expiryCheckbox);
        setValue(replicateToSpinner, replicateTo, replicateToCheckbox);
        setValue(lockSpinner, lock, lockCheckbox);
        setValue(jsonComboBox, json);
        setValue(noopComboBox, noop);
        setValue(subdocComboBox, subdoc);
        setValue(pathcountSpinner, pathcount, pathcountCheckbox);
    }

    private JSpinner createJSpinner(int minValue) {
        JSpinner currentJSpinner = new JSpinner(new SpinnerNumberModel(minValue, minValue, Integer.MAX_VALUE, 1));
        return currentJSpinner;
    }

    private JCheckBox createCheckbox(JSpinner spinner) {
        JCheckBox checkbox = new JCheckBox();
        spinner.setEnabled(false);
        checkbox.addChangeListener(e -> spinner.setEnabled(checkbox.isSelected()));
        return checkbox;
    }

    private void setValue(ComboBox comboBox, String value) {
        comboBox.setSelectedItem(value);
    }

    private void setValue(JSpinner spinner, String value, JCheckBox checkBox) {
        if (value != null) {
            spinner.setValue(Integer.valueOf(value));
            checkBox.setSelected(true);
        }
    }

    private static JBPanel createLabelWithBalloon(String labelText, String hintText) {
        JBLabel label = new JBLabel(labelText);
        JBLabel questionMark = new JBLabel("?");
        questionMark.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                showBalloonHint(questionMark, hintText);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hideBalloonHint();
            }
        });

        JBPanel panel = new JBPanel(new BorderLayout());
        panel.add(label, BorderLayout.WEST);
        panel.add(questionMark, BorderLayout.EAST);

        return panel;
    }

    private static Balloon balloonHint;

    private static void showBalloonHint(JBLabel questionMark, String hintText) {
        JBPopupFactory factory = JBPopupFactory.getInstance();
        JTextArea textArea = new JTextArea(hintText);
        textArea.setBackground(UIManager.getColor("ToolTip.background"));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setColumns(40);
        textArea.setSize(textArea.getPreferredSize());

        balloonHint = factory.createBalloonBuilder(textArea).setFillColor(UIManager.getColor("ToolTip.background")).setAnimationCycle(0).createBalloon();
        balloonHint.show(factory.guessBestPopupLocation(questionMark), Balloon.Position.below);
    }

    private static void hideBalloonHint() {
        if (balloonHint != null) {
            balloonHint.hide();
            balloonHint = null;
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        panel.add(createLabelWithBalloon("Available Buckets:        ", "A bucket is the fundamental space for storing data in Couchbase Server"), gbc);

        gbc.gridx++;
        panel.add(bucketComboBox, gbc);

        gbc.gridx++;
        JPanel rightPaddingPanel = new JPanel();
        rightPaddingPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        panel.add(rightPaddingPanel, gbc);
        gbc.gridx--;

        gbc.gridx--;
        gbc.gridy++;
        panel.add(createLabelWithBalloon("Durability:        ", "Specify durability level for mutation operations"), gbc);

        gbc.gridx++;
        panel.add(durabilityComboBox, gbc);

        gbc.gridx--;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JPanel collapsiblePanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbcCollapsable = new GridBagConstraints();
        gbcCollapsable.gridx = 0;
        gbcCollapsable.gridy = 0;
        gbcCollapsable.insets = JBUI.insets(5);
        gbcCollapsable.anchor = GridBagConstraints.WEST;
        gbcCollapsable.fill = GridBagConstraints.HORIZONTAL;
        gbcCollapsable.weightx = 1.0;
        gbcCollapsable.weighty = 1.0;

        collapsiblePanel.setVisible(false);

        collapsiblePanel.add(createLabelWithBalloon("Persist-to: ", "Wait until the item has been persisted to at least NUMNODES nodes' disk. If NUMNODES is 1 then wait until only the master node has persisted the item for this key. You may not specify a number greater than the number of nodes actually in the cluster. -1 is special value, which mean to use all available nodes."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(persistToSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(persistToCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Batch Size: ", "This controls how many commands are scheduled per cycles. To simulate one operation at a time, set this value to 1."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(batchSizeSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(batchSizeCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Number of Items: ", "Set the total number of items the workload will access within the cluster. This will also determine the working set size at the server and may affect disk latencies if set to a high number."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(numberItemsSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(numberItemsCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Key Prefix: ", "Set the prefix to prepend to all keys in the cluster. Useful if you do not wish the items to conflict with existing data."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(keyPrefixTextField, gbcCollapsable);
        keyPrefixTextField.getEmptyText().setText("Enter a prefix to prepend");

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Number of Threads: ", "Set the number of threads (and thus the number of client instances) to run concurrently. Each thread is assigned its own client object."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(numberThreadsSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(numberThreadsCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Percentage", "The percentage of operations which should be mutations. A value of 100 means only mutations while a value of 0 means only retrievals."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(percentageSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(percentageCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("No Population: ", "By default cbc-pillowfight will load all the items (see --num-items) into the cluster and then begin performing the normal workload. Specifying this option bypasses this stage. Useful if the items have already been loaded in a previous run."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(noPopulationComboBox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Populate Only: ", "Stop after population. Useful to populate buckets with large amounts of data."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(populateOnlyComboBox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Min Size: ", "Specify the minimum size to be stored into the cluster. This is typically a range, in which case each value generated will be between Min Size and Max Size bytes."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(minSizeSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(minSizeCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Max Size: ", "Specify the maximum size to be stored into the cluster. This is typically a range, in which case each value generated will be between Min Size and Max Size bytes."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(maxSizeSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(maxSizeCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Number of Cycles: ", "Specify the number of times the workload should cycle. During each cycle an amount of --batch-size operations are executed. Setting this to -1 will cause the workload to run infinitely."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(numberCyclesSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(numberCyclesCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Sequential: ", "Specify that the access pattern should be done in a sequential manner. This is useful for bulk-loading many documents in a single server."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(sequentialComboBox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Start At: ", "This specifies the starting offset for the items. The items by default are generated with the key prefix (--key-prefix) up to the number of items (--num-items). The --start-at value will increase the lower limit of the items. This is useful to resume a previously cancelled load operation."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(startAtSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(startAtCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Timings: ", "Enabled timing recorded. Timing histogram will be dumped to STDERR on SIGQUIT (CTRL-/). When specified second time, it will dump a histogram of command timings and latencies to the screen every second."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(timingsComboBox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Expiry: ", "Set the expiration time on the document for SECONDS when performing each operation. Note that setting this too low may cause not-found errors to appear on the screen."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(expirySpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(expiryCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Replicate To: ", "Wait until the item has been replicated to at least NREPLICAS replica nodes. The bucket must be configured with at least one replica, and at least NREPLICAS replica nodes must be online. -1 is special value, which mean to use all available replicas."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(replicateToSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(replicateToCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Lock: ", "This will retrieve and lock an item before update, making it inaccessible for modification until the update completed, or TIME has passed."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(lockSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(lockCheckbox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("JSON: ", "Make pillowfight store document as JSON rather than binary. This will allow the documents to nominally be analyzed by other Couchbase services such as Query and MapReduce."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(jsonComboBox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("NOOP: ", "Use couchbase NOOP operations when running the workload. This mode ignores population, and all other document operations. Useful as the most lightweight workload."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(noopComboBox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Subdoc: ", "Use couchbase sub-document operations when running the workload. In this mode pillowfight will use Couchbase sub-document operations to perform gets and sets of data. This option must be used with --json"), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(subdocComboBox, gbcCollapsable);

        gbcCollapsable.gridx--;
        gbcCollapsable.gridy++;
        collapsiblePanel.add(createLabelWithBalloon("Pathcount: ", "Specify the number of paths a single sub-document operation should contain. By default, each subdoc operation operates on only a single path within the document. You can specify multiple paths to atomically executed multiple subdoc operations within a single command."), gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(pathcountSpinner, gbcCollapsable);

        gbcCollapsable.gridx++;
        collapsiblePanel.add(pathcountCheckbox, gbcCollapsable);

        gbc.gridy++;
        CollapsiblePanel collapsiblePanel1 = new CollapsiblePanel("Advanced Options", collapsiblePanel);
        panel.add(collapsiblePanel1, gbc);

        gbc.gridx--;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        errorMessage = new JLabel("");
        errorMessage.setForeground(Color.decode("#FF4444"));
        panel.add(errorMessage, gbc);

        JBScrollPane jbScrollPane = new JBScrollPane(panel);
        jbScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Set the preferred height of the JScrollPane to take up the entire screen initially
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        jbScrollPane.setPreferredSize(new Dimension(jbScrollPane.getPreferredSize().width, (int) (screenSize.height * 0.8)));

        return jbScrollPane;
    }

    private boolean componentIsEnabled(JSpinner spinner) {
        return spinner.isEnabled();
    }

    private String getValue(JSpinner spinner) {
        return componentIsEnabled(spinner) ? spinner.getValue().toString() : null;
    }

    private String getValue(ComboBox comboBox) {
        return comboBox.getSelectedItem().toString();
    }

    @Override
    protected void doOKAction() {
        List<String> errors = new ArrayList<>();
        if (bucketComboBox.getItemCount() == 0) {
            errors.add("No available buckets. Please connect to a cluster.");
        }

        if (componentIsEnabled(numberCyclesSpinner) && populateOnlyComboBox.getSelectedItem() == "Enable") {
            errors.add("Number of Cycles is incompatible with Populate Only");
        }

        if (subdocComboBox.getSelectedItem() == "Enable" && jsonComboBox.getSelectedItem() == "Disable") {
            errors.add("Subdoc must be used with JSON");
        }

        if (componentIsEnabled(pathcountSpinner) && jsonComboBox.getSelectedItem() == "Disable") {
            errors.add("Pathcount must be used with JSON");
        }

        try {
            if (componentIsEnabled(lockSpinner) && componentIsEnabled(numberItemsSpinner) && componentIsEnabled(batchSizeSpinner) && componentIsEnabled(numberThreadsSpinner) && (int) numberItemsSpinner.getValue() < (int) batchSizeSpinner.getValue() * (int) numberThreadsSpinner.getValue()) {
                errors.add("Number of Items cannot be smaller than Batch Size multiplied to Number of Threads when used with Lock");
            }
        } catch (Exception e) {
            Log.error(e);
        }

        if (errors.isEmpty()) {
            super.doOKAction();
            try {
                PillowFightRunner pillowFightRunner = new PillowFightRunner();
                pillowFightRunner.runPillowFightCommand(getValue(bucketComboBox), getValue(durabilityComboBox), getValue(persistToSpinner), getValue(batchSizeSpinner), getValue(numberItemsSpinner), keyPrefixTextField.getText(), getValue(numberThreadsSpinner), getValue(percentageSpinner), getValue(noPopulationComboBox), getValue(populateOnlyComboBox), getValue(minSizeSpinner), getValue(maxSizeSpinner), getValue(numberCyclesSpinner), getValue(sequentialComboBox), getValue(startAtSpinner), getValue(timingsComboBox), getValue(expirySpinner), getValue(replicateToSpinner), getValue(lockSpinner), getValue(jsonComboBox), getValue(noopComboBox), getValue(subdocComboBox), getValue(pathcountSpinner));
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            errorMessage.setText("<html>" + errors.stream().collect(Collectors.joining("<br>")) + "</html>");
        }
    }
}