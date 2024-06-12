package com.couchbase.intellij.actions;

import com.couchbase.intellij.tree.cblite.sqlppl.SQLPPLiteFileEditor;
import com.couchbase.intellij.workbench.CustomSqlFileEditor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.intellij.sdk.language.completion.FeedbackUi;
import org.jetbrains.annotations.NotNull;

public class CompletionFeedbackAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        FileEditor editor = FileEditorManager.getInstance(e.getProject()).getSelectedEditor();
        if (editor instanceof CustomSqlFileEditor || editor instanceof SQLPPLiteFileEditor) {
            e.getPresentation().setEnabledAndVisible(true);
        } else {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        FeedbackUi feedbackUi = new FeedbackUi(e.getProject());
        feedbackUi.show();
    }
}
