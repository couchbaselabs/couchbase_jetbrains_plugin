package com.couchbase.intellij.tree;

import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.intellij.DocumentFormatter;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.database.DataLoader;
import com.couchbase.intellij.persistence.SavedCluster;
import com.couchbase.intellij.persistence.storage.QueryFiltersStorage;
import com.couchbase.intellij.tools.CBExport;
import com.couchbase.intellij.tools.CBImport;
import com.couchbase.intellij.tools.CBShell;
import com.couchbase.intellij.tools.CBTools;
import com.couchbase.intellij.tree.docfilter.DocumentFilterDialog;
import com.couchbase.intellij.tree.examples.CardDialog;
import com.couchbase.intellij.tree.node.*;
import com.couchbase.intellij.tree.overview.IndexOverviewDialog;
import com.couchbase.intellij.tree.overview.ServerOverviewDialog;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColorChooser;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import utils.OSUtil;
import utils.TimeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class CouchbaseWindowContent extends JPanel {

    private static DefaultTreeModel treeModel;
    private static Project project;

    private static JPanel toolBarPanel;

    public CouchbaseWindowContent(Project project) {
        CouchbaseWindowContent.project = project;
        setLayout(new BorderLayout());
        treeModel = getTreeModel(project);
        Tree tree = new Tree(treeModel);

        tree.setRootVisible(false);
        tree.setCellRenderer(new NodeDescriptorRenderer());


        // create the toolbar
        toolBarPanel = new JPanel(new BorderLayout());

        AnAction newWorkbench = new AnAction("New Query Workbench") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        Project project = e.getProject();

                        String fileName = "virtual.sqlpp";
                        VirtualFile virtualFile = new LightVirtualFile(fileName, FileTypeManager.getInstance().getFileTypeByExtension("sqlpp"), "");
                        // Open the file in the editor
                        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                        fileEditorManager.openFile(virtualFile, true);
                    } catch (Exception ex) {
                        Log.error(ex);
                        ex.printStackTrace();
                    }
                });
            }
        };

        newWorkbench.getTemplatePresentation().setIcon(IconLoader.getIcon("/assets/icons/new_query.svg", CouchbaseWindowContent.class));

        AnAction addConnectionAction = new AnAction("Add New Connection") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Add connection action code here
                DatabaseConnectionDialog dialog = new DatabaseConnectionDialog(project, tree);
                dialog.show();
            }
        };
        addConnectionAction.getTemplatePresentation().setIcon(IconLoader.getIcon("/assets/icons/new_database.svg", CouchbaseWindowContent.class));

        AnAction cbshellAction = new AnAction("Open New CB Shell") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                if (ActiveCluster.getInstance().get() != null) {
                    CBShell.openNewTerminal();
                } else {
                    Messages.showErrorDialog("You need to connecto to a cluster first before running CB Shell", "Couchbase Plugin Error");
                }
            }
        };
        cbshellAction.getTemplatePresentation().setIcon(IconLoader.getIcon("/assets/icons/cbshell.svg", CouchbaseWindowContent.class));

        AnAction ellipsisAction = new AnAction("More Options") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Open menu code here
                JBPopupMenu menu = new JBPopupMenu();
                JBMenuItem item1 = new JBMenuItem("New Project from Template");
                menu.add(item1);

                item1.addActionListener(e1 -> {
                    CardDialog dialog = new CardDialog(project);
                    dialog.show();
                });


                Component component = e.getInputEvent().getComponent();
                menu.show(component, component.getWidth() / 2, component.getHeight() / 2);
            }
        };
        ellipsisAction.getTemplatePresentation().setIcon(IconLoader.getIcon("/assets/icons/ellipsis_horizontal.svg", CouchbaseWindowContent.class));
        ellipsisAction.getTemplatePresentation().setDescription("More options");

        DefaultActionGroup leftActionGroup = new DefaultActionGroup();
        leftActionGroup.add(addConnectionAction);
        leftActionGroup.addSeparator();
        leftActionGroup.add(newWorkbench);
        leftActionGroup.addSeparator();

        if (OSUtil.isMacOS()) {
            leftActionGroup.add(cbshellAction);
        }

        DefaultActionGroup rightActionGroup = new DefaultActionGroup();
        rightActionGroup.add(ellipsisAction);

        ActionToolbar leftActionToolbar = ActionManager.getInstance().createActionToolbar("Explorer", leftActionGroup, true);
        leftActionToolbar.setTargetComponent(this);
        toolBarPanel.add(leftActionToolbar.getComponent(), BorderLayout.WEST);

        ActionToolbar rightActionToolbar = ActionManager.getInstance().createActionToolbar("MoreOptions", rightActionGroup, true);
        rightActionToolbar.setTargetComponent(this);
        toolBarPanel.add(rightActionToolbar.getComponent(), BorderLayout.EAST);

        add(toolBarPanel, BorderLayout.NORTH);

        tree.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    mouseClicked(e);
                } else {
                    TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
                    if (clickedPath != null) {
                        DefaultMutableTreeNode clickedNode = (DefaultMutableTreeNode) clickedPath.getLastPathComponent();
                        if (SwingUtilities.isRightMouseButton(e)) {
                            Object userObject = clickedNode.getUserObject();
                            int row = tree.getClosestRowForLocation(e.getX(), e.getY());
                            tree.setSelectionRow(row);

                            if (userObject instanceof ConnectionNodeDescriptor) {
                                handleConnectionRightClick(e, clickedNode, (ConnectionNodeDescriptor) userObject, tree);
                            } else if (userObject instanceof BucketNodeDescriptor) {
                                handleBucketRightClick(e, clickedNode, tree);
                            } else if (userObject instanceof ScopeNodeDescriptor) {
                                handleScopeRightClick(e, clickedNode, tree);
                            } else if (userObject instanceof CollectionNodeDescriptor) {
                                handleCollectionRightClick(e, clickedNode, (CollectionNodeDescriptor) userObject, tree);
                            } else if (userObject instanceof FileNodeDescriptor) {
                                handleDocumentRightClick(e, clickedNode, (FileNodeDescriptor) userObject, tree);
                            } else if (userObject instanceof IndexNodeDescriptor) {
                                handleIndexRightClick(e, clickedNode, (IndexNodeDescriptor) userObject, tree);
                            }
                        } else if (clickedNode.getUserObject() instanceof LoadMoreNodeDescriptor) {
                            LoadMoreNodeDescriptor loadMore = (LoadMoreNodeDescriptor) clickedNode.getUserObject();
                            DataLoader.listDocuments((DefaultMutableTreeNode) clickedNode.getParent(), tree, loadMore.getNewOffset());
                        }
                    }
                }
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
                if (clickedPath != null) {
                    DefaultMutableTreeNode clickedNode = (DefaultMutableTreeNode) clickedPath.getLastPathComponent();
                    Object userObject = clickedNode.getUserObject();
                    if (e.getClickCount() == 2) {
                        if (userObject instanceof FileNodeDescriptor) {
                            FileNodeDescriptor descriptor = (FileNodeDescriptor) userObject;
                            DataLoader.loadDocument(project, descriptor, tree, false);
                            VirtualFile virtualFile = descriptor.getVirtualFile();
                            if (virtualFile != null) {
                                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                                fileEditorManager.openFile(virtualFile, true);


                            } else {
                                System.err.println("virtual file is null");
                            }
                        } else if (userObject instanceof IndexNodeDescriptor) {
                            IndexNodeDescriptor descriptor = (IndexNodeDescriptor) userObject;
                            VirtualFile virtualFile = descriptor.getVirtualFile();
                            if (virtualFile != null) {
                                OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project, virtualFile);
                                FileEditorManager.getInstance(project).openEditor(fileDescriptor, true);


                            } else {
                                System.err.println("virtual file is null");
                            }
                        } else if (userObject instanceof MissingIndexNodeDescriptor) {
                            MissingIndexNodeDescriptor node = (MissingIndexNodeDescriptor) userObject;
                            int result = Messages.showYesNoDialog("<html>Are you sure that you would like to create a primary index on <strong>" + node.getBucket() + "." + node.getScope() + "." + node.getCollection() + "</strong>?<br><br>" + "<small>We don't recommend primary indexes in production environments.</small><br>" + "<small>This operation might take a while.</small></html>", "Create New Index", Messages.getQuestionIcon());
                            if (result == Messages.YES) {
                                DataLoader.createPrimaryIndex(node.getBucket(), node.getScope(), node.getCollection());
                                tree.collapsePath(clickedPath.getParentPath());
                            }
                        }
                    }

                }
            }
        });

        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                Object expandedNode = event.getPath().getLastPathComponent();
                if (expandedNode instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode expandedTreeNode = (DefaultMutableTreeNode) expandedNode;

                    if (expandedTreeNode.getUserObject() instanceof ConnectionNodeDescriptor) {
                        DataLoader.listBuckets(expandedTreeNode, tree);
                    } else if (expandedTreeNode.getUserObject() instanceof BucketNodeDescriptor) {
                        DataLoader.listScopes(expandedTreeNode, tree);
                    } else if (expandedTreeNode.getUserObject() instanceof ScopeNodeDescriptor) {
                        DataLoader.listCollections(expandedTreeNode, tree);
                    } else if (expandedTreeNode.getUserObject() instanceof CollectionNodeDescriptor) {
                        DataLoader.listDocuments(expandedTreeNode, tree, 0);
                    } else if (expandedTreeNode.getUserObject() instanceof SchemaNodeDescriptor) {
                        DataLoader.showSchema(expandedTreeNode, treeModel, tree);
                    } else if (expandedTreeNode.getUserObject() instanceof TooltipNodeDescriptor) {
                        // Do Nothing
                    } else if (expandedTreeNode.getUserObject() instanceof IndexesNodeDescriptor) {
                        DataLoader.listIndexes(expandedTreeNode, tree);
                    } else {
                        throw new UnsupportedOperationException("Not implemented yet");
                    }
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                // No action needed
            }
        });
        // tree.setShowsRootHandles(true);

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    private static void handleConnectionRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, ConnectionNodeDescriptor userObject, Tree tree) {
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

    private static void handleBucketRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();
        JBMenuItem menuItem = new JBMenuItem("Refresh Scopes");
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            TreePath treePath = new TreePath(clickedNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });

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
        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleScopeRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();
        popup.addSeparator();
        ScopeNodeDescriptor scope = (ScopeNodeDescriptor) clickedNode.getUserObject();
        String bucketName = scope.getBucket();
        String scopeName = scope.getText();

        //can't delete the default scope
        if (!"_default".equals(scope.getText())) {
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
            popup.addSeparator();

        }

        JBMenuItem refreshCollections = new JBMenuItem("Refresh Collections");
        popup.add(refreshCollections);
        refreshCollections.addActionListener(e12 -> {
            TreePath treePath = new TreePath(clickedNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });
        popup.add(refreshCollections);

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

        popup.addSeparator();

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


    private static void handleDocumentRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, FileNodeDescriptor col, Tree tree) {
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
                    treeModel.removeNodeFromParent(clickedNode);
                }
                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(parentNode);
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.error("An error occurred while trying to delete the document " + col.getId(), ex);
                Messages.showErrorDialog("Could not delete the document. Please check the logs for more.", "Couchbase Plugin Error");
            }

        });
        popup.add(deleteDoc);
        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleIndexRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, IndexNodeDescriptor idx, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();
        JBMenuItem viewIdxStats = new JBMenuItem("View Stats");
        viewIdxStats.addActionListener(l -> {
            IndexOverviewDialog dialog = new IndexOverviewDialog(idx.getBucket(), idx.getScope(), idx.getCollection(), idx.getText().substring(0, idx.getText().lastIndexOf('.')));
            dialog.show();
        });
        popup.add(viewIdxStats);
        popup.show(tree, e.getX(), e.getY());
    }


    private static void handleCollectionRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, CollectionNodeDescriptor col, Tree tree) {
        JBPopupMenu popup = new JBPopupMenu();

        JBMenuItem openDocument = new JBMenuItem("Open/Create Document");
        openDocument.addActionListener(e12 -> {
            OpenDocumentDialog dialog = new OpenDocumentDialog(project, tree, col.getBucket(), col.getScope(), col.getText());
            dialog.show();
        });
        popup.add(openDocument);

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

        //cbexport and cbimport are installed together, so if one is available the other also is
        if (CBTools.getCbExport().isAvailable()) {
            popup.addSeparator();
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

    public static DefaultTreeModel getTreeModel(Project project) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        Map<String, SavedCluster> sortedClusters = new TreeMap<>(DataLoader.getSavedClusters());
        for (Map.Entry<String, SavedCluster> entry : sortedClusters.entrySet()) {
            DefaultMutableTreeNode adminLocal = new DefaultMutableTreeNode(new ConnectionNodeDescriptor(entry.getKey(), entry.getValue(), false));
            root.add(adminLocal);
        }
        return new DefaultTreeModel(root);
    }

    static class NodeDescriptorRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof TooltipNodeDescriptor) {
                    TooltipNodeDescriptor descriptor = (TooltipNodeDescriptor) userObject;
                    setText(descriptor.getText());
                    setIcon(descriptor.getIcon());
                    if (descriptor.getTooltip() != null) {
                        setToolTipText(descriptor.getTooltip());
                    }
                } else if (userObject instanceof CollectionNodeDescriptor) {
                    CollectionNodeDescriptor descriptor = (CollectionNodeDescriptor) userObject;
                    setText(descriptor.getText());
                    if (descriptor.getQueryFilter() == null || descriptor.getQueryFilter().trim().isEmpty()) {
                        setIcon(descriptor.getIcon());
                    } else {
                        setIcon(IconLoader.getIcon("/assets/icons/filter.svg", CouchbaseWindowContent.class));
                    }
                } else if (userObject instanceof NodeDescriptor) {
                    NodeDescriptor descriptor = (NodeDescriptor) userObject;
                    setIcon(descriptor.getIcon());
                    setText(descriptor.getText());
                }
            }
            return this;
        }
    }

}