package com.fulinlin.action;

import com.intellij.ide.ActivityTracker;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommitPanelActionSupport {
    private static final Map<CommitMessageI, CommitMessageLoadingState> LOADING_PANELS = new WeakHashMap<>();
    private static final Pattern COMMIT_HASH_PATTERN = Pattern.compile("(?<![0-9a-fA-F])([0-9a-fA-F]{7,40})(?![0-9a-fA-F])");

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
        CommitMessageI commitPanel = getCommitPanel(event);
        event.getPresentation().setEnabled(visible && event.getProject() != null && !isCommitMessageLoading(commitPanel));
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

    @Nullable
    public static String findEditedCommitHash(@Nullable CommitMessageI commitPanel) {
        if (!(commitPanel instanceof Component)) {
            return null;
        }
        Window window = SwingUtilities.getWindowAncestor((Component) commitPanel);
        if (!(window instanceof Dialog)) {
            return null;
        }
        String titleHash = extractCommitHash(((Dialog) window).getTitle());
        if (titleHash != null) {
            return titleHash;
        }
        return findCommitHashInLabels(window, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Nullable
    private static String findCommitHashInLabels(@Nullable Component component, @NotNull Set<Component> visited) {
        if (component == null || visited.contains(component)) {
            return null;
        }
        visited.add(component);
        if (component instanceof JLabel) {
            String hash = extractCommitHash(((JLabel) component).getText());
            if (hash != null) {
                return hash;
            }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                String hash = findCommitHashInLabels(child, visited);
                if (hash != null) {
                    return hash;
                }
            }
        }
        return null;
    }

    @Nullable
    private static String extractCommitHash(@Nullable String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = COMMIT_HASH_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    @NotNull
    public static CommitMessageLoadingState startCommitMessageLoading(@NotNull CommitMessageI commitPanel,
                                                                      @NotNull String placeholder) {
        if (SwingUtilities.isEventDispatchThread()) {
            return startCommitMessageLoadingOnEdt(commitPanel, placeholder);
        }
        CommitMessageLoadingState[] state = new CommitMessageLoadingState[1];
        try {
            SwingUtilities.invokeAndWait(() -> state[0] = startCommitMessageLoadingOnEdt(commitPanel, placeholder));
        } catch (Exception ignored) {
            return CommitMessageLoadingState.empty(commitPanel);
        }
        return state[0] == null ? CommitMessageLoadingState.empty(commitPanel) : state[0];
    }

    public static boolean isCommitMessageLoading(@Nullable CommitMessageI commitPanel) {
        if (commitPanel == null) {
            return false;
        }
        synchronized (LOADING_PANELS) {
            return LOADING_PANELS.containsKey(commitPanel);
        }
    }

    @NotNull
    private static CommitMessageLoadingState startCommitMessageLoadingOnEdt(@NotNull CommitMessageI commitPanel,
                                                                           @NotNull String placeholder) {
        CommitMessageLoadingState state = createLoadingState(commitPanel);
        synchronized (LOADING_PANELS) {
            LOADING_PANELS.put(commitPanel, state);
        }
        ActivityTracker.getInstance().inc();
        commitPanel.setCommitMessage(placeholder);
        state.setReadOnly(true);
        return state;
    }

    @NotNull
    private static CommitMessageLoadingState createLoadingState(@NotNull CommitMessageI commitPanel) {
        EditorTextField editorField = findCommitMessageEditorField(commitPanel);
        return editorField == null
                ? CommitMessageLoadingState.empty(commitPanel)
                : new CommitMessageLoadingState(commitPanel, editorField, editorField.isViewer());
    }

    @Nullable
    private static EditorTextField findCommitMessageEditorField(@Nullable Object target) {
        return findCommitMessageEditorField(target, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Nullable
    private static EditorTextField findCommitMessageEditorField(@Nullable Object target, @NotNull Set<Object> visited) {
        if (target == null || visited.contains(target)) {
            return null;
        }
        visited.add(target);
        if (target instanceof EditorTextField) {
            return (EditorTextField) target;
        }
        if (target instanceof CommitMessage) {
            return ((CommitMessage) target).getEditorField();
        }

        EditorTextField editorField = findCommitMessageEditorField(invokeNoArg(target, "getEditorField"), visited);
        if (editorField != null) {
            return editorField;
        }

        editorField = findCommitMessageEditorField(invokeNoArg(target, "getCommitMessageUi"), visited);
        if (editorField != null) {
            return editorField;
        }

        if (target instanceof CheckinProjectPanel) {
            Object workflowHandler = ((CheckinProjectPanel) target).getCommitWorkflowHandler();
            editorField = findCommitMessageEditorField(invokeNoArg(workflowHandler, "getUi"), visited);
            if (editorField != null) {
                return editorField;
            }
        }

        Object component = invokeNoArg(target, "getComponent");
        if (component != target) {
            editorField = findCommitMessageEditorField(component, visited);
            if (editorField != null) {
                return editorField;
            }
        }

        if (target instanceof Container) {
            for (Component child : ((Container) target).getComponents()) {
                editorField = findCommitMessageEditorField(child, visited);
                if (editorField != null) {
                    return editorField;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Object invokeNoArg(@NotNull Object target, @NotNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
        }

        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Exception ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    public static final class CommitMessageLoadingState {
        private final CommitMessageI commitPanel;
        private final EditorTextField editorField;
        private final boolean previousViewer;

        private CommitMessageLoadingState(@NotNull CommitMessageI commitPanel,
                                          @Nullable EditorTextField editorField,
                                          boolean previousViewer) {
            this.commitPanel = commitPanel;
            this.editorField = editorField;
            this.previousViewer = previousViewer;
        }

        @NotNull
        private static CommitMessageLoadingState empty(@NotNull CommitMessageI commitPanel) {
            return new CommitMessageLoadingState(commitPanel, null, false);
        }

        private void setReadOnly(boolean readOnly) {
            if (editorField != null) {
                editorField.setViewer(readOnly);
            }
        }

        public void finish() {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    if (editorField != null) {
                        editorField.setViewer(previousViewer);
                    }
                } finally {
                    synchronized (LOADING_PANELS) {
                        LOADING_PANELS.remove(commitPanel);
                    }
                    ActivityTracker.getInstance().inc();
                }
            });
        }
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
