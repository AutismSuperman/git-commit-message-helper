package com.fulinlin.storage;

import com.fulinlin.constant.GitCommitConstants;
import com.fulinlin.model.DataSettings;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.utils.I18nUtil;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
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
@State(name = "GitCommitMessageHelperSettings" + GitCommitConstants.ACTION_SUFFIX,
        storages = {@Storage(value = GitCommitConstants.ACTION_PREFIX + "-settings.xml")})
public class GitCommitMessageHelperSettings implements PersistentStateComponent<GitCommitMessageHelperSettings> {
    private static final Logger log = Logger.getInstance(GitCommitMessageHelperSettings.class);
    private DataSettings dataSettings;


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
            typeAliases.add(new TypeAlias("feature", I18nUtil.getInfo("feature.description")));
            typeAliases.add(new TypeAlias("fix", I18nUtil.getInfo("fix.description")));
            typeAliases.add(new TypeAlias("docs", I18nUtil.getInfo("docs.description")));
            typeAliases.add(new TypeAlias("style", I18nUtil.getInfo("style.description")));
            typeAliases.add(new TypeAlias("refactor", I18nUtil.getInfo("refactor.description")));
            typeAliases.add(new TypeAlias("perf", I18nUtil.getInfo("perf.description")));
            typeAliases.add(new TypeAlias("test", I18nUtil.getInfo("test.description")));
            typeAliases.add(new TypeAlias("build", I18nUtil.getInfo("build.description")));
            typeAliases.add(new TypeAlias("ci", I18nUtil.getInfo("ci.description")));
            typeAliases.add(new TypeAlias("chore", I18nUtil.getInfo("chore.description")));
            typeAliases.add(new TypeAlias("revert", I18nUtil.getInfo("revert.description")));
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
