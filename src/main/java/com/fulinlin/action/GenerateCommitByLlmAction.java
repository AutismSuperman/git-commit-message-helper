package com.fulinlin.action;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.service.LlmCommitService;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.actionSystem.Presentation;
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
import com.intellij.ui.AnimatedIcon;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenerateCommitByLlmAction extends AnAction implements DumbAware {

    private final GitCommitMessageHelperSettings settings;
    private final LlmCommitService llmCommitService = new LlmCommitService();
    private volatile boolean loading;

    public GenerateCommitByLlmAction() {
        this.settings = GitCommitMessageHelperSettings.getInstance();
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent actionEvent) {
        CommitPanelActionSupport.CommitContext commitContext = CommitPanelActionSupport.getContext(actionEvent);
        CommitMessageI commitPanel = commitContext.getCommitPanel();
        Project project = actionEvent == null ? null : actionEvent.getProject();
        if (commitPanel == null || project == null) {
            return;
        }
        boolean hasSelection = commitContext.hasSelection();
        String editedCommitHash = hasSelection ? null : CommitPanelActionSupport.findEditedCommitHash(commitPanel);
        if (!hasSelection && editedCommitHash == null) {
            Messages.showWarningDialog(project, PluginBundle.get("action.generate.empty.selection"), PluginBundle.get("action.llm.error.title"));
            return;
        }
        if (!LlmCommitService.isConfigured(settings)) {
            Messages.showWarningDialog(project, PluginBundle.get("action.llm.not.configured"), PluginBundle.get("action.llm.error.title"));
            return;
        }
        if (loading || CommitPanelActionSupport.isCommitMessageLoading(commitPanel)) {
            return;
        }

        String originalMessage = CommitPanelActionSupport.getCurrentCommitMessage(commitPanel);
        loading = true;
        updateLoadingPresentation(actionEvent, true);
        CommitPanelActionSupport.CommitMessageLoadingState loadingState =
                CommitPanelActionSupport.startCommitMessageLoading(commitPanel, PluginBundle.get("action.generate.progress") + "...");
        String historicalCommitHash = editedCommitHash;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, PluginBundle.get("action.generate.progress"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                StringBuilder builder = new StringBuilder();
                try {
                    if (historicalCommitHash != null) {
                        llmCommitService.generateCommitMessageForCommit(
                                project,
                                settings,
                                historicalCommitHash,
                                delta -> {
                                    builder.append(delta);
                                    CommitPanelActionSupport.setCommitMessage(commitPanel, builder.toString());
                                }
                        );
                    } else {
                        llmCommitService.generateCommitMessage(
                                project,
                                settings,
                                commitContext.getSelectedChanges(),
                                commitContext.getSelectedFiles(),
                                delta -> {
                                    builder.append(delta);
                                    CommitPanelActionSupport.setCommitMessage(commitPanel, builder.toString());
                                }
                        );
                    }
                    if (builder.length() == 0) {
                        CommitPanelActionSupport.setCommitMessage(commitPanel, originalMessage);
                    }
                } catch (Exception e) {
                    if (builder.length() == 0) {
                        CommitPanelActionSupport.setCommitMessage(commitPanel, originalMessage);
                    }
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog(project, e.getMessage(), PluginBundle.get("action.llm.error.title"))
                    );
                } finally {
                    loading = false;
                    loadingState.finish();
                    ApplicationManager.getApplication().invokeLater(() -> updateLoadingPresentation(actionEvent, false));
                }
            }
        });
    }

    @Override
    public void update(@Nullable AnActionEvent e) {
        boolean visible = settings.getCentralSettings().getActionSettings().getGenerateCommitActionVisible();
        CommitPanelActionSupport.updatePresentation(e, visible);
        if (e != null) {
            updateLoadingPresentation(e, loading);
        }
    }

    private void updateLoadingPresentation(@Nullable AnActionEvent event, boolean loading) {
        if (event == null) {
            return;
        }
        Presentation presentation = event.getPresentation();
        presentation.setIcon(loading ? AnimatedIcon.Default.INSTANCE : PluginIcons.AI_GENERATE);
        presentation.setDisabledIcon(loading ? AnimatedIcon.Default.INSTANCE : null);
        boolean visible = settings.getCentralSettings().getActionSettings().getGenerateCommitActionVisible();
        CommitMessageI commitPanel = CommitPanelActionSupport.getCommitPanel(event);
        presentation.setEnabled(!loading && visible && event.getProject() != null && !CommitPanelActionSupport.isCommitMessageLoading(commitPanel));
    }
}
