package com.couchbase.intellij.tree.cblite;

import com.couchbase.intellij.DocumentFormatter;
import com.couchbase.intellij.tree.cblite.dialog.CBLAttachBlobDialog;
import com.couchbase.intellij.tree.cblite.dialog.CBLCreateCollectionDialog;
import com.couchbase.intellij.tree.cblite.dialog.CBLExportDialog;
import com.couchbase.intellij.tree.cblite.dialog.CBLImportDialog;
import com.couchbase.intellij.tree.cblite.nodes.*;
import com.couchbase.intellij.tree.cblite.storage.CBLBlobHandler;
import com.couchbase.intellij.workbench.Log;
import com.couchbase.lite.*;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class CBLTreeRightClickListener {

    private static final String COUCHBASE_LITE_PLUGIN_ERROR = "Couchbase Lite Plugin Error";

    private CBLTreeRightClickListener() {
    }

    public static void handle(Tree tree, Project project, MouseEvent e, DefaultMutableTreeNode clickedNode) {
        Object userObject = clickedNode.getUserObject();
        int row = tree.getClosestRowForLocation(e.getX(), e.getY());
        tree.setSelectionRow(row);

        if (userObject instanceof CBLDatabaseNodeDescriptor) {
            handleDatabase(project, e, clickedNode, (CBLDatabaseNodeDescriptor) userObject, tree);
        } else if (userObject instanceof CBLScopeNodeDescriptor) {
            handleScope(project, e, clickedNode, (CBLScopeNodeDescriptor) userObject, tree);
        } else if (userObject instanceof CBLCollectionNodeDescriptor) {
            handleCollection(project, e, clickedNode, (CBLCollectionNodeDescriptor) userObject, tree);
        } else if (userObject instanceof CBLFileNodeDescriptor) {
            handleDocument(project, e, clickedNode, (CBLFileNodeDescriptor) userObject, tree);
        } else if (userObject instanceof CBLIndexesNodeDescriptor) {
            handleIndexes(project, e, clickedNode, (CBLIndexesNodeDescriptor) userObject, tree);
        } else if (userObject instanceof CBLIndexNodeDescriptor) {
            handleIndex(project, e, clickedNode, (CBLIndexNodeDescriptor) userObject, tree);
        } else if (userObject instanceof CBLBlobNodeDescriptor) {
            handleBlob(project, e, clickedNode, (CBLBlobNodeDescriptor) userObject, tree);
        }
    }

    private static void handleIndex(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CBLIndexNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction deleteIndex = new AnAction("Delete Index") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                int result = Messages.showYesNoDialog("Are you sure that you want to delete the index \"" + userObject.getText() + "\"", "Delete Index", Messages.getQuestionIcon());

                if (result != Messages.YES) {
                    return;
                }
                try {
                    CBLDataLoader.deleteIndex(userObject.getScope(), userObject.getCollection(), userObject.getText());
                    tree.collapsePath(new TreePath(((DefaultMutableTreeNode) clickedNode.getParent()).getPath()));
                    tree.expandPath(new TreePath(((DefaultMutableTreeNode) clickedNode.getParent()).getPath()));
                } catch (Exception ex) {
                    Log.error("Could not delete the index " + userObject.getText(), ex);
                    Messages.showErrorDialog("Could not delete the index. Please check the logs for more.", "Couchbase Plugin Error");
                }
            }
        };
        actionGroup.add(deleteIndex);

        showPopup(e, tree, actionGroup);
    }

    private static void handleIndexes(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CBLIndexesNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction createIndex = new AnAction("Create Index") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                CBLCreateIndexDialog dialog = new CBLCreateIndexDialog(project, clickedNode, tree);
                dialog.show();
            }
        };
        actionGroup.add(createIndex);

        showPopup(e, tree, actionGroup);
    }

    private static void handleDatabase(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CBLDatabaseNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        boolean isActive = userObject.getDatabase().getId().equals(ActiveCBLDatabase.getInstance().getDatabaseId());
        if (isActive) {
            AnAction menuItem = new AnAction("Disconnect") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {

                    try {
                        CBLTreeHandler.disconnectFromCluster(clickedNode, userObject, tree);
                    } catch (CouchbaseLiteException ex) {
                        Log.error(ex);
                        SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not disconnect from the database.", "Couchbase Lite Connection Error"));
                    }
                }
            };
            actionGroup.add(menuItem);

            AnAction createCollection = new AnAction("Create a Collection") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    try {
                        CBLCreateCollectionDialog dialog = new CBLCreateCollectionDialog();
                        dialog.show();
                        CBLDataLoader.listScopesAndCollections(clickedNode);
                        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(clickedNode);
                    } catch (Exception ex) {
                        Log.error(ex);
                        SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not create the collection.", COUCHBASE_LITE_PLUGIN_ERROR));
                    }
                }
            };
            actionGroup.add(createCollection);

            DefaultActionGroup toolsAction = new DefaultActionGroup("Tools", true);

            AnAction cblExport = new AnAction("Couchbase Lite Export") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    CBLExportDialog exportDialog = new CBLExportDialog(project,tree);
                    exportDialog.show();
                }
            };
            toolsAction.add(cblExport);
            AnAction cblImport = new AnAction("Couchbase Lite Import") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    CBLImportDialog importDialog = new CBLImportDialog(project);
                    importDialog.show();
                }
            };
            toolsAction.add(cblImport);
            actionGroup.add(toolsAction);

        } else {
            AnAction menuItem = new AnAction("Connect") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    CBLTreeHandler.connectToDatabase(project, userObject.getDatabase(), tree);
                }
            };
            actionGroup.add(menuItem);

        }

        AnAction deleteConnection = new AnAction("Delete Connection") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                try {
                    CBLTreeHandler.deleteConnection(clickedNode, userObject, tree);
                } catch (CouchbaseLiteException ex) {
                    Log.error(ex);
                    SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not delete the connection.", COUCHBASE_LITE_PLUGIN_ERROR));
                }
            }
        };
        actionGroup.add(deleteConnection);
        showPopup(e, tree, actionGroup);
    }

    private static void handleScope(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CBLScopeNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction deleteScope = new AnAction("Delete Scope") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int result = Messages.showYesNoDialog("<html>Are you sure you want to delete the scope <strong>" + userObject.getText() + "</strong>?</html>", "Delete Scope", Messages.getQuestionIcon());
                if (result != Messages.YES) {
                    return;
                }

                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) clickedNode.getParent();

                // Create a background task for deleting the scope
                Task.Backgroundable task = new Task.Backgroundable(project, "Deleting scope", true) {
                    public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            boolean anyDocumentHadBlob;
                            for (int i = 0; i < clickedNode.getChildCount(); i++) {
                                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) clickedNode.getChildAt(i);
                                CBLCollectionNodeDescriptor collectionNode = (CBLCollectionNodeDescriptor) childNode.getUserObject();
                                Collection collection = Objects.requireNonNull(ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getText())).getCollection(collectionNode.getText());
                                anyDocumentHadBlob = CBLBlobHandler.collectionHasBlob(collection);
                                ActiveCBLDatabase.getInstance().getDatabase().deleteCollection(collectionNode.getText(), userObject.getText());
                                // After deleting the scope, perform maintenance with COMPACT type if any
                                // document had a blob
                                if (anyDocumentHadBlob) {
                                    Log.info("Performing maintenance with COMPACT type after deleting the scope " + userObject.getText());
                                    ActiveCBLDatabase.getInstance().getDatabase().performMaintenance(MaintenanceType.COMPACT);
                                }
                            }
                            ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(clickedNode);
                            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                        } catch (Exception ex) {
                            Log.error(ex);
                            SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not delete the scope.", COUCHBASE_LITE_PLUGIN_ERROR));
                        }
                    }
                };

                // Run the background task
                ProgressManager.getInstance().run(task);
            }
        };
        actionGroup.add(deleteScope);

        showPopup(e, tree, actionGroup);
    }

    private static void handleCollection(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CBLCollectionNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction openDocument = new AnAction("Open Document") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                CBLOpenDocumentDialog dialog = new CBLOpenDocumentDialog(false, project, tree, clickedNode, userObject.getScope(), userObject.getText());
                dialog.show();
            }
        };
        actionGroup.add(openDocument);

        AnAction createDocument = new AnAction("Create Document") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                CBLOpenDocumentDialog dialog = new CBLOpenDocumentDialog(true, project, tree, clickedNode, userObject.getScope(), userObject.getText());
                dialog.show();
            }
        };
        actionGroup.add(createDocument);

        AnAction deleteCollection = new AnAction("Delete Collection") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int result = Messages.showYesNoDialog("<html>Are you sure you want to delete the collection <strong>" + userObject.getText() + "</strong>?</html>", "Delete Collection", Messages.getQuestionIcon());
                if (result != Messages.YES) {
                    return;
                }

                Task.Backgroundable deleteCollectionTask = new Task.Backgroundable(project, "Deleting collection") {
                    public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            Collection collection = Objects.requireNonNull(ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getScope())).getCollection(userObject.getText());
                            boolean anyDocumentHadBlob = CBLBlobHandler.collectionHasBlob(collection);
                            ActiveCBLDatabase.getInstance().getDatabase().deleteCollection(userObject.getText(), userObject.getScope());
                            if (anyDocumentHadBlob) {
                                Log.info("Performing maintenance with COMPACT type after deleting the collection " + userObject.getText());
                                ActiveCBLDatabase.getInstance().getDatabase().performMaintenance(MaintenanceType.COMPACT);
                            }
                            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) clickedNode.getParent();
                            Scope scope = ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getScope());
                            SwingUtilities.invokeLater(() -> {
                                ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(clickedNode);
                                // If the scope has no collections after deleting the collection, remove the
                                // scope
                                try {
                                    if ((scope == null) || (scope.getCollections().isEmpty())) {
                                        DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();
                                        ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(parentNode);
                                        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(grandParentNode);
                                    } else {
                                        parentNode.removeAllChildren();
                                        CBLDataLoader.listCollections(parentNode, userObject.getScope());
                                        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                                    }
                                } catch (CouchbaseLiteException e) {
                                    Log.error(e);
                                    SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not delete the scope after deleting the collection.", COUCHBASE_LITE_PLUGIN_ERROR));
                                }
                            });
                        } catch (Exception ex) {
                            Log.error(ex);
                            SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not delete the collection.", COUCHBASE_LITE_PLUGIN_ERROR));
                        }
                    }
                };
                deleteCollectionTask.queue();
            }
        };
        actionGroup.add(deleteCollection);

        showPopup(e, tree, actionGroup);
    }

    private static void handleDocument(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CBLFileNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction openDocument = new AnAction("View Metadata") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                String metadata = CBLDataLoader.getDocMetadata(userObject.getScope(), userObject.getCollection(), userObject.getId());

                if (metadata != null) {
                    VirtualFile virtualFile = new LightVirtualFile("(read-only) " + userObject.getId() + "_meta.json", FileTypeManager.getInstance().getFileTypeByExtension("json"), metadata);
                    DocumentFormatter.formatFile(project, virtualFile);
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    fileEditorManager.openFile(virtualFile, true);
                }
            }
        };
        actionGroup.add(openDocument);

        actionGroup.addSeparator();

        AnAction setDocumentExpiration = new AnAction("Set Document Expiration") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                DocumentExpirationDialog dialog = null;
                try {
                    dialog = new DocumentExpirationDialog(userObject.getScope(), userObject.getCollection(), userObject.getId());
                } catch (CouchbaseLiteException ex) {
                    Log.error("An error occurred while loading the document expiration dialog", ex);
                }
                dialog.show();
            }
        };
        actionGroup.add(setDocumentExpiration);

        AnAction attachBlob = new AnAction("Attach Blob") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                try {
                    Scope scope = ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getScope());
                    Collection collection = Objects.requireNonNull(scope).getCollection(userObject.getCollection());
                    assert collection != null;
                    Document document = collection.getDocument(userObject.getId());
                    CBLAttachBlobDialog dialog = new CBLAttachBlobDialog(project, collection, document);
                    if (dialog.showAndGet()) {
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) clickedNode.getParent();
                        parentNode.removeAllChildren();
                        CBLDataLoader.listDocuments(parentNode, tree, 0);
                        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                    }
                } catch (Exception ex) {
                    Log.error("An error occurred while trying to attach a blob to the document " + userObject.getId(), ex);
                    SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not attach the blob. Please check the logs for more.", "Couchbase Plugin Error"));
                }
            }
        };
        actionGroup.add(attachBlob);

        AnAction deleteDocument = new AnAction("Delete Document") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int result = Messages.showYesNoDialog("<html>Are you sure you want to delete the document <strong>" + userObject.getId() + "</strong>?</html>", "Delete Document", Messages.getQuestionIcon());
                if (result != Messages.YES) {
                    return;
                }

                try {

                    if (userObject.getVirtualFile() != null) {
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            try {
                                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                                fileEditorManager.closeFile(userObject.getVirtualFile());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Log.debug("Could not close the file", ex);
                            }
                        });
                    }

                    Document document = ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getScope()).getCollection(userObject.getCollection()).getDocument(userObject.getId());
                    ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getScope()).getCollection(userObject.getCollection()).delete(document);

                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) clickedNode.getParent();
                    SwingUtilities.invokeLater(() -> {
                        if (parentNode != null) {
                            ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(clickedNode);
                        }
                        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                    });
                } catch (Exception ex) {
                    Log.error("An error occurred while trying to delete the document " + userObject.getId(), ex);
                    SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not delete the document. Please check the logs for more.", "Couchbase Plugin Error"));
                }
            }
        };
        actionGroup.add(deleteDocument);

        showPopup(e, tree, actionGroup);
    }

    private static void handleBlob(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CBLBlobNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // AnAction openBlob = new AnAction("Open Blob") {
        // @Override
        // public void actionPerformed(@NotNull AnActionEvent e) {
        //     // TODO: Implement this
        //  }
        // };
        // actionGroup.add(openBlob);

        AnAction deleteBlob = new AnAction("Delete Blob") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int result = Messages.showYesNoDialog("<html>Are you sure you want to delete the blob <strong>" + userObject.getBlobName() + "</strong>?</html>", "Delete Blob", Messages.getQuestionIcon());
                if (result != Messages.YES) {
                    return;
                }
                try {
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) clickedNode.getParent();
                    DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();

                    Document document = ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getScope()).getCollection(userObject.getCollection()).getDocument(userObject.getDocument());
                    MutableDocument mutableDocument = document.toMutable();
                    Collection collection = ActiveCBLDatabase.getInstance().getDatabase().getScope(userObject.getScope()).getCollection(userObject.getCollection());
                    CBLBlobHandler.removeBlobFromDocument(collection, mutableDocument, userObject.getBlob());
                    Task.Backgroundable deleteBlobTask = new Task.Backgroundable(project, "Deleting blob", true) {
                        public void run(@NotNull ProgressIndicator indicator) {
                            try {
                                Log.info("Performing maintenance with COMPACT type after deleting the blob " + userObject.getBlobName());
                                ActiveCBLDatabase.getInstance().getDatabase().performMaintenance(MaintenanceType.COMPACT);
                            } catch (Exception ex) {
                                Log.error("An error occurred while trying to delete the blob " + userObject.getDigest(), ex);
                                SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not delete the blob. Please check the logs for more.", COUCHBASE_LITE_PLUGIN_ERROR));
                            }
                        }
                    };
                    deleteBlobTask.queue();
                    ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(clickedNode);
                    ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(grandParentNode);
                } catch (Exception ex) {
                    Log.error("An error occurred while trying to delete the blob " + userObject.getDigest(), ex);
                    SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Could not delete the blob. Please check the logs for more.", COUCHBASE_LITE_PLUGIN_ERROR));
                }
            }
        };
        actionGroup.add(deleteBlob);
        showPopup(e, tree, actionGroup);
    }

    private static void showPopup(MouseEvent e, Tree tree, DefaultActionGroup actionGroup) {
        DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        JBPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, actionGroup, dataContext, JBPopupFactory.ActionSelectionAid.MNEMONICS, false);
        popup.show(new RelativePoint(e));
    }
}
