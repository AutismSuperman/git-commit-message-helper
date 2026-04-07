package com.fulinlin.configurable;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.ui.central.LlmSettingPanel;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LlmSettingConfigurable implements SearchableConfigurable {

    private LlmSettingPanel llmSettingPanel;

    private GitCommitMessageHelperSettings settings;

    public LlmSettingConfigurable() {
        settings = GitCommitMessageHelperSettings.getInstance();
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "plugins.gitcommitmessagehelper.llm";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (llmSettingPanel == null) {
            llmSettingPanel = new LlmSettingPanel(settings);
        }
        return llmSettingPanel.getMainPanel();
    }

    @Override
    public void reset() {
        if (llmSettingPanel != null) {
            llmSettingPanel.reset(settings);
        }
    }

    @Override
    public boolean isModified() {
        return llmSettingPanel != null && llmSettingPanel.isModified(settings);
    }

    @Override
    public void apply() {
        GitCommitMessageHelperSettings panelSettings = llmSettingPanel.getSettings();
        GitCommitMessageHelperSettings applicationSettings = GitCommitMessageHelperSettings.getInstance();
        applicationSettings.getCentralSettings().setLlmSettings(panelSettings.getCentralSettings().getLlmSettings());
        settings = applicationSettings;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return PluginBundle.get("setting.configurable.llm");
    }
}
