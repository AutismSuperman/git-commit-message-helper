package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class LlmCapabilityCache {

    private static final Set<String> THINKING_PARAMETERS_UNSUPPORTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> COMPLETION_TOKENS_UNSUPPORTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> STREAMING_UNSUPPORTED = ConcurrentHashMap.newKeySet();

    private LlmCapabilityCache() {
    }

    static boolean shouldSkipThinkingParameters(@NotNull LlmProfile profile) {
        return THINKING_PARAMETERS_UNSUPPORTED.contains(key(profile));
    }

    static void markThinkingParametersUnsupported(@NotNull LlmProfile profile) {
        THINKING_PARAMETERS_UNSUPPORTED.add(key(profile));
    }

    /**
     * Gateways that expose reasoning models but reject {@code max_completion_tokens} and still
     * expect {@code max_tokens}.
     */
    static boolean shouldSkipCompletionTokens(@NotNull LlmProfile profile) {
        return COMPLETION_TOKENS_UNSUPPORTED.contains(key(profile));
    }

    static void markCompletionTokensUnsupported(@NotNull LlmProfile profile) {
        COMPLETION_TOKENS_UNSUPPORTED.add(key(profile));
    }

    static boolean shouldSkipStreaming(@NotNull LlmProfile profile) {
        return STREAMING_UNSUPPORTED.contains(key(profile));
    }

    static void markStreamingUnsupported(@NotNull LlmProfile profile) {
        STREAMING_UNSUPPORTED.add(key(profile));
    }

    static void clearForTests() {
        THINKING_PARAMETERS_UNSUPPORTED.clear();
        COMPLETION_TOKENS_UNSUPPORTED.clear();
        STREAMING_UNSUPPORTED.clear();
    }

    @NotNull
    private static String key(@NotNull LlmProfile profile) {
        return LlmProvider.fromNullable(profile.getProvider()).name()
                + "|" + normalize(profile.getBaseUrl())
                + "|" + normalize(profile.getModel());
    }

    @NotNull
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
