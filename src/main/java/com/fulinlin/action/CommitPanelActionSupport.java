package com.fulinlin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.Refreshable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

public final class CommitPanelActionSupport {

    private CommitPanelActionSupport() {
    }

    @Nullable
    public static CommitMessageI getCommitPanel(@Nullable AnActionEvent e) {
        return getContext(e).getCommitPanel();
    }

    @NotNull
    public static CommitContext getContext(@Nullable AnActionEvent e) {
        if (e == null) {
            return CommitContext.empty();
        }
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        CheckinProjectPanel checkinProjectPanel = data instanceof CheckinProjectPanel ? (CheckinProjectPanel) data : null;
        if (data instanceof CommitMessageI) {
            return new CommitContext((CommitMessageI) data, checkinProjectPanel);
        }
        return new CommitContext(VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext()), checkinProjectPanel);
    }

    public static void updatePresentation(@Nullable AnActionEvent event, boolean visible) {
        if (event == null) {
            return;
        }
        event.getPresentation().setVisible(visible);
        event.getPresentation().setEnabled(visible && event.getProject() != null);
    }

    @NotNull
    public static String getCurrentCommitMessage(@NotNull CommitMessageI commitPanel) {
        String[] methodNames = {"getComment", "getCommitMessage", "getText"};
        for (String methodName : methodNames) {
            try {
                Method method = commitPanel.getClass().getMethod(methodName);
                Object value = method.invoke(commitPanel);
                if (value instanceof String) {
                    return ((String) value).trim();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    public static void setCommitMessage(@NotNull CommitMessageI commitPanel, @NotNull String message) {
        ApplicationManager.getApplication().invokeLater(() -> commitPanel.setCommitMessage(message));
    }

    public static final class CommitContext {

        private final CommitMessageI commitPanel;
        private final CheckinProjectPanel checkinProjectPanel;

        private CommitContext(@Nullable CommitMessageI commitPanel, @Nullable CheckinProjectPanel checkinProjectPanel) {
            this.commitPanel = commitPanel;
            this.checkinProjectPanel = checkinProjectPanel;
        }

        @NotNull
        public static CommitContext empty() {
            return new CommitContext(null, null);
        }

        @Nullable
        public CommitMessageI getCommitPanel() {
            return commitPanel;
        }

        @NotNull
        public Collection<Change> getSelectedChanges() {
            return checkinProjectPanel != null ? checkinProjectPanel.getSelectedChanges() : Collections.emptyList();
        }

        @NotNull
        public Collection<File> getSelectedFiles() {
            return checkinProjectPanel != null ? checkinProjectPanel.getFiles() : Collections.emptyList();
        }

        public boolean hasSelection() {
            return !getSelectedChanges().isEmpty() || !getSelectedFiles().isEmpty();
        }
    }
}
