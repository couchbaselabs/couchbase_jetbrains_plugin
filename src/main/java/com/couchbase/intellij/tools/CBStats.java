package com.couchbase.intellij.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.workbench.Log;

import utils.OSUtil;
import utils.ProcessUtils;

public class CBStats {
    private final String bucketName;
    private final String scopeName;
    private final String collectionName;

    private final String type;

    public CBStats(String bucketName, String scopeName, String collectionName, String type) {
        this.bucketName = bucketName;
        this.scopeName = scopeName;
        this.collectionName = collectionName;
        this.type = type;
    }

    public String executeCommand() throws IOException, InterruptedException {
        StringBuilder output;

        List<String> command = new ArrayList<>();
        // command.add(CBTools.getTool(CBTools.Type.CBSTATS).getPath());
        String osArch = OSUtil.getOSArch();

        // Determine the command path based on the operating system and architecture
        switch (osArch) {
            case OSUtil.MACOS_ARM:
            case OSUtil.MACOS_64:
                command.add("/Applications/Couchbase Server.app/Contents/Resources/couchbase-core/bin/cbstats");
                break;
            case OSUtil.WINDOWS_ARM:
            case OSUtil.WINDOWS_64:
                command.add("C:\\Program Files\\Couchbase\\Server\\bin\\cbstats");
                break;
            case OSUtil.LINUX_ARM:
            case OSUtil.LINUX_64:
                command.add("/opt/couchbase/bin/cbstats");
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operating system: " + osArch);
        }
        command.add((ActiveCluster.getInstance().getClusterURL().replaceFirst("^couchbase://", "")) + ":"
                + (ActiveCluster.getInstance().isSSLEnabled() ? "11207" : "11210"));
        command.add("-u");
        command.add(ActiveCluster.getInstance().getUsername());
        command.add("-p");
        command.add(ActiveCluster.getInstance().getPassword());

        command.add("-b");
        command.add(bucketName);

        if (type.equalsIgnoreCase("collection")) {
            command.add("collections");
            command.add(scopeName + "." + collectionName);
        } else if (type.equalsIgnoreCase("scope")) {
            command.add("scopes");
            command.add(scopeName);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        output = new StringBuilder(ProcessUtils.returnOutput(process));

        if (process.waitFor() == 0) {
            Log.info("Command executed successfully");
        } else {
            Log.error("Command execution failed");
        }

        return output.toString();
    }
}
