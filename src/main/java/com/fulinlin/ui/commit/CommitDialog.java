package com.fulinlin.ui.commit;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.utils.I18nUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CommitDialog extends DialogWrapper {

    private final CommitPanel panel;

    public CommitDialog(@Nullable Project project, GitCommitMessageHelperSettings settings, CommitTemplate commitMessageTemplate) {
        super(project);
        panel = new CommitPanel(project, settings, commitMessageTemplate);
        setTitle(I18nUtil.getInfo("commit.title"));
        setOKButtonText(I18nUtil.getInfo("commit.ok.button"));
        setCancelButtonText(I18nUtil.getInfo("commit.cancel.button"));
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