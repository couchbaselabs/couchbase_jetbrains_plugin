package com.couchbase.intellij.workbench.explain;

import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;

import javax.swing.*;
import java.awt.*;

public class HtmlPanel extends JPanel {
    private final JBCefBrowser jbCefBrowser;

    public HtmlPanel() {
        super(new BorderLayout());
        setBackground(Color.decode("#3c3f41"));
        JBCefClient jbCefClient = JBCefApp.getInstance().createClient();
        jbCefBrowser = JBCefBrowser.createBuilder().setClient(jbCefClient)
            .setUrl(null)
            .build();
        add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
    }

    public void loadHTML(String html) {
        jbCefBrowser.loadHTML(html);
    }

    public void loadURL(String url) {
        jbCefBrowser.loadURL(url);
    }
}