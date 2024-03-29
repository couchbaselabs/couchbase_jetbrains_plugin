package com.couchbase.intellij.database;

import com.couchbase.client.core.api.query.CoreQueryResult;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.intellij.persistence.SavedCluster;
import com.couchbase.intellij.persistence.storage.RelationshipStorage;
import com.couchbase.intellij.tree.node.SchemaDataArrayNodeDescriptor;
import com.couchbase.intellij.tree.node.SchemaDataNodeDescriptor;
import com.couchbase.intellij.tree.node.SchemaDataObjectNodeDescriptor;
import com.couchbase.intellij.tree.node.SchemaFlavorNodeDescriptor;
import com.couchbase.intellij.workbench.Log;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InferHelper {

    public static JsonObject inferSchema(String collectionName, String scopeName, String bucketName) {
        try {
            SavedCluster savedCluster = ActiveCluster.getInstance().getSavedCluster();
            String path = String.format("%s.%s.%s", bucketName, scopeName, collectionName).toLowerCase();
            if (savedCluster.isInferCacheValid(path)) {
                Log.debug("Loaded " + path + " cached infer results");
                return savedCluster.getInferCacheValue(path);
            }
            String query = "INFER `" + bucketName + "`.`" + scopeName + "`.`" + collectionName + "` WITH {\"sample_size\": 2000}";
            QueryResult result = ActiveCluster.getInstance().get().query(query);

            try {
                result.rowsAsObject();
                throw new IllegalStateException("Infer didn't throw an Exception, this indicates that a bug in the SDK has been fixed");
            } catch (Exception ex) {
                if (ex.getMessage().startsWith("Deserialization of content into target class com.couchbase.client.java.json.JsonObject failed")) {
                    Field field = QueryResult.class.getDeclaredField("internal");
                    field.setAccessible(true);
                    CoreQueryResult internal = (CoreQueryResult) field.get(result);
                    List<JsonObject> objList = new ArrayList<>();
                    internal.rows().forEach(e -> {
                                JsonObject obj = JsonObject.create();
                                obj.put("content", JsonArray.fromJson(e.data()));
                                objList.add(obj);
                            }
                    );
                    JsonObject r = objList.get(0);
                    savedCluster.setInferCacheValue(path, r);
                    return r;
                } else {
                    throw ex;
                }
            }
        } catch (Exception e) {
            Log.debug("Could not infer the schema of the collection " + collectionName);
            return null;
        }
    }

    public static void invalidateCache(String bucket, String scope, String collection) {
        String path = String.format("%s.%s.%s", bucket, scope, collection).toLowerCase();
        SavedCluster savedCluster = ActiveCluster.getInstance().getSavedCluster();
        if (savedCluster != null) {
            Log.debug("Invalidated infer cache for " + path);
            ActiveCluster.getInstance().getSavedCluster().getInferValuesUpdateTimes().put(path, 0L);
        } else {
            throw new RuntimeException("No active cluster");
        }
    }

    public static void invalidateInferCacheIfOlder(String bucket, String scope, String collection, long period) {
        String path = String.format("%s.%s.%s", bucket, scope, collection).toLowerCase();
        SavedCluster savedCluster = ActiveCluster.getInstance().getSavedCluster();
        if (savedCluster != null) {
            if (savedCluster.getInferValuesUpdateTimes().get(path) == null || System.currentTimeMillis() - savedCluster.getInferValuesUpdateTimes().get(path) >= period) {
                invalidateCache(bucket, scope, collection);
            }
        } else {
            throw new RuntimeException("No active cluster");
        }
    }

    public static void extractArray(DefaultMutableTreeNode parentNode, JsonArray array, String path) {
        for (int i = 0; i < array.size(); i++) {
            JsonObject inferSchemaRow = array.getObject(i);

            String tooltip = "#docs: " + inferSchemaRow.getNumber("#docs") + ", pattern: " + inferSchemaRow.getString("Flavor");
            SchemaFlavorNodeDescriptor sf = new SchemaFlavorNodeDescriptor("Pattern #" + (i + 1), tooltip);
            DefaultMutableTreeNode flavorNode = new DefaultMutableTreeNode(sf);
            parentNode.add(flavorNode);
            JsonObject properties = inferSchemaRow.getObject("properties");

            if (properties != null) {
                extractTypes(flavorNode, inferSchemaRow.getObject("properties"), path);

            } else {
                JsonArray samples = inferSchemaRow.getArray("samples");
                if (samples != null) {
                    String additionalTooltip = samples.toList().stream().map(Object::toString).collect(Collectors.joining(","));
                    sf.setTooltip(sf.getTooltip() + ", samples: " + additionalTooltip);
                } else {
                    Log.debug("Infer reached an unexpected state");
                }
            }
        }
    }

    public static void extractTypes(DefaultMutableTreeNode parentNode, JsonObject properties, String path) {

        Map<String, String> relationships = RelationshipStorage.getInstance().getValue().getRelationships()
                .getOrDefault(ActiveCluster.getInstance().getId(), new HashMap<>());
        for (String key : properties.getNames()) {
            JsonObject property = properties.getObject(key);
            String type = property.get("type").toString();
            //if it is an Object
            if (type.equals("object")) {
                String objPath = path + "." + key;
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new SchemaDataObjectNodeDescriptor(key, objPath));
                extractTypes(childNode, property.getObject("properties"), objPath);
                parentNode.add(childNode);

            } else if (type.equals("array")) {
                try {
                    JsonObject items = property.getObject("items");
                    String itemTypeString = (String) items.get("type");
                    String objPath = path + "." + key + "[*]";
                    if (itemTypeString != null) {
                        DefaultMutableTreeNode childNode;
                        if (itemTypeString.equals("object")) {
                            childNode = new DefaultMutableTreeNode(new SchemaDataArrayNodeDescriptor(key, "array of objects", null, objPath));
                            extractTypes(childNode, items.getObject("properties"), objPath);
                        } else {
                            childNode = new DefaultMutableTreeNode(new SchemaDataNodeDescriptor(key,
                                    "array of " + itemTypeString + "s", null, objPath, relationships.get(objPath)));
                        }
                        parentNode.add(childNode);
                    } else {
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new SchemaDataArrayNodeDescriptor(key, type, null, objPath));
                        parentNode.add(childNode);
                    }
                } catch (ClassCastException cce) {
                    JsonArray array = property.getArray("items");
                    extractArray(parentNode, array, path);
                }
            } else {
                addLeaf(parentNode, key, property, path);
            }
        }
    }

    private static void addLeaf(DefaultMutableTreeNode parentNode, String key, JsonObject property, String path) {
        Map<String, String> relationships = RelationshipStorage.getInstance().getValue().getRelationships()
                .get(ActiveCluster.getInstance().getId());

        if (relationships == null) {
            relationships = new HashMap<>();
        }

        String type = property.get("type").toString();
        boolean containsNull = false;
        if (type.contains("[")) {
            type = type.replace("[", "").replace("]", "").replace("\"", "").replace(",", " | ");

            if (type.contains("null")) {
                containsNull = true;
            }
        }

        String samples = null;
        JsonArray samplesArray = property.getArray("samples");
        if (samplesArray != null) {
            if (containsNull) {
                if (samplesArray.size() > 1) {
                    //ignoring the first array that will only contains null
                    samplesArray = samplesArray.getArray(1);
                } else {
                    samplesArray = samplesArray.getArray(0);
                }
            }
            samples = samplesArray.toList().stream().map(e -> e == null ? "null" : e.toString()).collect(Collectors.joining(" , "));
        }

        String objPath = path + "." + key;
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(
                new SchemaDataNodeDescriptor(key, type, samples, objPath, relationships.get(objPath)));
        parentNode.add(childNode);
    }
}
