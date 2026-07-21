package com.fulinlin.configurable;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.ui.setting.PromptEditPanel;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;

public class PromptConfigurable implements SearchableConfigurable {
    private PromptEditPanel panel;
    private GitCommitMessageHelperSettings settings = GitCommitMessageHelperSettings.getInstance();
    @Override public @NotNull String getId(){return "plugins.gitcommitmessagehelper.prompt";}
    @Override public String getDisplayName(){return PluginBundle.get("setting.configurable.prompt");}
    @Override public JComponent createComponent(){if(panel==null)panel=new PromptEditPanel(settings);return panel.getMainPanel();}
    @Override public void reset(){if(panel!=null)panel.reset(settings);}
    @Override public boolean isModified(){return panel!=null&&panel.isModified(settings);}
    @Override public void apply(){settings=panel.getSettings(); GitCommitMessageHelperSettings.getInstance().setDateSettings(settings.getDateSettings());}
    @Override public void disposeUIResources(){panel=null;}
}
