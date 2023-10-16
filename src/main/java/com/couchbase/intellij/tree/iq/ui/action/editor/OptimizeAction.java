/*
 * Copyright (c) 2023 Mariusz Bernacki <consulting@didalgo.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.couchbase.intellij.tree.iq.ui.action.editor;

import com.couchbase.intellij.tree.iq.ChatGptBundle;

public class OptimizeAction extends GenericEditorAction {
    public OptimizeAction() {
        super(() -> ChatGptBundle.message("action.code.optimize.menu"), "Optimize this code");
    }
}