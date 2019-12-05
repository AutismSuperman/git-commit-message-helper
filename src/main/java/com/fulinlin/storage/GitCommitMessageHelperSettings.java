package com.fulinlin.storage;

import com.fulinlin.pojo.DataSettings;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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
    public void loadState(@NotNull GitCommitMessageHelperSettings state) {
        XmlSerializerUtil.copyBean(dataSettings, this);
    }

    /**
     * 加载默认配置
     */
    private void loadDefaultSettings() {
        dataSettings = new DataSettings();
        try {
            String velocityTemplate = FileUtil.loadTextAndClose(GitCommitMessageHelperSettings.class.getResourceAsStream("/template/" + "defaultTemplate.vm"));
            dataSettings.setTemplate(velocityTemplate);
            Map<String, String> codeTemplates = new LinkedHashMap<>();
            codeTemplates.put("feature", "A new feature");
            codeTemplates.put("fix", "A bug fix");
            dataSettings.setTypeMap(codeTemplates);
        } catch (IOException e) {
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
        DataSettings newDateSettings = new DataSettings();
        newDateSettings.setTemplate(dataSettings.getTemplate());
        newDateSettings.setTypeMap(dataSettings.getTypeMap());
        return newDateSettings;
    }

    public void setDateSettings(DataSettings dateSettings) {
        this.dataSettings = dateSettings;
    }


    public void updateTemplate(String template) {
        dataSettings.setTemplate(template);
    }

    public void updateTypeMap(LinkedHashMap<String, String> linkedHashMap) {
        dataSettings.setTypeMap(linkedHashMap);
    }
}
