package com.couchbase.intellij.eventing.components;

import java.awt.event.*;
import javax.swing.JComponent;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;

public class HelpIcon {

    public static JBLabel createHelpIcon(String tooltipText) {
        JBLabel helpIcon = new JBLabel();
        helpIcon.setIcon(AllIcons.General.ContextHelp);
        helpIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                JComponent component = (JComponent) e.getComponent();
                Balloon balloon = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
                        tooltipText,
                        null,
                        JBColor.background(),
                        null).setFadeoutTime(1500).createBalloon();
                balloon.showInCenterOf(component);
            }
        });
        return helpIcon;
    }

}
