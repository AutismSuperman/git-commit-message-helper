package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LlmRequestDiagnostics {

    private String provider = "";
    private String baseUrl = "";
    private String model = "";
    private boolean streamRequested;
    private boolean reasoningCompatibilityRequested;
    private boolean reasoningCompatibilitySkippedByCache;
    private boolean reasoningCompatibilityApplied;
    private boolean compatibilityFallbackUsed;
    private boolean streamingFallbackUsed;
    private boolean streamingSkippedByCache;
    private int requestAttempts;
    private String tokenField = "";
    private final Set<String> requestParameters = new LinkedHashSet<>();

    void recordRequest(@NotNull LlmProfile profile,
                       boolean stream,
                       boolean compatibilityRequested,
                       boolean compatibilitySkippedByCache,
                       @NotNull JsonObject requestBody) {
        provider = LlmProvider.fromNullable(profile.getProvider()).name();
        baseUrl = safe(profile.getBaseUrl());
        model = safe(profile.getModel());
        streamRequested = stream;
        reasoningCompatibilityRequested = compatibilityRequested;
        reasoningCompatibilitySkippedByCache |= compatibilitySkippedByCache;
        requestAttempts++;

        if (requestBody.has("max_completion_tokens")) {
            tokenField = "max_completion_tokens";
        } else if (requestBody.has("max_tokens")) {
            tokenField = "max_tokens";
        }

        recordIfPresent(requestBody, "enable_thinking");
        recordIfPresent(requestBody, "reasoning_effort");
        recordIfPresent(requestBody, "thinking");
        reasoningCompatibilityApplied = reasoningCompatibilityApplied
                || requestBody.has("enable_thinking")
                || requestBody.has("reasoning_effort")
                || requestBody.has("thinking");
    }

    void markCompatibilityFallbackUsed() {
        compatibilityFallbackUsed = true;
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
        appendLine(builder, "Compatibility", formatCompatibility());
        appendLine(builder, "Extra params", requestParameters.isEmpty()
                ? "none"
                : requestParameters.stream().collect(Collectors.joining(", ")));
        if (compatibilityFallbackUsed) {
            appendLine(builder, "Fallback", "retried without compatibility params");
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
    private String formatCompatibility() {
        if (!reasoningCompatibilityRequested) {
            return "off";
        }
        if (reasoningCompatibilitySkippedByCache) {
            return "on, skipped by cache";
        }
        return reasoningCompatibilityApplied ? "on, applied" : "on, no extra params needed";
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
