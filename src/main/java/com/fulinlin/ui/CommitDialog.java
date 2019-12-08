package com.fulinlin.ui;

import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CommitDialog extends DialogWrapper {

    private final CommitPanel panel;

    public CommitDialog(@Nullable Project project, GitCommitMessageHelperSettings settings) {
        super(project);
        panel = new CommitPanel(project,settings);
        setTitle("Commit");
        setOKButtonText("OK");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel.getMainPanel();
    }

    public CommitMessage getCommitMessage(GitCommitMessageHelperSettings settings) {
        return panel.getCommitMessage(settings);
    }

}