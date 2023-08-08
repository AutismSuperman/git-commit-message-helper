package com.chivenh.storage;

import com.chivenh.constant.GitCommitConstants;
import com.chivenh.model.DataSettings;
import com.chivenh.model.TypeAlias;
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
 * @author Chivenh
 * @create: 2019-12-05 21:13
 **/
@State(name = "GitCommitMsgHelperSettings", storages = {@Storage("$APP_CONFIG$/GitCommitMsgHelperSettings-settings.xml")})
public class GitCommitMsgHelperSettings implements PersistentStateComponent<GitCommitMsgHelperSettings> {
    private static final Logger log = Logger.getInstance(GitCommitMsgHelperSettings.class);

    public GitCommitMsgHelperSettings() {
    }


    private DataSettings dataSettings;


    @Nullable
    @Override
    public GitCommitMsgHelperSettings getState() {
        if (this.dataSettings == null) {
            loadDefaultSettings();
        }
        return this;
    }


    @Override
    public void loadState(@NotNull GitCommitMsgHelperSettings gitCommitMessageHelperSettings) {
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
            typeAliases.add(new TypeAlias("feature", "新功能(A new feature)"));
            typeAliases.add(new TypeAlias("fix", "修复BUG(A bug fix)"));
            typeAliases.add(new TypeAlias("docs", "文档更新(Documentation only changes)"));
            typeAliases.add(new TypeAlias("style", "格式更新(Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc))"));
            typeAliases.add(new TypeAlias("refactor", "重构(A code change that neither fixes a bug nor adds a feature)"));
            typeAliases.add(new TypeAlias("perf", "优化相关(A code change that improves performance)"));
            typeAliases.add(new TypeAlias("test", "测试相关(Adding missing tests or correcting existing tests)"));
            typeAliases.add(new TypeAlias("build", "构建相关(Changes that affect the build system or external dependencies (example scopes: gulp, broccoli, npm))"));
            typeAliases.add(new TypeAlias("ci", "CI相关(Changes to our CI configuration files and scripts (example scopes: Travis, Circle, BrowserStack, SauceLabs))"));
            typeAliases.add(new TypeAlias("chore", "其它非源码或测试(Other changes that don't modify src or test files)"));
            typeAliases.add(new TypeAlias("revert", "回滚(Reverts a previous commit)"));
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
    public GitCommitMsgHelperSettings clone() {
        Cloner cloner = new Cloner();
        cloner.nullInsteadOfClone();
        return cloner.deepClone(this);
    }

}
