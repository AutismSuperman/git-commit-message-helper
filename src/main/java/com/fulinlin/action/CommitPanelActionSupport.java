package com.fulinlin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.Refreshable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class CommitPanelActionSupport {

    private CommitPanelActionSupport() {
    }

    @Nullable
    public static CommitMessageI getCommitPanel(@Nullable AnActionEvent e) {
        if (e == null) {
            return null;
        }
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (data instanceof CommitMessageI) {
            return (CommitMessageI) data;
        }
        return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());
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
}
