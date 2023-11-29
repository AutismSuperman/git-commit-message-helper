package com.fulinlin.ui.commit;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CommitDialog extends DialogWrapper {

    private final CommitPanel panel;

    public CommitDialog(@Nullable Project project, GitCommitMessageHelperSettings settings, CommitTemplate commitMessageTemplate) {
        super(project);
        panel = new CommitPanel(project, settings, commitMessageTemplate);
        setTitle(PluginBundle.get("commit.panel.title"));
        setOKButtonText(PluginBundle.get("commit.panel.ok.button"));
        setCancelButtonText(PluginBundle.get("commit.panel.cancel.button"));
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

    public CommitTemplate getCommitMessageTemplate() {
        return panel.getCommitMessageTemplate();
    }


}