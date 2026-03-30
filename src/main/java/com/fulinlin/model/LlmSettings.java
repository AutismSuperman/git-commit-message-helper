package com.fulinlin.model;

public class LlmSettings {

    private String baseUrl;

    private String apiKey;

    private String model;

    private Double temperature;

    private String responseLanguage;

    private Boolean smartEchoEnabled;

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
}
