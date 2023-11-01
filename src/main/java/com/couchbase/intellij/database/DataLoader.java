package com.couchbase.intellij.database;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.error.IndexFailureException;
import com.couchbase.client.core.error.PlanningFailureException;
import com.couchbase.client.core.error.TimeoutException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.client.java.manager.query.QueryIndex;
import com.couchbase.intellij.VirtualFileKeys;
import com.couchbase.intellij.database.entity.CouchbaseCollection;
import com.couchbase.intellij.persistence.*;
import com.couchbase.intellij.persistence.storage.ClustersStorage;
import com.couchbase.intellij.persistence.storage.PasswordStorage;
import com.couchbase.intellij.persistence.storage.QueryFiltersStorage;
import com.couchbase.intellij.tree.node.*;
import com.couchbase.intellij.tree.overview.apis.CouchbaseRestAPI;
import com.couchbase.intellij.workbench.Log;
import com.couchbase.intellij.workbench.SQLPPQueryUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UserBinaryFileType;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.treeStructure.Tree;
import org.intellij.sdk.language.SQLPPFormatter;
import org.jetbrains.annotations.NotNull;
import utils.IndexUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.couchbase.intellij.VirtualFileKeys.READ_ONLY;

@SuppressWarnings("ALL")
public class DataLoader {

    public static void listBuckets(DefaultMutableTreeNode parentNode, Tree tree) {

        Object userObject = parentNode.getUserObject();
        tree.setPaintBusy(true);
        if (userObject instanceof ConnectionNodeDescriptor) {
            CompletableFuture.runAsync(() -> {
                try {
                    Set<String> buckets = ActiveCluster.getInstance().get().buckets().getAllBuckets().keySet();
                    parentNode.removeAllChildren();

                    if (!buckets.isEmpty()) {
                        for (String bucket : buckets) {

                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new BucketNodeDescriptor(bucket, ActiveCluster.getInstance().getId()));
                            childNode.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));
                            parentNode.add(childNode);
                        }
                    } else {
                        parentNode.add(new DefaultMutableTreeNode(new NoResultsNodeDescriptor()));
                    }

                    ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                } catch (Exception e) {
                    Log.error(e);
                    e.printStackTrace();
                } finally {
                    tree.setPaintBusy(false);
                }
            });
        } else {
            throw new IllegalStateException("The expected parent was ConnectionNode but got something else");
        }
    }

    public static void listScopes(DefaultMutableTreeNode parentNode, Tree tree) {
        Object userObject = parentNode.getUserObject();
        tree.setPaintBusy(true);
        if (userObject instanceof BucketNodeDescriptor) {
            BucketNodeDescriptor node = (BucketNodeDescriptor) userObject;
            CompletableFuture.runAsync(() -> {
                try {
                    String bucketName = node.getText();
                    List<ScopeSpec> scopes = ActiveCluster.getInstance().get().bucket(bucketName).collections().getAllScopes();
                    parentNode.removeAllChildren();
                    node.setCounter(formatCount(scopes.size()));

                    if (!scopes.isEmpty()) {

                        for (ScopeSpec scopeSpec : scopes) {
                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new ScopeNodeDescriptor(scopeSpec.name(), ActiveCluster.getInstance().getId(), bucketName));

//                            DefaultMutableTreeNode collections = new DefaultMutableTreeNode(
//                                    new CollectionsNodeDescriptor(ActiveCluster.getInstance().getId(), bucketName,
//                                            scopeSpec.name()));
//                            collections.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));
                            childNode.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));

                            parentNode.add(childNode);
                        }
                    } else {
                        parentNode.add(new DefaultMutableTreeNode(new NoResultsNodeDescriptor()));
                    }

                    ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                } catch (Exception e) {
                    Log.error(e);
                    e.printStackTrace();
                } finally {
                    tree.setPaintBusy(false);
                }
            });
        } else {
            throw new IllegalStateException("The expected parent was BucketNode but got something else");
        }
    }

    public static void listCollections(DefaultMutableTreeNode parentNode, Tree tree) {
        Object userObject = parentNode.getUserObject();
        tree.setPaintBusy(true);
        if (userObject instanceof ScopeNodeDescriptor) {
            CompletableFuture.runAsync(() -> {
                try {
                    parentNode.removeAllChildren();
                    ScopeNodeDescriptor scopeDesc = (ScopeNodeDescriptor) userObject;
                    Map<String, Integer> counts = CouchbaseRestAPI.getCollectionCounts(scopeDesc.getBucket(), scopeDesc.getText());

                    List<CollectionSpec> collections = ActiveCluster.getInstance().get().bucket(scopeDesc.getBucket()).collections().getAllScopes().stream().filter(scope -> scope.name().equals(scopeDesc.getText())).flatMap(scope -> scope.collections().stream()).collect(Collectors.toList());
                    scopeDesc.setCounter(formatCount(collections.size()));
                    if (!collections.isEmpty()) {
                        for (CollectionSpec spec : collections) {

                            String filter = QueryFiltersStorage.getInstance().getValue().getQueryFilter(ActiveCluster.getInstance().getId(), scopeDesc.getBucket(), scopeDesc.getText(), spec.name());

                            CollectionNodeDescriptor colNodeDesc = new CollectionNodeDescriptor(spec.name(), ActiveCluster.getInstance().getId(), scopeDesc.getBucket(), scopeDesc.getText(), filter);
                            colNodeDesc.setCounter(formatCount(counts.get(scopeDesc.getBucket() + "." + scopeDesc.getText() + "." + spec.name())));
                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(colNodeDesc);
                            childNode.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));
                            parentNode.add(childNode);
                        }
                    } else {
                        parentNode.add(new DefaultMutableTreeNode(new NoResultsNodeDescriptor()));
                    }
                    ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                } catch (Exception e) {
                    Log.error(e);
                    e.printStackTrace();
                } finally {
                    tree.setPaintBusy(false);
                }
            });
        } else {
            throw new IllegalStateException("The expected parent was CollectionsNodeDescriptor but got something else");
        }
    }

    public static void listDocuments(DefaultMutableTreeNode parentNode, Tree tree, int newOffset) {
        Object userObject = parentNode.getUserObject();
        tree.setPaintBusy(true);
        if (userObject instanceof CollectionNodeDescriptor) {
            CollectionNodeDescriptor colNode = (CollectionNodeDescriptor) parentNode.getUserObject();
            try {
                //When KV
                if (!ActiveCluster.getInstance().hasQueryService()) {
                    if (newOffset == 0) {
                        //removed loading node
                        parentNode.removeAllChildren();
                    } else {
                        //removes "Load More" node
                        parentNode.remove(parentNode.getChildCount() - 1);
                    }
                    loadKVDocuments(parentNode, tree, newOffset, colNode);
                } else {
                    if (newOffset == 0) {
                        parentNode.removeAllChildren();
                        DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(new SchemaNodeDescriptor());
                        schemaNode.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));
                        parentNode.add(schemaNode);

                        DefaultMutableTreeNode indexes = new DefaultMutableTreeNode(new IndexesNodeDescriptor(colNode.getBucket(), colNode.getScope(), colNode.getText()));
                        indexes.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));
                        parentNode.add(indexes);
                    } else {
                        //removes "Load More" node
                        parentNode.remove(parentNode.getChildCount() - 1);
                    }

                    String filter = colNode.getQueryFilter();
                    String query = "Select meta(couchbaseAlias).id as cbFileNameId, meta(couchbaseAlias).type as cbMetaType  from `" + colNode.getText() + "` as couchbaseAlias " + ((filter == null || filter.isEmpty()) ? "" : (" where " + filter)) + (SQLPPQueryUtils.hasOrderBy(filter) ? "" : "  order by meta(couchbaseAlias).id ") + (newOffset == 0 ? "" : " OFFSET " + newOffset) + " limit 10";

                    final List<JsonObject> results = ActiveCluster.getInstance().get().bucket(colNode.getBucket()).scope(colNode.getScope()).query(query).rowsAsObject();
                    InferHelper.invalidateInferCacheIfOlder(colNode.getBucket(), colNode.getScope(), colNode.getText(), TimeUnit.MINUTES.toMillis(5));

                    if (!results.isEmpty()) {
                        for (JsonObject obj : results) {
                            String docId = obj.getString("cbFileNameId");
                            String type = obj.getString("cbMetaType");
                            String fileName = docId + ".json";
                            FileNodeDescriptor.FileType fileType = "base64".equals(type) ? FileNodeDescriptor.FileType.BINARY : FileNodeDescriptor.FileType.JSON;
                            if (fileType == FileNodeDescriptor.FileType.BINARY) {
                                fileName = docId;
                            }
                            FileNodeDescriptor node = new FileNodeDescriptor(fileName, colNode.getBucket(), colNode.getScope(), colNode.getText(), docId,
                                    fileType, null);
                            DefaultMutableTreeNode jsonFileNode = new DefaultMutableTreeNode(node);
                            parentNode.add(jsonFileNode);
                        }

                        if (results.size() == 10) {
                            DefaultMutableTreeNode loadMoreNode = new DefaultMutableTreeNode(new LoadMoreNodeDescriptor(colNode.getBucket(), colNode.getScope(), colNode.getText(), newOffset + 10));
                            parentNode.add(loadMoreNode);
                        }
                    } else if (newOffset == 0) {
                        parentNode.add(new DefaultMutableTreeNode(new NoResultsNodeDescriptor()));
                    }
                    ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                }
            } catch (PlanningFailureException | IndexFailureException ex) {
                //This catch handles when the user has no indexes in the collection
                if (newOffset == 0) {
                    parentNode.removeAllChildren();
                    MissingIndexNodeDescriptor idx = new MissingIndexNodeDescriptor(colNode.getBucket(), colNode.getScope(), colNode.getText());
                    MissingIndexFootNoteNodeDescriptor footIdx = new MissingIndexFootNoteNodeDescriptor(colNode.getBucket(), colNode.getScope(), colNode.getText());
                    parentNode.add(new DefaultMutableTreeNode(idx));
                    parentNode.add(new DefaultMutableTreeNode(footIdx));
                } else {
                    //removes "Load More" node
                    parentNode.remove(parentNode.getChildCount() - 1);
                }

                try {
                    loadKVDocuments(parentNode, tree, newOffset, colNode);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.debug("Could not load documents from KV", e);
                }
            } catch (Exception e) {
                Log.error(e);
                e.printStackTrace();
            } finally {
                tree.setPaintBusy(false);
            }
        } else {
            throw new IllegalStateException("The expected parent was CollectionNodeDescriptor but got something else");
        }
    }

    private static void loadKVDocuments(DefaultMutableTreeNode parentNode, Tree tree, int newOffset, CollectionNodeDescriptor colNode) throws Exception {
        List<String> docIds = CouchbaseRestAPI.listKVDocuments(colNode.getBucket(), colNode.getScope(), colNode.getText(), newOffset, 10);

        if (!docIds.isEmpty()) {
            for (String id : docIds) {
                FileNodeDescriptor node = new FileNodeDescriptor(id, colNode.getBucket(), colNode.getScope(), colNode.getText(), id,
                        FileNodeDescriptor.FileType.UNKNOWN, null);
                DefaultMutableTreeNode jsonFileNode = new DefaultMutableTreeNode(node);
                parentNode.add(jsonFileNode);
            }
            if (docIds.size() == 10) {
                DefaultMutableTreeNode loadMoreNode = new DefaultMutableTreeNode(new LoadMoreNodeDescriptor(colNode.getBucket(), colNode.getScope(), colNode.getText(), newOffset + 10));
                parentNode.add(loadMoreNode);
            }

        } else if (newOffset == 0) {
            parentNode.add(new DefaultMutableTreeNode(new NoResultsNodeDescriptor()));
        }

        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
    }

    public static boolean stubsAvailable(String bucket, String scope, String collection) {
        return ActiveCluster.getInstance().getChild(bucket)
                .flatMap(b -> b.getChild(scope))
                .flatMap(s -> s.getChild(collection))
                .map(col -> ((CouchbaseCollection) col).generateDocument())
                .filter(Objects::nonNull)
                .findAny().isPresent();
    }

    /**
     * Creates the file before it is opened.
     *
     * @param project the project where the folder will be created and the file will be stored
     * @param node    Node where the virtual file will be stored
     * @param tree    used to set the loading status
     */
    public static void loadDocument(Project project, FileNodeDescriptor node, Tree tree, boolean isNew, boolean generateStub) {
        tree.setPaintBusy(true);

        if (node.getVirtualFile() != null) {
            return;
        }

        String docContent = "{}";
        String cas = null;
        boolean isDifferentFormat = false;

        try {
            GetResult result = ActiveCluster.getInstance().get().bucket(node.getBucket()).scope(node.getScope()).collection(node.getCollection()).get(node.getId());
            cas = String.valueOf(result.cas());

            docContent = new String(result.contentAsBytes());

            //checks if document us a valid Json
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.readTree(docContent);
            } catch (JsonProcessingException e) {
                isDifferentFormat = true;
            }

        } catch (DocumentNotFoundException dnf) {
            //document was not found because the user wants to create a new one.
            if (!isNew) {
                SwingUtilities.invokeLater(() -> Messages.showInfoMessage("<html>The document <strong>" + node.getId() + "</strong> doesn't exists anymore.</html>", "Couchbase Plugin Error"));
                tree.setPaintBusy(false);
                return;
            } else if (generateStub) {
                docContent = ActiveCluster.getInstance().getChild(node.getBucket())
                        .flatMap(bucket -> bucket.getChild(node.getScope()))
                        .flatMap(scope -> scope.getChild(node.getCollection()))
                        .map(col -> ((CouchbaseCollection) col).generateDocument())
                        .filter(Objects::nonNull)
                        .peek(o -> {
                            if (o.containsKey("id")) {
                                o.put("id", node.getId());
                            } else if (o.containsKey("ID")) {
                                o.put("ID", node.getId());
                            }
                        })
                        .map(JsonObject::toString)
                        .findFirst().orElse(docContent);
            }
        } catch (TimeoutException te) {
            te.printStackTrace();
            Log.error("Request to get the document " + node.getId() + " timed out.", te);
            SwingUtilities.invokeLater(() -> Messages.showInfoMessage("<html>The request to get the document <strong>" + node.getId() + "</strong> timed out. Please try again or check your network connection.</html>", "Couchbase Plugin Error"));
            tree.setPaintBusy(false);
            return;
        } catch (Exception e) {
            Log.error("Could not load the document " + node.getId() + ".", e);
            SwingUtilities.invokeLater(() -> Messages.showInfoMessage("<html>Could not load the document <strong>" + node.getId() + "</strong>. Please check the log for more.</html>", "Couchbase Plugin Error"));
            tree.setPaintBusy(false);
            return;
        }

        final boolean isBinary = isDifferentFormat;
        final String content = docContent;
        final String docCass = cas;
        try {
            ApplicationManager.getApplication().runWriteAction(() -> {
                final FileType type = isBinary ? UserBinaryFileType.INSTANCE : JsonFileType.INSTANCE;
                CouchbaseDocumentVirtualFile virtualFile = new CouchbaseDocumentVirtualFile(
                        project, type, node.getBucket(), node.getScope(), node.getCollection(), node.getId()
                );

                virtualFile.putUserData(VirtualFileKeys.CONN_ID, ActiveCluster.getInstance().getId());
                virtualFile.putUserData(VirtualFileKeys.CLUSTER, ActiveCluster.getInstance().getId());
                virtualFile.putUserData(VirtualFileKeys.BUCKET, node.getBucket());
                virtualFile.putUserData(VirtualFileKeys.SCOPE, node.getScope());
                virtualFile.putUserData(VirtualFileKeys.COLLECTION, node.getCollection());
                virtualFile.putUserData(VirtualFileKeys.ID, node.getId());
                virtualFile.putUserData(VirtualFileKeys.CAS, String.valueOf(docCass));
                if (isBinary) {
                    virtualFile.putUserData(READ_ONLY, String.valueOf(isBinary));
                }

                node.setVirtualFile(virtualFile);
            });
        } catch (Exception e) {
            tree.setPaintBusy(false);
            Log.error("An error occurred while trying to load the file", e);
            SwingUtilities.invokeLater(() -> Messages.showInfoMessage("<html>Could not load the document <strong>" + node.getId() + "</strong>. Please check the log for more.</html>", "Couchbase Plugin Error"));
        }
    }

    public static void showSchema(DefaultMutableTreeNode parentNode, DefaultTreeModel treeModel, Tree tree) {
        Object userObject = parentNode.getUserObject();
        tree.setPaintBusy(true);
        if (userObject instanceof SchemaNodeDescriptor) {
            CompletableFuture.runAsync(() -> {
                try {
                    parentNode.removeAllChildren();

                    CollectionNodeDescriptor colNode = (CollectionNodeDescriptor) ((DefaultMutableTreeNode) parentNode.getParent()).getUserObject();
                    String collectionName = colNode.getText();
                    String scopeName = colNode.getScope();
                    String bucketName = colNode.getBucket();

                    new Task.ConditionalModal(null, String.format("Running INFER for collection %s.%s.%s", bucketName, scopeName, collectionName), true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            JsonObject inferenceQueryResults = InferHelper.inferSchema(collectionName, scopeName, bucketName);
                            if (inferenceQueryResults != null) {
                                JsonArray array = inferenceQueryResults.getArray("content");
                                InferHelper.extractArray(parentNode, array);
                            } else {
                                Log.debug("Could not infer the schema for " + colNode.getText());
                            }

                            treeModel.nodeStructureChanged(parentNode);
                        }
                    }.queue();
                } catch (Exception e) {
                    Log.error(e);
                    e.printStackTrace();
                } finally {
                    tree.setPaintBusy(false);
                }
            });
        } else {
            throw new IllegalStateException("The expected parent was SchemaNodeDescriptor but got something else");
        }
    }

    public static String adjustClusterProtocol(String cluster, boolean ssl) {
        if (cluster.startsWith("couchbase://") || cluster.startsWith("couchbases://")) {
            return cluster;
        }

        String protocol;
        if (ssl) {
            protocol = "couchbases://";
        } else {
            protocol = "couchbase://";
        }
        return protocol + cluster;
    }

    public static Set<String> listBucketNames(String clusterUrl, boolean ssl, String username, String password) {

        Cluster cluster = null;
        try {
            cluster = Cluster.connect(adjustClusterProtocol(clusterUrl, ssl), ClusterOptions.clusterOptions(username, password).environment(env -> {
                //env.applyProfile("wan-development");
            }));
            cluster.waitUntilReady(Duration.ofSeconds(5));

            return cluster.buckets().getAllBuckets().keySet();
        } catch (Exception e) {
            Log.error(e);
            assert cluster != null;
            cluster.disconnect();
            throw e;
        }

    }

    public static SavedCluster saveDatabaseCredentials(String name, String url, String queryParams, boolean isSSL, String username, String password, String defaultBucket,String apiKey, String apiSecret) {
        String key = username + ":" + name;
        SavedCluster sc = new SavedCluster();
        sc.setId(key);
        sc.setName(name);
        sc.setQueryParams(queryParams);
        sc.setSslEnable(isSSL);
        sc.setUsername(username);
        sc.setUrl(adjustClusterProtocol(url, isSSL));
        sc.setDefaultBucket(defaultBucket);
        sc.setApiKey(apiKey);
        sc.setApiSecret(apiSecret);

        Clusters clusters = ClustersStorage.getInstance().getValue();

        if (clusters.getMap() == null) {
            clusters.setMap(new HashMap<>());
        }

        if (clusters.getMap().containsKey(sc.getId())) {
            throw new DuplicatedClusterNameAndUserException();
        }

        for (Map.Entry<String, SavedCluster> entry : clusters.getMap().entrySet()) {
            if (entry.getValue().getUrl().equals(sc.getUrl()) && entry.getValue().getUsername().equals(username)) {
                throw new ClusterAlreadyExistsException();
            }
        }

        clusters.getMap().put(key, sc);
        ClustersStorage.getInstance().setValue(clusters);
        PasswordStorage.savePassword(sc, password);

        return sc;
    }

    public static Map<String, SavedCluster> getSavedClusters() {
        if (ClustersStorage.getInstance().getValue() == null || ClustersStorage.getInstance().getValue().getMap() == null) {
            return new HashMap<>();
        }
        return ClustersStorage.getInstance().getValue().getMap();
    }

    public static String getClusterPassword(SavedCluster sv) {
        return PasswordStorage.getPassword(sv);
    }

    public static void deleteSavedCluster(SavedCluster sv) {
        PasswordStorage.savePassword(sv, null);
        ClustersStorage.getInstance().getValue().getMap().remove(sv.getId());
    }

    public static void createPrimaryIndex(String bucket, String scope, String collection) {

        CompletableFuture.runAsync(() -> {
            try {
                ActiveCluster.getInstance().get().bucket(bucket).scope(scope).collection(collection).queryIndexes().createPrimaryIndex();

                SwingUtilities.invokeLater(() -> Messages.showInfoMessage("The primary index for the collection " + bucket + "." + scope + "." + collection + " was created successfully.", "Primary Index Creation"));
            } catch (Exception e) {
                Log.error(e);
                e.printStackTrace();
            }
        });
    }

    public static void listIndexes(DefaultMutableTreeNode parentNode, Tree tree) {
        Object userObject = parentNode.getUserObject();
        tree.setPaintBusy(true);
        if (userObject instanceof IndexesNodeDescriptor) {
            IndexesNodeDescriptor idxs = (IndexesNodeDescriptor) userObject;
            parentNode.removeAllChildren();

            List<QueryIndex> results = ActiveCluster.getInstance().get().bucket(idxs.getBucket()).scope(idxs.getScope()).collection(idxs.getCollection()).queryIndexes().getAllIndexes();

            if (!results.isEmpty()) {
                for (QueryIndex qi : results) {
                    String fileName = qi.name() + ".sqlpp";
                    VirtualFile virtualFile = new LightVirtualFile(fileName, FileTypeManager.getInstance().getFileTypeByExtension("sqlpp"), SQLPPFormatter.format(IndexUtils.getIndexDefinition(qi, false)));
                    virtualFile.putUserData(READ_ONLY, "true");

                    IndexNodeDescriptor node = new IndexNodeDescriptor(idxs.getBucket(), idxs.getScope(), idxs.getCollection(), fileName, virtualFile);
                    DefaultMutableTreeNode jsonFileNode = new DefaultMutableTreeNode(node);
                    parentNode.add(jsonFileNode);
                }
            } else {
                parentNode.add(new DefaultMutableTreeNode(new NoResultsNodeDescriptor()));
            }
            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
        } else {
            throw new IllegalStateException("The expected parent was IndexesNodeDescriptor but got something else");
        }
    }

    public static List<QueryIndex> listIndexes(String bucket, String scope, String collection) {
        if (ActiveCluster.getInstance().hasQueryService()) {
            return ActiveCluster.getInstance().get().bucket(bucket).scope(scope).collection(collection).queryIndexes().getAllIndexes();
        } else {
            return new ArrayList<>();
        }

    }

    public static String getDocMetadata(String bucket, String scope, String collection, String docId) {

        try {
            String query = "Select meta().* from `" + collection + "` use keys \"" + docId + "\"";
            final List<JsonObject> results = ActiveCluster.getInstance().get().bucket(bucket).scope(scope).query(query).rowsAsObject();

            return results.get(0).toString();
        } catch (Exception e) {
            e.printStackTrace();
            Log.error("Failed to load the metadata for document " + docId, e);
            return null;
        }
    }

    public static String formatCount(Integer num) {
        if (num == null) {
            return "?";
        }
        if (num < 1000) {
            return String.valueOf(num);
        } else if (num < 1000000) {
            return String.format("%.1fk", num / 1000.0);
        } else {
            return String.format("%.2fM", num / 1000000.0);
        }
    }
}
