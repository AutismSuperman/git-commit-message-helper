package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.enums.LlmProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class LlmClient {

    private final Map<LlmProvider, LlmProviderClient> clients = new EnumMap<>(LlmProvider.class);

    public LlmClient() {
        clients.put(LlmProvider.OPENAI_COMPATIBLE, new OpenAiCompatibleLlmProviderClient());
        clients.put(LlmProvider.ANTHROPIC, new AnthropicLlmProviderClient());
    }

    @NotNull
    public String chat(@NotNull LlmProfile profile,
                       @NotNull LlmSettings settings,
                       @NotNull String systemPrompt,
                       @NotNull String userPrompt) throws IOException {
        return getClient(profile).chat(profile, settings, systemPrompt, userPrompt);
    }

    @NotNull
    public String chat(@NotNull LlmProfile profile,
                       @NotNull LlmSettings settings,
                       @NotNull String systemPrompt,
                       @NotNull String userPrompt,
                       @NotNull LlmRequestDiagnostics diagnostics) throws IOException {
        return getClient(profile).chat(profile, settings, systemPrompt, userPrompt, diagnostics);
    }

    public void streamChat(@NotNull LlmProfile profile,
                           @NotNull LlmSettings settings,
                           @NotNull String systemPrompt,
                           @NotNull String userPrompt,
                           @NotNull Consumer<String> onDelta) throws IOException {
        getClient(profile).streamChat(profile, settings, systemPrompt, userPrompt, onDelta);
    }

    public void streamChat(@NotNull LlmProfile profile,
                           @NotNull LlmSettings settings,
                           @NotNull String systemPrompt,
                           @NotNull String userPrompt,
                           @NotNull Consumer<String> onDelta,
                           @NotNull LlmRequestDiagnostics diagnostics) throws IOException {
        getClient(profile).streamChat(profile, settings, systemPrompt, userPrompt, onDelta, diagnostics);
    }

    @NotNull
    private LlmProviderClient getClient(@NotNull LlmProfile profile) {
        LlmProvider provider = LlmProvider.fromNullable(profile.getProvider());
        return clients.get(provider);
    }
}
