package com.couchbase.intellij.tree;

import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.eventing.FunctionDeploymentDialog;
import com.couchbase.intellij.eventing.FunctionDeploymentSettings;
import com.couchbase.intellij.tools.CBShell;
import com.couchbase.intellij.tree.examples.CardDialog;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class TreeToolBarBuilder {

    private static int workbenchCounter = 0;

    public static JPanel build(Project project, Tree tree) {
        JPanel toolBarPanel = new JPanel(new BorderLayout());

        AnAction newWorkbench = new AnAction("New Query Workbench") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        Project project = e.getProject();
                        workbenchCounter++;
                        String fileName = "workbench" + workbenchCounter + ".sqlpp";
                        VirtualFile virtualFile = new LightVirtualFile(fileName,
                                FileTypeManager.getInstance().getFileTypeByExtension("sqlpp"), "");
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

        newWorkbench.getTemplatePresentation()
                .setIcon(IconLoader.getIcon("/assets/icons/new_query.svg", CouchbaseWindowContent.class));

        AnAction addConnectionAction = new AnAction("Add New Connection") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Add connection action code here
                NewConnectionDialog dialog = new NewConnectionDialog(project, tree);
                dialog.show();
            }
        };
        addConnectionAction.getTemplatePresentation()
                .setIcon(IconLoader.getIcon("/assets/icons/new_database.svg", CouchbaseWindowContent.class));

        AnAction cbshellAction = new AnAction("Open New CB Shell") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

                if (ActiveCluster.getInstance().get() != null) {
                    CBShell.openNewTerminal();
                } else {
                    Messages.showErrorDialog("You need to connecto to a cluster first before running CB Shell",
                            "Couchbase Plugin Error");
                }
            }
        };
        cbshellAction.getTemplatePresentation()
                .setIcon(IconLoader.getIcon("/assets/icons/cbshell.svg", CouchbaseWindowContent.class));

        AnAction ellipsisAction = new AnAction("More Options") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // Open menu code here
                JBPopupMenu menu = new JBPopupMenu();
                JBMenuItem item1 = new JBMenuItem("New Project from Template");
                JBMenuItem item2 = new JBMenuItem("Deploy Function");
                JBMenuItem item3 = new JBMenuItem("Function Settings");

                menu.add(item1);
                menu.add(item2);
                menu.add(item3);

                item1.addActionListener(e1 -> {
                    CardDialog dialog = new CardDialog(project);
                    dialog.show();
                });

                item2.addActionListener(e2 -> {
                    FunctionDeploymentDialog dialog = new FunctionDeploymentDialog(project);
                    dialog.show();
                });

                item3.addActionListener(e3 -> {
                    FunctionDeploymentSettings settings = new FunctionDeploymentSettings();
                    settings.setVisible(true);
                });

                Component component = e.getInputEvent().getComponent();
                menu.show(component, component.getWidth() / 2, component.getHeight() / 2);
            }
        };
        ellipsisAction.getTemplatePresentation()
                .setIcon(IconLoader.getIcon("/assets/icons/ellipsis_horizontal.svg", CouchbaseWindowContent.class));
        ellipsisAction.getTemplatePresentation().setDescription("More options");

        DefaultActionGroup leftActionGroup = new DefaultActionGroup();
        leftActionGroup.add(addConnectionAction);
        leftActionGroup.addSeparator();
        leftActionGroup.add(newWorkbench);
        leftActionGroup.addSeparator();

        // Disabling CBSHELL for now
        // if (OSUtil.isMacOS()) {
        // leftActionGroup.add(cbshellAction);
        // }

        DefaultActionGroup rightActionGroup = new DefaultActionGroup();
        rightActionGroup.add(ellipsisAction);

        ActionToolbar leftActionToolbar = ActionManager.getInstance().createActionToolbar("Explorer", leftActionGroup,
                true);
        leftActionToolbar.setTargetComponent(toolBarPanel);
        toolBarPanel.add(leftActionToolbar.getComponent(), BorderLayout.WEST);

        ActionToolbar rightActionToolbar = ActionManager.getInstance().createActionToolbar("MoreOptions",
                rightActionGroup, true);
        rightActionToolbar.setTargetComponent(toolBarPanel);
        toolBarPanel.add(rightActionToolbar.getComponent(), BorderLayout.EAST);

        return toolBarPanel;
    }
}
