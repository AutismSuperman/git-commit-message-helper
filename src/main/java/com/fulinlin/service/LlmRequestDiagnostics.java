package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LlmRequestDiagnostics {

    private String provider = "";
    private String baseUrl = "";
    private String model = "";
    private String thinkingLevel = "";
    private boolean streamRequested;
    private boolean thinkingRequested;
    private boolean thinkingSkippedByCache;
    private boolean thinkingParametersApplied;
    private boolean thinkingFallbackUsed;
    private boolean completionTokenFallbackUsed;
    private boolean temperatureFallbackUsed;
    private boolean streamingFallbackUsed;
    private boolean streamingSkippedByCache;
    private int requestAttempts;
    private String tokenField = "";
    private int tokenLimit;
    private final Set<String> requestParameters = new LinkedHashSet<>();

    void recordRequest(@NotNull LlmProfile profile,
                       boolean stream,
                       boolean thinkingRequested,
                       boolean thinkingSkippedByCache,
                       @NotNull JsonObject requestBody) {
        provider = LlmProvider.fromNullable(profile.getProvider()).name();
        baseUrl = safe(profile.getBaseUrl());
        model = safe(profile.getModel());
        thinkingLevel = ThinkingLevel.fromNullable(profile.getThinkingLevel()).name();
        streamRequested = stream;
        this.thinkingRequested = thinkingRequested;
        this.thinkingSkippedByCache |= thinkingSkippedByCache;
        requestAttempts++;

        if (requestBody.has("max_completion_tokens")) {
            tokenField = "max_completion_tokens";
            tokenLimit = requestBody.get("max_completion_tokens").getAsInt();
        } else if (requestBody.has("max_output_tokens")) {
            tokenField = "max_output_tokens";
            tokenLimit = requestBody.get("max_output_tokens").getAsInt();
        } else if (requestBody.has("max_tokens")) {
            tokenField = "max_tokens";
            tokenLimit = requestBody.get("max_tokens").getAsInt();
        }

        recordIfPresent(requestBody, "enable_thinking");
        recordIfPresent(requestBody, "reasoning_effort");
        recordIfPresent(requestBody, "reasoning");
        recordIfPresent(requestBody, "thinking");
        recordIfPresent(requestBody, "output_config");
        thinkingParametersApplied = thinkingParametersApplied
                || requestBody.has("enable_thinking")
                || requestBody.has("reasoning_effort")
                || requestBody.has("reasoning")
                || requestBody.has("thinking")
                || requestBody.has("output_config");
    }

    void markThinkingFallbackUsed() {
        thinkingFallbackUsed = true;
    }

    void markCompletionTokenFallbackUsed() {
        completionTokenFallbackUsed = true;
    }

    void markTemperatureFallbackUsed() {
        temperatureFallbackUsed = true;
    }

    void markStreamingFallbackUsed() {
        streamingFallbackUsed = true;
    }

    void markStreamingSkippedByCache(@NotNull LlmProfile profile) {
        provider = LlmProvider.fromNullable(profile.getProvider()).name();
        baseUrl = safe(profile.getBaseUrl());
        model = safe(profile.getModel());
        streamRequested = true;
        streamingSkippedByCache = true;
    }

    @NotNull
    public String toUserSummary() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Provider", provider);
        appendLine(builder, "Base URL", baseUrl);
        appendLine(builder, "Model", model);
        appendLine(builder, "Mode", streamRequested ? "stream" : "non-stream");
        appendLine(builder, "Attempts", String.valueOf(requestAttempts));
        appendLine(builder, "Token field", emptyFallback(tokenField));
        appendLine(builder, "Token limit", tokenLimit > 0 ? String.valueOf(tokenLimit) : "none");
        appendLine(builder, "Thinking", formatThinking());
        appendLine(builder, "Extra params", requestParameters.isEmpty()
                ? "none"
                : requestParameters.stream().collect(Collectors.joining(", ")));
        if (thinkingFallbackUsed) {
            appendLine(builder, "Thinking fallback", "retried without thinking parameters");
        }
        if (completionTokenFallbackUsed) {
            appendLine(builder, "Token fallback", "retried with max_tokens");
        }
        if (temperatureFallbackUsed) {
            appendLine(builder, "Temperature fallback", "retried without temperature");
        }
        if (streamingFallbackUsed) {
            appendLine(builder, "Streaming fallback", "used non-stream response");
        }
        if (streamingSkippedByCache) {
            appendLine(builder, "Streaming cache", "streaming skipped for this model");
        }
        return builder.toString().trim();
    }

    private void recordIfPresent(@NotNull JsonObject requestBody, @NotNull String parameter) {
        if (requestBody.has(parameter)) {
            requestParameters.add(parameter);
        }
    }

    @NotNull
    private String formatThinking() {
        String level = emptyFallback(thinkingLevel).toLowerCase();
        if (!thinkingRequested) {
            return "off (default)";
        }
        if (thinkingSkippedByCache) {
            return level + ", skipped by cache";
        }
        return level + (thinkingParametersApplied ? ", applied" : ", no extra params needed");
    }

    @NotNull
    private static String emptyFallback(String value) {
        return value == null || value.trim().isEmpty() ? "none" : value;
    }

    private static void appendLine(@NotNull StringBuilder builder, @NotNull String name, @NotNull String value) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(name).append(": ").append(value);
    }

    @NotNull
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
