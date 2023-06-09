package com.couchbase.intellij.workbench;


import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.intellij.VirtualFileKeys;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.persistence.SavedCluster;
import com.couchbase.intellij.persistence.storage.ClustersStorage;
import com.couchbase.intellij.persistence.storage.QueryHistoryStorage;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.intellij.sdk.language.SQLPPFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.couchbase.intellij.workbench.QueryExecutor.QueryType.*;

public class CustomSqlFileEditor implements FileEditor {
    public static final String NO_QUERY_CONTEXT_SELECTED = "No Query Context Selected";
    private final EditorWrapper queryEditor;
    private final VirtualFile file;
    private final Project project;
    JPanel panel;
    private JLabel historyLabel;
    private JComponent component;
    private int currentHistoryIndex;
    private String selectedBucketContext;
    private String selectedScopeContext;
    private String cachedPreviousSelectedConnection;
    private JPanel topPanel;

    CustomSqlFileEditor(Project project, VirtualFile file) {
        this.file = file;
        this.project = project;
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument(LoadTextUtil.loadText(file));

        boolean isViewer = false;
        if ("true".equals(file.getUserData(VirtualFileKeys.READ_ONLY))) {
            isViewer = true;
            this.queryEditor = new EditorWrapper(EditorFactory.getInstance().createEditor(document, project, file, isViewer), null);
        } else {
            this.queryEditor = new EditorWrapper(null, (TextEditor) TextEditorProvider.getInstance().createEditor(project, file)); //Edit
        }

        this.panel = new JPanel(new BorderLayout());
        init(isViewer);
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        return file;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return this.component;
    }

    public void init(boolean isViewer) {

        if (!isViewer) {
            buildToolbar();
        }
        panel.add(queryEditor.getComponent(), BorderLayout.CENTER);
        queryEditor.getContentComponent().requestFocusInWindow();
        component = panel;
    }

    private void buildToolbar() {
        DefaultActionGroup executeGroup = new DefaultActionGroup();

        Icon executeIcon = IconLoader.getIcon("/assets/icons/play.svg", CustomSqlFileEditor.class);
        executeGroup.add(new AnAction("Execute", "Execute the query statement in the editor", executeIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                String editorText = queryEditor.getDocument().getText();
                SelectionModel selectionModel = queryEditor.textEditor.getEditor().getSelectionModel();

                if (selectionModel.hasSelection()) {
                    editorText = selectionModel.getSelectedText();
                }

                if (QueryExecutor.executeQuery(NORMAL, editorText, selectedBucketContext, selectedScopeContext,
                        currentHistoryIndex, project)) {
                    int historySize = QueryHistoryStorage.getInstance().getValue().getHistory().size();
                    currentHistoryIndex = historySize - 1;
                    SwingUtilities.invokeLater(() -> {
                        historyLabel.setText("history (" + historySize + "/" + historySize + ")");
                        historyLabel.revalidate();
                    });
                }
            }
        });

        executeGroup.addSeparator();

        Icon adviseIcon = IconLoader.getIcon("/assets/icons/advise.svg", CustomSqlFileEditor.class);
        executeGroup.add(new AnAction("Advise", "Get index recommendations about your query", adviseIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                String editorText = queryEditor.getDocument().getText();
                SelectionModel selectionModel = queryEditor.textEditor.getEditor().getSelectionModel();

                if (selectionModel.hasSelection()) {
                    editorText = selectionModel.getSelectedText();
                }

                QueryExecutor.executeQuery(ADVISE, editorText, selectedBucketContext, selectedScopeContext,
                        -1, project);
            }
        });

        Icon explainIcon = IconLoader.getIcon("/assets/icons/explain.svg", CustomSqlFileEditor.class);
        executeGroup.add(new AnAction("Explain", "Explains the query phases", explainIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                String editorText = queryEditor.getDocument().getText();
                SelectionModel selectionModel = queryEditor.textEditor.getEditor().getSelectionModel();

                if (selectionModel.hasSelection()) {
                    editorText = selectionModel.getSelectedText();
                }

                QueryExecutor.executeQuery(EXPLAIN, editorText, selectedBucketContext, selectedScopeContext,
                        -1, project);
            }
        });


        executeGroup.addSeparator();


        Icon favoriteList = IconLoader.getIcon("/assets/icons/favorites-list.svg", CustomSqlFileEditor.class);
        executeGroup.add(new AnAction("Favorite List", "List of favorite queries", favoriteList) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                FavoriteQueryDialog dialog = new FavoriteQueryDialog(queryEditor.getDocument());
                dialog.show();
            }
        });

        executeGroup.addSeparator();
        Icon formatCode = IconLoader.getIcon("/assets/icons/format.svg", CustomSqlFileEditor.class);
        executeGroup.add(new AnAction("Format Code", "Formats a SQL++ code", formatCode) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().runWriteAction(() -> queryEditor.getDocument().setText(SQLPPFormatter.format(queryEditor.getDocument().getText())));
            }
        });
        executeGroup.addSeparator();

        ActionToolbar executeToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, executeGroup, true);
        executeToolbar.setTargetComponent(queryEditor.getComponent());

        Icon leftIcon = IconLoader.getIcon("/assets/icons/chevron-left.svg", CustomSqlFileEditor.class);
        Icon rightIcon = IconLoader.getIcon("/assets/icons/chevron-right.svg", CustomSqlFileEditor.class);

        DefaultActionGroup prevActionGroup = new DefaultActionGroup();
        prevActionGroup.add(new AnAction("Previous History", "Previous history", leftIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (currentHistoryIndex - 1 >= 0) {
                    currentHistoryIndex--;
                    ApplicationManager.getApplication().runWriteAction(() -> queryEditor.getDocument().setText(QueryHistoryStorage.getInstance().getValue().getHistory().get(currentHistoryIndex)));


                    SwingUtilities.invokeLater(() -> {
                        historyLabel.setText("history (" + (currentHistoryIndex + 1) + "/" + QueryHistoryStorage.getInstance().getValue().getHistory().size() + ")");
                        historyLabel.revalidate();
                    });

                }
            }
        });

        DefaultActionGroup nextActionGroup = new DefaultActionGroup();
        nextActionGroup.add(new AnAction("Next History", "Next history", rightIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (currentHistoryIndex + 1 < QueryHistoryStorage.getInstance().getValue().getHistory().size()) {
                    currentHistoryIndex++;

                    ApplicationManager.getApplication().runWriteAction(() -> queryEditor.getDocument()
                            .setText(QueryHistoryStorage.getInstance()
                                    .getValue().getHistory().get(currentHistoryIndex)));

                    SwingUtilities.invokeLater(() -> {
                        historyLabel.setText("history (" + (currentHistoryIndex + 1) + "/" + QueryHistoryStorage.getInstance().getValue().getHistory().size() + ")");
                        historyLabel.revalidate();
                    });
                }
            }
        });

        ActionToolbar prevToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, prevActionGroup, true);
        prevToolbar.setTargetComponent(panel);
        ActionToolbar nextToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, nextActionGroup, true);
        nextToolbar.setTargetComponent(panel);

        int historySize = QueryHistoryStorage.getInstance().getValue().getHistory().size();
        currentHistoryIndex = Math.max((historySize - 1), 0) + 1;
        historyLabel = new JLabel("history (" + (historySize + 1) + "/" + historySize + ")");
        historyLabel.setFont(historyLabel.getFont().deriveFont(10.0f));
        historyLabel.setBorder(JBUI.Borders.emptyRight(12));

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(prevToolbar.getComponent(), BorderLayout.WEST);
        historyPanel.add(historyLabel, BorderLayout.CENTER);
        historyPanel.add(nextToolbar.getComponent(), BorderLayout.EAST);

        JPanel favorite = new JPanel(new BorderLayout());
        DefaultActionGroup favoriteActionGroup = new DefaultActionGroup();
        ActionToolbar favToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, favoriteActionGroup, true);
        favToolbar.setTargetComponent(panel);

        favoriteActionGroup.add(new AnAction("Favorite Query", "Favorite query", IconLoader.getIcon("/assets/icons/star-empty.svg", CustomSqlFileEditor.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                NewFavoriteCatalog dialog = new NewFavoriteCatalog(queryEditor.getDocument(), this, favoriteActionGroup, favToolbar);
                dialog.show();
            }
        });
        favorite.add(favToolbar.getComponent(), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(historyPanel, BorderLayout.CENTER);
        rightPanel.add(favorite, BorderLayout.EAST);


        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(executeToolbar.getComponent(), BorderLayout.WEST);
        leftPanel.add(getQueryContextPanel(), BorderLayout.CENTER);

        topPanel = new JPanel(new BorderLayout());
        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);

        if (ActiveCluster.getInstance().getColor() != null) {
            Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, ActiveCluster.getInstance().getColor());
            Border margin = BorderFactory.createEmptyBorder(0, 0, 1, 0);
            Border compound = BorderFactory.createCompoundBorder(margin, line);
            topPanel.setBorder(compound);
        }

        panel.add(topPanel, BorderLayout.NORTH);
    }


    private JPanel getQueryContextPanel() {
        JPanel contextPanel = new JPanel(new FlowLayout());
        JLabel conLabel = new JLabel("Connection:");
        conLabel.setFont(conLabel.getFont().deriveFont(10.0f));
        contextPanel.add(conLabel);

        DefaultActionGroup option1Group = new DefaultActionGroup("Set Query Context", true);
        option1Group.getTemplatePresentation().setIcon(IconLoader.getIcon("/assets/icons/query_context.svg", CustomSqlFileEditor.class));
        DefaultActionGroup option1Action = new DefaultActionGroup(option1Group) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ActionManager.getInstance().createActionPopupMenu("Set Query Context", this).getComponent().show(e.getInputEvent().getComponent(), e.getInputEvent().getComponent().getWidth(), 0);
            }
        };

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("QueryContext", option1Action, true);
        actionToolbar.setTargetComponent(contextPanel);
        JLabel contextLabel = new JLabel(NO_QUERY_CONTEXT_SELECTED);
        contextLabel.setFont(conLabel.getFont().deriveFont(10.0f));

        Map<String, SavedCluster> clusters = ClustersStorage.getInstance().getValue().getMap();

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setFont(comboBox.getFont().deriveFont(10f));
        Dimension maxSize = new Dimension(200, 20);
        comboBox.setMaximumSize(maxSize);

        for (Map.Entry<String, SavedCluster> entry : clusters.entrySet()) {
            comboBox.addItem(entry.getValue().getId());
        }
        contextPanel.add(comboBox);
        comboBox.addActionListener(e -> {
            String selectedClusterId = (String) comboBox.getSelectedItem();

            if (selectedClusterId == null) {
                return;
            }

            option1Group.removeAll();
            AnAction clearContextAction = new AnAction("Clear Context") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    contextLabel.setText(NO_QUERY_CONTEXT_SELECTED);
                    contextLabel.revalidate();
                    selectedBucketContext = null;
                    selectedScopeContext = null;
                }
            };

            option1Group.add(clearContextAction);
            option1Group.addSeparator("Buckets");

            List<String> buckets = new ArrayList<>(ActiveCluster.getInstance().get().buckets().getAllBuckets().keySet());
            for (String bucket : buckets) {

                DefaultActionGroup bucketsGroup = new DefaultActionGroup(bucket, true);
                bucketsGroup.addSeparator("Scopes");

                List<ScopeSpec> scopes = ActiveCluster.getInstance().get().bucket(bucket).collections().getAllScopes();
                for (ScopeSpec spec : scopes) {
                    AnAction scopeAction = new AnAction(spec.name()) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            contextLabel.setText(bucket + " > " + spec.name());
                            contextLabel.revalidate();
                            selectedBucketContext = bucket;
                            selectedScopeContext = spec.name();
                        }
                    };

                    bucketsGroup.add(scopeAction);
                }

                option1Group.add(bucketsGroup);
            }
        });

        comboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String item = (String) e.getItem();

                if (item == null) {
                    return;
                }
                if (ActiveCluster.getInstance().get() == null || !item.equals(ActiveCluster.getInstance().getId())) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("You can't select a cluster that you are not connected.", "Workbench Error"));

                    SwingUtilities.invokeLater(() -> comboBox
                            .setSelectedItem(cachedPreviousSelectedConnection));
                } else {
                    SwingUtilities.invokeLater(() -> {
                        contextLabel.setText(NO_QUERY_CONTEXT_SELECTED);
                        contextLabel.revalidate();

                        if (ActiveCluster.getInstance().getColor() != null) {
                            Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, ActiveCluster.getInstance().getColor());
                            Border margin = BorderFactory.createEmptyBorder(0, 0, 1, 0); // Top, left, bottom, right margins
                            Border compound = BorderFactory.createCompoundBorder(margin, line);
                            topPanel.setBorder(compound);
                            topPanel.revalidate();
                        } else {
                            topPanel.setBorder(JBUI.Borders.empty());
                            topPanel.revalidate();
                        }
                    });
                    selectedBucketContext = null;
                    selectedScopeContext = null;
                    cachedPreviousSelectedConnection = e.getItem().toString();
                }
            }
        });

        if (ActiveCluster.getInstance().get() == null) {
            comboBox.setSelectedItem(null);
            cachedPreviousSelectedConnection = null;
        } else {
            comboBox.setSelectedItem(ActiveCluster.getInstance().getId());
            cachedPreviousSelectedConnection = ActiveCluster.getInstance().getId();
        }


        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
        toolbarPanel.setBorder(JBUI.Borders.emptyRight(-12)); // Adjust these values as per your requirement

        JPanel myPanel = new JPanel();
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
        myPanel.add(toolbarPanel);
        myPanel.add(Box.createRigidArea(new Dimension(0, 0))); // Adjust the value 5 as per your requirement
        myPanel.add(contextLabel);
        myPanel.setBorder(JBUI.Borders.emptyLeft(10));

        contextPanel.add(myPanel);

        return contextPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return queryEditor.getComponent();
    }

    @NotNull
    @Override
    public String getName() {
        return "Custom SQL File Editor";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // Do nothing
    }

    @Override
    public void dispose() {
        queryEditor.release();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return (otherState, level1) -> false;
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, T value) {
    }


    static class EditorWrapper {
        private final Editor viewer;
        private final TextEditor textEditor;

        public EditorWrapper(Editor viewer, TextEditor textEditor) {
            this.textEditor = textEditor;
            this.viewer = viewer;
        }

        public JComponent getComponent() {
            return textEditor == null ? viewer.getComponent() : textEditor.getComponent();
        }

        public JComponent getContentComponent() {
            return textEditor == null ? viewer.getContentComponent() : textEditor.getEditor().getContentComponent();
        }

        public Document getDocument() {
            return textEditor == null ? viewer.getDocument() : textEditor.getEditor().getDocument();
        }

        public void release() {
            EditorFactory.getInstance().releaseEditor(Objects.requireNonNullElseGet(viewer, textEditor::getEditor));
        }
    }


}