package com.chivenh;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @program: git-commit-message-helper
 * @author Chivenh
 * @create: 2019-12-03 16:33
 **/
public class GitCommitMsgHelper implements ApplicationComponent {

    public GitCommitMsgHelper() {
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
        return "GitCommitMsgHelper";
    }

}
