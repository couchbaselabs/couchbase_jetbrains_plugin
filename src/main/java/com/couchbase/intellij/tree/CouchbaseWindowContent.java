package com.couchbase.intellij.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.NotNull;

import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.intellij.DocumentFormatter;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.database.DataLoader;
import com.couchbase.intellij.persistence.SavedCluster;
import com.couchbase.intellij.persistence.storage.QueryFiltersStorage;
import com.couchbase.intellij.tree.docfilter.DocumentFilterDialog;
import com.couchbase.intellij.tree.examples.CardDialog;
import com.couchbase.intellij.tree.node.BucketNodeDescriptor;
import com.couchbase.intellij.tree.node.CollectionNodeDescriptor;
import com.couchbase.intellij.tree.node.CollectionsNodeDescriptor;
import com.couchbase.intellij.tree.node.ConnectionNodeDescriptor;
import com.couchbase.intellij.tree.node.LoadMoreNodeDescriptor;
import com.couchbase.intellij.tree.node.SchemaNodeDescriptor;
import com.couchbase.intellij.tree.node.ScopeNodeDescriptor;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.treeStructure.Tree;

public class CouchbaseWindowContent extends JPanel {

    private static DefaultTreeModel treeModel;
    private static Project project;

    public CouchbaseWindowContent(Project project) {
        CouchbaseWindowContent.project = project;
        setLayout(new BorderLayout());

        treeModel = getTreeModel(project);
        Tree tree = new Tree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new NodeDescriptorRenderer());

        // create the toolbar
        JPanel toolBarPanel = new JPanel(new BorderLayout());
        AnAction newWorkbench = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        Project project = e.getProject();

                        String fileName = "virtual.sqlpp";
                        VirtualFile virtualFile = new LightVirtualFile(fileName,
                                FileTypeManager.getInstance().getFileTypeByExtension("sqlpp"), "");
                        if (virtualFile != null) {
                            // Open the file in the editor
                            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                            fileEditorManager.openFile(virtualFile, true);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        };

        newWorkbench.getTemplatePresentation().setIcon(
                IconLoader.findIcon("./assets/icons/new_query.svg", CouchbaseWindowContent.class, false, true));
        newWorkbench.getTemplatePresentation().setDescription("Refresh");

        AnAction addConnectionAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Add connection action code here
                DatabaseConnectionDialog dialog = new DatabaseConnectionDialog(tree);
                dialog.show();
            }
        };
        addConnectionAction.getTemplatePresentation().setIcon(
                IconLoader.findIcon("./assets/icons/new_database.svg", CouchbaseWindowContent.class, false, true));
        addConnectionAction.getTemplatePresentation().setDescription("Add New Connection");

        AnAction importDataAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Import data action code here
            }
        };
        importDataAction.getTemplatePresentation().setIcon(
                IconLoader.findIcon("./assets/icons/database_import.svg", CouchbaseWindowContent.class, false, true));
        importDataAction.getTemplatePresentation().setDescription("Import Data");

        AnAction ellipsisAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Open menu code here
                JPopupMenu menu = new JPopupMenu();
                JMenuItem item1 = new JMenuItem("New Project from Template");
                JMenuItem item2 = new JMenuItem("Pillow Fight");
                menu.add(item1);
                menu.add(item2);

                item1.addActionListener(e1 -> {
                    CardDialog dialog = new CardDialog(project);
                    dialog.show();
                });

                item2.addActionListener(e2 -> {
                    PillowFightDialog dialog = new PillowFightDialog(project);
                    dialog.show();
                });

                Component component = e.getInputEvent().getComponent();
                menu.show(component, component.getWidth() / 2, component.getHeight() / 2);
            }
        };
        ellipsisAction.getTemplatePresentation().setIcon(IconLoader.findIcon("./assets/icons/ellipsis_horizontal.svg",
                CouchbaseWindowContent.class, false, true));
        ellipsisAction.getTemplatePresentation().setDescription("More Options");

        DefaultActionGroup leftActionGroup = new DefaultActionGroup();
        leftActionGroup.add(addConnectionAction);
        leftActionGroup.addSeparator();
        leftActionGroup.add(newWorkbench);
        leftActionGroup.addSeparator();
        leftActionGroup.add(importDataAction);

        DefaultActionGroup rightActionGroup = new DefaultActionGroup();
        rightActionGroup.add(ellipsisAction);

        ActionToolbar leftActionToolbar = ActionManager.getInstance().createActionToolbar("Explorer", leftActionGroup,
                true);
        leftActionToolbar.setTargetComponent(this);
        toolBarPanel.add(leftActionToolbar.getComponent(), BorderLayout.WEST);

        ActionToolbar rightActionToolbar = ActionManager.getInstance().createActionToolbar("MoreOptions",
                rightActionGroup, true);
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
                        DefaultMutableTreeNode clickedNode = (DefaultMutableTreeNode) clickedPath
                                .getLastPathComponent();
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
                            } else if (userObject instanceof CollectionsNodeDescriptor) {
                                handleCollectionsRightClick(e, clickedNode, tree);
                            } else if (userObject instanceof CollectionNodeDescriptor) {
                                handleCollectionRightClick(e, clickedNode, (CollectionNodeDescriptor) userObject, tree);
                            }
                        } else if (clickedNode.getUserObject() instanceof LoadMoreNodeDescriptor) {
                            LoadMoreNodeDescriptor loadMore = (LoadMoreNodeDescriptor) clickedNode.getUserObject();
                            DataLoader.listDocuments(project, (DefaultMutableTreeNode) clickedNode.getParent(), tree,
                                    loadMore.getNewOffset());
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
                            VirtualFile virtualFile = descriptor.getVirtualFile();
                            if (virtualFile != null) {

                                OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project, virtualFile);
                                FileEditorManager.getInstance(project).openEditor(fileDescriptor, true);
                                DocumentFormatter.formatFile(project, virtualFile);

                            } else {
                                System.err.println("virtual file is null");
                            }
                        }
                    } else {
                        // do nothing for now
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
                        // Do Nothing
                    } else if (expandedTreeNode.getUserObject() instanceof CollectionsNodeDescriptor) {
                        DataLoader.listCollections(expandedTreeNode, tree);
                    } else if (expandedTreeNode.getUserObject() instanceof CollectionNodeDescriptor) {
                        DataLoader.listDocuments(project, expandedTreeNode, tree, 0);
                    } else if (expandedTreeNode.getUserObject() instanceof SchemaNodeDescriptor) {
                        DataLoader.showSchema(expandedTreeNode, treeModel, tree);
                    } else if (expandedTreeNode.getUserObject() instanceof TooltipNodeDescriptor) {
                        // Do Nothing
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

    private static void handleConnectionRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode,
            ConnectionNodeDescriptor userObject, Tree tree) {
        JPopupMenu popup = new JPopupMenu();
        ConnectionNodeDescriptor con = userObject;

        if (con.isActive()) {
            JMenuItem refreshBuckets = new JMenuItem("Refresh Buckets");
            refreshBuckets.addActionListener(e12 -> {
                TreePath treePath = new TreePath(clickedNode.getPath());
                tree.collapsePath(treePath);
                tree.expandPath(treePath);
            });
            popup.add(refreshBuckets);
            popup.addSeparator();

            JMenuItem menuItem = new JMenuItem("Disconnect");
            popup.add(menuItem);
            menuItem.addActionListener(event -> {
                TreeActionHandler.disconnectFromCluster(clickedNode, con, tree);
            });

        } else {
            JMenuItem menuItem = new JMenuItem("Connect");
            popup.add(menuItem);
            menuItem.addActionListener(e12 -> {
                TreeActionHandler.connectToCluster(con.getSavedCluster(), tree);
            });
        }

        popup.addSeparator();
        JMenuItem menuItem = new JMenuItem("Delete Connection");
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            TreeActionHandler.deleteConnection(clickedNode, con, tree);
        });

        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleBucketRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        JPopupMenu popup = new JPopupMenu();
        popup.addSeparator();
        JMenuItem menuItem = new JMenuItem("Refresh Scopes");
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            TreePath treePath = new TreePath(clickedNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });

        // Add "Add New Scope" option
        JMenuItem addNewScopeItem = new JMenuItem("Add New Scope");
        addNewScopeItem.addActionListener(e1 -> {
            String bucketName = ((BucketNodeDescriptor) clickedNode.getUserObject()).getText();

            NewEntityCreationDialog entityCreationDialog = new NewEntityCreationDialog(project, EntityType.SCOPE,
                    bucketName);
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
        JPopupMenu popup = new JPopupMenu();
        popup.addSeparator();

        // Add "Delete Scope" option
        JMenuItem deleteScopeItem = new JMenuItem("Delete Scope");
        deleteScopeItem.addActionListener(e1 -> {
            ScopeNodeDescriptor scopeNodeDescriptor = (ScopeNodeDescriptor) clickedNode.getUserObject();
            // Show confirmation dialog before deleting scope
            int result = JOptionPane.showConfirmDialog(tree,
                    "Are you sure you want to delete the scope " + scopeNodeDescriptor.getText() + "?",
                    "Delete Scope", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                ActiveCluster.getInstance().get().bucket(scopeNodeDescriptor.getBucket()).collections()
                        .dropScope(scopeNodeDescriptor.getText());
            }

            // Refresh buckets
            DefaultMutableTreeNode bucketTreeNode = ((DefaultMutableTreeNode) clickedNode.getParent());
            TreePath treePath = new TreePath(bucketTreeNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });
        popup.add(deleteScopeItem);
        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleCollectionsRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode, Tree tree) {
        JPopupMenu popup = new JPopupMenu();
        popup.addSeparator();
        JMenuItem menuItem = new JMenuItem("Refresh Collections");
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            TreePath treePath = new TreePath(clickedNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });
        popup.addSeparator();

        // Add "Add New Collection" option
        JMenuItem addNewCollectionItem = new JMenuItem("Add New Collection");
        addNewCollectionItem.addActionListener(e1 -> {
            CollectionsNodeDescriptor cols = (CollectionsNodeDescriptor) clickedNode.getUserObject();

            String bucketName = cols.getBucket();
            String scopeName = cols.getScope();

            NewEntityCreationDialog entityCreationDialog = new NewEntityCreationDialog(project,
                    EntityType.COLLECTION, bucketName, scopeName);
            entityCreationDialog.show();

            if (entityCreationDialog.isOK()) {
                String collectionName = entityCreationDialog.getEntityName();
                ActiveCluster.getInstance().get().bucket(bucketName).collections()
                        .createCollection(CollectionSpec.create(collectionName, scopeName));
                DataLoader.listCollections(clickedNode, tree);
            }
        });

        popup.add(addNewCollectionItem);
        popup.show(tree, e.getX(), e.getY());
    }

    private static void handleCollectionRightClick(MouseEvent e, DefaultMutableTreeNode clickedNode,
            CollectionNodeDescriptor col, Tree tree) {
        JPopupMenu popup = new JPopupMenu();
        popup.addSeparator();
        String filter = "Add Document Filter";
        boolean hasDeleteFilter = false;
        if (col.getQueryFilter() != null && !col.getQueryFilter().trim().isEmpty()) {
            filter = "Edit Document Filter";
            hasDeleteFilter = true;
        }
        JMenuItem menuItem = new JMenuItem(filter);
        popup.add(menuItem);
        menuItem.addActionListener(e12 -> {
            DocumentFilterDialog dialog = new DocumentFilterDialog(tree, clickedNode, col.getBucket(), col.getScope(),
                    col.getText());
            dialog.show();
        });

        if (hasDeleteFilter) {
            JMenuItem clearDocFilter = new JMenuItem("Clear Document Filter");
            popup.add(clearDocFilter);
            clearDocFilter.addActionListener(e12 -> {
                QueryFiltersStorage.getInstance().getValue()
                        .saveQueryFilter(
                                ActiveCluster.getInstance().getId(),
                                col.getBucket(),
                                col.getScope(),
                                col.getText(),
                                null);

                col.setQueryFilter(null);
                TreePath treePath = new TreePath(clickedNode.getPath());
                tree.collapsePath(treePath);
                tree.expandPath(treePath);
            });
        }

        // Add "Delete Collection" option
        JMenuItem deleteCollectionItem = new JMenuItem("Delete Collection");
        deleteCollectionItem.addActionListener(e1 -> {
            int result = JOptionPane.showConfirmDialog(tree,
                    "Are you sure you want to delete the collection " + col.getText() + "?",
                    "Delete Collection", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                ActiveCluster.getInstance().get().bucket(col.getBucket()).collections()
                        .dropCollection(CollectionSpec.create(col.getText(), col.getScope()));
            }

            // Refresh collections
            DefaultMutableTreeNode colsTreeNode = ((DefaultMutableTreeNode) clickedNode
                    .getParent());
            TreePath treePath = new TreePath(colsTreeNode.getPath());
            tree.collapsePath(treePath);
            tree.expandPath(treePath);
        });
        popup.add(deleteCollectionItem);

        popup.show(tree, e.getX(), e.getY());
    }

    class NodeDescriptorRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
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
                    if (descriptor.getQueryFilter() == null
                            || descriptor.getQueryFilter().trim().isEmpty()) {
                        setIcon(descriptor.getIcon());
                    } else {
                        setIcon(IconLoader.findIcon("./assets/icons/filter.svg"));
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

    public static DefaultTreeModel getTreeModel(Project project) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        Map<String, SavedCluster> sortedClusters = new TreeMap<>(DataLoader.getSavedClusters());
        for (Map.Entry<String, SavedCluster> entry : sortedClusters.entrySet()) {
            DefaultMutableTreeNode adminLocal = new DefaultMutableTreeNode(
                    new ConnectionNodeDescriptor(entry.getKey(), entry.getValue(), false));
            root.add(adminLocal);
        }
        return new DefaultTreeModel(root);
    }

}