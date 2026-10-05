package com.fulinlin.model;

import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;

import java.util.Objects;

public class LlmProfile {

    /**
     * Response budget used when the profile does not override it. Reasoning models and long
     * diffs routinely need more than this, which is why it is configurable per profile.
     */
    public static final int DEFAULT_MAX_RESPONSE_TOKENS = 4096;
    public static final int MIN_MAX_RESPONSE_TOKENS = 256;
    public static final int MAX_MAX_RESPONSE_TOKENS = 128000;

    private String id;

    private String name;

    private String baseUrl;

    private String apiKey;

    private String model;

    private LlmProvider provider;

    private ThinkingLevel thinkingLevel;

    /**
     * Legacy 1.6.4 flag that only switched reasoning off. Settings files written before thinking
     * levels existed are migrated to {@link ThinkingLevel#DISABLED} on load and the field is
     * cleared so the new level becomes the single source of truth.
     */
    @Deprecated
    private Boolean reasoningCompatibilityEnabled;

    private Integer maxResponseTokens;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public LlmProvider getProvider() {
        return provider;
    }

    public void setProvider(LlmProvider provider) {
        this.provider = provider;
    }

    public ThinkingLevel getThinkingLevel() {
        return thinkingLevel;
    }

    public void setThinkingLevel(ThinkingLevel thinkingLevel) {
        this.thinkingLevel = thinkingLevel;
    }

    @Deprecated
    public Boolean getReasoningCompatibilityEnabled() {
        return reasoningCompatibilityEnabled;
    }

    @Deprecated
    public void setReasoningCompatibilityEnabled(Boolean reasoningCompatibilityEnabled) {
        this.reasoningCompatibilityEnabled = reasoningCompatibilityEnabled;
    }

    public Integer getMaxResponseTokens() {
        return maxResponseTokens;
    }

    public void setMaxResponseTokens(Integer maxResponseTokens) {
        this.maxResponseTokens = maxResponseTokens;
    }

    /**
     * Response token budget to request from the provider. A null value means the profile never
     * configured one, so the default applies; out-of-range values are clamped rather than
     * rejected so a hand-edited settings file cannot produce an unusable request.
     */
    public static int resolveMaxResponseTokens(Integer configured) {
        if (configured == null) {
            return DEFAULT_MAX_RESPONSE_TOKENS;
        }
        return Math.max(MIN_MAX_RESPONSE_TOKENS, Math.min(MAX_MAX_RESPONSE_TOKENS, configured));
    }

    public int resolveMaxResponseTokens() {
        return resolveMaxResponseTokens(maxResponseTokens);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LlmProfile)) return false;
        LlmProfile that = (LlmProfile) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(baseUrl, that.baseUrl)
                && Objects.equals(apiKey, that.apiKey)
                && Objects.equals(model, that.model)
                && provider == that.provider
                && Objects.equals(thinkingLevel, that.thinkingLevel)
                && Objects.equals(maxResponseTokens, that.maxResponseTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, baseUrl, apiKey, model, provider, thinkingLevel, maxResponseTokens);
    }
}
