package com.couchbase.intellij.tree;

import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.tools.CBTools;
import com.couchbase.intellij.workbench.Log;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.couchbase.intellij.tree.CouchbaseWindowContent.project;

public class PillowFightRunner {
    public void runPillowFightCommand(String bucket, String durability, String persistTo, String batchSize, String numberItems, String keyPrefix, String numberThreads, String percentage, String noPopulation, String populateOnly, String minSize, String maxSize, String numberCycles, String sequential, String startAt, String timings, String expiry, String replicateTo, String lock, String json, String noop, String subdoc, String pathcount) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(CBTools.getTool(CBTools.Type.CBC_PILLOW_FIGHT).getPath());
        command.add("-U");
        command.add(String.format("%s/%s", ActiveCluster.getInstance().getClusterURL(), bucket));
        command.add("-u");
        command.add(ActiveCluster.getInstance().getUsername());
        command.add("-P");
        command.add(ActiveCluster.getInstance().getPassword());
        command.add("--durability");
        command.add(durability);

        if (persistTo != null) {
            command.add("--persist-to");
            command.add(persistTo);
        }
        if (batchSize != null) {
            command.add("--batch-size");
            command.add(batchSize);
        }
        if (numberItems != null) {
            command.add("--num-items");
            command.add(numberItems);
        }
        if (!keyPrefix.trim().isEmpty()) {
            command.add("--key-prefix");
            command.add(keyPrefix);
        }
        if (numberThreads != null) {
            command.add("--num-threads");
            command.add(numberThreads);
        }
        if (percentage != null) {
            command.add("--set-pct");
            command.add(percentage);
        }
        if (noPopulation.equals("Enable")) {
            command.add("--no-population");
        }
        if (populateOnly.equals("Enable")) {
            command.add(" --populate-only");
        }
        if (minSize != null) {
            command.add("--min-size");
            command.add(minSize);
        }
        if (maxSize != null) {
            command.add("--max-size");
            command.add(maxSize);
        }
        if (numberCycles != null) {
            command.add("--num-cycles");
            command.add(numberCycles);
        }
        if (sequential.equals("Enable")) {
            command.add("--sequential");
        }
        if (startAt != null) {
            command.add("--start-at");
            command.add(startAt);
        }
        if (timings.equals("Enable")) {
            command.add("--timings");
        }
        if (expiry != null) {
            command.add("--expiry");
            command.add(expiry);
        }
        if (replicateTo != null) {
            command.add("--replicate-to");
            command.add(replicateTo);
        }
        if (lock != null) {
            command.add("--lock");
            command.add(lock);
        }
        if (json.equals("Enable")) {
            command.add("--json");
        }
        if (noop.equals("Enable")) {
            command.add(" --noop");
        }
        if (subdoc.equals("Enable")) {
            command.add("--subdoc");
        }
        if (pathcount != null) {
            command.add("--pathcount");
            command.add(pathcount);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process proc = processBuilder.start();

        PillowFightDialog pillowFightDialog = new PillowFightDialog(project, bucket, durability, persistTo, batchSize, numberItems, keyPrefix, numberThreads, percentage, noPopulation, populateOnly, minSize, maxSize, numberCycles, sequential, startAt, timings, expiry, replicateTo, lock, json, noop, subdoc, pathcount);

        StopButtonDialog stopButtonDialog = new StopButtonDialog(proc, pillowFightDialog);
        stopButtonDialog.setVisible(true);
    }

    public class StopButtonDialog extends JDialog {
        private JButton stopButton;
        private JTextArea opsPerSecLabel;

        public StopButtonDialog(Process proc, PillowFightDialog pillowFightDialog) {
            super();
            setTitle("Stop Pillow Fight");
            opsPerSecLabel = new JTextArea("OPS/SEC:           ");
            opsPerSecLabel.setEditable(false);
            opsPerSecLabel.setBorder(JBUI.Borders.empty(0, 100));
            stopButton = new JButton("Stop");

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(opsPerSecLabel, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = JBUI.insets(5);
            panel.add(stopButton, gbc);

            add(panel);
            pack();

            setLocationRelativeTo(null);

            stopButton.addActionListener(e -> {
                if (proc != null) {
                    proc.destroy();
                }
                dispose();
                pillowFightDialog.show();
            });

            new Thread(() -> {
                try {
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    String line;
                    while ((line = stdError.readLine()) != null) {
                        if (line.equals("Running. Press Ctrl-C to terminate...")) {
                            line = "     Running...    ";
                        }
                        opsPerSecLabel.setText(line);
                    }
                } catch (Exception e) {
                    Log.error(e);
                }
            }).start();
        }
    }
}
