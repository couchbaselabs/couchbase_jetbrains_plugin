package com.couchbase.intellij.tree;

import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.intellij.DocumentFormatter;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.database.DataLoader;
import com.couchbase.intellij.database.InferHelper;
import com.couchbase.intellij.persistence.storage.QueryFiltersStorage;
import com.couchbase.intellij.tools.CBExport;
import com.couchbase.intellij.tools.CBImport;
import com.couchbase.intellij.tools.CBTools;
import com.couchbase.intellij.tools.PillowFightDialog;
import com.couchbase.intellij.tools.dialog.CbstatsDialog;
import com.couchbase.intellij.tools.dialog.DDLExportDialog;
import com.couchbase.intellij.tools.dialog.ExportDialog;
import com.couchbase.intellij.tools.dialog.ImportDialog;
import com.couchbase.intellij.tree.docfilter.DocumentFilterDialog;
import com.couchbase.intellij.tree.node.*;
import com.couchbase.intellij.tree.overview.IndexOverviewDialog;
import com.couchbase.intellij.tree.overview.ServerOverviewDialog;
import com.couchbase.intellij.tree.overview.apis.CouchbaseRestAPI;
import com.couchbase.intellij.workbench.Log;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColorPicker;
import com.intellij.ui.ColorPickerListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.TimeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TreeRightClickListener {

    public static void handle(Tree tree, Project project, JPanel toolbarPanel, MouseEvent e, DefaultMutableTreeNode clickedNode) {
        Object userObject = clickedNode.getUserObject();
        int row = tree.getClosestRowForLocation(e.getX(), e.getY());
        tree.setSelectionRow(row);

        if (userObject instanceof ConnectionNodeDescriptor) {
            handleConnectionRightClick(project, toolbarPanel, e, clickedNode, (ConnectionNodeDescriptor) userObject, tree);
        } else if (userObject instanceof BucketNodeDescriptor) {
            handleBucketRightClick(project, e, clickedNode, tree);
        } else if (userObject instanceof ScopeNodeDescriptor) {
            handleScopeRightClick(project, e, clickedNode, tree);
        } else if (userObject instanceof CollectionNodeDescriptor) {
            handleCollectionRightClick(project, e, clickedNode, (CollectionNodeDescriptor) userObject, tree);
        } else if (userObject instanceof FileNodeDescriptor) {
            handleDocumentRightClick(project, e, clickedNode, (FileNodeDescriptor) userObject, tree);
        } else if (userObject instanceof IndexNodeDescriptor) {
            handleIndexRightClick(project, e, clickedNode, (IndexNodeDescriptor) userObject, tree);
        }
    }

    private static void handleConnectionRightClick(Project project, JPanel toolBarPanel, MouseEvent e, DefaultMutableTreeNode clickedNode, ConnectionNodeDescriptor userObject, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        if (userObject.isActive()) {

            AnAction clusterOverview = new AnAction("Cluster Overview") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    ServerOverviewDialog overview = new ServerOverviewDialog(true);
                    overview.show();
                }
            };
            actionGroup.add(clusterOverview);
            actionGroup.addSeparator();

            if (!ActiveCluster.getInstance().isReadOnlyMode() && !ActiveCluster.getInstance().isCapella()) {
                // Add "Create New Bucket" option
                AnAction createNewBucketItem = new AnAction("Create New Bucket") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        try {
                            new NewBucketCreationDialog(project).show((cancelled, err) -> {
                                TreePath treePath = new TreePath(clickedNode.getPath());
                                tree.collapsePath(treePath);
                                tree.expandPath(treePath);
                                tree.invalidate();
                            });
                        } catch (Exception ex) {
                            Log.error("Bucket Creation failed ", ex);
                        }
                    }
                };
                actionGroup.add(createNewBucketItem);
            }

            AnAction refreshBuckets = new AnAction("Refresh Buckets") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    TreePath treePath = new TreePath(clickedNode.getPath());
                    tree.collapsePath(treePath);
                    tree.expandPath(treePath);
                }
            };
            actionGroup.add(refreshBuckets);
            actionGroup.addSeparator();

            AnAction menuItem = new AnAction("Disconnect") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    TreeActionHandler.disconnectFromCluster(clickedNode, userObject, tree);
                }
            };
            actionGroup.add(menuItem);

            DefaultActionGroup tools = new DefaultActionGroup("Tools", true);

            if (!ActiveCluster.getInstance().isReadOnlyMode() && CBTools.getTool(CBTools.Type.CBC_PILLOW_FIGHT).isAvailable()) {
                AnAction pillowFight = new AnAction("Pillow Fight") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        PillowFightDialog pillowFightDialog = new PillowFightDialog(project);
                        pillowFightDialog.show();
                    }
                };
                tools.add(pillowFight);
                tools.addSeparator();
            }

            AnAction ddlExport = new AnAction("DDL Export") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    DDLExportDialog dialog = new DDLExportDialog();
                    dialog.show();
                }
            };
            tools.add(ddlExport);

            if (CBTools.getTool(CBTools.Type.CB_EXPORT).isAvailable()) {
                AnAction cbexport = new AnAction("Data Export") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        ExportDialog dialog = new ExportDialog();
                        dialog.show();
                    }
                };
                tools.add(cbexport);
            }

            if (!ActiveCluster.getInstance().isReadOnlyMode() && CBTools.getTool(CBTools.Type.CB_IMPORT).isAvailable()) {

                AnAction cbimport = new AnAction("Data Import") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        ImportDialog importDialog = new ImportDialog(project);
                        importDialog.show();
                    }
                };
                tools.add(cbimport);
                tools.addSeparator();
            }

            DefaultActionGroup settings = new DefaultActionGroup("Settings", true);
            DefaultActionGroup colors = new DefaultActionGroup("Connection Colors", true);

            AnAction colorAction = new AnAction("Set Connection Color") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    Color initialColor = JBColor.RED;
                    ColorPickerListener colorPickerListener = new ColorPickerListener() {
                        @Override
                        public void colorChanged(Color newColor) {
                            if (newColor != null) {
                                Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, newColor);
                                Border margin = BorderFactory.createEmptyBorder(0, 0, 1, 0); // Top, left, bottom, right
                                // margins
                                Border compound = BorderFactory.createCompoundBorder(margin, line);
                                toolBarPanel.setBorder(compound);
                                toolBarPanel.revalidate();
                                ActiveCluster.getInstance().setColor(newColor);
                            }
                        }

                        @Override
                        public void closed(@Nullable Color color) {
                            // do nothing
                        }
                    };

                    ColorPicker.showDialog(tree, "Choose a Color for This Connection", initialColor, true, List.of(colorPickerListener), true);
                }
            };
            colors.add(colorAction);

            if (!ActiveCluster.getInstance().isReadOnlyMode()) {
                AnAction readOnlyMode = new AnAction("Enable Read-Only Mode") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        ActiveCluster.getInstance().setReadOnlyMode(true);
                        SwingUtilities.invokeLater(() -> Messages.showWarningDialog("<html>The <strong>Read Only Mode</strong> is a simple guardrail in the plugin to avoid unwanted changes in sensible environments. Please note that this is a <strong>best effort</strong> approach. For true read-only approach, connect to the cluster using read-only credentials.</html>", "Couchbase Plugin Warning"));

                    }
                };
                settings.add(readOnlyMode);
            } else {
                AnAction readWriteMode = new AnAction("Disable Read-Only Mode") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        ActiveCluster.getInstance().setReadOnlyMode(false);
                    }
                };
                settings.add(readWriteMode);
            }

            if (ActiveCluster.getInstance().getColor() != null) {
                AnAction clearConnectionColor = new AnAction("Clear") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        toolBarPanel.setBorder(JBUI.Borders.empty());
                        toolBarPanel.revalidate();
                        ActiveCluster.getInstance().setColor(null);
                    }
                };
                colors.add(clearConnectionColor);
            }

            settings.add(colors);
            actionGroup.add(tools);
            actionGroup.add(settings);
        } else {
            AnAction menuItem = new AnAction("Connect") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    TreeActionHandler.connectToCluster(project, userObject.getSavedCluster(), tree, toolBarPanel);
                }
            };
            actionGroup.add(menuItem);
        }

        actionGroup.addSeparator();
        AnAction editConnection = new AnAction("Edit Connection") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                NewConnectionDialog dialog = new NewConnectionDialog(project, tree, userObject.getSavedCluster(), clickedNode);
                dialog.show();
            }
        };

        actionGroup.add(editConnection);

        AnAction deleteConnection = new AnAction("Delete Connection") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreeActionHandler.deleteConnection(clickedNode, userObject, tree);
            }
        };
        actionGroup.add(deleteConnection);

        showPopup(e, tree, actionGroup);
    }

    private static void handleBucketRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        AnAction menuItem = new AnAction("Refresh Scopes") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath treePath = new TreePath(clickedNode.getPath());
                tree.collapsePath(treePath);
                tree.expandPath(treePath);
            }
        };
        actionGroup.add(menuItem);

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            // Add "Add New Scope" option
            AnAction addNewScopeItem = new AnAction("Add New Scope") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    try {

                        String bucketName = ((BucketNodeDescriptor) clickedNode.getUserObject()).getText();

                    NewScopeCreationDialog entityCreationDialog = new NewScopeCreationDialog(project, bucketName);
                    entityCreationDialog.show();

                        if (entityCreationDialog.isOK()) {
                            String scopeName = entityCreationDialog.getEntityName();
                            ActiveCluster.getInstance().get().bucket(bucketName).collections().createScope(scopeName);
                            DataLoader.listScopes(clickedNode, tree);
                        }
                    } catch (Exception ex) {
                        Log.error("Scope creation failed ", ex);
                    }
                }
            };
            actionGroup.add(addNewScopeItem);

            // Add "Delete Bucket" option
            AnAction deleteBucketItem = new AnAction("Delete Bucket") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    try {

                        String bucketName = ((BucketNodeDescriptor) clickedNode.getUserObject()).getText();

                        // Show confirmation dialog before deleting bucket
                        int result = Messages.showYesNoDialog("Are you sure you want to delete the bucket " + bucketName + "?", "Delete Bucket", Messages.getQuestionIcon());
                        if (result != Messages.YES) {
                            return;
                        }

                        ActiveCluster.ReconnectOnDisconnect.set(true);
                        ActiveCluster.getInstance().get().buckets().dropBucket(bucketName);
                        ActiveCluster.ReconnectOnDisconnect.set(false);
                        // Refresh buckets
                        TreePath treePath = new TreePath(clickedNode.getPath());
                        tree.collapsePath(treePath.getParentPath());
                        tree.expandPath(treePath.getParentPath());
                    } catch (Exception ex) {
                        Log.error("Bucket deletion failed ", ex);
                    }
                }
            };
            actionGroup.add(deleteBucketItem);
        }

        showPopup(e, tree, actionGroup);
    }

    private static void handleScopeRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        ScopeNodeDescriptor scope = (ScopeNodeDescriptor) clickedNode.getUserObject();
        String bucketName = scope.getBucket();
        String scopeName = scope.getText();

        AnAction refreshCollections = new AnAction("Refresh Collections") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath treePath = new TreePath(clickedNode.getPath());
                tree.collapsePath(treePath);
                tree.expandPath(treePath);
            }
        };
        actionGroup.add(refreshCollections);

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            // Add "Add New Collection" option
            AnAction addNewCollectionItem = new AnAction("Add New Collection") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {

                    NewCollectionDialog dialog = new NewCollectionDialog(project, bucketName, scopeName, clickedNode, tree);
                    dialog.show();
                }
            };

            actionGroup.add(addNewCollectionItem);

            if (!"_default".equals(scope.getText())) {
                actionGroup.addSeparator();
                // Add "Delete Scope" option
                AnAction deleteScopeItem = new AnAction("Delete Scope") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        try {

                            // Show confirmation dialog before deleting scope
                            int result = Messages.showYesNoDialog("Are you sure you want to delete the scope " + scopeName + "?", "Delete Scope", Messages.getQuestionIcon());
                            if (result != Messages.YES) {
                                return;
                            }

                            ActiveCluster.getInstance().get().bucket(bucketName).collections().dropScope(scopeName);
                            // Refresh buckets
                            DefaultMutableTreeNode bucketTreeNode = ((DefaultMutableTreeNode) clickedNode.getParent());
                            TreePath treePath = new TreePath(bucketTreeNode.getPath());
                            tree.collapsePath(treePath);
                            tree.expandPath(treePath);
                        } catch (Exception ex) {
                            Log.error("Scope deletion failed ", ex);
                        }
                    }
                };
                actionGroup.add(deleteScopeItem);

            }
        }

        actionGroup.addSeparator();

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            AnAction simpleImport = new AnAction("Simple Import") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    // Show confirmation dialog before deleting scope
                    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json");
                    VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
                    if (file != null) {
                        CBImport.simpleScopeImport(scope.getBucket(), scope.getText(), file.getPath(), project);
                    } else {
                        Messages.showErrorDialog("Simple Import requires a .json file. Please try again.", "Simple Import Error");
                    }
                }
            };
            actionGroup.add(simpleImport);
        }

        AnAction simpleExport = new AnAction("Simple Export") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                FileSaverDescriptor fsd = new FileSaverDescriptor("Simple Scope Export", "Choose where you want to save the file:");
                VirtualFileWrapper wrapper = FileChooserFactory.getInstance().createSaveFileDialog(fsd, project).save(("cb_export-" + scope.getText() + "-" + TimeUtils.getCurrentDateTime() + ".json"));
                if (wrapper != null) {
                    File file = wrapper.getFile();
                    CBExport.simpleScopeExport(scope.getBucket(), scope.getText(), file.getAbsolutePath());
                }
            }
        };
        actionGroup.add(simpleExport);

        actionGroup.addSeparator();

        AnAction viewStats = new AnAction("View Scope Statistics") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // You can replace the following line with your own implementation for viewing scope statistics.
                CbstatsDialog cbstatsDialog = new CbstatsDialog(scope.getBucket(),scope.getText(),"","scope");
                cbstatsDialog.show();
            }
        };
        actionGroup.add(viewStats);

        showPopup(e, tree, actionGroup);
    }

    private static void handleDocumentRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, FileNodeDescriptor col, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        String bucket = col.getBucket();
        String scope = col.getScope();
        String collection = col.getCollection();
        String docId = col.getId();

        AnAction viewMetaData = new AnAction("View Metadata") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                String metadata = null;
                if (ActiveCluster.getInstance().hasQueryService()) {
                    metadata = DataLoader.getDocMetadata(bucket, scope, collection, docId);
                } else {
                    try {
                        metadata = CouchbaseRestAPI.getMetaDocument(bucket, scope, collection, docId);
                    } catch (Exception ex) {
                        Log.debug("Could not get the metadata of the document via the API", ex);
                    }
                }
                if (metadata != null) {
                    VirtualFile virtualFile = new LightVirtualFile("(read-only) " + docId + "_meta.json", FileTypeManager.getInstance().getFileTypeByExtension("json"), metadata);
                    DocumentFormatter.formatFile(project, virtualFile);
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    fileEditorManager.openFile(virtualFile, true);
                }
            }
        };
        actionGroup.add(viewMetaData);
        actionGroup.addSeparator();

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {

            AnAction deleteDoc = new AnAction("Delete Document") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    int result = Messages.showYesNoDialog("<html>Are you sure you want to delete the document <strong>" + col.getId() + "</strong>?</html>", "Delete Document", Messages.getQuestionIcon());
                    if (result != Messages.YES) {
                        return;
                    }

                    try {
                        ActiveCluster.getInstance().get().bucket(bucket).scope(scope).collection(collection).remove(col.getId());

                        if (col.getVirtualFile() != null) {
                            try {
                                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                                fileEditorManager.closeFile(col.getVirtualFile());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Log.debug("Could not close the file", ex);
                            }
                        }

                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) clickedNode.getParent();
                        if (parentNode != null) {
                            ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(clickedNode);
                        }
                        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Log.error("An error occurred while trying to delete the document " + col.getId(), ex);
                        Messages.showErrorDialog("Could not delete the document. Please check the logs for more.", "Couchbase Plugin Error");
                    }

                }
            };
            actionGroup.add(deleteDoc);
        }

        showPopup(e, tree, actionGroup);
    }

    private static void handleIndexRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, IndexNodeDescriptor idx, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction viewIdxStatsAction = new AnAction("View Stats") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                IndexOverviewDialog dialog = new IndexOverviewDialog(project, idx.getBucket(), idx.getScope(), idx.getCollection(), idx.getText().substring(0, idx.getText().lastIndexOf('.')));
                dialog.show();
            }
        };
        actionGroup.add(viewIdxStatsAction);

        showPopup(e, tree, actionGroup);
    }

    private static void showPopup(MouseEvent e, Tree tree, DefaultActionGroup actionGroup) {
        DataContext dataContext = DataManager.getInstance().getDataContext(tree);
        JBPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, actionGroup, dataContext, JBPopupFactory.ActionSelectionAid.MNEMONICS, false);
        popup.show(new RelativePoint(e));
    }

    private static void handleCollectionRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CollectionNodeDescriptor col, Tree tree) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction openDocument = new AnAction("Open Document") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                OpenDocumentDialog dialog = new OpenDocumentDialog(false, project, tree, col.getBucket(), col.getScope(), col.getText());
                dialog.show();
            }
        };
        actionGroup.add(openDocument);

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            AnAction createDocument = new AnAction("Create Document") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    OpenDocumentDialog dialog = new OpenDocumentDialog(true, project, tree, col.getBucket(), col.getScope(), col.getText());
                    dialog.show();
                }
            };
            actionGroup.add(createDocument);
        }

        if (ActiveCluster.getInstance().hasQueryService()) {
            actionGroup.addSeparator();
            String filter = "Add Document Filter";
            boolean hasDeleteFilter = false;
            if (col.getQueryFilter() != null && !col.getQueryFilter().trim().isEmpty()) {
                filter = "Edit Document Filter";
                hasDeleteFilter = true;
            }
            AnAction menuItem = new AnAction(filter) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    DocumentFilterDialog dialog = new DocumentFilterDialog(tree, clickedNode, col.getBucket(), col.getScope(), col.getText());
                    dialog.show();
                }
            };
            actionGroup.add(menuItem);

            if (hasDeleteFilter) {
                AnAction clearDocFilter = new AnAction("Clear Document Filter") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        QueryFiltersStorage.getInstance().getValue().saveQueryFilter(ActiveCluster.getInstance().getId(), col.getBucket(), col.getScope(), col.getText(), null);

                        col.setQueryFilter(null);
                        TreePath treePath = new TreePath(clickedNode.getPath());
                        tree.collapsePath(treePath);
                        tree.expandPath(treePath);
                    }
                };
                actionGroup.add(clearDocFilter);
            }
        }

        actionGroup.addSeparator();
        AnAction refreshDocuments = new AnAction("Refresh Documents") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                InferHelper.invalidateInferCacheIfOlder(col.getBucket(), col.getScope(), col.getText(), TimeUnit.MINUTES.toMillis(1));
                DataLoader.listDocuments(clickedNode, tree, 0);
            }
        };
        actionGroup.add(refreshDocuments);

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {

            if (!"_default".equals(col.getScope()) || (!"_default".equals(col.getText()) && "_default".equals(col.getScope()))) {
                // Add "Delete Collection" option
                actionGroup.addSeparator();
                AnAction deleteCollectionItem = new AnAction("Delete Collection") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        try {

                            int result = Messages.showYesNoDialog("Are you sure you want to delete the collection " + col.getText() + "?", "Delete Collection", Messages.getQuestionIcon());
                            if (result != Messages.YES) {
                                return;
                            }

                            ActiveCluster.getInstance().get().bucket(col.getBucket()).collections().dropCollection(CollectionSpec.create(col.getText(), col.getScope()));
                            // Refresh collections
                            DefaultMutableTreeNode colsTreeNode = ((DefaultMutableTreeNode) clickedNode.getParent());
                            TreePath treePath = new TreePath(colsTreeNode.getPath());
                            tree.collapsePath(treePath);
                            tree.expandPath(treePath);
                        } catch (Exception ex) {
                            Log.error("Collection deletion failed ", ex);
                        }
                    }
                };
                actionGroup.add(deleteCollectionItem);
            }
        }

        // cbexport and cbimport are installed together, so if one is available the
        // other also is
        if (CBTools.getTool(CBTools.Type.CB_EXPORT).isAvailable()) {
            actionGroup.addSeparator();

            if (!ActiveCluster.getInstance().isReadOnlyMode()) {
                AnAction simpleImport = new AnAction("Simple Import") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json");
                        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
                        if (file != null) {
                            CBImport.simpleCollectionImport(col.getBucket(), col.getScope(), col.getText(), file.getPath(), null);
                        } else {
                            Messages.showErrorDialog("Simple Import requires a .json file. Please try again.", "Simple Import Error");
                        }
                    }
                };
                actionGroup.add(simpleImport);
            }

            AnAction simpleExport = new AnAction("Simple Export") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    FileSaverDescriptor fsd = new FileSaverDescriptor("Simple Collection Export", "Choose where you want to save the file:");
                    VirtualFileWrapper wrapper = FileChooserFactory.getInstance().createSaveFileDialog(fsd, project).save(("cb_export-" + col.getScope() + "_" + col.getText() + "-" + TimeUtils.getCurrentDateTime() + ".json"));
                    if (wrapper != null) {
                        File file = wrapper.getFile();
                        CBExport.simpleCollectionExport(col.getBucket(), col.getScope(), col.getText(), file.getAbsolutePath(), null);
                    }
                }
            };
            actionGroup.add(simpleExport);
        }

        actionGroup.addSeparator();
        AnAction viewStats = new AnAction("View Collection Statistics") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                CbstatsDialog cbstatsDialog = new CbstatsDialog(
                        col.getBucket(),
                        col.getScope(),
                        col.getText(),
                        "collection"
                );
                cbstatsDialog.show();
            }
        };
        actionGroup.add(viewStats);

        showPopup(e, tree, actionGroup);
    }
}
