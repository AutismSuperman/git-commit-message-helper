package com.fulinlin.storage;

import com.fulinlin.constant.GitCommitConstants;
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
@State(name = "GitCommitMessageHelperSettings", storages = {@Storage("$APP_CONFIG$/GitCommitMessageHelperSettings-settings.xml")})
public class GitCommitMessageHelperSettings implements PersistentStateComponent<GitCommitMessageHelperSettings> {
    private static final Logger log = Logger.getInstance(GitCommitMessageHelperSettings.class);

    public GitCommitMessageHelperSettings() {
    }


    private DataSettings dataSettings;


    @Nullable
    @Override
    public GitCommitMessageHelperSettings getState() {
        if (this.dataSettings == null) {
            loadDefaultSettings();
        }
        return this;
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
            typeAliases.add(new TypeAlias("feat", "A new feature"));
            typeAliases.add(new TypeAlias("fix", "A bug fix"));
            typeAliases.add(new TypeAlias("docs", "Documentation only changes"));
            typeAliases.add(new TypeAlias("style", "Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)"));
            typeAliases.add(new TypeAlias("refactor", "A code change that neither fixes a bug nor adds a feature"));
            typeAliases.add(new TypeAlias("perf", "A code change that improves performance"));
            typeAliases.add(new TypeAlias("test", "Adding missing tests or correcting existing tests"));
            typeAliases.add(new TypeAlias("build", "Changes that affect the build system or external dependencies (example scopes: gulp, broccoli, npm)"));
            typeAliases.add(new TypeAlias("ci", "Changes to our CI configuration files and scripts (example scopes: Travis, Circle, BrowserStack, SauceLabs)"));
            typeAliases.add(new TypeAlias("chore", "Other changes that don't modify src or test files"));
            typeAliases.add(new TypeAlias("revert", "Reverts a previous commit"));
            dataSettings.setTypeAliases(typeAliases);
        } catch (Exception e) {
            log.error("loadDefaultSettings failed", e);
        }
    }

    /**
     * Getter method for property <tt>codeTemplates</tt>.
     *
     * @return property value of codeTemplates
     */
    public DataSettings getDateSettings() {
        if (dataSettings == null) {
            loadDefaultSettings();
        }
        return dataSettings;
    }


    public void setDateSettings(DataSettings dateSettings) {
        this.dataSettings = dateSettings;
    }


    public void updateTemplate(String template) {
        dataSettings.setTemplate(template);
    }

    public void updateTypeMap(List<TypeAlias> typeAliases) {
        dataSettings.setTypeAliases(typeAliases);
    }


    @Override
    public GitCommitMessageHelperSettings clone() {
        Cloner cloner = new Cloner();
        cloner.nullInsteadOfClone();
        return cloner.deepClone(this);
    }

}
