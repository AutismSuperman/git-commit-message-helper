package com.fulinlin.action;

import com.fulinlin.ui.commit.AdditionalCommitContextDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CommitMessageI;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GenerateCommitWithAdditionalContextAction extends GenerateCommitByLlmAction {

    private static String lastAdditionalContext = "";

    @Override
    @Nullable
    protected String getAdditionalContext(@NotNull Project project, @NotNull CommitMessageI commitPanel) {
        AdditionalCommitContextDialog dialog = new AdditionalCommitContextDialog(project, lastAdditionalContext);
        if (!dialog.showAndGet()) {
            return null;
        }
        lastAdditionalContext = dialog.getAdditionalContext();
        return lastAdditionalContext;
    }

    @Override
    protected boolean isActionVisible() {
        return settings.getCentralSettings().getActionSettings().getGenerateCommitWithAdditionalContextActionVisible();
    }

    @Override
    @NotNull
    protected Icon getActionIcon() {
        return PluginIcons.AI_GENERATE_CONTEXT;
    }
}
