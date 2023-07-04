package com.couchbase.intellij.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.database.DataLoader;
import com.couchbase.intellij.persistence.SavedCluster;
import com.couchbase.intellij.tree.node.CollectionNodeDescriptor;
import com.couchbase.intellij.tree.node.ConnectionNodeDescriptor;
import com.couchbase.intellij.tree.node.FileNodeDescriptor;
import com.couchbase.intellij.tree.node.IndexNodeDescriptor;
import com.couchbase.intellij.tree.node.LoadMoreNodeDescriptor;
import com.couchbase.intellij.tree.node.MissingIndexNodeDescriptor;
import com.couchbase.intellij.tree.node.NodeDescriptor;
import com.couchbase.intellij.tree.node.TooltipNodeDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

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
        toolBarPanel = TreeToolBarBuilder.build(project, tree);
        add(toolBarPanel, BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);

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
                            TreeRightClickListener.handle(tree, project, toolBarPanel, e, clickedNode);
                        } else if (clickedNode.getUserObject() instanceof LoadMoreNodeDescriptor) {
                            LoadMoreNodeDescriptor loadMore = (LoadMoreNodeDescriptor) clickedNode.getUserObject();
                            DataLoader.listDocuments((DefaultMutableTreeNode) clickedNode.getParent(), tree,
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

                            // always force to load the file from server on read only mode.
                            if (ActiveCluster.getInstance().isReadOnlyMode()) {
                                descriptor.setVirtualFile(null);
                            }

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

                            if (ActiveCluster.getInstance().isReadOnlyMode()) {
                                Messages.showErrorDialog(
                                        "You can't create indexes when your connection is on read-only mode",
                                        "Couchbase Plugin Error");
                            } else {
                                MissingIndexNodeDescriptor node = (MissingIndexNodeDescriptor) userObject;
                                int result = Messages.showYesNoDialog(
                                        "<html>Are you sure that you would like to create a primary index on <strong>"
                                                + node.getBucket() + "." + node.getScope() + "." + node.getCollection()
                                                + "</strong>?<br><br>"
                                                + "<small>We don't recommend primary indexes in production environments.</small><br>"
                                                + "<small>This operation might take a while.</small></html>",
                                        "Create New Index", Messages.getQuestionIcon());
                                if (result == Messages.YES) {
                                    DataLoader.createPrimaryIndex(node.getBucket(), node.getScope(),
                                            node.getCollection());
                                    tree.collapsePath(clickedPath.getParentPath());
                                }
                            }
                        }
                    }

                }
            }
        });

        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreeExpandListener.handle(tree, event);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                // No action needed
            }
        });

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

    public Icon combine(ImageIcon icon1, ImageIcon icon2) {
        // Create a new image that's the sum of both icon widths and the height of the
        // taller one
        int w = icon1.getIconWidth() + icon2.getIconWidth();
        int h = Math.max(icon1.getIconHeight(), icon2.getIconHeight());
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // Paint both icons onto this image
        Graphics2D g = image.createGraphics();
        g.drawImage(icon1.getImage(), 0, 0, null);
        g.drawImage(icon2.getImage(), icon1.getIconWidth(), 0, null);
        g.dispose();

        return new ImageIcon(image);
    }

    static class NodeDescriptorRenderer extends DefaultTreeCellRenderer {
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
                    if (descriptor.getQueryFilter() == null || descriptor.getQueryFilter().trim().isEmpty()) {
                        setIcon(descriptor.getIcon());
                    } else {
                        setIcon(IconLoader.getIcon("/assets/icons/filter.svg", CouchbaseWindowContent.class));
                    }
                } else if (userObject instanceof ConnectionNodeDescriptor) {
                    ConnectionNodeDescriptor descriptor = (ConnectionNodeDescriptor) userObject;
                    if (descriptor.getText().equals(ActiveCluster.getInstance().getId())
                            && ActiveCluster.getInstance().isReadOnlyMode()) {
                        return new IconPanel(descriptor.getIcon(),
                                IconLoader.getIcon("/assets/icons/eye.svg", CouchbaseWindowContent.class),
                                new JLabel(descriptor.getText()));

                    } else {
                        setIcon(descriptor.getIcon());
                        setText(descriptor.getText());
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

    static class IconPanel extends JPanel {

        public IconPanel(Icon icon1, Icon icon2, JLabel text) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 2));
            add(new JLabel(icon1));
            JLabel lbl2 = new JLabel(icon2);
            lbl2.setToolTipText("Cluster is on read-only mode");
            lbl2.setBorder(JBUI.Borders.empty(0, 3));
            add(lbl2);
            add(text);
        }
    }
}