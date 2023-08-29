package com.chivenh.ui;

import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CommitDialog extends DialogWrapper {

    private final CommitPanel panel;

    public CommitDialog(@Nullable String currentMsg,@Nullable Project project, GitCommitMsgHelperSettings settings) {
        super(project);
        panel = new CommitPanel(currentMsg,project,settings);
        setTitle("Commit Msg");
        setOKButtonText("OK");
        init();
    }

	/**
	 * 开启:OK时进行数据项验证
	 * @return -
	 */
	@Override
	protected @Nullable ValidationInfo doValidate() {
		return panel.doValidate();
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