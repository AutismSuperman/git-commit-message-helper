package com.fulinlin.action;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.MessageStorage;
import com.fulinlin.service.LlmCommitService;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.storage.GitCommitMessageStorage;
import com.fulinlin.ui.commit.CommitDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import org.jetbrains.annotations.Nullable;

/**
 * @author fulin
 */
public class CreateCommitAction extends AnAction implements DumbAware {

    private final GitCommitMessageHelperSettings settings;
    private final LlmCommitService llmCommitService = new LlmCommitService();

    public CreateCommitAction() {
        this.settings = ServiceManager.getService(GitCommitMessageHelperSettings.class);
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent actionEvent) {
        final CommitMessageI commitPanel = CommitPanelActionSupport.getCommitPanel(actionEvent);
        if (commitPanel == null || actionEvent == null) {
            return;
        }
        Project project = actionEvent.getProject();
        if (project == null) {
            return;
        }
        GitCommitMessageStorage storage = project.getService(GitCommitMessageStorage.class);
        GitCommitMessageStorage state = storage.getState();
        if (state == null) {
            return;
        }
        MessageStorage messageStorage = state.getMessageStorage();
        CommitTemplate initialCommitTemplate = resolveInitialCommitTemplate(project, commitPanel, messageStorage.getCommitTemplate());
        CommitDialog dialog = new CommitDialog(project, settings, initialCommitTemplate);
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

    @Override
    public void update(@Nullable AnActionEvent e) {
        boolean visible = settings.getCentralSettings().getActionSettings().getCreateCommitActionVisible();
        CommitPanelActionSupport.updatePresentation(e, visible);
    }

    private CommitTemplate resolveInitialCommitTemplate(Project project,
                                                        CommitMessageI commitPanel,
                                                        CommitTemplate cachedCommitTemplate) {
        String currentMessage = CommitPanelActionSupport.getCurrentCommitMessage(commitPanel);
        if (currentMessage.isEmpty()
                || !LlmCommitService.isConfigured(settings)
                || !LlmCommitService.isSmartEchoEnabled(settings)) {
            return cachedCommitTemplate;
        }
        try {
            return ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    () -> llmCommitService.parseCommitMessageToTemplate(project, settings, currentMessage),
                    PluginBundle.get("action.smart.echo.progress"),
                    false,
                    project
            );
        } catch (Exception e) {
            Messages.showErrorDialog(project, e.getMessage(), PluginBundle.get("action.llm.error.title"));
            return cachedCommitTemplate;
        }
    }
}
