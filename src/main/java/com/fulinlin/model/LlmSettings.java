package com.fulinlin.model;

import java.util.List;
import java.util.Objects;

public class LlmSettings {

    private String baseUrl;

    private String apiKey;

    private String model;

    private Double temperature;

    private String responseLanguage;

    private Boolean smartEchoEnabled;

    private Boolean streamingResponseEnabled;

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
                && Objects.equals(activeProfileId, that.activeProfileId)
                && Objects.equals(profiles, that.profiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, apiKey, model, temperature, responseLanguage, smartEchoEnabled, streamingResponseEnabled, activeProfileId, profiles);
    }
}
