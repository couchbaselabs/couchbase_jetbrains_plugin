package com.couchbase.intellij.tree.overview;

import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.tree.overview.apis.*;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

import static utils.TemplateUtil.*;

public class ServerOverviewDialog extends DialogWrapper {

    public static final String GENERAL = "General";
    private SidePanel sidePanel;
    private JPanel rightPanel;
    private ServerOverview overview;
    private List<String> nodes;

    private Map<String, BucketSettings> bucketMap;
    private Project project;


    public ServerOverviewDialog(boolean canBeParent) {
        super(canBeParent);
        getPeer().getWindow().setMinimumSize(new Dimension(860, 800));
        getPeer().getWindow().setMaximumSize(new Dimension(860, 800));
        setResizable(false);
        init();
    }

    public static String mbToGb(long sizeInMb) {
        if (sizeInMb < 1024) {
            return sizeInMb + " Mb";
        } else {
            double sizeInGb = sizeInMb / 1024.0;
            return String.format("%.2f Gb", sizeInGb);
        }
    }


    private JPanel getErrorPanel(Exception e) {
        JPanel error = new JPanel();
        Log.error("Could not get the server overview.", e);
        error.add(new JLabel("An error occurred while trying to get the server overview"));
        return error;
    }

    @Override
    protected JComponent createCenterPanel() {
        Splitter splitter = new Splitter(false, 0.2f);
        sidePanel = new SidePanel(e -> {

            if ("General".equals(e)) {
                rightPanel.removeAll();
                rightPanel.add(getOverviewPanel());
            } else if (bucketMap.containsKey(e)) {
                rightPanel.removeAll();
                rightPanel.add(getBucketPanel(e));
            } else if (nodes.contains(e)) {
                rightPanel.removeAll();
                rightPanel.add(getNodePanel(e));
            } else {
                throw new IllegalStateException("Not implemented Yet");
            }

            rightPanel.revalidate();
        });


        rightPanel = new JPanel(new BorderLayout());
        splitter.setFirstComponent(sidePanel);
        splitter.setSecondComponent(rightPanel);

        try {
            overview = CouchbaseRestAPI.getOverview();
            bucketMap = ActiveCluster.getInstance().get().buckets().getAllBuckets();

            sidePanel.addSeparator("Overview");
            sidePanel.addPlace(GENERAL);

            sidePanel.addSeparator("Nodes");
            nodes = overview.getNodes().stream().map(CBNode::getHostname).collect(Collectors.toList());
            nodes.forEach(e -> sidePanel.addPlace(e));

            sidePanel.addSeparator("Buckets");
            bucketMap.keySet().forEach(e -> sidePanel.addPlace(e));

        } catch (Exception e) {
            e.printStackTrace();
            Log.error("An error occurred while trying to load the cluster overview", e);
            rightPanel.add(getErrorPanel(e));
        }

        sidePanel.select(GENERAL);

        return splitter;
    }

    private JPanel getBucketPanel(String bucket) {

        JPanel panel = new JPanel();
        panel.setBorder(JBUI.Borders.empty(10, 15));


        try {
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            var btOverview = CouchbaseRestAPI.getBucketOverview(bucket);


            String[] keys = {"Type", "Storage Backend", "Replicas", "Eviction Policy", "Durability Level", "Max TTL",
                    "Compression Mode", "Conflict Resolution"};
            String[] values = {
                    btOverview.getBucketType().replace("membase", "Couchbase"),
                    btOverview.getStorageBackend(),
                    String.valueOf(btOverview.getReplicaNumber()),
                    btOverview.getEvictionPolicy(),
                    btOverview.getDurabilityMinLevel(),
                    String.valueOf(btOverview.getMaxTTL()),
                    btOverview.getCompressionMode(),
                    btOverview.getConflictResolutionType()
            };
            JPanel keyValuesPanel = createKeyValuePanel(keys, values, 1);
            panel.add(keyValuesPanel);

            panel.add(getSeparator("Quota"));

            String[] ramKeys = {"RAM", "Raw RAM"};
            String[] ramValues = {fmtByte(btOverview.getQuota().getRam()), fmtByte(btOverview.getQuota().getRawRAM())};
            JPanel ramPanel = createKeyValuePanel(ramKeys, ramValues, 2);
            panel.add(ramPanel);

            panel.add(getSeparator("Basic Stats"));
            String[] statsKeys = {"Ops per Sec", "Disk Fetches", "Item Count", "Data Used", "Disk Used", "Memory Used", "# Active vBucket Non Residents"};
            String[] statsValues = {String.format("%.3f", btOverview.getBasicStats().getOpsPerSec()),
                    String.valueOf(btOverview.getBasicStats().getDiskFetches()),
                    formatNumber(btOverview.getBasicStats().getItemCount()),
                    fmtByte(btOverview.getBasicStats().getDataUsed()),
                    fmtByte(btOverview.getBasicStats().getDiskUsed()),
                    fmtByte(btOverview.getBasicStats().getMemUsed()),
                    String.valueOf(btOverview.getBasicStats().getVbActiveNumNonResident())
            };
            JPanel statsPanel = createKeyValuePanel(statsKeys, statsValues, 2);
            panel.add(statsPanel);

        } catch (Exception e) {
            String message = "An error occurred while loading the bucket overview";
            e.printStackTrace();
            Log.error(message, e);
            panel.add(new JLabel(message));
        }
        return panel;
    }

    private JPanel getOverviewPanel() {

        JPanel panel = new JPanel();
        panel.setBorder(JBUI.Borders.empty(10, 15));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String versions = overview.getNodes().stream().map(CBNode::getVersion).distinct().collect(Collectors.joining(", "));
        String status = overview.getNodes().stream().map(CBNode::getStatus).distinct().collect(Collectors.joining(", "));
        status = status.replace("healthy", "<html><span style='color: #6A8759'><strong>Healthy</strong></span></html>");
        String services = overview.getNodes().stream().flatMap(node -> node.getServices().stream()).distinct().collect(Collectors.joining(", "));
        services = formatServices(services);

        String os = overview.getNodes().stream().map(CBNode::getOs).distinct().collect(Collectors.joining(", "));


        String[] keys = {"Couchbase Version", "Status", "Services", "Nodes", "Buckets", "OS"};
        String[] values = {versions, status, services, String.valueOf(overview.getNodes().size()), String.valueOf(bucketMap.size()), os};
        JPanel keyValuesPanel = createKeyValuePanel(keys, values, 1);


        String[] quotaKeys = {"Data", "Index", "Search", "Eventing", "Analytics"};
        String[] quotaValues = {mbToGb(overview.getMemoryQuota()), mbToGb(overview.getIndexMemoryQuota()), mbToGb(overview.getFtsMemoryQuota()), mbToGb(overview.getEventingMemoryQuota()), mbToGb(overview.getCbasMemoryQuota())};
        JPanel quotaPanel = createKeyValuePanel(quotaKeys, quotaValues, 2);

        panel.setBorder(JBUI.Borders.emptyTop(10));
        panel.add(keyValuesPanel);
        panel.add(getSeparator("Quota"));
        panel.add(quotaPanel);

        panel.add(getSeparator("RAM"));
        String[] ramKeys = {"Total", "Used", "Quota Total", "Quota Used", "Quota Used per Node", "Quota Total per Node", "Used by Data"};
        RAM ram = overview.getStorageTotals().getRam();
        String[] ramValues = {fmtByte(ram.getTotal()), fmtByte(ram.getUsed()), fmtByte(ram.getQuotaTotal()), fmtByte(ram.getQuotaUsed()), fmtByte(ram.getQuotaUsedPerNode()), fmtByte(ram.getQuotaTotalPerNode()), fmtByte(ram.getUsedByData())};
        JPanel ramPanel = createKeyValuePanel(ramKeys, ramValues, 2);
        panel.add(ramPanel);

        panel.add(getSeparator("Storage"));
        String[] hddKeys = {"Total", "Used", "Quota Total", "Used by Data", "Free"};
        HDD hdd = overview.getStorageTotals().getHdd();
        String[] hddValues = {fmtByte(hdd.getTotal()), fmtByte(hdd.getUsed()), fmtByte(hdd.getQuotaTotal()), fmtByte(hdd.getUsedByData()), fmtByte(hdd.getFree())};
        JPanel hddPanel = createKeyValuePanel(hddKeys, hddValues, 2);
        panel.add(hddPanel);

        return panel;
    }

    private JPanel getNodePanel(String hostname) {
        CBNode node = overview.getNodes().stream().filter(e -> hostname.equals(e.getHostname())).toList().get(0);

        JPanel panel = new JPanel();
        panel.setBorder(JBUI.Borders.empty(10, 15));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String status = node.getStatus().replace("healthy", "<html><span style='color: #6A8759'><strong>Healthy</strong></span></html>");
        String services = formatServices(String.join(", ", node.getServices()));


        String[] keys = {"Couchbase Version", "Status", "Membership", "Services", "OS", "Hostname", "Node Encryption", "Up Time"};
        String[] values = {node.getVersion(), status, node.getClusterMembership(), services, node.getOs(), node.getHostname(), String.valueOf(node.isNodeEncryption()), formatDuration(Long.parseLong(node.getUptime()))};
        JPanel keyValuesPanel = createKeyValuePanel(keys, values, 1);
        panel.add(keyValuesPanel);

        panel.add(getSeparator("Hardware"));
        String[] hardwareKeys = {"Total Memory", "Free Memory", "Reserved MCD Memory", "Allocated MCD Memory", "CPUs"};
        String[] hardwareValues = {fmtByte(node.getMemoryTotal()), fmtByte(node.getMemoryFree()), fmtByte(node.getMcdMemoryReserved()), fmtByte(node.getMcdMemoryAllocated()), String.valueOf(node.getCpuCount())};
        JPanel hardwarePanel = createKeyValuePanel(hardwareKeys, hardwareValues, 2);
        panel.add(hardwarePanel);

        panel.add(getSeparator("System Stats"));
        String[] systemKeys = {"CPU Utilization Rate", "Swap Total", "Total Memory", "Memory Limit", "CPU Stole Rate", "Swap Used", "Free Memory", "Cores Available"};
        String[] systemValues = {String.format("%.3f", node.getSystemStats().getCpu_utilization_rate()),
                fmtByte( node.getSystemStats().getSwap_total()),
                fmtByte(node.getSystemStats().getMem_total()),
                fmtByte(node.getSystemStats().getMem_limit()),
                String.format("%.3f", node.getSystemStats().getCpu_stolen_rate()),
                fmtByte( node.getSystemStats().getSwap_used()),
                fmtByte(node.getSystemStats().getMem_free()),
                String.valueOf(node.getSystemStats().getCpu_cores_available())

        };
        JPanel systemPanel = createKeyValuePanel(systemKeys, systemValues, 2);
        panel.add(systemPanel);

        panel.add(getSeparator("Interesting Stats"));
        String[] interKeys = {"Documents Data Size", "Documents Data Size on Disk", "Spatial Data Size", "Spatial Data Size on Disk",
                "Views Data Size", "Views Data Size on Disk", "Items", "Total Items", "Ep. Bg. Fetched", "Hits", "Index Data Size",
                "Index Data Size on Disk", "Memory Used", "Ops", "# vBucket Non Resident", "Current vBucket Replica Items"};
        String[] interValues = {fmtByte(node.getInterestingStats().getCouch_docs_data_size()),
                fmtByte(node.getInterestingStats().getCouch_docs_actual_disk_size()),
                fmtByte(node.getInterestingStats().getCouch_spatial_data_size()),
                fmtByte(node.getInterestingStats().getCouch_spatial_disk_size()),
                fmtByte(node.getInterestingStats().getCouch_views_data_size()),
                fmtByte(node.getInterestingStats().getCouch_views_actual_disk_size()),
                formatNumber(node.getInterestingStats().getCurr_items()),
                formatNumber(node.getInterestingStats().getCurr_items_tot()),
                String.valueOf(node.getInterestingStats().getEp_bg_fetched()),
                String.format("%.3f", node.getInterestingStats().getGet_hits()),
                fmtByte(node.getInterestingStats().getIndex_data_size()),
                fmtByte(node.getInterestingStats().getIndex_disk_size()),
                fmtByte(node.getInterestingStats().getMem_used()),
                String.format("%.3f", node.getInterestingStats().getOps()),
                String.valueOf(node.getInterestingStats().getVb_active_num_non_resident()),
                String.valueOf(node.getInterestingStats().getVb_replica_curr_items())
        };
        JPanel interPanel = createKeyValuePanel(interKeys, interValues, 2);
        panel.add(interPanel);


        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JBScrollPane(panel), BorderLayout.CENTER);
        return mainPanel;
    }


    private String formatServices(String services) {
        return services.replace("backup", "Backup").replace("eventing", "Eventing").replace("fts", "Search").replace("index", "Index").replace("kv", "Data").replace("n1ql", "Query");
    }


    @Override
    protected Action @NotNull [] createActions() {
        Action exportAction = new AbstractAction("Export") {
            @Override
            public void actionPerformed(ActionEvent e) {

                FileSaverDescriptor fsd = new FileSaverDescriptor("Export Cluster Overview", "Choose where you want to save the file:");
                VirtualFileWrapper wrapper = FileChooserFactory.getInstance().createSaveFileDialog(fsd, project).save(("ExportedContent.pdf"));
                if (wrapper != null) {
                    File file = wrapper.getFile();
                    exportContent(file.getAbsolutePath());
                }
            }
        };

        return new Action[]{exportAction, getOKAction()};
    }

    private void exportContent(String filePath) {
        List<CBNode> nodes = overview.getNodes();
        List<BucketName> buckets = overview.getBucketNames();

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Exporting cluster overview", false) {
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try (PDDocument document = new PDDocument()) {
                    createPageAndAddHeader(document, "Server Overview");
                    addServerOverviewContent(document, overview);

                    createPageAndAddHeader(document, "Nodes");

                    int totalNodes = nodes.size();

                    for (int i = 0; i < totalNodes; i++) {
                        CBNode node = nodes.get(i);
                        boolean isFirstNode = (i == 0); // Check if the current node is the first one
                        addNodeContent(document, node, isFirstNode);
                    }

                    createPageAndAddHeader(document, "Buckets");

                    int totalBuckets = buckets.size();

                    for (int i = 0; i < totalBuckets; i++) {
                        BucketName bucket = buckets.get(i);
                        boolean isFirstBucket = (i == 0); // Check if the current bucket is the first one
                        addBucketsContent(document, bucket, isFirstBucket);
                    }

                    document.save(filePath);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Log.info("File " + filePath + " was exported successfully");
                        Messages.showInfoMessage("File saved successfully.", "Cluster Overview Export");
                    });
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("An error occurred while trying to export the Cluster Overview", "Cluster Overview Export Error"));
                    Log.error(e);
                    e.printStackTrace();
                }
            }
        });
    }

    private void createPageAndAddHeader(PDDocument document, String header) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, 700);
        contentStream.showText(header);
        contentStream.endText();
        contentStream.close();
    }

    private void addServerOverviewContent(PDDocument document, ServerOverview overview) throws IOException {
        PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true);

        int yOffset = 670;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("General");
        contentStream.endText();

        String versions = overview.getNodes().stream().map(CBNode::getVersion).distinct().collect(Collectors.joining(", "));
        String status = overview.getNodes().stream().map(CBNode::getStatus).distinct().collect(Collectors.joining(", "));
        status = status.replace("healthy", "Healthy");
        String services = overview.getNodes().stream().flatMap(node -> node.getServices().stream()).distinct().collect(Collectors.joining(", "));
        services = formatServices(services);

        String os = overview.getNodes().stream().map(CBNode::getOs).distinct().collect(Collectors.joining(", "));

        String[] keys = {"Couchbase Version", "Status", "Services", "Nodes", "Buckets", "OS"};
        String[] values = {versions, status, services, String.valueOf(overview.getNodes().size()), String.valueOf(bucketMap.size()), os};

        yOffset -= 10;

        for (int i = 0; i < keys.length; i++) {
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(keys[i] + ": " + values[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("Quota");
        contentStream.endText();

        yOffset -= 10;

        String[] quotaKeys = {"Data", "Index", "Search", "Eventing", "Analytics"};
        String[] quotaValues = {mbToGb(overview.getMemoryQuota()), mbToGb(overview.getIndexMemoryQuota()), mbToGb(overview.getFtsMemoryQuota()), mbToGb(overview.getEventingMemoryQuota()), mbToGb(overview.getCbasMemoryQuota())};

        for (int i = 0; i < quotaKeys.length; i++) {
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(quotaKeys[i] + ": " + quotaValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("RAM");
        contentStream.endText();

        yOffset -= 10;

        String[] ramKeys = {"Total", "Used", "Quota Total", "Quota Used", "Quota Used per Node", "Quota Total per Node", "Used by Data"};
        RAM ram = overview.getStorageTotals().getRam();
        String[] ramValues = {fmtByte(ram.getTotal()), fmtByte(ram.getUsed()), fmtByte(ram.getQuotaTotal()), fmtByte(ram.getQuotaUsed()), fmtByte(ram.getQuotaUsedPerNode()), fmtByte(ram.getQuotaTotalPerNode()), fmtByte(ram.getUsedByData())};

        for (int i = 0; i < ramKeys.length; i++) {
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(ramKeys[i] + ": " + ramValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("Storage");
        contentStream.endText();

        yOffset -= 10;

        String[] hddKeys = {"Total", "Used", "Quota Total", "Used by Data", "Free"};
        HDD hdd = overview.getStorageTotals().getHdd();
        String[] hddValues = {fmtByte(hdd.getTotal()), fmtByte(hdd.getUsed()), fmtByte(hdd.getQuotaTotal()), fmtByte(hdd.getUsedByData()), fmtByte(hdd.getFree())};

        for (int i = 0; i < hddKeys.length; i++) {
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(hddKeys[i] + ": " + hddValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        contentStream.close();
    }


    private void addNodeContent(PDDocument document, CBNode cbNode, boolean isFirstNode) throws IOException {
        String nodeName = cbNode.getHostname();
        CBNode node = overview.getNodes().stream().filter(e -> nodeName.equals(e.getHostname())).toList().get(0);

        PDPage currentPage;
        if (isFirstNode) {
                currentPage = document.getPage(document.getNumberOfPages() - 1);
        } else {
            currentPage = new PDPage();
            document.addPage(currentPage);
        }

        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true);

        int yOffset = 670;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText(nodeName);
        contentStream.endText();

        String nodeStatus = node.getStatus().replace("healthy", "Healthy");
        String nodeServices = formatServices(String.join(", ", node.getServices()));
        String[] nodeKeys = {"Couchbase Version", "Status", "Membership", "Services", "OS", "Hostname", "Node Encryption", "Up Time"};
        String[] nodeValues = {node.getVersion(), nodeStatus, node.getClusterMembership(), nodeServices, node.getOs(), node.getHostname(), String.valueOf(node.isNodeEncryption()), formatDuration(Long.parseLong(node.getUptime()))};

        yOffset -= 10;

        for (int i = 0; i < nodeKeys.length; i++) {
            if (yOffset < 50) {
                contentStream.close();
                PDPage nextPage = new PDPage();
                document.addPage(nextPage);
                contentStream = new PDPageContentStream(document, nextPage);
                yOffset = 700;
            }

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(nodeKeys[i] + ": " + nodeValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        if (yOffset < 50) {
            contentStream.close();
            PDPage nextPage = new PDPage();
            document.addPage(nextPage);
            contentStream = new PDPageContentStream(document, nextPage);
            yOffset = 700;
        }

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("Hardware");
        contentStream.endText();

        yOffset -= 10;

        String[] hardwareKeys = {"Total Memory", "Free Memory", "Reserved MCD Memory", "Allocated MCD Memory", "CPUs"};
        String[] hardwareValues = {fmtByte(node.getMemoryTotal()), fmtByte(node.getMemoryFree()), fmtByte(node.getMcdMemoryReserved()), fmtByte(node.getMcdMemoryAllocated()), String.valueOf(node.getCpuCount())};

        for (int i = 0; i < hardwareKeys.length; i++) {
            if (yOffset < 50) {
                contentStream.close();
                PDPage nextPage = new PDPage();
                document.addPage(nextPage);
                contentStream = new PDPageContentStream(document, nextPage);
                yOffset = 700;
            }

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(hardwareKeys[i] + ": " + hardwareValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        if (yOffset < 50) {
            contentStream.close();
            PDPage nextPage = new PDPage();
            document.addPage(nextPage);
            contentStream = new PDPageContentStream(document, nextPage);
            yOffset = 700;
        }

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("System Stats");
        contentStream.endText();

        yOffset -= 10;

        SystemStats systemStats = node.getSystemStats();
        String[] systemKeys = {"CPU Utilization Rate", "Swap Total", "Total Memory", "Memory Limit", "CPU Stole Rate", "Swap Used", "Free Memory", "Cores Available"};
        String[] systemValues = {
                String.format("%.3f", systemStats.getCpu_utilization_rate()),
                fmtByte(systemStats.getSwap_total()),
                fmtByte(systemStats.getMem_total()),
                fmtByte(systemStats.getMem_limit()),
                String.format("%.3f", systemStats.getCpu_stolen_rate()),
                fmtByte(systemStats.getSwap_used()),
                fmtByte(systemStats.getMem_free()),
                String.valueOf(systemStats.getCpu_cores_available())
        };

        for (int i = 0; i < systemKeys.length; i++) {
            if (yOffset < 50) {
                contentStream.close();
                PDPage nextPage = new PDPage();
                document.addPage(nextPage);
                contentStream = new PDPageContentStream(document, nextPage);
                yOffset = 700;
            }

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(systemKeys[i] + ": " + systemValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        if (yOffset < 50) {
            contentStream.close();
            PDPage nextPage = new PDPage();
            document.addPage(nextPage);
            contentStream = new PDPageContentStream(document, nextPage);
            yOffset = 700;
        }

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("Interesting Stats");
        contentStream.endText();

        yOffset -= 10;

        String[] interKeys = {"Documents Data Size", "Documents Data Size on Disk", "Spatial Data Size", "Spatial Data Size on Disk",
                "Views Data Size", "Views Data Size on Disk", "Items", "Total Items", "Ep. Bg. Fetched", "Hits", "Index Data Size",
                "Index Data Size on Disk", "Memory Used", "Ops", "# vBucket Non Resident", "Current vBucket Replica Items"};
        String[] interValues = {
                fmtByte(node.getInterestingStats().getCouch_docs_data_size()),
                fmtByte(node.getInterestingStats().getCouch_docs_actual_disk_size()),
                fmtByte(node.getInterestingStats().getCouch_spatial_data_size()),
                fmtByte(node.getInterestingStats().getCouch_spatial_disk_size()),
                fmtByte(node.getInterestingStats().getCouch_views_data_size()),
                fmtByte(node.getInterestingStats().getCouch_views_actual_disk_size()),
                formatNumber(node.getInterestingStats().getCurr_items()),
                formatNumber(node.getInterestingStats().getCurr_items_tot()),
                String.valueOf(node.getInterestingStats().getEp_bg_fetched()),
                String.format("%.3f", node.getInterestingStats().getGet_hits()),
                fmtByte(node.getInterestingStats().getIndex_data_size()),
                fmtByte(node.getInterestingStats().getIndex_disk_size()),
                fmtByte(node.getInterestingStats().getMem_used()),
                String.format("%.3f", node.getInterestingStats().getOps()),
                String.valueOf(node.getInterestingStats().getVb_active_num_non_resident()),
                String.valueOf(node.getInterestingStats().getVb_replica_curr_items())
        };

        for (int i = 0; i < interKeys.length; i++) {
            if (yOffset < 50) {
                contentStream.close();
                PDPage nextPage = new PDPage();
                document.addPage(nextPage);
                contentStream = new PDPageContentStream(document, nextPage);
                yOffset = 700;
            }

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(interKeys[i] + ": " + interValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        contentStream.close();
    }


    private void addBucketsContent(PDDocument document, BucketName bucket, boolean isFirstBucket) throws Exception {
        String bucketName = bucket.getBucketName();

        PDPage currentPage;
        if (isFirstBucket) {
                currentPage = document.getPage(document.getNumberOfPages() - 1);
        } else {
            currentPage = new PDPage();
            document.addPage(currentPage);
        }

        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true);

        int yOffset = 670;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText(bucketName);
        contentStream.endText();

        BucketOverview btOverview = CouchbaseRestAPI.getBucketOverview(bucketName);


        String[] keys = {"Type", "Storage Backend", "Replicas", "Eviction Policy", "Durability Level", "Max TTL",
                "Compression Mode", "Conflict Resolution"};
        String[] values = {
                btOverview.getBucketType().replace("membase", "Couchbase"),
                btOverview.getStorageBackend(),
                String.valueOf(btOverview.getReplicaNumber()),
                btOverview.getEvictionPolicy(),
                btOverview.getDurabilityMinLevel(),
                String.valueOf(btOverview.getMaxTTL()),
                btOverview.getCompressionMode(),
                btOverview.getConflictResolutionType()
        };

        yOffset -= 10;

        for (int i = 0; i < keys.length; i++) {
            if (yOffset < 50) {
                contentStream.close();
                PDPage nextPage = new PDPage();
                document.addPage(nextPage);
                contentStream = new PDPageContentStream(document, nextPage);
                yOffset = 700;
            }

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(keys[i] + ": " + values[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        if (yOffset < 50) {
            contentStream.close();
            PDPage nextPage = new PDPage();
            document.addPage(nextPage);
            contentStream = new PDPageContentStream(document, nextPage);
            yOffset = 700;
        }

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("Quota");
        contentStream.endText();

        yOffset -= 10;

        String[] ramKeys = {"RAM", "Raw RAM"};
        String[] ramValues = {fmtByte(btOverview.getQuota().getRam()), fmtByte(btOverview.getQuota().getRawRAM())};

        for (int i = 0; i < ramKeys.length; i++) {
            if (yOffset < 50) {
                contentStream.close();
                PDPage nextPage = new PDPage();
                document.addPage(nextPage);
                contentStream = new PDPageContentStream(document, nextPage);
                yOffset = 700;
            }

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(ramKeys[i] + ": " + ramValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        yOffset -= 30;

        if (yOffset < 50) {
            contentStream.close();
            PDPage nextPage = new PDPage();
            document.addPage(nextPage);
            contentStream = new PDPageContentStream(document, nextPage);
            yOffset = 700;
        }

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, yOffset);
        contentStream.showText("Basic Stats");
        contentStream.endText();

        yOffset -= 10;

        String[] statsKeys = {"Ops per Sec", "Disk Fetches", "Item Count", "Data Used", "Disk Used", "Memory Used", "# Active vBucket Non Residents"};
        String[] statsValues = {String.format("%.3f", btOverview.getBasicStats().getOpsPerSec()),
                String.valueOf(btOverview.getBasicStats().getDiskFetches()),
                formatNumber(btOverview.getBasicStats().getItemCount()),
                fmtByte(btOverview.getBasicStats().getDataUsed()),
                fmtByte(btOverview.getBasicStats().getDiskUsed()),
                fmtByte(btOverview.getBasicStats().getMemUsed()),
                String.valueOf(btOverview.getBasicStats().getVbActiveNumNonResident())
        };

        for (int i = 0; i < statsKeys.length; i++) {
            if (yOffset < 50) {
                contentStream.close();
                PDPage nextPage = new PDPage();
                document.addPage(nextPage);
                contentStream = new PDPageContentStream(document, nextPage);
                yOffset = 700;
            }

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yOffset - 20);
            contentStream.showText(statsKeys[i] + ": " + statsValues[i]);
            contentStream.endText();
            yOffset -= 20;
        }

        contentStream.close();
    }
}