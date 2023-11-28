package com.couchbase.intellij.tree.iq.ui.action.editor;

import com.couchbase.intellij.tree.iq.ChatGptBundle;

public class MinimizeAction extends GenericEditorAction {
    public MinimizeAction() {
        super(() -> ChatGptBundle.message("action.code.minimize.menu"), "Rewrite this code in the shortest and the most concise form as you can think of.");
    }
}
