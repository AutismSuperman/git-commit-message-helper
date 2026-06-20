package com.fulinlin.action;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.MessageStorage;
import com.fulinlin.service.LlmCommitService;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.storage.GitCommitMessageStorage;
import com.fulinlin.ui.commit.CommitDialog;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author fulin
 */
public class CreateCommitAction extends AnAction implements DumbAware {

    private final GitCommitMessageHelperSettings settings;
    private final LlmCommitService llmCommitService = new LlmCommitService();
    private volatile boolean loading;

    public CreateCommitAction() {
        this.settings = GitCommitMessageHelperSettings.getInstance();
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent actionEvent) {
        CommitPanelActionSupport.CommitContext commitContext = CommitPanelActionSupport.getContext(actionEvent);
        final CommitMessageI commitPanel = commitContext.getCommitPanel();
        if (commitPanel == null || actionEvent == null) {
            return;
        }
        Project project = actionEvent.getProject();
        if (project == null) {
            return;
        }
        if (loading && CommitPanelActionSupport.isCommitMessageLoading(commitPanel)) {
            CommitPanelActionSupport.cancelCommitMessageLoading(commitPanel);
            return;
        }
        if (loading || CommitPanelActionSupport.isCommitMessageLoading(commitPanel)) {
            return;
        }
        GitCommitMessageStorage storage = project.getService(GitCommitMessageStorage.class);
        GitCommitMessageStorage state = storage.getState();
        if (state == null) {
            return;
        }
        MessageStorage messageStorage = state.getMessageStorage();
        CommitTemplate cachedCommitTemplate = messageStorage.getCommitTemplate();
        String currentMessage = CommitPanelActionSupport.getCurrentCommitMessage(commitPanel);
        if (!shouldUseSmartEcho(currentMessage)) {
            showCommitDialog(project, commitPanel, storage, cachedCommitTemplate);
            return;
        }

        loading = true;
        updateLoadingPresentation(actionEvent, true);
        CommitPanelActionSupport.CommitMessageLoadingState loadingState =
                CommitPanelActionSupport.startCommitMessageLoading(commitPanel);
        String editedCommitHash = commitContext.hasSelection() ? null : CommitPanelActionSupport.findEditedCommitHash(commitPanel);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, PluginBundle.get("action.smart.echo.progress"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                CommitPanelActionSupport.setCommitMessageLoadingIndicator(commitPanel, indicator);
                CommitTemplate initialCommitTemplate = cachedCommitTemplate;
                Exception failure = null;
                boolean canceled = false;
                try {
                    indicator.checkCanceled();
                    if (editedCommitHash != null) {
                        initialCommitTemplate = llmCommitService.parseCommitMessageToTemplateForCommit(
                                project,
                                settings,
                                editedCommitHash,
                                currentMessage
                        );
                    } else {
                        initialCommitTemplate = llmCommitService.parseCommitMessageToTemplate(
                                project,
                                settings,
                                commitContext.getSelectedChanges(),
                                commitContext.getSelectedFiles(),
                                currentMessage
                        );
                    }
                    indicator.checkCanceled();
                } catch (ProcessCanceledException e) {
                    canceled = true;
                } catch (Exception e) {
                    failure = e;
                } finally {
                    loading = false;
                    loadingState.finish();
                    ApplicationManager.getApplication().invokeLater(() -> updateLoadingPresentation(actionEvent, false));
                }
                if (!canceled) {
                    CommitTemplate template = initialCommitTemplate;
                    Exception error = failure;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (error != null) {
                            Messages.showErrorDialog(project, error.getMessage(), PluginBundle.get("action.llm.error.title"));
                        }
                        showCommitDialog(project, commitPanel, storage, template);
                    }, ModalityState.any());
                }
            }
        });
    }

    private void showCommitDialog(@NotNull Project project,
                                  @NotNull CommitMessageI commitPanel,
                                  @NotNull GitCommitMessageStorage storage,
                                  @Nullable CommitTemplate initialCommitTemplate) {
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
        if (e != null) {
            updateLoadingPresentation(e, loading);
        }
    }

    private boolean shouldUseSmartEcho(@NotNull String currentMessage) {
        return !currentMessage.isEmpty()
                && LlmCommitService.isConfigured(settings)
                && LlmCommitService.isSmartEchoEnabled(settings);
    }

    private void updateLoadingPresentation(@Nullable AnActionEvent event, boolean loading) {
        if (event == null) {
            return;
        }
        Presentation presentation = event.getPresentation();
        presentation.setIcon(loading ? PluginIcons.STOP : PluginIcons.EDIT);
        presentation.setDisabledIcon(null);
        boolean visible = settings.getCentralSettings().getActionSettings().getCreateCommitActionVisible();
        CommitMessageI commitPanel = CommitPanelActionSupport.getCommitPanel(event);
        presentation.setEnabled(loading || visible && event.getProject() != null && !CommitPanelActionSupport.isCommitMessageLoading(commitPanel));
    }
}
