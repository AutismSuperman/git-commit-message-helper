package com.fulinlin.storage;

import com.fulinlin.constant.GitCommitConstants;
import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.DataSettings;
import com.fulinlin.model.TypeAlias;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.rits.cloning.Cloner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-05 21:13
 **/
@State(name = "GitCommitMessageHelperSettings",
        storages = {@Storage(value = GitCommitConstants.ACTION_PREFIX + "-settings.xml")})
public class GitCommitMessageHelperSettings implements PersistentStateComponent<GitCommitMessageHelperSettings> {
    private static final Logger log = Logger.getInstance(GitCommitMessageHelperSettings.class);
    private DataSettings dataSettings;

    public GitCommitMessageHelperSettings() {
    }

    @Nullable
    @Override
    public GitCommitMessageHelperSettings getState() {
        if (this.dataSettings == null) {
            loadDefaultSettings();
        }
        return this;
    }

    public DataSettings getDateSettings() {
        if (dataSettings == null) {
            loadDefaultSettings();
        }
        return dataSettings;
    }

    public void setDateSettings(DataSettings dateSettings) {
        this.dataSettings = dateSettings;
    }

    @Override
    public void loadState(@NotNull GitCommitMessageHelperSettings gitCommitMessageHelperSettings) {
        XmlSerializerUtil.copyBean(gitCommitMessageHelperSettings, this);
    }

    /**
     * 加载默认配置
     */
    private void loadDefaultSettings() {
        dataSettings = new DataSettings();
        try {
            dataSettings.setTemplate(GitCommitConstants.DEFAULT_TEMPLATE);
            List<TypeAlias> typeAliases = new LinkedList<>();
            // default init i18n
            typeAliases.add(new TypeAlias("feature", PluginBundle.get("feature.description")));
            typeAliases.add(new TypeAlias("fix", PluginBundle.get("fix.description")));
            typeAliases.add(new TypeAlias("docs", PluginBundle.get("docs.description")));
            typeAliases.add(new TypeAlias("style", PluginBundle.get("style.description")));
            typeAliases.add(new TypeAlias("refactor", PluginBundle.get("refactor.description")));
            typeAliases.add(new TypeAlias("perf", PluginBundle.get("perf.description")));
            typeAliases.add(new TypeAlias("test", PluginBundle.get("test.description")));
            typeAliases.add(new TypeAlias("build", PluginBundle.get("build.description")));
            typeAliases.add(new TypeAlias("ci", PluginBundle.get("ci.description")));
            typeAliases.add(new TypeAlias("chore", PluginBundle.get("chore.description")));
            typeAliases.add(new TypeAlias("revert", PluginBundle.get("revert.description")));
            dataSettings.setTypeAliases(typeAliases);
        } catch (Exception e) {
            log.error("loadDefaultSettings failed", e);
        }
    }

    public void updateTemplate(String template) {
        dataSettings.setTemplate(template);
    }

    public void updateTypeMap(List<TypeAlias> typeAliases) {
        dataSettings.setTypeAliases(typeAliases);
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public GitCommitMessageHelperSettings clone() {
        Cloner cloner = new Cloner();
        cloner.nullInsteadOfClone();
        return cloner.deepClone(this);
    }

}
