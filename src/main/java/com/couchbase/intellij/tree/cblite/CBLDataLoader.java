package com.couchbase.intellij.tree.cblite;

import com.couchbase.intellij.VirtualFileKeys;
import com.couchbase.intellij.tree.cblite.nodes.*;
import com.couchbase.intellij.tree.cblite.storage.*;
import com.couchbase.intellij.tree.node.LoadingNodeDescriptor;
import com.couchbase.intellij.tree.node.NoResultsNodeDescriptor;
import com.couchbase.intellij.workbench.Log;
import com.couchbase.lite.*;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.Tree;
import org.json.JSONObject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class CBLDataLoader {


    public static SavedCBLDatabase saveNewDatabase(String id, String name, String path) {

        if (id == null) {
            throw new IllegalStateException("The database id can't be null");
        }

        if (name == null) {
            throw new IllegalStateException("The database name can't be null");
        }

        if (path == null) {
            throw new IllegalStateException("The path can't be null");
        }


        CBLDatabases databases = CBLDatabaseStorage.getInstance().getValue();

        for (SavedCBLDatabase db : databases.getSavedDatabases()) {
            if (db.getId().equals(id)) {
                throw new CBLDuplicateNewDatabaseNameException();
            }
        }

        SavedCBLDatabase newdDb = new SavedCBLDatabase();
        newdDb.setId(id);
        newdDb.setName(name);
        newdDb.setPath(path);

        databases.getSavedDatabases().add(newdDb);

        return newdDb;
    }

    public static void listCollections(DefaultMutableTreeNode parentNode, String scopeName) throws CouchbaseLiteException {

        parentNode.removeAllChildren();

        Database database = ActiveCBLDatabase.getInstance().getDatabase();
        for (Collection collection : database.getCollections(scopeName)) {
            DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new CBLCollectionNodeDescriptor(collection.getName(), scopeName));
            collectionNode.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));
            parentNode.add(collectionNode);
        }
    }

    public static void createIndex(String scope, String collection, String indexName, List<String> attributes) throws CouchbaseLiteException {
        ValueIndexItem[] properties = attributes.stream().map(e -> ValueIndexItem.property(e)).collect(Collectors.toList()).toArray(new ValueIndexItem[attributes.size()]);

        ActiveCBLDatabase.getInstance().getDatabase().getScope(scope).getCollection(collection).createIndex(indexName, IndexBuilder.valueIndex(properties));

    }

    public static void deleteIndex(String scope, String collection, String indexName) throws CouchbaseLiteException {
        ActiveCBLDatabase.getInstance().getDatabase().getScope(scope).getCollection(collection).deleteIndex(indexName);

    }


    public static void setDocumentExpiration(String scope, String collection, String docId, Date date) {

        try {
            Collection col = ActiveCBLDatabase.getInstance().getDatabase().getScope(scope).getCollection(collection);
            col.setDocumentExpiration(docId, date);

        } catch (Exception e) {
            ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("An error ocurred while setting the expiration date for the document " + docId + ". Check the logs for more.", "Couchbase Plugin Error"));
            Log.error("Failed to set the expiration for document " + docId, e);
        }

    }


    public static String getDocMetadata(String scope, String collection, String docId) {

        try {

            Document document = ActiveCBLDatabase.getInstance().getDatabase().getScope(scope).getCollection(collection).getDocument(docId);

            JSONObject metadata = new JSONObject();

            boolean isDeleted = document.toMap().containsKey("_deleted") && (boolean) document.toMap().get("_deleted");

            Date expirationDate = ActiveCBLDatabase.getInstance().getDatabase().getScope(scope).getCollection(collection).getDocumentExpiration(docId);

            metadata.put("_id", document.getId());
            metadata.put("_revisionID", document.getRevisionID());
            metadata.put("_deleted", isDeleted);
            metadata.put("_sequence", document.getSequence());

            if (expirationDate != null) {
                metadata.put("_expiration", expirationDate.toString());
            } else {
                metadata.put("_expiration", JSONObject.NULL);
            }

            return metadata.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Log.error("Failed to load the metadata for document " + docId, e);
            return null;
        }
    }

    public static void listIndexes(DefaultMutableTreeNode parentNode, Tree tree) {

        Object userObject = parentNode.getUserObject();
        tree.setPaintBusy(true);

        try {

            if (userObject instanceof CBLIndexesNodeDescriptor) {
                parentNode.removeAllChildren();
                CBLIndexesNodeDescriptor indexNode = (CBLIndexesNodeDescriptor) parentNode.getUserObject();

                Set<String> indexes = ActiveCBLDatabase.getInstance().getDatabase().getScope(indexNode.getScope()).getCollection(indexNode.getCollection()).getIndexes();

                if (!indexes.isEmpty()) {
                    for (String index : indexes) {
                        DefaultMutableTreeNode idx = new DefaultMutableTreeNode(new CBLIndexNodeDescriptor(index, indexNode.getScope(), indexNode.getCollection()));
                        parentNode.add(idx);
                    }
                } else {
                    parentNode.add(new DefaultMutableTreeNode(new CBLEmptyNodeDescriptor("No Indexes found")));
                }

                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
            } else {
                throw new IllegalStateException("Called listIndexes with a node that is not an instance of CBLIndexesNodeDescriptor");
            }
        } catch (Exception e) {
            Log.error("An error occurred while listing the indexes", e);
        } finally {
            tree.setPaintBusy(false);
        }
    }

    public static void listDocuments(DefaultMutableTreeNode parentNode, Tree tree, int newOffset) {
        Object userObject = parentNode.getUserObject();


        if (userObject instanceof CBLCollectionNodeDescriptor) {
            tree.setPaintBusy(true);

            try {
                CBLCollectionNodeDescriptor colNode = (CBLCollectionNodeDescriptor) parentNode.getUserObject();

                if (newOffset == 0) {
                    //removed loading node
                    parentNode.removeAllChildren();
                    DefaultMutableTreeNode indexesNode = new DefaultMutableTreeNode(new CBLIndexesNodeDescriptor(colNode.getScope(), colNode.getText()));
                    indexesNode.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));
                    parentNode.add(indexesNode);

                } else {
                    //removes "Load More" node
                    parentNode.remove(parentNode.getChildCount() - 1);
                }

                String query = "Select meta(couchbaseAlias).id as cbFileNameId  " + "from `" + colNode.getScope() + "`.`" + colNode.getText() + "` as couchbaseAlias  order by meta(couchbaseAlias).id " + (newOffset == 0 ? "" : " OFFSET " + newOffset) + " limit 10";

                Query thisQuery = ActiveCBLDatabase.getInstance().getDatabase().createQuery(query);
                List<Result> results = thisQuery.execute().allResults();

                if (!results.isEmpty()) {
                    for (Result result : results) {
                        String docId = result.getString("cbFileNameId");
                        String fileName = docId + ".json";

                        CBLFileNodeDescriptor node = new CBLFileNodeDescriptor(fileName, colNode.getScope(), colNode.getText(), docId, null);
                        DefaultMutableTreeNode jsonFileNode = new DefaultMutableTreeNode(node);

                        Document document = ActiveCBLDatabase.getInstance().getDatabase().getScope(colNode.getScope()).getCollection(colNode.getText()).getDocument(docId);
                        if (CBLBlobHandler.documentHasBlob(document)) {
                            DefaultMutableTreeNode blobLoadingNode = new DefaultMutableTreeNode(new LoadingNodeDescriptor());
                            jsonFileNode.add(blobLoadingNode);
                        }
                        parentNode.add(jsonFileNode);
                    }

                    if (results.size() == 10) {
                        DefaultMutableTreeNode loadMoreNode = new DefaultMutableTreeNode(new CBLLoadMoreNodeDescriptor(colNode.getScope(), colNode.getText(), newOffset + 10));
                        parentNode.add(loadMoreNode);
                    }
                } else if (newOffset == 0) {
                    parentNode.add(new DefaultMutableTreeNode(new NoResultsNodeDescriptor()));
                }
                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);

            } catch (Exception e) {
                Log.error(e);
                e.printStackTrace();
            } finally {
                tree.setPaintBusy(false);
            }

        } else {
            tree.setPaintBusy(false);
            throw new IllegalStateException("The expected parent was CBLCollectionNodeDescriptor but got something else");
        }

    }


    public static void listScopesAndCollections(DefaultMutableTreeNode parent) throws CouchbaseLiteException {

        parent.removeAllChildren();

        Database database = ActiveCBLDatabase.getInstance().getDatabase();

        for (Scope scope : database.getScopes()) {
            DefaultMutableTreeNode scopeNode = new DefaultMutableTreeNode(new CBLScopeNodeDescriptor(scope.getName()));
            parent.add(scopeNode);

            for (Collection col : database.getCollections(scope.getName())) {
                DefaultMutableTreeNode colNode = new DefaultMutableTreeNode(new CBLCollectionNodeDescriptor(col.getName(), scope.getName()));
                colNode.add(new DefaultMutableTreeNode(new LoadingNodeDescriptor()));

                scopeNode.add(colNode);
            }
        }
    }

    public static void listBlobs(DefaultMutableTreeNode documentNode, Tree tree) {
        try {
            tree.setPaintBusy(true);

            CBLFileNodeDescriptor fileNode = (CBLFileNodeDescriptor) documentNode.getUserObject();
            Document document = ActiveCBLDatabase.getInstance().getDatabase().getScope(fileNode.getScope()).getCollection(fileNode.getCollection()).getDocument(fileNode.getId());

            // This could be run on a separate thread if fetching the blobs is a
            // long-running operation
            Map<String, Blob> blobs = CBLBlobHandler.getDocumentBlobsWithNames(document);

            // Once the blobs are loaded, update the tree on the Swing event dispatch thread
            ApplicationManager.getApplication().invokeLater(() -> {
                documentNode.removeAllChildren();
                for (Map.Entry<String, Blob> entry : blobs.entrySet()) {
                    DefaultMutableTreeNode blobNode = new DefaultMutableTreeNode(new CBLBlobNodeDescriptor(entry.getValue(), entry.getKey(), fileNode.getScope(), fileNode.getCollection(), fileNode.getId()));
                    documentNode.add(blobNode);
                }
                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(documentNode);
            });
        } catch (Exception e) {
            Log.error("An error occurred while trying to load the blobs", e);
            ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage("<html>Could not load the blobs for the document <strong>" + documentNode.getUserObject() + "</strong>. Please check the log for more.</html>", "Couchbase Plugin Error"));
        } finally {
            tree.setPaintBusy(false);
        }
    }


    public static void deleteConnection(String id) {

        CBLDatabases databases = CBLDatabaseStorage.getInstance().getValue();

        databases.setSavedDatabases(databases.getSavedDatabases().stream().filter(e -> !e.getId().equals(id)).collect(Collectors.toList()));
    }


    public static void loadDocument(Project project, CBLFileNodeDescriptor node, Tree tree) {
        tree.setPaintBusy(true);

        if (node.getVirtualFile() != null) {
            return;
        }

        String cas = null;
        try {
            Document document = ActiveCBLDatabase.getInstance().getDatabase().getScope(node.getScope()).getCollection(node.getCollection()).getDocument(node.getId());

            if (document != null) {
                cas = document.getRevisionID();
            }


        } catch (CouchbaseLiteException e) {
            Log.error("Could not load the document " + node.getId() + ".", e);
            ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage("<html>Could not load the document <strong>" + node.getId() + "</strong>. Please check the log for more.</html>", "Couchbase Plugin Error"));
            tree.setPaintBusy(false);
            return;
        }

        final String docCass = cas;
        try {
            ApplicationManager.getApplication().runWriteAction(() -> {
                CBLDocumentVirtualFile virtualFile = new CBLDocumentVirtualFile(project, JsonFileType.INSTANCE, node.getScope(), node.getCollection(), node.getId());

                virtualFile.putUserData(VirtualFileKeys.CBL_CON_ID, ActiveCBLDatabase.getInstance().getDatabaseId());
                virtualFile.putUserData(VirtualFileKeys.SCOPE, node.getScope());
                virtualFile.putUserData(VirtualFileKeys.COLLECTION, node.getCollection());
                virtualFile.putUserData(VirtualFileKeys.ID, node.getId());
                virtualFile.putUserData(VirtualFileKeys.CAS, docCass);
                node.setVirtualFile(virtualFile);
            });
        } catch (Exception e) {
            Log.error("An error occurred while trying to load the file", e);
            ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage("<html>Could not load the document <strong>" + node.getId() + "</strong>. Please check the log for more.</html>", "Couchbase Plugin Error"));
        } finally {
            tree.setPaintBusy(false);
        }
    }

}
