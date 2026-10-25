package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.model.enums.ThinkingParameterPreset;
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
    private ThinkingParameterPreset thinkingPreset = ThinkingParameterPreset.MODEL_DEFAULT;
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
        thinkingPreset = ThinkingParameterPreset.fromNullable(profile.getThinkingParameterPreset());
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

        for (String parameter : requestBody.keySet()) {
            if (!isStandardRequestField(parameter)) {
                requestParameters.add(parameter);
            }
        }
        thinkingParametersApplied = thinkingParametersApplied || !requestParameters.isEmpty();
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
        appendLine(builder, "Thinking preset", thinkingPreset.name().toLowerCase());
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

    private static boolean isStandardRequestField(@NotNull String parameter) {
        return "model".equals(parameter)
                || "messages".equals(parameter)
                || "system".equals(parameter)
                || "input".equals(parameter)
                || "instructions".equals(parameter)
                || "stream".equals(parameter)
                || "temperature".equals(parameter)
                || "max_tokens".equals(parameter)
                || "max_completion_tokens".equals(parameter)
                || "max_output_tokens".equals(parameter);
    }

    @NotNull
    private String formatThinking() {
        String level = emptyFallback(thinkingLevel).toLowerCase();
        if ("default".equals(level) || thinkingPreset == ThinkingParameterPreset.MODEL_DEFAULT) {
            return "model default (no override)";
        }
        if (!thinkingRequested) {
            return level + ", not sent (unmapped or invalid)";
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
