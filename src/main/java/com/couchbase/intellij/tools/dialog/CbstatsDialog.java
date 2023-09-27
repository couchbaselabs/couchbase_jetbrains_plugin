package com.couchbase.intellij.tools.dialog;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.couchbase.intellij.tools.CBStats;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;

import utils.TemplateUtil;

public class CbstatsDialog extends DialogWrapper {

    private final String bucketName;
    private final String scopeName;
    private final String collectionName;
    private final String type;

    private static final String COLLECTION_LITERAL = "collection";
    private static final String SCOPE_LITERAL = "scope";

    private static final String MEMORY_USED_BY_COLLECTION = "Memory Used By Collection";
    private static final String COLLECTION_DATA_SIZE = "Collection Data Size";
    private static final String COLLECTION_NAME = "Collection Name";
    private static final String NAME_OF_PARENT_SCOPE = "Name Of the Parent Scope";

    private static final String MEMORY_USED_BY_SCOPE = "Memory Used By Scope";
    private static final String SCOPE_DATA_SIZE = "Scope Data Size";
    private static final String SCOPE_NAME = "Scope Name";

    private static final String NUMBER_OF_ITEMS_IN_COLLECTION = "# Items In Collection";
    private static final String NUMBER_OF_DELETE_OPERATIONS_IN_COLLECTION = "# Delete Operations In Collection";
    private static final String NUMBER_OF_GET_OPERATIONS_IN_COLLECTION = "# Get Operations In Collection";
    private static final String NUMBER_OF_STORE_OPERATIONS_IN_COLLECTION = "# Store Operations In Collection";

    private static final String NUMBER_OF_ITEMS_IN_SCOPE = "# Items In Scope";
    private static final String NUMBER_OF_DELETE_OPERATIONS_IN_SCOPE = "# Delete Operations In Scope";
    private static final String NUMBER_OF_GET_OPERATIONS_IN_SCOPE = "# Get Operations In Scope";
    private static final String NUMBER_OF_STORE_OPERATIONS_IN_SCOPE = "# Store Operations In Scope";

    private static final String COLLECTIONS_IN_SCOPE = "# Collections in Scope";

    private static final Map<String, String> COLLECTION_KEY_MAPPINGS;
    private static final Map<String, String> SCOPE_KEY_MAPPINGS;
    private static final Map<String, String> HELP_TEXT_MAPPINGS;

    static {
        // Initialize collection key mappings
        COLLECTION_KEY_MAPPINGS = new HashMap<>();
        COLLECTION_KEY_MAPPINGS.put("collections_mem_used", MEMORY_USED_BY_COLLECTION);
        COLLECTION_KEY_MAPPINGS.put("data_size", COLLECTION_DATA_SIZE);
        COLLECTION_KEY_MAPPINGS.put("name", COLLECTION_NAME);
        COLLECTION_KEY_MAPPINGS.put("items", NUMBER_OF_ITEMS_IN_COLLECTION);
        COLLECTION_KEY_MAPPINGS.put("ops_delete", NUMBER_OF_DELETE_OPERATIONS_IN_COLLECTION);
        COLLECTION_KEY_MAPPINGS.put("ops_get", NUMBER_OF_GET_OPERATIONS_IN_COLLECTION);
        COLLECTION_KEY_MAPPINGS.put("ops_store", NUMBER_OF_STORE_OPERATIONS_IN_COLLECTION);
        COLLECTION_KEY_MAPPINGS.put("scope_name", NAME_OF_PARENT_SCOPE);

        // Initialize scope key mappings
        SCOPE_KEY_MAPPINGS = new HashMap<>();
        SCOPE_KEY_MAPPINGS.put("collections", COLLECTIONS_IN_SCOPE);
        SCOPE_KEY_MAPPINGS.put("collections_mem_used", MEMORY_USED_BY_SCOPE);
        SCOPE_KEY_MAPPINGS.put("data_size", SCOPE_DATA_SIZE);
        SCOPE_KEY_MAPPINGS.put("name", SCOPE_NAME);
        SCOPE_KEY_MAPPINGS.put("items", NUMBER_OF_ITEMS_IN_SCOPE);
        SCOPE_KEY_MAPPINGS.put("ops_delete", NUMBER_OF_DELETE_OPERATIONS_IN_SCOPE);
        SCOPE_KEY_MAPPINGS.put("ops_get", NUMBER_OF_GET_OPERATIONS_IN_SCOPE);
        SCOPE_KEY_MAPPINGS.put("ops_store", NUMBER_OF_STORE_OPERATIONS_IN_SCOPE);

        // Initialize help text mappings
        HELP_TEXT_MAPPINGS = new HashMap<>();
        HELP_TEXT_MAPPINGS.put(MEMORY_USED_BY_COLLECTION, "Memory used by the collection.");
        HELP_TEXT_MAPPINGS.put(COLLECTION_DATA_SIZE, "Size of the data in the collection.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_ITEMS_IN_COLLECTION, "Number of items in the collection.");
        HELP_TEXT_MAPPINGS.put(COLLECTION_NAME, "Name of the collection.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_DELETE_OPERATIONS_IN_COLLECTION,
                "Number of delete operations in the collection.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_GET_OPERATIONS_IN_COLLECTION, "Number of get operations in the collection.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_STORE_OPERATIONS_IN_COLLECTION,
                "Number of store operations in the collection.");
        HELP_TEXT_MAPPINGS.put(NAME_OF_PARENT_SCOPE, "Name of the parent scope for this collection");
        HELP_TEXT_MAPPINGS.put(MEMORY_USED_BY_SCOPE, "Memory used by the scope.");
        HELP_TEXT_MAPPINGS.put(SCOPE_DATA_SIZE, "Size of the data in the scope.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_ITEMS_IN_SCOPE, "Number of items in the scope.");
        HELP_TEXT_MAPPINGS.put(SCOPE_NAME, "Name of the scope.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_DELETE_OPERATIONS_IN_SCOPE, "Number of delete operations in the scope.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_GET_OPERATIONS_IN_SCOPE, "Number of get operations in the scope.");
        HELP_TEXT_MAPPINGS.put(NUMBER_OF_STORE_OPERATIONS_IN_SCOPE, "Number of store operations in the scope.");
        HELP_TEXT_MAPPINGS.put(COLLECTIONS_IN_SCOPE, "Number of collections in the scope.");

    }

    public CbstatsDialog(String bucket, String scope, String collection, String type) {
        super(true);
        this.bucketName = bucket;
        this.scopeName = scope;
        this.collectionName = collection;
        this.type = type;
        init();
        setTitle("Stats for " + type);
        setOKButtonText("Okay");
        setResizable(true);
    }

    @NotNull
    @Override
    protected Action @NotNull [] createActions() {
        return new Action[] { getOKAction() };
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        CBStats cbStats = new CBStats(bucketName, scopeName, collectionName, type);
        String output = "";
        try {
            output = cbStats.executeCommand();
        } catch (Exception ex) {
            Log.error(ex);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        JPanel dialogPanel = new JPanel(new BorderLayout());

        if (type.equalsIgnoreCase(COLLECTION_LITERAL)) {
            dialogPanel = createCollectionCenterPanel(output);
        } else if (type.equalsIgnoreCase(SCOPE_LITERAL)) {
            dialogPanel = createScopeCenterPanel(output);
        }

        return dialogPanel;
    }

    private JPanel createCollectionCenterPanel(String output) {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        String[] lines = output.split("\n");
        String[] keys = new String[lines.length];
        String[] values = new String[lines.length];
        String[] helpTexts = new String[lines.length]; // array for help texts

        for (int i = 0; i < lines.length; i++) {
            int keyStartIndex = lines[i].indexOf(':', lines[i].indexOf(':') + 1) + 1;
            int valueStartIndex = lines[i].lastIndexOf(':') + 1;
            keys[i] = getFriendlyKeyName(lines[i].substring(keyStartIndex, valueStartIndex - 1).trim(),
                    COLLECTION_LITERAL);
            values[i] = getFriendlyValue(lines[i].substring(valueStartIndex).trim(), keys[i]);
            helpTexts[i] = getHelpText(keys[i]); // get help text for each key
        }

        JPanel keyValuePanel = TemplateUtil.createKeyValuePanelWithHelp(keys, values, helpTexts, 1);

        dialogPanel.add(keyValuePanel, BorderLayout.CENTER);
        return dialogPanel;
    }

    private JPanel createScopeCenterPanel(String output) {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        String[] lines = output.split("\n");
        JPanel keyValuePanel = new JPanel();
        keyValuePanel.setLayout(new BoxLayout(keyValuePanel, BoxLayout.Y_AXIS));

        Map<String, String> cacheMap = new LinkedHashMap<>();
        String currentEntityForStatistic;

        for (String line : lines) {
            String[] parts = line.split(":");
            String key;
            String value;

            if (parts.length == 4) {
                key = parts[2].trim();
                value = parts[3].trim();
                currentEntityForStatistic = COLLECTION_LITERAL;
            } else if (parts.length == 3) {
                key = parts[1].trim();
                value = parts[2].trim();
                currentEntityForStatistic = SCOPE_LITERAL;
            } else {
                continue; // Skip lines with unexpected format
            }

            String friendlyKey = getFriendlyKeyName(key, currentEntityForStatistic);
            String friendlyValue = getFriendlyValue(value, friendlyKey);

            // If this is the start of a new collection's stats, add a separator and flush
            // the cache
            if ((friendlyKey.equals(COLLECTION_NAME) && currentEntityForStatistic.equals(COLLECTION_LITERAL))) {
                keyValuePanel.add(TemplateUtil.getSeparator(friendlyValue));
            } else if ((friendlyKey.equals(SCOPE_NAME) && currentEntityForStatistic.equals(SCOPE_LITERAL))) {
                keyValuePanel.add(TemplateUtil.getSeparator("Scope Summary: "));
            } else if (cacheMap.containsKey(friendlyKey) || friendlyKey.equals(COLLECTIONS_IN_SCOPE)) {
                keyValuePanel.add(createKeyValuePanelFromCache(cacheMap));
                cacheMap.clear();
            }

            cacheMap.put(friendlyKey, friendlyValue);
        }

        // Flush the remaining items in the cache
        if (!cacheMap.isEmpty()) {
            keyValuePanel.add(createKeyValuePanelFromCache(cacheMap));
        }

        // Wrap the keyValuePanel in a JScrollPane
        JBScrollPane scrollPane = new JBScrollPane(keyValuePanel);
        dialogPanel.add(scrollPane, BorderLayout.CENTER);
        return dialogPanel;
    }

    private JPanel createKeyValuePanelFromCache(Map<String, String> cacheMap) {
        int size = cacheMap.size();
        String[] keys = new String[size];
        String[] values = new String[size];
        String[] helpTexts = new String[size];

        int index = 0;
        for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
            keys[index] = entry.getKey();
            values[index] = entry.getValue();
            helpTexts[index] = getHelpText(keys[index]);
            index++;
        }

        return TemplateUtil.createKeyValuePanelWithHelp(keys, values, helpTexts, 1);
    }

    private String getFriendlyKeyName(String key, String type) {

        // Assign the appropriate key mappings based on the type
        Map<String, String> keyMappings = (type.equals(COLLECTION_LITERAL)) ? COLLECTION_KEY_MAPPINGS
                : SCOPE_KEY_MAPPINGS;

        // Default case, return the original key if no match is found
        return keyMappings.getOrDefault(key, key);
    }

    private String getHelpText(String key) {
        // Default case, return an empty string if no match is found
        return HELP_TEXT_MAPPINGS.getOrDefault(key, "");
    }

    private String getFriendlyValue(String value, String key) {
        try {
            // Attempt to parse the value as a long
            long longValue = Long.parseLong(value);

            if (key.equals(MEMORY_USED_BY_COLLECTION) || key.equals(COLLECTION_DATA_SIZE) ||
                    key.equals(MEMORY_USED_BY_SCOPE) || key.equals(SCOPE_DATA_SIZE)) {

                // Value is a memory size
                double sizeInMB = longValue / (1024.0 * 1024.0);
                if (sizeInMB >= 1024) {
                    double sizeInGB = sizeInMB / 1024.0;
                    return String.format("%.2f GB", sizeInGB);
                } else {
                    return String.format("%.2f MB", sizeInMB);
                }
            } else {
                // Value is a count
                if (longValue >= 1_000_000) {
                    return String.format("%.2f M", longValue / 1_000_000.0);
                } else if (longValue >= 1_000) {
                    return String.format("%.2f K", longValue / 1_000.0);
                } else {
                    return String.valueOf(longValue);
                }
            }
        } catch (NumberFormatException e) {
            // Handle the case where the value is not a valid number
            return value;
        }
    }

}
