package com.fulinlin.model;

import java.util.List;
import java.util.Objects;

public class LlmSettings {

    /**
     * Character budget for the git diff sent to the model. Large changes get truncated below
     * this limit, so it is configurable; the default keeps ordinary requests small and cheap.
     */
    public static final int DEFAULT_MAX_DIFF_LENGTH = 12000;
    public static final int MIN_MAX_DIFF_LENGTH = 2000;
    public static final int MAX_MAX_DIFF_LENGTH = 200000;

    private String baseUrl;

    private String apiKey;

    private String model;

    private Double temperature;

    private String responseLanguage;

    private Boolean smartEchoEnabled;

    private Boolean streamingResponseEnabled;

    private Integer maxDiffLength;

    private String activeProfileId;

    private List<LlmProfile> profiles;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getResponseLanguage() {
        return responseLanguage;
    }

    public void setResponseLanguage(String responseLanguage) {
        this.responseLanguage = responseLanguage;
    }

    public Boolean getSmartEchoEnabled() {
        return smartEchoEnabled;
    }

    public void setSmartEchoEnabled(Boolean smartEchoEnabled) {
        this.smartEchoEnabled = smartEchoEnabled;
    }

    public Boolean getStreamingResponseEnabled() {
        return streamingResponseEnabled;
    }

    public void setStreamingResponseEnabled(Boolean streamingResponseEnabled) {
        this.streamingResponseEnabled = streamingResponseEnabled;
    }

    public Integer getMaxDiffLength() {
        return maxDiffLength;
    }

    public void setMaxDiffLength(Integer maxDiffLength) {
        this.maxDiffLength = maxDiffLength;
    }

    /**
     * Diff character budget to use when collecting git context. Null means the profile never
     * configured one, so the default applies; out-of-range values are clamped rather than
     * rejected so a hand-edited settings file cannot break prompt assembly.
     */
    public static int resolveMaxDiffLength(Integer configured) {
        if (configured == null) {
            return DEFAULT_MAX_DIFF_LENGTH;
        }
        return Math.max(MIN_MAX_DIFF_LENGTH, Math.min(MAX_MAX_DIFF_LENGTH, configured));
    }

    public int resolveMaxDiffLength() {
        return resolveMaxDiffLength(maxDiffLength);
    }

    public String getActiveProfileId() {
        return activeProfileId;
    }

    public void setActiveProfileId(String activeProfileId) {
        this.activeProfileId = activeProfileId;
    }

    public List<LlmProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<LlmProfile> profiles) {
        this.profiles = profiles;
    }

    public LlmProfile getActiveProfile() {
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        if (activeProfileId != null) {
            for (LlmProfile profile : profiles) {
                if (activeProfileId.equals(profile.getId())) {
                    return profile;
                }
            }
        }
        return profiles.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LlmSettings)) return false;
        LlmSettings that = (LlmSettings) o;
        return Objects.equals(baseUrl, that.baseUrl)
                && Objects.equals(apiKey, that.apiKey)
                && Objects.equals(model, that.model)
                && Objects.equals(temperature, that.temperature)
                && Objects.equals(responseLanguage, that.responseLanguage)
                && Objects.equals(smartEchoEnabled, that.smartEchoEnabled)
                && Objects.equals(streamingResponseEnabled, that.streamingResponseEnabled)
                && Objects.equals(maxDiffLength, that.maxDiffLength)
                && Objects.equals(activeProfileId, that.activeProfileId)
                && Objects.equals(profiles, that.profiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, apiKey, model, temperature, responseLanguage, smartEchoEnabled, streamingResponseEnabled, maxDiffLength, activeProfileId, profiles);
    }
}
