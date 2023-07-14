package com.couchbase.intellij.tree;

import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.intellij.DocumentFormatter;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.database.DataLoader;
import com.couchbase.intellij.persistence.storage.QueryFiltersStorage;
import com.couchbase.intellij.tools.CBExport;
import com.couchbase.intellij.tools.CBImport;
import com.couchbase.intellij.tools.CBTools;
import com.couchbase.intellij.tools.dialog.ExportDialog;
import com.couchbase.intellij.tree.docfilter.DocumentFilterDialog;
import com.couchbase.intellij.tree.node.*;
import com.couchbase.intellij.tree.overview.IndexOverviewDialog;
import com.couchbase.intellij.tree.overview.ServerOverviewDialog;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColorChooser;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import utils.TimeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;

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
        JBPopupMenu popup = new JBPopupMenu();

        if (userObject.isActive()) {

            JBMenuItem clusterOverview = new JBMenuItem("Cluster Overview");
            clusterOverview.addActionListener(l -> {
                ServerOverviewDialog overview = new ServerOverviewDialog(true);
                overview.show();
            });
            popup.add(clusterOverview);
            popup.addSeparator();


            JBMenuItem refreshBuckets = new JBMenuItem("Refresh Buckets");
            refreshBuckets.addActionListener(e12 -> {
                TreePath treePath = new TreePath(clickedNode.getPath());
                tree.collapsePath(treePath);
                tree.expandPath(treePath);
            });
            popup.add(refreshBuckets);
            popup.addSeparator();

            JBMenuItem menuItem = new JBMenuItem("Disconnect");
            popup.add(menuItem);
            menuItem.addActionListener(event -> TreeActionHandler.disconnectFromCluster(clickedNode, userObject, tree));

            JMenu tools = new JMenu("Tools");
            JBMenuItem pillowFight = new JBMenuItem("Pillow Fight");
            pillowFight.addActionListener(e13 -> {
                PillowFightDialog pillowFightDialog = new PillowFightDialog(project);
                pillowFightDialog.show();
            });
            tools.add(pillowFight);

            JBMenuItem cbexport = new JBMenuItem("Data Export");
            cbexport.addActionListener(event -> {
                ExportDialog dialog = new ExportDialog();
                dialog.show();
            });
            tools.add(cbexport);

            if (!ActiveCluster.getInstance().isReadOnlyMode()) {
                JBMenuItem cbimport = new JBMenuItem("Data Import");
                tools.add(cbimport);
            }

            JMenu settings = new JMenu("Settings");
            JMenu colors = new JMenu("Connection Colors");

            JBMenuItem colorAction = new JBMenuItem("Set Connection Color");
            colorAction.addActionListener(event -> {
                Color initialColor = Color.RED;  // the color initially selected in the dialog
                boolean enableOpacity = true;    // whether to allow the user to choose an opacity
                String title = "Choose a Color for This Connection"; // the title of the dialog

                Color chosenColor = ColorChooser.chooseColor(tree, title, initialColor, enableOpacity);
                if (chosenColor != null) {
                    Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, chosenColor);
                    Border margin = BorderFactory.createEmptyBorder(0, 0, 1, 0); // Top, left, bottom, right margins
                    Border compound = BorderFactory.createCompoundBorder(margin, line);
                    toolBarPanel.setBorder(compound);
                    toolBarPanel.revalidate();
                    ActiveCluster.getInstance().setColor(chosenColor);
                }
            });
            colors.add(colorAction);


            if (!ActiveCluster.getInstance().isReadOnlyMode()) {
                JBMenuItem readOnlyMode = new JBMenuItem("Enable Read Only Mode");
                readOnlyMode.addActionListener(event -> {
                    ActiveCluster.getInstance().setReadOnlyMode(true);
                    SwingUtilities.invokeLater(() -> Messages.showWarningDialog("<html>The <strong>Read Only Mode</strong> is a simple guardrail in the plugin to avoid unwanted changes in sensible environments. Please note that this is a <strong>best effort</strong> approach. For true read-only approach, connect to the cluster using read-only credentials.</html>", "Couchbase Plugin Warning"));

                });
                settings.add(readOnlyMode);
            } else {
                JBMenuItem readWriteMode = new JBMenuItem("Enable Read/Write Mode");
                readWriteMode.addActionListener(event -> {
                    ActiveCluster.getInstance().setReadOnlyMode(false);
                });
                settings.add(readWriteMode);
            }

            if (ActiveCluster.getInstance().getColor() != null) {
                JBMenuItem clearConnectionColor = new JBMenuItem("Clear");
                clearConnectionColor.addActionListener(event -> {
                    toolBarPanel.setBorder(JBUI.Borders.empty());
                    toolBarPanel.revalidate();
                    ActiveCluster.getInstance().setColor(null);
                });
                colors.add(clearConnectionColor);
            }

            settings.add(colors);
            popup.add(tools);
            popup.add(settings);
        } else {
            JBMenuItem menuItem = new JBMenuItem("Connect");
            popup.add(menuItem);
            menuItem.addActionListener(e12 -> TreeActionHandler.connectToCluster(project, userObject.getSavedCluster(), tree, toolBarPanel));
        }

        popup.addSeparator();
        JBMenuItem menuItem = new JBMenuItem("Delete Connection");
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            TreeActionHandler.deleteConnection(clickedNode, userObject, tree);
        });


        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleBucketRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();
        JBMenuItem menuItem = new JBMenuItem("Refresh Scopes");
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            TreePath treePath = new TreePath(clickedNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            // Add "Add New Scope" option
            JBMenuItem addNewScopeItem = new JBMenuItem("Add New Scope");
            addNewScopeItem.addActionListener(e1 -> {
                String bucketName = ((BucketNodeDescriptor) clickedNode.getUserObject()).getText();

                NewEntityCreationDialog entityCreationDialog = new NewEntityCreationDialog(project, EntityType.SCOPE, bucketName);
                entityCreationDialog.show();

                if (entityCreationDialog.isOK()) {
                    String scopeName = entityCreationDialog.getEntityName();
                    ActiveCluster.getInstance().get().bucket(bucketName).collections().createScope(scopeName);
                    DataLoader.listScopes(clickedNode, tree);
                }
            });

            popup.add(addNewScopeItem);
        }
        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleScopeRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();
        ScopeNodeDescriptor scope = (ScopeNodeDescriptor) clickedNode.getUserObject();
        String bucketName = scope.getBucket();
        String scopeName = scope.getText();

        //can't delete the default scope


        JBMenuItem refreshCollections = new JBMenuItem("Refresh Collections");
        popup.add(refreshCollections);
        refreshCollections.addActionListener(e12 -> {
            TreePath treePath = new TreePath(clickedNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });
        popup.add(refreshCollections);

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            // Add "Add New Collection" option
            JBMenuItem addNewCollectionItem = new JBMenuItem("Add New Collection");
            addNewCollectionItem.addActionListener(e1 -> {

                NewEntityCreationDialog entityCreationDialog = new NewEntityCreationDialog(project, EntityType.COLLECTION, bucketName, scopeName);
                entityCreationDialog.show();

                if (entityCreationDialog.isOK()) {
                    String collectionName = entityCreationDialog.getEntityName();
                    ActiveCluster.getInstance().get().bucket(bucketName).collections().createCollection(CollectionSpec.create(collectionName, scopeName));
                    DataLoader.listCollections(clickedNode, tree);
                }
            });

            popup.add(addNewCollectionItem);


            if (!"_default".equals(scope.getText())) {
                popup.addSeparator();
                // Add "Delete Scope" option
                JBMenuItem deleteScopeItem = new JBMenuItem("Delete Scope");
                deleteScopeItem.addActionListener(e1 -> {
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
                });
                popup.add(deleteScopeItem);


            }
        }

        popup.addSeparator();

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            JBMenuItem simpleImport = new JBMenuItem("Simple Import");
            simpleImport.addActionListener(e1 -> {
                FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json");
                VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
                if (file != null) {
                    CBImport.simpleScopeImport(scope.getBucket(), scope.getText(), file.getPath(), project);
                } else {
                    Messages.showErrorDialog("Simple Import requires a .json file. Please try again.", "Simple Import Error");
                }
            });
            popup.add(simpleImport);
        }

        JBMenuItem simpleExport = new JBMenuItem("Simple Export");
        simpleExport.addActionListener(e1 -> {
            FileSaverDescriptor fsd = new FileSaverDescriptor("Simple Scope Export", "Choose where you want to save the file:");
            VirtualFileWrapper wrapper = FileChooserFactory.getInstance().createSaveFileDialog(fsd, project).save(("cb_export-" + scope.getText() + "-" + TimeUtils.getCurrentDateTime() + ".json"));
            if (wrapper != null) {
                File file = wrapper.getFile();
                CBExport.simpleScopeExport(scope.getBucket(), scope.getText(), file.getAbsolutePath());
            }
        });
        popup.add(simpleExport);


        popup.show(tree, e.getX(), e.getY());
    }


    private static void handleDocumentRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, FileNodeDescriptor col, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();
        JBMenuItem viewMetaData = new JBMenuItem("View Metadata");
        String bucket = col.getBucket();
        String scope = col.getScope();
        String collection = col.getCollection();
        String docId = col.getId();
        viewMetaData.addActionListener(e12 -> {
            String metadata = DataLoader.getDocMetadata(bucket, scope, collection, docId);
            if (metadata != null) {
                VirtualFile virtualFile = new LightVirtualFile("(read-only) " + docId + "_meta.json", FileTypeManager.getInstance().getFileTypeByExtension("json"), metadata);
                DocumentFormatter.formatFile(project, virtualFile);
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                fileEditorManager.openFile(virtualFile, true);
            }
        });
        popup.add(viewMetaData);
        popup.addSeparator();

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            JBMenuItem deleteDoc = new JBMenuItem("Delete Document");
            deleteDoc.addActionListener(e12 -> {
                int result = Messages.showYesNoDialog("<html>Are you sure you want to delete the document <strong>" + col.getId() + "</strong>?</html>", "Delete Document", Messages.getQuestionIcon());
                if (result != Messages.YES) {
                    return;
                }

                try {
                    ActiveCluster.getInstance().get().bucket(bucket).scope(scope).collection(collection).remove(col.getId());

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

            });
            popup.add(deleteDoc);
        }
        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleIndexRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, IndexNodeDescriptor idx, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();
        JBMenuItem viewIdxStats = new JBMenuItem("View Stats");
        viewIdxStats.addActionListener(l -> {
            IndexOverviewDialog dialog = new IndexOverviewDialog(idx.getBucket(), idx.getScope(), idx.getCollection(), idx.getText().substring(0, idx.getText().lastIndexOf('.')));
            dialog.show();
        });
        popup.add(viewIdxStats);
        popup.show(tree, e.getX(), e.getY());
    }


    private static void handleCollectionRightClick(Project project, MouseEvent e, DefaultMutableTreeNode clickedNode, CollectionNodeDescriptor col, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();

        JBMenuItem openDocument = new JBMenuItem("Open Document");
        openDocument.addActionListener(e12 -> {
            OpenDocumentDialog dialog = new OpenDocumentDialog(false, project, tree, col.getBucket(), col.getScope(), col.getText());
            dialog.show();
        });
        popup.add(openDocument);

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            JBMenuItem createDocument = new JBMenuItem("Create Document");
            createDocument.addActionListener(e12 -> {
                OpenDocumentDialog dialog = new OpenDocumentDialog(true, project, tree, col.getBucket(), col.getScope(), col.getText());
                dialog.show();
            });
            popup.add(createDocument);
        }
        popup.addSeparator();

        String filter = "Add Document Filter";
        boolean hasDeleteFilter = false;
        if (col.getQueryFilter() != null && !col.getQueryFilter().trim().isEmpty()) {
            filter = "Edit Document Filter";
            hasDeleteFilter = true;
        }
        JBMenuItem menuItem = new JBMenuItem(filter);
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            DocumentFilterDialog dialog = new DocumentFilterDialog(tree, clickedNode, col.getBucket(), col.getScope(), col.getText());
            dialog.show();
        });

        if (hasDeleteFilter) {
            JBMenuItem clearDocFilter = new JBMenuItem("Clear Document Filter");
            popup.add(clearDocFilter);
            clearDocFilter.addActionListener(e12 -> {
                QueryFiltersStorage.getInstance().getValue().saveQueryFilter(ActiveCluster.getInstance().getId(), col.getBucket(), col.getScope(), col.getText(), null);

                col.setQueryFilter(null);
                TreePath treePath = new TreePath(clickedNode.getPath());
                tree.collapsePath(treePath);
                tree.expandPath(treePath);
            });
        }

        if (!ActiveCluster.getInstance().isReadOnlyMode()) {
            popup.addSeparator();
            if (!"_default".equals(col.getText()) && !"_default".equals(col.getScope())) {
                // Add "Delete Collection" option
                JBMenuItem deleteCollectionItem = new JBMenuItem("Delete Collection");
                deleteCollectionItem.addActionListener(e1 -> {
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
                });
                popup.add(deleteCollectionItem);
            }
        }

        //cbexport and cbimport are installed together, so if one is available the other also is
        if (CBTools.getTool(CBTools.Type.CB_EXPORT).isAvailable()) {
            popup.addSeparator();

            if (!ActiveCluster.getInstance().isReadOnlyMode()) {
                JBMenuItem simpleImport = new JBMenuItem("Simple Import");
                simpleImport.addActionListener(e12 -> {
                    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json");
                    VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
                    if (file != null) {
                        CBImport.simpleCollectionImport(col.getBucket(), col.getScope(), col.getText(), file.getPath(), null);
                    } else {
                        Messages.showErrorDialog("Simple Import requires a .json file. Please try again.", "Simple Import Error");
                    }
                });
                popup.add(simpleImport);
            }

            JBMenuItem simpleExport = new JBMenuItem("Simple Export");
            simpleExport.addActionListener(e12 -> {
                FileSaverDescriptor fsd = new FileSaverDescriptor("Simple Collection Export", "Choose where you want to save the file:");
                VirtualFileWrapper wrapper = FileChooserFactory.getInstance().createSaveFileDialog(fsd, project).save(("cb_export-" + col.getScope() + "_" + col.getText() + "-" + TimeUtils.getCurrentDateTime() + ".json"));
                if (wrapper != null) {
                    File file = wrapper.getFile();
                    CBExport.simpleCollectionExport(col.getBucket(), col.getScope(), col.getText(), file.getAbsolutePath(), null);
                }
            });
            popup.add(simpleExport);
        }

        popup.show(tree, e.getX(), e.getY());
    }
}
