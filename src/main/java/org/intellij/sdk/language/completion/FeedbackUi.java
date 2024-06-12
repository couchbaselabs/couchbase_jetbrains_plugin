package org.intellij.sdk.language.completion;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.intellij.tree.iq.ui.MessageComponent;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import it.unimi.dsi.fastutil.ints.H;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public class FeedbackUi extends DialogWrapper {
    private Project project;
    private JPanel ratings;
    private JTextArea reasoning;
    private JPanel centerPanel;

    @Override
    protected void doOKAction() {
        centerPanel.setEnabled(false);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String rating = Arrays.stream(ratings.getComponents()).filter(c -> c.getBackground() != null).map(c -> ((JButton) c).getText()).findFirst().orElse("okay");
            String reason = reasoning.getText();
            JsonObject body = JsonObject.create();
            body.put("type", "highlighter_feedback");
            body.put("origin", "jetbrains");
            body.put("rating", rating);
            body.put("reason", reason);

            try {
                HttpRequest request = HttpRequest.newBuilder(new URI(MessageComponent.FEEDBACK_ENDPOINT))
                        .header("X-Secret", MessageComponent.SECRET)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toBytes()))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                client.sendAsync(request, info -> {
                    if (info.statusCode() > 299) {
                        Log.error(String.format("failed to send feedback, response status: %d", info.statusCode()));
                    }
                    this.close(0);
                    return HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
                })
                .handleAsync((response, error) -> {
                    if (error != null) {
                        Log.error("failed to send feedback entry %s", error);
                    }
                    centerPanel.setEnabled(true);
                    return null;
                });
            } catch (Exception e) {
                Log.error("failed to send feedback entry %s", e);
            }
        });

        this.close(OK_EXIT_CODE);
    }

    public FeedbackUi(@Nullable Project project) {
        super(project);
        this.project = project;
        this.init();
        setTitle("Editor Feedback");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel ratingLabel = new JLabel("What do you think of Couchbase SQL++ and SQL++ Lite highlighting and completion features?");
        ratingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(ratingLabel, gbc);
        gbc.gridy++;

        ratings = new JPanel(new GridBagLayout());
        GridBagConstraints rtngbc = new GridBagConstraints();
        rtngbc.fill = GridBagConstraints.BOTH;
        rtngbc.gridx = 0;
        rtngbc.gridy = 0;
        JButton terrible = prepareFeedbackButton(ratings, "terrible");
        JButton bad = prepareFeedbackButton(ratings, "bad");
        JButton okay = prepareFeedbackButton(ratings, "okay");
        JButton good = prepareFeedbackButton(ratings, "good");
        JButton amazing = prepareFeedbackButton(ratings, "amazing");
        okay.setBackground(Color.LIGHT_GRAY);

        ratings.add(terrible, rtngbc);
        rtngbc.gridx++;
        ratings.add(bad, rtngbc);
        rtngbc.gridx++;
        ratings.add(okay, rtngbc);
        rtngbc.gridx++;
        ratings.add(good, rtngbc);
        rtngbc.gridx++;
        ratings.add(amazing, rtngbc);
        rtngbc.gridx++;

        centerPanel.add(ratings, gbc);
        gbc.gridy++;

        JLabel reasoningLabel = new JLabel("What are the main reasons for your rating?");
        reasoningLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        centerPanel.add(reasoningLabel, gbc);
        gbc.gridy++;

        JPanel reasoningWrapper = new JPanel(new BorderLayout());
        reasoningWrapper.setBorder(BorderFactory.createEmptyBorder(10,15,15,15));
        reasoning = new JTextArea();
        reasoning.setRows(10);
        reasoningWrapper.add(reasoning, BorderLayout.CENTER);
        centerPanel.add(reasoningWrapper, gbc);

        return centerPanel;
    }

    private JButton prepareFeedbackButton(JPanel parent, String label) {
        JButton button = new JButton(String.format("%s%s", label.substring(0, 1).toUpperCase(), label.substring(1)));
        button.setIcon(getIcon(String.format("/icons/feedback/%s.png", label)));
        button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        button.setBackground(null);
        button.setMargin(JBUI.insets(5));
        button.addActionListener(a -> {
            Arrays.stream(parent.getComponents()).forEach(component -> component.setBackground(null));
            button.setBackground(Color.LIGHT_GRAY);
        });
        return button;
    }

    private Icon getIcon(String resourceName) {
        Image image = new ImageIcon(FeedbackUi.class.getResource(resourceName)).getImage();
        Image scaled = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
