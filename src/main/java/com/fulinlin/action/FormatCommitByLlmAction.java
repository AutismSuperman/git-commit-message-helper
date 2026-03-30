package com.fulinlin.action;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.service.LlmCommitService;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FormatCommitByLlmAction extends AnAction implements DumbAware {

    private final GitCommitMessageHelperSettings settings;
    private final LlmCommitService llmCommitService = new LlmCommitService();

    public FormatCommitByLlmAction() {
        this.settings = GitCommitMessageHelperSettings.getInstance();
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent actionEvent) {
        CommitMessageI commitPanel = CommitPanelActionSupport.getCommitPanel(actionEvent);
        Project project = actionEvent == null ? null : actionEvent.getProject();
        if (commitPanel == null || project == null) {
            return;
        }
        if (!LlmCommitService.isConfigured(settings)) {
            Messages.showWarningDialog(project, PluginBundle.get("action.llm.not.configured"), PluginBundle.get("action.llm.error.title"));
            return;
        }
        String currentMessage = CommitPanelActionSupport.getCurrentCommitMessage(commitPanel);
        if (currentMessage.isEmpty()) {
            Messages.showWarningDialog(project, PluginBundle.get("action.format.empty.message"), PluginBundle.get("action.llm.error.title"));
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, PluginBundle.get("action.format.progress"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                StringBuilder builder = new StringBuilder();
                CommitPanelActionSupport.setCommitMessage(commitPanel, "");
                try {
                    llmCommitService.formatCommitMessage(project, settings, currentMessage, delta -> {
                        builder.append(delta);
                        CommitPanelActionSupport.setCommitMessage(commitPanel, builder.toString());
                    });
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog(project, e.getMessage(), PluginBundle.get("action.llm.error.title"))
                    );
                }
            }
        });
    }

    @Override
    public void update(@Nullable AnActionEvent e) {
        boolean visible = settings.getCentralSettings().getActionSettings().getFormatCommitActionVisible();
        CommitPanelActionSupport.updatePresentation(e, visible);
    }
}
