package com.fulinlin.ui.setting;

import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 这个类Settings 中的属性被创建的时候
 *
 * @author: fulin
 */
public class TemplateEditPaneConfigurable implements SearchableConfigurable {

    private TemplateEditPanel templateEditPanel;

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
        if (templateEditPanel == null) {
            templateEditPanel = new TemplateEditPanel(settings);
        }
        return templateEditPanel.getMainPanel();
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


    public void reset() {
        if (templateEditPanel != null) {
            templateEditPanel.reset(settings);
        }
    }

    @Override
    public boolean isModified() {
        return templateEditPanel != null && templateEditPanel.isSettingsModified(settings);
    }


    @Override
    public void apply() {
        settings.setDateSettings(templateEditPanel.getSettings().getDateSettings());
        settings = templateEditPanel.getSettings().clone();
    }
}