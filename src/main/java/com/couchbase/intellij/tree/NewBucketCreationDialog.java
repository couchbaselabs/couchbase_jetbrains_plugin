package com.couchbase.intellij.tree;

import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.*;
import com.couchbase.client.protostellar.admin.bucket.v1.EvictionMode;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.tree.overview.apis.CouchbaseRestAPI;
import com.couchbase.intellij.tree.overview.apis.ServerOverview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.jgoodies.forms.factories.Borders;
import org.jetbrains.annotations.Nullable;
import utils.Mouse;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.BiConsumer;

public class NewBucketCreationDialog extends DialogWrapper {
    private JTextField bucketName;
    private ButtonGroup bucketTypes;
    private ButtonGroup storageBackends;
    private JSpinner memQuota;
    private JCheckBox enableReplicas;
    private ComboBox<Integer> replicaNum;
    private JTextArea replicasErrorLabel;
    private JCheckBox replicateViewIndexes;
    private JCheckBox enableTTL;
    private JTextField bucketTTL;
    private ButtonGroup compressionMode;
    private ButtonGroup conflictResolutionType;
    private ButtonGroup ejectionMethod;
    private ButtonGroup bucketPriority;
    private ComboBox<String> minimumDurabilityLevel;
    private JCheckBox autoCompactionOverride;
    private JCheckBox compactionEnableOnPercentFragmentation;
    private JSpinner compactionOnPercentFragmentation;
    private JCheckBox compactionEnableOnSizeFragmentation;
    private JSpinner compactionOnSizeFragmentation;
    private JCheckBox enableViewCompactOnPercentageFragmentation;
    private JSpinner compactViewOnPercentFragmentation;
    private JCheckBox compactionViewEnableOnSizeFragmentation;
    private JSpinner compactionViewOnSizeFragmentation;
    private JCheckBox enableCompactTimeInterval;
    private JSpinner compactIntervalStartHour;
    private JSpinner compactIntervalStartMinute;
    private JSpinner compactIntervalEndHour;
    private JSpinner compactIntervalEndMinute;
    private JCheckBox abortCompactionsAtIntervalEnd;
    private JCheckBox compactInParallel;
    private JTextField metadataPurgeInterval;
    private JCheckBox enableFlush;
    private JLabel bucketNameError;

    private int totalMemQuota = 0;
    private int usedMemQuota = 0;
    private int nodeCount;
    private JLabel freeMemLabel;
    private JLabel thisBucketMemLabel;
    private JPanel freeMemBar;
    private JPanel thisBucketMemBar;
    private BiConsumer<Boolean, Throwable> listener;
    private final Component advoptSeparator = new JSeparator();

    public void show(BiConsumer<Boolean, Throwable> listener) {
        this.listener = listener;
        super.show();
    }

    @Override
    protected void doOKAction() {
        if (ActiveCluster.getInstance().isCapella()) {
            JsonObject payload = JsonObject.create()
                    .put("name", this.bucketName.getText())
                    .put("type", BucketType.valueOf(bucketTypes.getSelection().getActionCommand()).name().toLowerCase())
                    .put("storageBackend", StorageBackend.of(storageBackends.getSelection().getActionCommand()).toString().toLowerCase())
                    .put("memoryAllocationInMb", Long.valueOf(memQuota.getValue().toString()))
                    .put("replicas", enableReplicas.isSelected() ? (Integer) replicaNum.getSelectedItem() : 1)
                    .put("replicaIndexes", enableReplicas.isSelected() && replicateViewIndexes.isSelected())
                    .put("flush", enableFlush.isSelected())
                    .put("timeToLiveInSeconds", enableTTL.isSelected() ? Long.valueOf(bucketTTL.getText()) : 0)
                    .put("bucketConflictResolution", ConflictResolutionType.valueOf(conflictResolutionType.getSelection().getActionCommand()).name().toLowerCase())
                    .put("durabilityLevel", DurabilityLevel.valueOf(minimumDurabilityLevel.getSelectedItem().toString()).name().toLowerCase())
                    .put("evictionPolicy", ejectionMethod.getSelection() != null ? EvictionPolicyType.valueOf(ejectionMethod.getSelection().getActionCommand()).name().toLowerCase() : "");

            try {
                String response = CouchbaseRestAPI.callPostSingleEndpoint(
                        "/v4/organizations/{organizationId}/projects/{projectId}/clusters/{clusterId}/buckets",
                        ActiveCluster.getInstance().getClusterURL(), payload.toString());
                Messages.showMessageDialog(response, "Bucket Creation Response", Messages.getInformationIcon());
                if (listener != null) {
                    listener.accept(false, null);
                }
            } catch (Exception e) {
                Messages.showErrorDialog(e.getMessage(), "Failed to create bucket");
                if (listener != null) {
                    listener.accept(false, e);
                }
                return;
            }
        } else {
            Cluster cluster = ActiveCluster.getInstance().getCluster();
            if (cluster == null) {
                Messages.showMessageDialog("There is no active connection to run this query", "Couchbase Plugin Error",
                        Messages.getErrorIcon());
            }

            BucketSettings bs = BucketSettings.create(bucketName.getText());
            bs.bucketType(BucketType.valueOf(bucketTypes.getSelection().getActionCommand()));
            bs.storageBackend(StorageBackend.of(storageBackends.getSelection().getActionCommand()));
            bs.ramQuotaMB(Long.valueOf(memQuota.getValue().toString()));
            if (enableReplicas.isSelected()) {
                bs.numReplicas((Integer) replicaNum.getSelectedItem());
                if (replicateViewIndexes.isSelected()) {
                    bs.replicaIndexes(true);
                }
            }
            if (enableTTL.isSelected()) {
                bs.maxExpiry(Duration.of(Long.valueOf(bucketTTL.getText()), ChronoUnit.SECONDS));
            }
            bs.compressionMode(CompressionMode.valueOf(compressionMode.getSelection().getActionCommand()));
            bs.conflictResolutionType(
                    ConflictResolutionType.valueOf(conflictResolutionType.getSelection().getActionCommand()));
            if (ejectionMethod.getSelection() != null) {
                bs.evictionPolicy(EvictionPolicyType.valueOf(ejectionMethod.getSelection().getActionCommand()));
            }
            bs.minimumDurabilityLevel(DurabilityLevel.values()[minimumDurabilityLevel.getSelectedIndex()]);
            bs.flushEnabled(enableFlush.isSelected());

            try {
                cluster.buckets().createBucket(bs);
                if (listener != null) {
                    listener.accept(false, null);
                }
            } catch (Exception e) {
                Messages.showErrorDialog(e.getMessage(), "Failed to create bucket");
                if (listener != null) {
                    listener.accept(false, e);
                }
                return;
            }
        }
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        if (listener != null) {
            listener.accept(true, null);
        }
        super.doCancelAction();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        Cluster cluster = ActiveCluster.getInstance().getCluster();
        if (cluster == null) {
            Messages.showMessageDialog("There is no active connection to run this query", "Couchbase Plugin Error", Messages.getErrorIcon());
        }
        if (bucketName.getText().trim().isEmpty()) {
            return new ValidationInfo("bucket name cannot be empty", bucketName);
        }

        if (cluster.buckets().getAllBuckets().keySet().stream()
                .anyMatch(bucketName.getText()::equalsIgnoreCase)) {
            return new ValidationInfo("bucket with this name already exists", bucketName);
        }

        try {
            Integer quota = Integer.valueOf(memQuota.getValue().toString());
            if (quota < 100) {
                return new ValidationInfo("Invalid memory quota", memQuota);
            }
        } catch (Exception e) {
            return new ValidationInfo("Invalid memory quota", memQuota);
        }
        return super.doValidate();
    }

    protected NewBucketCreationDialog(@Nullable Project project) {
        super(project);

        init();
        setTitle("Create new bucket");
    }

    private void readClusterInformation() {
        Cluster cluster = ActiveCluster.getInstance().getCluster();
        if (cluster == null) {
            Messages.showMessageDialog("There is no active connection to run this query", "Couchbase Plugin Error", Messages.getErrorIcon());
            this.doCancelAction();
        }

        try {
            ServerOverview info = CouchbaseRestAPI.getOverview();
            nodeCount = info.getNodes().size();
            totalMemQuota = Math.toIntExact(info.getMemoryQuota());
            usedMemQuota = info.getBucketNames().stream().mapToInt(b -> {
                try {
                    return Math.toIntExact(CouchbaseRestAPI.getBucketOverview(b.getBucketName())
                            .getQuota().getRam() / 1024 / 1024);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).sum();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        readClusterInformation();
        JPanel mainFrame = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JBScrollPane(mainFrame);
        scrollPane.setBorder(Borders.EMPTY_BORDER);
        GridBagConstraints gbc = new GridBagConstraints();
        Insets internalInsets = gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        GridBagConstraints subGbc = new GridBagConstraints();
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        subGbc.anchor = GridBagConstraints.NORTHWEST;
        Insets sectionInsets = subGbc.insets = JBUI.insets(0, 0, 2, 5);
        subGbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel bucketNamePanel = new JPanel(new GridBagLayout());
        gbc.insets = JBUI.emptyInsets();
        mainFrame.add(bucketNamePanel, gbc);
        JLabel bucketNameLabel = new JLabel("Name");
        Font sectionLabelFont = new Font(
                bucketNameLabel.getFont().getName(),
                Font.BOLD,
                bucketNameLabel.getFont().getSize()
        );
        bucketNameLabel.setFont(sectionLabelFont);
        subGbc.insets = sectionInsets;
        bucketNamePanel.add(bucketNameLabel, subGbc);

        bucketName = new JTextField(40);
        bucketName.getDocument().addDocumentListener(new NameChangeListener());
        subGbc.gridy++;
        subGbc.fill = GridBagConstraints.HORIZONTAL;
        subGbc.gridwidth = GridBagConstraints.REMAINDER;
        subGbc.insets = internalInsets;
        bucketNamePanel.add(bucketName, subGbc);

        subGbc.gridy++;
        subGbc.fill = GridBagConstraints.NONE;
        subGbc.gridwidth = 1;
        bucketNameError = new JLabel("Bucket name cannot be empty");
        Font errorFont = new Font(
                bucketNameError.getFont().getName(),
                bucketNameError.getFont().getStyle(),
                (int) (bucketNameError.getFont().getSize() / 1.5)
        );
        Color errorColor = Color.decode("#FF4444");
        bucketNameError.setFont(errorFont);
        bucketNameError.setForeground(errorColor);
        bucketNameError.setVisible(false);
        bucketNamePanel.add(bucketNameError, subGbc);

        gbc.gridy++;
        gbc.insets = sectionInsets;
        JLabel bucketTypeLabel = new JLabel("Bucket type");
        bucketTypeLabel.setFont(sectionLabelFont);
        mainFrame.add(bucketTypeLabel, gbc);

        JPanel bucketTypePanel = new JPanel(new GridBagLayout());
        gbc.gridy++;
        gbc.insets = internalInsets;
        mainFrame.add(bucketTypePanel, gbc);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        Insets optionInsets = subGbc.insets = JBUI.insets(5, 0, 5, 5);
        subGbc.fill = GridBagConstraints.NONE;
        subGbc.anchor = GridBagConstraints.WEST;
        bucketTypes = new ButtonGroup();
        JRadioButton bucketTypeCouchbase = new JRadioButton("Couchbase");
        bucketTypeCouchbase.setActionCommand(BucketType.COUCHBASE.toString());
        bucketTypeCouchbase.setSelected(true);
        bucketTypes.add(bucketTypeCouchbase);
        bucketTypePanel.add(bucketTypeCouchbase, subGbc);

        JRadioButton bucketTypesMemcached = new JRadioButton("Memcached");
        bucketTypesMemcached.setActionCommand(BucketType.MEMCACHED.toString());
        bucketTypes.add(bucketTypesMemcached);
        subGbc.gridx++;
        bucketTypePanel.add(bucketTypesMemcached, subGbc);

        JRadioButton bucketTypesEphemeral = new JRadioButton("Ephemeral");
        bucketTypesEphemeral.setActionCommand(BucketType.EPHEMERAL.toString());
        bucketTypes.add(bucketTypesEphemeral);
        subGbc.gridx++;
        bucketTypePanel.add(bucketTypesEphemeral, subGbc);

        gbc.insets = sectionInsets;
        gbc.gridy++;
        JLabel storageBackendLabel = new JLabel("Storage Backend");
        storageBackendLabel.setFont(sectionLabelFont);
        mainFrame.add(storageBackendLabel, gbc);
        storageBackends = new ButtonGroup();

        JPanel storageBackendsPanel = new JPanel(new GridBagLayout());
        gbc.insets = internalInsets;
        gbc.gridy++;
        mainFrame.add(storageBackendsPanel, gbc);
        JRadioButton storageBackendCouchStore = new JRadioButton("CouchStore");
        storageBackendCouchStore.setActionCommand(StorageBackend.COUCHSTORE.alias());
        storageBackendCouchStore.setSelected(true);
        storageBackends.add(storageBackendCouchStore);
        subGbc.gridy = 0;
        subGbc.gridx = 0;
        storageBackendsPanel.add(storageBackendCouchStore, subGbc);

        JRadioButton storageBackendMagma = new JRadioButton("Magma");
        storageBackendMagma.setActionCommand(StorageBackend.MAGMA.alias());
        storageBackends.add(storageBackendMagma);
        subGbc.gridx++;
        storageBackendsPanel.add(storageBackendMagma, subGbc);

        JPanel memQuotaLabels = new JPanel(new GridBagLayout());
        gbc.insets = sectionInsets;
        gbc.gridy++;
        mainFrame.add(memQuotaLabels, gbc);

        JLabel memoryQuotaLabel = new JLabel("Memory Quota");
        memoryQuotaLabel.setFont(sectionLabelFont);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        subGbc.anchor = GridBagConstraints.BASELINE;
        subGbc.insets = JBUI.emptyInsets();
        memQuotaLabels.add(memoryQuotaLabel, subGbc);

        JLabel memQuotaInfo = new JLabel("in MegaBytes per server node");
        memQuotaInfo.setForeground(JBColor.GRAY);
        Font tipFont = new Font(
                memQuotaInfo.getFont().getName(),
                memQuotaInfo.getFont().getStyle(),
                (int) (memQuotaInfo.getFont().getSize() / 1.25)
        );
        memQuotaInfo.setFont(tipFont);
        subGbc.gridx++;
        subGbc.anchor = GridBagConstraints.BASELINE;
        subGbc.insets = JBUI.insets(0, 5, 2, 5);
        memQuotaLabels.add(memQuotaInfo, subGbc);

        JPanel memQuotaPanel = new JPanel(new GridBagLayout());
        gbc.gridy++;
        gbc.insets = internalInsets;
        mainFrame.add(memQuotaPanel, gbc);

        memQuota = new JSpinner(new SpinnerNumberModel(100, 1, totalMemQuota - usedMemQuota, 100));
        memQuota.setValue(100);
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) memQuota.getEditor();
        editor.getTextField().setColumns(10);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        subGbc.insets = JBUI.emptyInsets();
        memQuotaPanel.add(memQuota, subGbc);
        subGbc.gridx++;
        JLabel memQuotaLabel = new JLabel("MiB");
        memQuotaLabel.setForeground(JBColor.GRAY);
        memQuotaPanel.add(memQuotaLabel, subGbc);

        GridBagLayout memStatusLayout = new GridBagLayout();
        JPanel memStatus = new JPanel(memStatusLayout);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy++;
        gbc.insets = JBUI.insets(5, 10);
        mainFrame.add(memStatus, gbc);

        JPanel usedMemBar = new JPanel();
        Color usedMemColor = new JBColor(new Color(142, 183, 230), new Color(142, 183, 230));
        usedMemBar.setBackground(usedMemColor);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        subGbc.fill = GridBagConstraints.HORIZONTAL;
        subGbc.weightx = (double) (usedMemQuota) / totalMemQuota;
        memStatus.add(usedMemBar, subGbc);

        thisBucketMemBar = new JPanel();
        Color thisBucketMemColor = new JBColor(new Color(179, 207, 239), new Color(179, 207, 239));
        thisBucketMemBar.setBackground(thisBucketMemColor);
        subGbc.weightx = (double) 100 / totalMemQuota;
        subGbc.gridx++;
        memStatus.add(thisBucketMemBar, subGbc);

        freeMemBar = new JPanel();
        Color freeMemColor = new JBColor(new Color(239, 235, 224), new Color(239, 235, 224));
        freeMemBar.setBackground(freeMemColor);
        subGbc.weightx = (double) (totalMemQuota - usedMemQuota - 100) / totalMemQuota;
        subGbc.gridx++;
        memStatus.add(freeMemBar, subGbc);

        subGbc.gridy = 0;
        subGbc.gridx = 0;
        subGbc.insets = internalInsets;
        subGbc.weightx = 1;
        JPanel memLegendPanel = new JPanel(new GridBagLayout());
        gbc.gridy++;
        mainFrame.add(memLegendPanel, gbc);

        JPanel usedMemLegend = new JPanel();
        usedMemLegend.setBackground(usedMemColor);
        memLegendPanel.add(usedMemLegend, subGbc);
        JLabel usedMemLabel = new JLabel(String.format("other buckets (%dMiB)", usedMemQuota));
        subGbc.gridx++;
        subGbc.fill = GridBagConstraints.HORIZONTAL;
        subGbc.weightx = 1;
        memLegendPanel.add(usedMemLabel, subGbc);

        JPanel thisBucketMemLegend = new JPanel();
        thisBucketMemLegend.setBackground(thisBucketMemColor);
        subGbc.gridx++;
        memLegendPanel.add(thisBucketMemLegend, subGbc);
        thisBucketMemLabel = new JLabel(String.format("this bucket (%dMiB)", 100));
        subGbc.gridx++;
        memLegendPanel.add(thisBucketMemLabel, subGbc);

        JPanel freeMemLegend = new JPanel();
        freeMemLegend.setBackground(freeMemColor);
        subGbc.gridx++;
        memLegendPanel.add(freeMemLegend, subGbc);
        freeMemLabel = new JLabel(String.format("available (%dMiB)", totalMemQuota));
        subGbc.gridx++;
        memLegendPanel.add(freeMemLabel, subGbc);
        subGbc.fill = GridBagConstraints.NONE;

        memQuota.addChangeListener(changeEvent -> {
            GridBagConstraints tbc = memStatusLayout.getConstraints(thisBucketMemBar);
            int quota = Integer.valueOf(memQuota.getValue().toString());
            tbc.weightx = (double) quota / totalMemQuota;
            memStatusLayout.setConstraints(thisBucketMemBar, tbc);
            tbc = memStatusLayout.getConstraints(freeMemBar);
            tbc.weightx = (double) (totalMemQuota - usedMemQuota - quota) / totalMemQuota;
            memStatusLayout.setConstraints(freeMemBar, tbc);
            thisBucketMemLabel.setText(String.format("this bucket (%dMiB)", quota));
            memStatus.invalidate();
            usedMemBar.invalidate();
            thisBucketMemBar.invalidate();
            freeMemBar.invalidate();
            this.pack();
        });

        JPanel optionsLabelPanel = new JPanel(new GridBagLayout());
        gbc.insets = JBUI.emptyInsets();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy++;
        mainFrame.add(optionsLabelPanel, gbc);
        gbc.fill = GridBagConstraints.NONE;

        final Icon closedIcon = IconLoader.getIcon("/assets/icons/triangle-right.svg", getClass().getClassLoader());
        final Icon openedIcon = IconLoader.getIcon("/assets/icons/triangle-down.svg", getClass().getClassLoader());
        JLabel optionsLabel = new JLabel("Advanced bucket settings", closedIcon, SwingConstants.CENTER);
        subGbc.gridy = 0;
        subGbc.gridx = 0;
        subGbc.insets = sectionInsets;
        subGbc.fill = GridBagConstraints.NONE;
        subGbc.anchor = GridBagConstraints.WEST;
        subGbc.weightx = 1;
        optionsLabelPanel.add(optionsLabel, subGbc);

        subGbc.gridx++;
        subGbc.fill = GridBagConstraints.HORIZONTAL;
        subGbc.insets = JBUI.emptyInsets();
        subGbc.weightx = 100;
        advoptSeparator.setVisible(false);
        optionsLabelPanel.add(advoptSeparator, subGbc);
        subGbc.fill = GridBagConstraints.NONE;
        subGbc.gridx--;

        JPanel advancedOptionsPanel = new JPanel(new GridBagLayout());
        advancedOptionsPanel.setVisible(false);
        gbc.gridy++;
        gbc.insets = internalInsets;
        mainFrame.add(advancedOptionsPanel, gbc);
        optionsLabel.addMouseListener(Mouse.click(mouseEvent -> {
            advancedOptionsPanel.setVisible(!advancedOptionsPanel.isVisible());
            advoptSeparator.setVisible(advancedOptionsPanel.isVisible());
            if (advancedOptionsPanel.isVisible()) {
                optionsLabel.setIcon(openedIcon);
            } else {
                optionsLabel.setIcon(closedIcon);
            }

            this.pack();
            SwingUtilities.invokeLater(() -> {

                int locLeft = (int) Math.max(1, (Toolkit.getDefaultToolkit().getScreenSize().getWidth()) / 2 - getSize().getWidth() / 2);
                int locTop = (int) Math.max(1, (Toolkit.getDefaultToolkit().getScreenSize().getHeight()) / 2 - getSize().getHeight() / 2);
                setLocation(locLeft, locTop);
                this.pack();
            });
        }));

        JLabel replicasLabel = new JLabel("Replicas");
        replicasLabel.setFont(sectionLabelFont);
        gbc.insets = sectionInsets;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        advancedOptionsPanel.add(replicasLabel, gbc);

        enableReplicas = new JCheckBox("Enable");
        enableReplicas.setSelected(true);
        gbc.insets = internalInsets;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        advancedOptionsPanel.add(enableReplicas, gbc);

        replicaNum = new ComboBox<Integer>(new Integer[]{1, 2, 3});
        gbc.gridx++;
        advancedOptionsPanel.add(replicaNum, gbc);

        JLabel replicaNumLabel = new JLabel("Number of replica (backup) copies");
        replicaNumLabel.setFont(tipFont);
        replicaNumLabel.setForeground(JBColor.GRAY);
        gbc.gridx++;
        advancedOptionsPanel.add(replicaNumLabel, gbc);

        JPanel replicasErrorPanel = new JPanel(new GridBagLayout());
        replicasErrorPanel.setVisible(nodeCount <= 1);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        advancedOptionsPanel.add(replicasErrorPanel, gbc);
        replicasErrorLabel = new JTextArea("Warning: you do not have enough data servers or server groups to support this number of replicas.");
        replicasErrorLabel.setEditable(false);
        replicasErrorLabel.setFont(errorFont);
        replicasErrorLabel.setForeground(errorColor);
        replicasErrorLabel.setBackground(replicasErrorPanel.getBackground());
        replicasErrorLabel.setLineWrap(true);
        replicasErrorLabel.setWrapStyleWord(true);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        subGbc.fill = GridBagConstraints.HORIZONTAL;
        subGbc.anchor = GridBagConstraints.WEST;
        subGbc.insets = JBUI.emptyInsets();
        replicasErrorPanel.add(replicasErrorLabel, subGbc);

        replicaNum.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int num = (int) replicaNum.getSelectedItem();
                replicasErrorPanel.setVisible(num >= nodeCount);
            }
        });

        replicateViewIndexes = new JCheckBox("Replicate view indexes");
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy++;
        advancedOptionsPanel.add(replicateViewIndexes, gbc);

        enableReplicas.addChangeListener(changeEvent -> {
            boolean e = enableReplicas.isSelected();
            replicaNum.setEnabled(e);
            replicateViewIndexes.setEnabled(e);
        });

        JLabel bucketMaxTTL = new JLabel("Bucket Max Time-To-Live");
        bucketMaxTTL.setFont(sectionLabelFont);
        gbc.insets = sectionInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(bucketMaxTTL, gbc);

        enableTTL = new JCheckBox("Enable");
        gbc.insets = internalInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(enableTTL, gbc);

        bucketTTL = new JTextField("0", 5);
        bucketTTL.setEnabled(false);
        enableTTL.addChangeListener(changeEvent -> bucketTTL.setEnabled(enableTTL.isSelected()));
        gbc.insets = JBUI.emptyInsets();
        gbc.gridx++;
        advancedOptionsPanel.add(bucketTTL, gbc);

        JLabel bucketTTLLabel = new JLabel("seconds");
        bucketTTLLabel.setFont(tipFont);
        bucketTTLLabel.setForeground(JBColor.GRAY);
        gbc.gridx++;
        advancedOptionsPanel.add(bucketTTLLabel, gbc);

        JLabel compressionModeSectionLabel = new JLabel("Compression Mode");
        compressionModeSectionLabel.setFont(sectionLabelFont);
        gbc.insets = sectionInsets;
        gbc.gridx = 0;
        gbc.gridy++;
        advancedOptionsPanel.add(compressionModeSectionLabel);

        compressionMode = new ButtonGroup();
        JPanel compressionModesPanel = new JPanel(new GridBagLayout());
        gbc.gridy++;
        gbc.insets = internalInsets;
        advancedOptionsPanel.add(compressionModesPanel, gbc);

        JRadioButton compressionModeOff = new JRadioButton("Off");
        compressionModeOff.setActionCommand(CompressionMode.OFF.toString());
        compressionMode.add(compressionModeOff);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        subGbc.insets = optionInsets;
        compressionModesPanel.add(compressionModeOff, subGbc);

        JRadioButton compressionModePassive = new JRadioButton("Passive");
        compressionModePassive.setActionCommand(CompressionMode.PASSIVE.toString());
        compressionModePassive.setSelected(true);
        compressionMode.add(compressionModePassive);
        subGbc.gridx++;
        compressionModesPanel.add(compressionModePassive, subGbc);

        JRadioButton compressionModeActive = new JRadioButton("Active");
        compressionModeActive.setActionCommand(CompressionMode.ACTIVE.toString());
        compressionMode.add(compressionModeActive);
        subGbc.gridx++;
        compressionModesPanel.add(compressionModeActive, subGbc);

        JLabel conflictResolutionSectionLabel = new JLabel("Conflict Resolution");
        conflictResolutionSectionLabel.setFont(sectionLabelFont);
        gbc.insets = sectionInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(conflictResolutionSectionLabel, gbc);

        JPanel confictResolutionPanel = new JPanel(new GridBagLayout());
        gbc.gridy++;
        gbc.insets = internalInsets;
        advancedOptionsPanel.add(confictResolutionPanel, gbc);

        conflictResolutionType = new ButtonGroup();
        JRadioButton conflictResolutionSequenceNo = new JRadioButton("Sequence number");
        conflictResolutionSequenceNo.setSelected(true);
        conflictResolutionSequenceNo.setActionCommand(ConflictResolutionType.SEQUENCE_NUMBER.toString());
        conflictResolutionType.add(conflictResolutionSequenceNo);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        confictResolutionPanel.add(conflictResolutionSequenceNo, subGbc);

        JRadioButton conflictResolutionTimestamp = new JRadioButton("Timestamp");
        conflictResolutionTimestamp.setActionCommand(ConflictResolutionType.TIMESTAMP.toString());
        conflictResolutionType.add(conflictResolutionTimestamp);
        subGbc.gridx++;
        confictResolutionPanel.add(conflictResolutionTimestamp, subGbc);

        JLabel ejectionMethodSectionLabel = new JLabel("Ejection Method");
        ejectionMethodSectionLabel.setFont(sectionLabelFont);
        gbc.gridy++;
        gbc.insets = sectionInsets;
        advancedOptionsPanel.add(ejectionMethodSectionLabel, gbc);

        JPanel ejectionMethodPanel = new JPanel(new GridBagLayout());
        gbc.insets = internalInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(ejectionMethodPanel, gbc);

        ejectionMethod = new ButtonGroup();
        JRadioButton ejectionMethodValue = new JRadioButton("Value-only");
        ejectionMethodValue.setActionCommand(EvictionMode.EVICTION_MODE_VALUE_ONLY.toString());
        ejectionMethod.add(ejectionMethodValue);
        subGbc.gridx = 0;
        subGbc.gridy = 0;
        ejectionMethodPanel.add(ejectionMethodValue, subGbc);

        JRadioButton ejectionMethodFull = new JRadioButton("Full");
        ejectionMethodFull.setActionCommand(EvictionMode.EVICTION_MODE_FULL.toString());
        ejectionMethod.add(ejectionMethodFull);
        subGbc.gridx++;
        ejectionMethodPanel.add(ejectionMethodFull, subGbc);

        JLabel minimumDurabilitySectionLabel = new JLabel("Minimum Durability Level");
        minimumDurabilitySectionLabel.setFont(sectionLabelFont);
        gbc.insets = sectionInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(minimumDurabilitySectionLabel, gbc);

        minimumDurabilityLevel = new ComboBox<String>(new String[]{
                "none", "majority", "majorityAndPersistActive", "persistToMajority"
        });
        gbc.insets = internalInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(minimumDurabilityLevel, gbc);

        JLabel flushSection = new JLabel("Flush");
        flushSection.setFont(sectionLabelFont);
        gbc.insets = sectionInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(flushSection, gbc);

        enableFlush = new JCheckBox("Enable");
        gbc.insets = internalInsets;
        gbc.gridy++;
        advancedOptionsPanel.add(enableFlush, gbc);


        return scrollPane;
    }

    private class NameChangeListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            bucketNameError.setVisible(bucketName.getText().trim().isEmpty());
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            bucketNameError.setVisible(bucketName.getText().trim().isEmpty());
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
        }
    }
}
