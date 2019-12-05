package com.fulinlin.setting.ui;

import com.fulinlin.anon.Remark;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 这个类Settings 中的属性被创建的时候
 * User: fulin
 */
public class TemplateEditPaneConfigurable implements SearchableConfigurable {

    private TemplateEditPane templateEditPane;

    private GitCommitMessageHelperSettings settings;

    public TemplateEditPaneConfigurable() {
        settings = ServiceManager.getService(GitCommitMessageHelperSettings.class);
    }

    @NotNull
    @Override
    public String getId() {
        return "plugins.gitcommitmessagehelper";
    }


    @Nullable
    @Override
    public JComponent createComponent() {
        if (templateEditPane == null) {
            templateEditPane = new TemplateEditPane(settings);
        }
        return templateEditPane.getMainPenel();
    }

    @Override
    public boolean isModified() {
        return false;
    }


    @Nullable
    @Override
    public String getHelpTopic() {
        return "help.gitcommitmessagehelper.configuration";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitCommitMessageHelper";
    }


    @Override
    public void apply() throws ConfigurationException {

    }
}