package com.chivenh.ui;

import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CommitDialog extends DialogWrapper {

    private final CommitPanel panel;

    public CommitDialog(@Nullable Project project, GitCommitMsgHelperSettings settings) {
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

    public CommitMessage getCommitMessage(GitCommitMsgHelperSettings settings) {
        return panel.getCommitMessage(settings);
    }

}