package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Consumer;

interface LlmProviderClient {

    @NotNull
    String chat(@NotNull LlmProfile profile,
                @NotNull LlmSettings settings,
                @NotNull String systemPrompt,
                @NotNull String userPrompt) throws IOException;

    @NotNull
    default String chat(@NotNull LlmProfile profile,
                        @NotNull LlmSettings settings,
                        @NotNull String systemPrompt,
                        @NotNull String userPrompt,
                        @NotNull LlmRequestDiagnostics diagnostics) throws IOException {
        return chat(profile, settings, systemPrompt, userPrompt);
    }

    void streamChat(@NotNull LlmProfile profile,
                    @NotNull LlmSettings settings,
                    @NotNull String systemPrompt,
                    @NotNull String userPrompt,
                    @NotNull Consumer<String> onDelta) throws IOException;

    default void streamChat(@NotNull LlmProfile profile,
                            @NotNull LlmSettings settings,
                            @NotNull String systemPrompt,
                            @NotNull String userPrompt,
                            @NotNull Consumer<String> onDelta,
                            @NotNull LlmRequestDiagnostics diagnostics) throws IOException {
        streamChat(profile, settings, systemPrompt, userPrompt, onDelta);
    }

    @NotNull
    String extractErrorMessage(@NotNull String responseBody);
}
