package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class LlmCapabilityCache {

    private static final Set<String> REASONING_COMPATIBILITY_UNSUPPORTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> STREAMING_UNSUPPORTED = ConcurrentHashMap.newKeySet();

    private LlmCapabilityCache() {
    }

    static boolean shouldSkipReasoningCompatibility(@NotNull LlmProfile profile) {
        return REASONING_COMPATIBILITY_UNSUPPORTED.contains(key(profile));
    }

    static void markReasoningCompatibilityUnsupported(@NotNull LlmProfile profile) {
        REASONING_COMPATIBILITY_UNSUPPORTED.add(key(profile));
    }

    static boolean shouldSkipStreaming(@NotNull LlmProfile profile) {
        return STREAMING_UNSUPPORTED.contains(key(profile));
    }

    static void markStreamingUnsupported(@NotNull LlmProfile profile) {
        STREAMING_UNSUPPORTED.add(key(profile));
    }

    static void clearForTests() {
        REASONING_COMPATIBILITY_UNSUPPORTED.clear();
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
