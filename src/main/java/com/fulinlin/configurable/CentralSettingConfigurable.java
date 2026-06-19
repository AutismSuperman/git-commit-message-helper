package com.fulinlin.configurable;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.ui.central.CentralSettingPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CentralSettingConfigurable implements SearchableConfigurable {

    private CentralSettingPanel centralSettingPanel;

    private GitCommitMessageHelperSettings settings;

    public CentralSettingConfigurable() {
        settings = GitCommitMessageHelperSettings.getInstance();
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "plugins.gitcommitmessagehelper";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (centralSettingPanel == null) {
            centralSettingPanel = new CentralSettingPanel(settings);
        }
        return centralSettingPanel.getMainPanel();
    }

    @Override
    public void reset() {
        centralSettingPanel.reset(settings);
    }

    @Override
    public boolean isModified() {
        return centralSettingPanel.isModified(settings);
    }

    @Override
    public void apply() {
        GitCommitMessageHelperSettings panelSettings = centralSettingPanel.getSettings();
        panelSettings.getCentralSettings().setLlmSettings(settings.getCentralSettings().getLlmSettings());
        GitCommitMessageHelperSettings applicationSettings = GitCommitMessageHelperSettings.getInstance();
        applicationSettings.setCentralSettings(panelSettings.getCentralSettings());
        settings = applicationSettings;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return PluginBundle.get("setting.configurable.main");
    }
}
