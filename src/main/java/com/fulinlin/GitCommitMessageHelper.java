package com.fulinlin;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-03 16:33
 **/
public class GitCommitMessageHelper implements ApplicationComponent {

    public GitCommitMessageHelper() {
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "GitCommitMessageHelper";
    }

}
