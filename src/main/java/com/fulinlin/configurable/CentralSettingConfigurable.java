package com.fulinlin.configurable;

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

    @Override
    public @NotNull @NonNls String getId() {
        return "plugins.gitcommitmessagehelper";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (centralSettingPanel == null) {
            centralSettingPanel = new CentralSettingPanel();
        }
        return centralSettingPanel.getMainPanel();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitCommitMessageHelper";
    }
}
