package com.fulinlin.model.enums;

import com.fulinlin.localization.PluginBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API format used to talk to a model endpoint. The values mirror the three protocols ZCode
 * supports for custom providers: Anthropic Messages, OpenAI Chat Completions, and OpenAI
 * Responses.
 */
public enum LlmProvider {
    OPENAI_COMPATIBLE("setting.llm.provider.openai", "setting.llm.provider.openai.hint",
            "https://api.openai.com/v1", "/chat/completions"),
    ANTHROPIC("setting.llm.provider.anthropic", "setting.llm.provider.anthropic.hint",
            "https://api.anthropic.com", "/v1/messages"),
    OPENAI_RESPONSES("setting.llm.provider.responses", "setting.llm.provider.responses.hint",
            "https://api.openai.com/v1", "/responses");

    private final String displayKey;
    private final String descriptionKey;
    private final String defaultBaseUrl;
    private final String endpointPath;

    LlmProvider(String displayKey, String descriptionKey, String defaultBaseUrl, String endpointPath) {
        this.displayKey = displayKey;
        this.descriptionKey = descriptionKey;
        this.defaultBaseUrl = defaultBaseUrl;
        this.endpointPath = endpointPath;
    }

    @NotNull
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * Path appended to a server base URL when the profile points at a bare host or {@code /v1}.
     */
    @NotNull
    public String getEndpointPath() {
        return endpointPath;
    }

    @NotNull
    public String getDisplayName() {
        return PluginBundle.get(displayKey);
    }

    /**
     * Short description of the request fields this format sends, shown as a tooltip so users can
     * pick the right protocol for a shared gateway.
     */
    @NotNull
    public String getDescription() {
        return PluginBundle.get(descriptionKey);
    }

    @NotNull
    public static LlmProvider defaultProvider() {
        return OPENAI_COMPATIBLE;
    }

    @NotNull
    public static LlmProvider fromNullable(@Nullable LlmProvider provider) {
        return provider == null ? defaultProvider() : provider;
    }
}
