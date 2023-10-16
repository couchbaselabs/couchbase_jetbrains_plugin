/*
 * Copyright (c) 2023 Mariusz Bernacki <consulting@didalgo.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.couchbase.intellij.tree.iq.text;

public interface CodeFragmentFormatter {

    String format(CodeFragment cf);

    CodeFragmentFormatter withoutDescription();
}
