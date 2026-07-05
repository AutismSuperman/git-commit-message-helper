package com.fulinlin.action;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.service.LlmCommitService;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GenerateCommitByLlmAction extends AnAction implements DumbAware {

    protected final GitCommitMessageHelperSettings settings;
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
        if (loading && CommitPanelActionSupport.isCommitMessageLoading(commitPanel)) {
            CommitPanelActionSupport.cancelCommitMessageLoading(commitPanel);
            return;
        }
        if (loading || CommitPanelActionSupport.isCommitMessageLoading(commitPanel)) {
            return;
        }

        String additionalContext = getAdditionalContext(project, commitPanel);
        if (additionalContext == null) {
            return;
        }

        String originalMessage = CommitPanelActionSupport.getCurrentCommitMessage(commitPanel);
        loading = true;
        updateLoadingPresentation(actionEvent, true);
        CommitPanelActionSupport.CommitMessageLoadingState loadingState =
                CommitPanelActionSupport.startCommitMessageLoading(commitPanel, PluginBundle.get("action.generate.progress") + "...");
        String historicalCommitHash = editedCommitHash;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, PluginBundle.get("action.generate.progress"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                CommitPanelActionSupport.setCommitMessageLoadingIndicator(commitPanel, indicator);
                StringBuilder builder = new StringBuilder();
                try {
                    indicator.checkCanceled();
                    if (historicalCommitHash != null) {
                        llmCommitService.generateCommitMessageForCommit(
                                project,
                                settings,
                                historicalCommitHash,
                                additionalContext,
                                delta -> {
                                    indicator.checkCanceled();
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
                                additionalContext,
                                delta -> {
                                    indicator.checkCanceled();
                                    builder.append(delta);
                                    CommitPanelActionSupport.setCommitMessage(commitPanel, builder.toString());
                                }
                        );
                    }
                    indicator.checkCanceled();
                    if (builder.length() == 0) {
                        CommitPanelActionSupport.setCommitMessage(commitPanel, originalMessage);
                    }
                } catch (ProcessCanceledException e) {
                    CommitPanelActionSupport.setCommitMessage(commitPanel, originalMessage);
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

    /**
     * @return additional user context to include in the generation prompt, an empty string for none,
     * or {@code null} when generation should be cancelled before it starts.
     */
    @Nullable
    protected String getAdditionalContext(@NotNull Project project, @NotNull CommitMessageI commitPanel) {
        return "";
    }

    @Override
    public void update(@Nullable AnActionEvent e) {
        boolean visible = isActionVisible();
        CommitPanelActionSupport.updatePresentation(e, visible);
        if (e != null) {
            updateLoadingPresentation(e, loading);
        }
    }

    protected boolean isActionVisible() {
        return settings.getCentralSettings().getActionSettings().getGenerateCommitActionVisible();
    }

    @NotNull
    protected Icon getActionIcon() {
        return PluginIcons.AI_GENERATE;
    }

    private void updateLoadingPresentation(@Nullable AnActionEvent event, boolean loading) {
        if (event == null) {
            return;
        }
        Presentation presentation = event.getPresentation();
        presentation.setIcon(loading ? PluginIcons.STOP : getActionIcon());
        presentation.setDisabledIcon(null);
        boolean visible = isActionVisible();
        CommitMessageI commitPanel = CommitPanelActionSupport.getCommitPanel(event);
        presentation.setEnabled(loading || visible && event.getProject() != null && !CommitPanelActionSupport.isCommitMessageLoading(commitPanel));
    }
}
