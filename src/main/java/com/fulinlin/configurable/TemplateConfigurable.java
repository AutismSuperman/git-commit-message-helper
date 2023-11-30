package com.fulinlin.configurable;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.ui.setting.TemplateEditPanel;
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
public class TemplateConfigurable implements SearchableConfigurable {

    private TemplateEditPanel templateEditPanel;

    private GitCommitMessageHelperSettings settings;


    public TemplateConfigurable() {
        settings = ServiceManager.getService(GitCommitMessageHelperSettings.class);
    }

    @NotNull
    @Override
    public String getId() {
        return "plugins.gitcommitmessagehelper.template";
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
        return PluginBundle.get("setting.configurable.template");
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