package com.fulinlin.action;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.MessageStorage;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.storage.GitCommitMessageStorage;
import com.fulinlin.ui.commit.CommitDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.Refreshable;
import org.jetbrains.annotations.Nullable;

/**
 * @author fulin
 */
public class CreateCommitAction extends AnAction implements DumbAware {

    private final GitCommitMessageHelperSettings settings;


    public CreateCommitAction() {
        this.settings = ServiceManager.getService(GitCommitMessageHelperSettings.class);
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent actionEvent) {
        final CommitMessageI commitPanel = getCommitPanel(actionEvent);
        if (commitPanel == null) {
            return;
        }
        Project project = actionEvent.getProject();
        assert project != null;
        GitCommitMessageStorage storage = project.getService(GitCommitMessageStorage.class);
        GitCommitMessageStorage state = storage.getState();
        assert state != null;
        MessageStorage messageStorage = state.getMessageStorage();
        CommitDialog dialog = new CommitDialog(
                project, settings,
                messageStorage.getCommitTemplate()
        );
        dialog.show();
        if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            commitPanel.setCommitMessage(dialog.getCommitMessage(settings).toString());
            storage.getMessageStorage().setCommitTemplate(null);
        }
        if (dialog.getExitCode() == DialogWrapper.CANCEL_EXIT_CODE) {
            CommitTemplate commitMessageTemplate = dialog.getCommitMessageTemplate();
            storage.getMessageStorage().setCommitTemplate(commitMessageTemplate);
        }
    }

    @Nullable
    private static CommitMessageI getCommitPanel(@Nullable AnActionEvent e) {
        if (e == null) {
            return null;
        }
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (data instanceof CommitMessageI) {
            return (CommitMessageI) data;
        }
        return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());
    }
}