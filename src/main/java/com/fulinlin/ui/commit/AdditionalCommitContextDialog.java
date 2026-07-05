package com.fulinlin.ui.commit;

import com.fulinlin.localization.PluginBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AdditionalCommitContextDialog extends DialogWrapper {

    private final JTextArea additionalContextTextArea;

    public AdditionalCommitContextDialog(@Nullable Project project, @Nullable String initialContext) {
        super(project);
        this.additionalContextTextArea = new JTextArea(initialContext == null ? "" : initialContext, 8, 70);
        this.additionalContextTextArea.setLineWrap(true);
        this.additionalContextTextArea.setWrapStyleWord(true);
        setTitle(PluginBundle.get("action.generate.additional.context.title"));
        setOKButtonText(PluginBundle.get("action.generate.additional.context.ok.button"));
        setCancelButtonText(PluginBundle.get("commit.panel.cancel.button"));
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JLabel tip = new JLabel(PluginBundle.get("action.generate.additional.context.message"));
        JScrollPane scrollPane = new JScrollPane(additionalContextTextArea);
        scrollPane.setPreferredSize(new Dimension(640, 180));
        panel.add(tip, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    @NotNull
    public String getAdditionalContext() {
        return additionalContextTextArea.getText().trim();
    }
}
