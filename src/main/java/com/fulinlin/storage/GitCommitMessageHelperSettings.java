package com.fulinlin.storage;

import com.fulinlin.constant.GitCommitConstants;
import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.ActionSettings;
import com.fulinlin.model.CentralSettings;
import com.fulinlin.model.DataSettings;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.model.enums.TypeDisplayStyleEnum;
import com.intellij.openapi.application.ApplicationManager;
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
    private static final double MIN_TEMPERATURE = 0.0D;
    private static final double MAX_TEMPERATURE = 2.0D;
    private static final Logger log = Logger.getInstance(GitCommitMessageHelperSettings.class);
    private DataSettings dataSettings;

    private CentralSettings centralSettings;

    public GitCommitMessageHelperSettings() {
    }

    @NotNull
    public static GitCommitMessageHelperSettings getInstance() {
        return ApplicationManager.getApplication().getService(GitCommitMessageHelperSettings.class);
    }

    @Nullable
    @Override
    public GitCommitMessageHelperSettings getState() {
        if (this.dataSettings == null) {
            loadDefaultDataSettings();
        } else {
            checkDefaultDataSettings(dataSettings);
        }
        if (centralSettings == null) {
            loadDefaultCentralSettings();
        } else {
            checkDefaultCentralSettings(centralSettings);
        }
        return this;
    }

    @Override
    public void loadState(@NotNull GitCommitMessageHelperSettings gitCommitMessageHelperSettings) {
        XmlSerializerUtil.copyBean(gitCommitMessageHelperSettings, this);
    }


    public CentralSettings getCentralSettings() {
        if (centralSettings == null) {
            loadDefaultCentralSettings();
        } else {
            checkDefaultCentralSettings(centralSettings);
        }
        return centralSettings;
    }

    /**
     * Spelling error here, in order to maintain the current status of existing user data
     */
    public DataSettings getDateSettings() {
        if (dataSettings == null) {
            loadDefaultDataSettings();
        } else {
            checkDefaultDataSettings(dataSettings);
        }
        return dataSettings;
    }


    private void loadDefaultCentralSettings() {
        centralSettings = new CentralSettings();
        try {
            centralSettings.setTypeDisplayStyle(TypeDisplayStyleEnum.CHECKBOX);
            centralSettings.setTypeDisplayNumber(-1);
            centralSettings.setSkipCiDefaultValue("[skip ci]");
            centralSettings.setSkipCiDefaultApprove(Boolean.FALSE);
            centralSettings.setSkipCiComboboxEnable(Boolean.FALSE);
            CentralSettings.Hidden hidden = new CentralSettings.Hidden();
            centralSettings.setHidden(hidden);
            centralSettings.getHidden().setType(Boolean.FALSE);
            centralSettings.getHidden().setScope(Boolean.FALSE);
            centralSettings.getHidden().setSubject(Boolean.FALSE);
            centralSettings.getHidden().setBody(Boolean.FALSE);
            centralSettings.getHidden().setClosed(Boolean.FALSE);
            centralSettings.getHidden().setChanges(Boolean.FALSE);
            centralSettings.getHidden().setSkipCi(Boolean.FALSE);
            centralSettings.setLlmSettings(createDefaultLlmSettings());
            ActionSettings actionSettings = new ActionSettings();
            actionSettings.setCreateCommitActionVisible(Boolean.TRUE);
            actionSettings.setGenerateCommitActionVisible(Boolean.TRUE);
            actionSettings.setFormatCommitActionVisible(Boolean.TRUE);
            centralSettings.setActionSettings(actionSettings);
        } catch (Exception e) {
            log.error("loadDefaultCentralSettings failed", e);
        }
    }

    private void checkDefaultCentralSettings(CentralSettings settings) {
        if (settings.getHidden() == null) {
            CentralSettings.Hidden hidden = new CentralSettings.Hidden();
            hidden.setType(Boolean.FALSE);
            hidden.setScope(Boolean.FALSE);
            hidden.setSubject(Boolean.FALSE);
            hidden.setBody(Boolean.FALSE);
            hidden.setClosed(Boolean.FALSE);
            hidden.setChanges(Boolean.FALSE);
            hidden.setSkipCi(Boolean.FALSE);
            settings.setHidden(hidden);
        }
        if (settings.getLlmSettings() == null) {
            settings.setLlmSettings(createDefaultLlmSettings());
        } else {
            checkDefaultLlmSettings(settings.getLlmSettings());
        }
        if (settings.getActionSettings() == null) {
            ActionSettings actionSettings = new ActionSettings();
            actionSettings.setCreateCommitActionVisible(Boolean.TRUE);
            actionSettings.setGenerateCommitActionVisible(Boolean.TRUE);
            actionSettings.setFormatCommitActionVisible(Boolean.TRUE);
            settings.setActionSettings(actionSettings);
        } else {
            if (settings.getActionSettings().getCreateCommitActionVisible() == null) {
                settings.getActionSettings().setCreateCommitActionVisible(Boolean.TRUE);
            }
            if (settings.getActionSettings().getGenerateCommitActionVisible() == null) {
                settings.getActionSettings().setGenerateCommitActionVisible(Boolean.TRUE);
            }
            if (settings.getActionSettings().getFormatCommitActionVisible() == null) {
                settings.getActionSettings().setFormatCommitActionVisible(Boolean.TRUE);
            }
        }
    }


    private void loadDefaultDataSettings() {
        dataSettings = new DataSettings();
        try {
            dataSettings.setTemplate(GitCommitConstants.DEFAULT_TEMPLATE);
            List<TypeAlias> typeAliases = createDefaultTypeAliases();
            dataSettings.setTypeAliases(typeAliases);
            List<String> skipCis = getSkipCis();
            dataSettings.setSkipCis(skipCis);
        } catch (Exception e) {
            log.error("loadDefaultDataSettings failed", e);
        }
    }

    private void checkDefaultDataSettings(DataSettings dataSettings) {
        if (dataSettings.getTemplate() == null) {
            dataSettings.setTemplate(GitCommitConstants.DEFAULT_TEMPLATE);
        }
        if (dataSettings.getTypeAliases() == null) {
            List<TypeAlias> typeAliases = createDefaultTypeAliases();
            dataSettings.setTypeAliases(typeAliases);
        }
        if (dataSettings.getSkipCis() == null) {
            List<String> skipCis = getSkipCis();
            dataSettings.setSkipCis(skipCis);
        }
    }


    @NotNull
    private static List<String> getSkipCis() {
        List<String> skipCis = new LinkedList<>();
        skipCis.add("[skip ci]");
        skipCis.add("[ci skip]");
        skipCis.add("[no ci]");
        skipCis.add("[skip actions]");
        skipCis.add("[actions skip]");
        skipCis.add("skip-checks:true");
        skipCis.add("skip-checks: true");
        return skipCis;
    }

    @NotNull
    public static List<TypeAlias> createDefaultTypeAliases() {
        List<TypeAlias> typeAliases = new LinkedList<>();
        // default init i18n
        typeAliases.add(new TypeAlias("feat", PluginBundle.get("feat.description")));
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
        return typeAliases;
    }


    public void updateTemplate(String template) {
        dataSettings.setTemplate(template);
    }

    public void updateTypeMap(List<TypeAlias> typeAliases) {
        dataSettings.setTypeAliases(typeAliases);
    }

    /**
     * Spelling error here, in order to maintain the current status of existing user data
     */
    public void setDateSettings(DataSettings dateSettings) {
        this.dataSettings = dateSettings;
    }

    public void setCentralSettings(CentralSettings centralSettings) {
        this.centralSettings = centralSettings;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public GitCommitMessageHelperSettings clone() {
        Cloner cloner = new Cloner();
        cloner.nullInsteadOfClone();
        return cloner.deepClone(this);
    }

    @NotNull
    public static LlmSettings createDefaultLlmSettings() {
        LlmSettings llmSettings = new LlmSettings();
        List<LlmProfile> profiles = new LinkedList<>();
        LlmProfile profile = createDefaultLlmProfile();
        profiles.add(profile);
        llmSettings.setActiveProfileId(profile.getId());
        llmSettings.setProfiles(profiles);
        llmSettings.setTemperature(normalizeTemperature(0.5D));
        llmSettings.setResponseLanguage("English");
        llmSettings.setSmartEchoEnabled(Boolean.FALSE);
        llmSettings.setStreamingResponseEnabled(Boolean.TRUE);
        syncLegacyLlmFields(llmSettings, profile);
        return llmSettings;
    }

    @NotNull
    public static LlmProfile createDefaultLlmProfile() {
        LlmProfile profile = new LlmProfile();
        profile.setId("default");
        profile.setName("Default");
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setApiKey("");
        profile.setModel("");
        return profile;
    }

    public static void checkDefaultLlmSettings(@NotNull LlmSettings llmSettings) {
        if (llmSettings.getProfiles() == null || llmSettings.getProfiles().isEmpty()) {
            LlmProfile profile = createDefaultLlmProfile();
            profile.setBaseUrl(defaultString(llmSettings.getBaseUrl(), profile.getBaseUrl()));
            profile.setApiKey(defaultString(llmSettings.getApiKey(), profile.getApiKey()));
            profile.setModel(defaultString(llmSettings.getModel(), profile.getModel()));
            List<LlmProfile> profiles = new LinkedList<>();
            profiles.add(profile);
            llmSettings.setProfiles(profiles);
            llmSettings.setActiveProfileId(profile.getId());
        }
        llmSettings.setTemperature(normalizeTemperature(llmSettings.getTemperature()));
        if (llmSettings.getResponseLanguage() == null) {
            llmSettings.setResponseLanguage("English");
        }
        if (llmSettings.getSmartEchoEnabled() == null) {
            llmSettings.setSmartEchoEnabled(Boolean.FALSE);
        }
        if (llmSettings.getStreamingResponseEnabled() == null) {
            llmSettings.setStreamingResponseEnabled(Boolean.TRUE);
        }
        for (LlmProfile profile : llmSettings.getProfiles()) {
            checkDefaultLlmProfile(profile);
        }
        LlmProfile activeProfile = llmSettings.getActiveProfile();
        if (activeProfile == null) {
            LlmProfile profile = createDefaultLlmProfile();
            llmSettings.getProfiles().add(profile);
            llmSettings.setActiveProfileId(profile.getId());
            activeProfile = profile;
        } else if (llmSettings.getActiveProfileId() == null || llmSettings.getActiveProfileId().trim().isEmpty()) {
            llmSettings.setActiveProfileId(activeProfile.getId());
        }
        syncLegacyLlmFields(llmSettings, activeProfile);
    }

    public static void checkDefaultLlmProfile(@NotNull LlmProfile profile) {
        if (profile.getId() == null || profile.getId().trim().isEmpty()) {
            profile.setId("profile-" + System.nanoTime());
        }
        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            profile.setName("Default");
        }
        if (profile.getBaseUrl() == null) {
            profile.setBaseUrl("https://api.openai.com/v1");
        }
        if (profile.getApiKey() == null) {
            profile.setApiKey("");
        }
        if (profile.getModel() == null) {
            profile.setModel("");
        }
    }

    private static void syncLegacyLlmFields(@NotNull LlmSettings llmSettings, @NotNull LlmProfile profile) {
        llmSettings.setBaseUrl(profile.getBaseUrl());
        llmSettings.setApiKey(profile.getApiKey());
        llmSettings.setModel(profile.getModel());
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static Boolean defaultBoolean(Boolean value, Boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static double normalizeTemperature(Double temperature) {
        if (temperature == null) {
            return 0.5D;
        }
        return Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, temperature));
    }

}
