package com.fulinlin.service;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LlmProviderClientTest {

    @Test
    public void everyProviderExposesItsApiFormatEndpoint() {
        assertEquals(3, LlmProvider.values().length);
        assertEquals("/chat/completions", LlmProvider.OPENAI_COMPATIBLE.getEndpointPath());
        assertEquals("/v1/messages", LlmProvider.ANTHROPIC.getEndpointPath());
        assertEquals("/responses", LlmProvider.OPENAI_RESPONSES.getEndpointPath());
        assertEquals(LlmProvider.OPENAI_COMPATIBLE, LlmProvider.defaultProvider());
    }

    @Test
    public void openAiRequestBodyUsesChatCompletionsShape() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("gpt-4.1");
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.6D);

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", true
        );

        assertEquals("gpt-4.1", requestBody.get("model").getAsString());
        assertTrue(requestBody.get("stream").getAsBoolean());
        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertFalse(requestBody.has("max_completion_tokens"));
        assertFalse(requestBody.has("thinking"));
        assertFalse(requestBody.has("reasoning_effort"));
        assertEquals(0.6D, requestBody.get("temperature").getAsDouble(), 0.0D);
        JsonArray messages = requestBody.getAsJsonArray("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
    }

    @Test
    public void defaultThinkingLevelSendsNoThinkingParameterForAnyProtocol() {
        LlmSettings settings = new LlmSettings();

        for (LlmProvider provider : LlmProvider.values()) {
            LlmProfile profile = new LlmProfile();
            profile.setProvider(provider);
            profile.setBaseUrl(provider.getDefaultBaseUrl());
            profile.setModel("test-model");
            profile.setThinkingLevel(ThinkingLevel.DEFAULT);

            JsonObject requestBody = createRequestBodyFor(provider, profile, settings);

            assertFalse(provider.name(), requestBody.has("thinking"));
            assertFalse(provider.name(), requestBody.has("reasoning"));
            assertFalse(provider.name(), requestBody.has("reasoning_effort"));
            assertFalse(provider.name(), requestBody.has("enable_thinking"));
            assertFalse(provider.name(), requestBody.has("output_config"));
        }
    }

    @Test
    public void chatCompletionsThinkingLevelsUseReasoningEffortForOpenAiStyleGateways() {
        LlmSettings settings = new LlmSettings();
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("o3-mini");
        profile.setThinkingLevel(ThinkingLevel.DISABLED);

        JsonObject disabledRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("low", disabledRequest.get("reasoning_effort").getAsString());
        assertEquals(4096, disabledRequest.get("max_completion_tokens").getAsInt());
        assertFalse(disabledRequest.has("max_tokens"));
        assertFalse(disabledRequest.has("temperature"));

        profile.setThinkingLevel(ThinkingLevel.HIGH);
        JsonObject highRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("high", highRequest.get("reasoning_effort").getAsString());

        profile.setThinkingLevel(ThinkingLevel.MAX);
        JsonObject maxRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("max", maxRequest.get("reasoning_effort").getAsString());
    }

    @Test
    public void chatCompletionsThinkingLevelsUseReasoningEffortForGeminiAndGrok() {
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.5D);

        LlmProfile gemini = new LlmProfile();
        gemini.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai");
        gemini.setModel("gemini-3-pro");
        gemini.setThinkingLevel(ThinkingLevel.MEDIUM);
        JsonObject geminiRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                gemini, settings, "system prompt", "user prompt", false
        );

        assertEquals("medium", geminiRequest.get("reasoning_effort").getAsString());
        assertEquals(4096, geminiRequest.get("max_tokens").getAsInt());
        assertEquals(0.5D, geminiRequest.get("temperature").getAsDouble(), 0.0D);

        LlmProfile grok = new LlmProfile();
        grok.setBaseUrl("https://api.x.ai/v1");
        grok.setModel("grok-4");
        grok.setThinkingLevel(ThinkingLevel.LOW);
        JsonObject grokRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                grok, settings, "system prompt", "user prompt", false
        );

        assertEquals("low", grokRequest.get("reasoning_effort").getAsString());
        assertFalse(grokRequest.has("thinking"));
        assertFalse(grokRequest.has("enable_thinking"));
    }

    @Test
    public void chatCompletionsThinkingLevelsUseThinkingObjectForDoubaoArk() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://ark.cn-beijing.volces.com/api/v3");
        profile.setModel("doubao-seed-1.6");
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertEquals("enabled", requestBody.getAsJsonObject("thinking").get("type").getAsString());
        assertFalse(requestBody.has("reasoning_effort"));

        profile.setThinkingLevel(ThinkingLevel.DISABLED);
        JsonObject disabledRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("disabled", disabledRequest.getAsJsonObject("thinking").get("type").getAsString());
    }

    @Test
    public void chatCompletionsThinkingLevelsUseReasoningObjectForOpenRouterStyleGateways() {
        LlmSettings settings = new LlmSettings();

        LlmProfile openRouter = new LlmProfile();
        openRouter.setBaseUrl("https://openrouter.ai/api/v1");
        openRouter.setModel("minimax/minimax-m2.7");
        openRouter.setThinkingLevel(ThinkingLevel.HIGH);
        JsonObject openRouterRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                openRouter, settings, "system prompt", "user prompt", false
        );

        assertEquals("high", openRouterRequest.getAsJsonObject("reasoning").get("effort").getAsString());
        assertFalse(openRouterRequest.has("thinking"));
        assertFalse(openRouterRequest.has("enable_thinking"));
        assertFalse(openRouterRequest.has("reasoning_effort"));

        openRouter.setThinkingLevel(ThinkingLevel.DISABLED);
        JsonObject openRouterDisabled = OpenAiCompatibleLlmProviderClient.createRequestBody(
                openRouter, settings, "system prompt", "user prompt", false
        );
        assertEquals("none", openRouterDisabled.getAsJsonObject("reasoning").get("effort").getAsString());

        LlmProfile minimax = new LlmProfile();
        minimax.setBaseUrl("https://api.minimaxi.com/v1");
        minimax.setModel("MiniMax-M2.7");
        minimax.setThinkingLevel(ThinkingLevel.LOW);
        JsonObject minimaxRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                minimax, settings, "system prompt", "user prompt", false
        );

        assertEquals("low", minimaxRequest.getAsJsonObject("reasoning").get("effort").getAsString());
    }

    @Test
    public void retryRequestKeepsCompletionTokenLimitAndDropsTemperatureForOpenAiReasoningModels() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("o3-mini");
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.5D);

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false,
                false, true, true
        );

        assertEquals(4096, requestBody.get("max_completion_tokens").getAsInt());
        assertFalse(requestBody.has("max_tokens"));
        assertFalse(requestBody.has("temperature"));
        assertFalse(requestBody.has("reasoning_effort"));
    }

    @Test
    public void reasoningModelsKeepCompletionTokenLimitEvenWithDefaultThinkingLevel() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("gpt-5.2");
        profile.setThinkingLevel(ThinkingLevel.DEFAULT);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        // The token field is a model capability, independent of the thinking level.
        assertEquals(4096, requestBody.get("max_completion_tokens").getAsInt());
        assertFalse(requestBody.has("max_tokens"));
        assertFalse("default level sends no thinking parameter", requestBody.has("reasoning_effort"));
    }

    @Test
    public void unsupportedCompletionTokenErrorsTriggerTokenFallback() {
        assertTrue(OpenAiCompatibleLlmProviderClient.errorIndicatesUnsupportedCompletionTokens(
                "{\"error\":{\"message\":\"Unsupported parameter: 'max_completion_tokens' is not supported with this model.\"}}"
        ));
        assertTrue(OpenAiCompatibleLlmProviderClient.errorIndicatesUnsupportedCompletionTokens(
                "{\"error\":{\"message\":\"unknown parameter: max_completion_tokens\"}}"
        ));
        assertFalse(OpenAiCompatibleLlmProviderClient.errorIndicatesUnsupportedCompletionTokens(
                "{\"error\":{\"message\":\"unknown parameter: enable_thinking\"}}"
        ));
        assertFalse(OpenAiCompatibleLlmProviderClient.errorIndicatesUnsupportedCompletionTokens(
                "{\"error\":{\"message\":\"bad api key\"}}"
        ));
    }

    @Test
    public void completionTokenFallbackIsRememberedPerModel() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://compatible.example.com/v1");
        profile.setModel("mimo-v2.5-pro");
        profile.setProvider(LlmProvider.OPENAI_COMPATIBLE);

        LlmCapabilityCache.clearForTests();
        assertFalse(LlmCapabilityCache.shouldSkipCompletionTokens(profile));

        LlmCapabilityCache.markCompletionTokensUnsupported(profile);

        assertTrue(LlmCapabilityCache.shouldSkipCompletionTokens(profile));
        assertFalse("other capabilities stay unaffected",
                LlmCapabilityCache.shouldSkipThinkingParameters(profile));
        LlmCapabilityCache.clearForTests();
    }

    @Test
    public void temperatureRejectionErrorsTriggerFallbackRetry() {
        assertTrue(AbstractHttpLlmProviderClient.errorIndicatesUnsupportedTemperature(
                "{\"error\":{\"message\":\"Unsupported value: 'temperature' does not support 0.5 with this model. "
                        + "Only the default (1) value is supported.\"}}"
        ));
        assertTrue(AbstractHttpLlmProviderClient.errorIndicatesUnsupportedTemperature(
                "{\"error\":{\"message\":\"1 validation error for Request\\nbody -> temperature\\n"
                        + "  value is not a valid float\"}}"
        ));
        assertFalse(AbstractHttpLlmProviderClient.errorIndicatesUnsupportedTemperature(
                "{\"error\":{\"message\":\"bad api key\"}}"
        ));
        assertFalse(AbstractHttpLlmProviderClient.errorIndicatesUnsupportedTemperature(
                "{\"error\":{\"message\":\"unknown parameter: enable_thinking\"}}"
        ));
    }

    @Test
    public void chatCompletionsThinkingLevelsUseQwenThinkingSwitch() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        profile.setModel("qwen3-coder-plus");
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertFalse(requestBody.has("max_completion_tokens"));
        assertTrue(requestBody.get("enable_thinking").getAsBoolean());

        profile.setThinkingLevel(ThinkingLevel.DISABLED);
        JsonObject disabledRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertFalse(disabledRequest.get("enable_thinking").getAsBoolean());
    }

    @Test
    public void chatCompletionsThinkingLevelsUseThinkingObjectForDeepSeekGateways() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.deepseek.com/v1");
        profile.setModel("deepseek-reasoner");
        profile.setThinkingLevel(ThinkingLevel.LOW);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertFalse(requestBody.has("max_completion_tokens"));
        assertEquals("enabled", requestBody.getAsJsonObject("thinking").get("type").getAsString());
        assertFalse(requestBody.has("reasoning_effort"));
    }

    @Test
    public void chatCompletionsThinkingLevelsUseThinkingObjectForCompatibleGateway() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://compatible.example.com/v1");
        profile.setModel("mimo-v2.5-pro");
        profile.setThinkingLevel(ThinkingLevel.DISABLED);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_completion_tokens").getAsInt());
        assertEquals("disabled", requestBody.getAsJsonObject("thinking").get("type").getAsString());
    }

    @Test
    public void anthropicRequestBodyUsesMessagesApiShape() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.anthropic.com");
        profile.setModel("claude-3-7-sonnet-latest");
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.3D);

        JsonObject requestBody = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals("claude-3-7-sonnet-latest", requestBody.get("model").getAsString());
        assertEquals("system prompt", requestBody.get("system").getAsString());
        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertFalse(requestBody.has("thinking"));
        assertEquals(0.3D, requestBody.get("temperature").getAsDouble(), 0.0D);
        JsonArray messages = requestBody.getAsJsonArray("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user prompt", messages.get(0).getAsJsonObject().get("content").getAsString());
    }

    @Test
    public void anthropicThinkingLevelsUseThinkingObjectAndDropTemperature() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.anthropic.com");
        profile.setModel("claude-3-7-sonnet-latest");
        profile.setProvider(LlmProvider.ANTHROPIC);
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.3D);

        JsonObject requestBody = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        JsonObject thinking = requestBody.getAsJsonObject("thinking");
        assertEquals("enabled", thinking.get("type").getAsString());
        assertTrue(thinking.get("budget_tokens").getAsInt() > 0);
        assertFalse(requestBody.has("temperature"));

        profile.setThinkingLevel(ThinkingLevel.DISABLED);
        JsonObject disabledRequest = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("disabled", disabledRequest.getAsJsonObject("thinking").get("type").getAsString());
        assertEquals(0.3D, disabledRequest.get("temperature").getAsDouble(), 0.0D);
    }

    @Test
    public void anthropicThinkingBudgetScalesWithLevelAndStaysBelowResponseLimit() {
        LlmProfile profile = new LlmProfile();
        profile.setProvider(LlmProvider.ANTHROPIC);

        profile.setMaxResponseTokens(8000);
        int low = LlmThinkingParameters.resolveThinkingBudget(profile, ThinkingLevel.LOW);
        int medium = LlmThinkingParameters.resolveThinkingBudget(profile, ThinkingLevel.MEDIUM);
        int high = LlmThinkingParameters.resolveThinkingBudget(profile, ThinkingLevel.HIGH);
        int max = LlmThinkingParameters.resolveThinkingBudget(profile, ThinkingLevel.MAX);

        assertTrue("low < medium", low < medium);
        assertTrue("medium < high", medium < high);
        assertTrue("high < max", high < max);
        assertTrue("budget stays below max_tokens", max < 8000);
        assertTrue("budget is a valid Anthropic budget", max >= 1024);
    }

    @Test
    public void anthropicSkipsThinkingWhenResponseBudgetCannotHoldAValidBudget() {
        LlmProfile profile = new LlmProfile();
        profile.setProvider(LlmProvider.ANTHROPIC);
        profile.setBaseUrl("https://api.anthropic.com");
        profile.setModel("claude-3-7-sonnet-latest");
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        profile.setMaxResponseTokens(LlmProfile.MIN_MAX_RESPONSE_TOKENS);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        // The request stays valid instead of asking for a budget Anthropic would reject.
        assertFalse(requestBody.has("thinking"));
        assertEquals(LlmProfile.MIN_MAX_RESPONSE_TOKENS, requestBody.get("max_tokens").getAsInt());
    }

    @Test
    public void anthropicEffortGatewaysReceiveOutputConfigInsteadOfBudget() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://open.bigmodel.cn/api/anthropic");
        profile.setModel("glm-5.3");
        profile.setProvider(LlmProvider.ANTHROPIC);
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals("enabled", requestBody.getAsJsonObject("thinking").get("type").getAsString());
        assertFalse(requestBody.getAsJsonObject("thinking").has("budget_tokens"));
        assertEquals("high", requestBody.getAsJsonObject("output_config").get("effort").getAsString());
    }

    @Test
    public void openAiResponsesRequestBodyUsesResponsesApiShape() {
        LlmProfile profile = new LlmProfile();
        profile.setProvider(LlmProvider.OPENAI_RESPONSES);
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("gpt-5.2");
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.4D);

        JsonObject requestBody = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals("gpt-5.2", requestBody.get("model").getAsString());
        assertEquals("system prompt", requestBody.get("instructions").getAsString());
        assertEquals("user prompt", requestBody.get("input").getAsString());
        assertEquals(4096, requestBody.get("max_output_tokens").getAsInt());
        assertFalse(requestBody.has("messages"));
        assertFalse(requestBody.has("max_tokens"));
        assertFalse(requestBody.has("max_completion_tokens"));
        // Without a thinking request, sampling parameters are still forwarded.
        assertEquals(0.4D, requestBody.get("temperature").getAsDouble(), 0.0D);

        profile.setThinkingLevel(ThinkingLevel.HIGH);
        JsonObject thinkingRequest = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        // Reasoning requests drop temperature, which reasoning models do not accept.
        assertFalse(thinkingRequest.has("temperature"));
    }

    @Test
    public void openAiResponsesThinkingLevelsUseReasoningEffort() {
        LlmProfile profile = new LlmProfile();
        profile.setProvider(LlmProvider.OPENAI_RESPONSES);
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("gpt-5.2");
        LlmSettings settings = new LlmSettings();

        profile.setThinkingLevel(ThinkingLevel.DISABLED);
        JsonObject disabled = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("none", disabled.getAsJsonObject("reasoning").get("effort").getAsString());

        profile.setThinkingLevel(ThinkingLevel.HIGH);
        JsonObject high = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("high", high.getAsJsonObject("reasoning").get("effort").getAsString());

        // Models that only expose none/high collapse graded levels instead of failing.
        profile.setModel("kimi-k2.7-code");
        profile.setThinkingLevel(ThinkingLevel.LOW);
        JsonObject collapsed = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals("high", collapsed.getAsJsonObject("reasoning").get("effort").getAsString());
    }

    @Test
    public void openAiResponsesResponseParsingExtractsTextFromOutputAndStream() {
        String response = "{\"output\":[{\"type\":\"reasoning\",\"content\":[]},"
                + "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"commit text\"}]}]}";
        String outputText = "{\"output_text\":\"direct text\"}";
        String delta = "{\"type\":\"response.output_text.delta\",\"delta\":\"part\"}";
        String completed = "{\"type\":\"response.completed\",\"response\":"
                + "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"done\"}]}]}}";

        assertEquals("commit text", OpenAiResponsesLlmProviderClient.extractChatResponse(response));
        assertEquals("direct text", OpenAiResponsesLlmProviderClient.extractChatResponse(outputText));
        assertEquals("part", OpenAiResponsesLlmProviderClient.extractStreamDelta("", delta));
        assertEquals("done", OpenAiResponsesLlmProviderClient.extractStreamDelta("", completed));
        assertEquals("part", OpenAiResponsesLlmProviderClient.extractChatResponseFromEventStream(
                "event: response.output_text.delta\ndata: " + delta + "\n\n"
        ));
    }

    @Test
    public void openAiResponsesStreamDoesNotDuplicateTextWhenCompletionFollowsDeltas() {
        String delta = "{\"type\":\"response.output_text.delta\",\"delta\":\"part\"}";
        String completed = "{\"type\":\"response.completed\",\"response\":"
                + "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"part\"}]}]}}";

        // Deltas win; the completion payload must not be appended on top of them.
        assertEquals("part", OpenAiResponsesLlmProviderClient.extractChatResponseFromEventStream(
                "data: " + delta + "\n\n"
                        + "data: " + completed + "\n\n"
        ));

        // When a gateway sends only the completion event, its text is used as the fallback.
        assertEquals("part", OpenAiResponsesLlmProviderClient.extractChatResponseFromEventStream(
                "event: response.completed\ndata: " + completed + "\n\n"
        ));
    }

    @Test
    public void openAiResponsesEndpointsAcceptBaseUrlOrFullPath() {
        LlmProfile profile = new LlmProfile();
        profile.setProvider(LlmProvider.OPENAI_RESPONSES);

        profile.setBaseUrl("https://api.openai.com/v1");
        assertEquals("https://api.openai.com/v1/responses",
                OpenAiResponsesLlmProviderClient.resolveResponsesEndpoint(profile));
        assertEquals("https://api.openai.com/v1/models",
                OpenAiResponsesLlmProviderClient.resolveModelsEndpoint(profile));

        profile.setBaseUrl("https://api.openai.com/v1/responses");
        assertEquals("https://api.openai.com/v1/responses",
                OpenAiResponsesLlmProviderClient.resolveResponsesEndpoint(profile));
        assertEquals("https://api.openai.com/v1/models",
                OpenAiResponsesLlmProviderClient.resolveModelsEndpoint(profile));

        profile.setBaseUrl("https://api.x.ai/v1/responses");
        assertTrue(OpenAiResponsesLlmProviderClient.isEventStream(
                "text/event-stream", "{}"));
    }

    @Test
    public void anthropicResponseParsingExtractsTextAndError() {
        String response = "{\"content\":[{\"type\":\"text\",\"text\":\"hello\"},{\"type\":\"text\",\"text\":\" world\"}]}";
        String sseResponse = "data:{\"content\":[{\"type\":\"text\",\"text\":\"hello world\"}]}\n\n";
        String wrappedSseResponse = "data:event: content_block_delta\n"
                + "data:data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"hello\"}}\n\n"
                + "data:event: content_block_delta\n"
                + "data:data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\" world\"}}\n\n";
        String error = "{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"bad api key\"}}";
        AnthropicLlmProviderClient client = new AnthropicLlmProviderClient();

        assertEquals("hello world", AnthropicLlmProviderClient.extractChatResponse(response));
        assertEquals("hello world", AnthropicLlmProviderClient.extractChatResponseFromEventStream(sseResponse));
        assertEquals("hello world", AnthropicLlmProviderClient.extractChatResponseFromEventStream(wrappedSseResponse));
        assertEquals("bad api key", client.extractErrorMessage(error));
        assertEquals("delta", AnthropicLlmProviderClient.extractStreamDelta(
                "content_block_delta",
                "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"delta\"}}"
        ));
    }

    @Test
    public void openAiResponseParsingExtractsMessageAndStreamText() {
        String response = "{\"choices\":[{\"message\":{\"content\":\"commit text\"}}]}";
        String stream = "{\"choices\":[{\"delta\":{\"content\":\"part\"}}]}";

        assertEquals("commit text", OpenAiCompatibleLlmProviderClient.extractChatResponse(response));
        assertEquals("part", OpenAiCompatibleLlmProviderClient.extractStreamDelta(stream));
    }

    @Test
    public void openAiModelListExtractsModelIds() {
        String response = "{\"object\":\"list\",\"data\":["
                + "{\"id\":\"gpt-4.1\",\"object\":\"model\"},"
                + "{\"id\":\"gpt-4.1\",\"object\":\"model\"},"
                + "{\"name\":\"custom-model\"},"
                + "\"string-model\""
                + "]}";

        java.util.List<String> models = OpenAiCompatibleLlmProviderClient.extractModelIds(response);

        assertEquals(3, models.size());
        assertEquals("gpt-4.1", models.get(0));
        assertEquals("custom-model", models.get(1));
        assertEquals("string-model", models.get(2));
    }

    @Test
    public void openAiModelsEndpointAcceptsBaseUrlOrFullChatCompletionsEndpoint() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        assertEquals("https://api.openai.com/v1/models", OpenAiCompatibleLlmProviderClient.resolveModelsEndpoint(profile));

        profile.setBaseUrl("https://api.openai.com/v1/chat/completions");
        assertEquals("https://api.openai.com/v1/models", OpenAiCompatibleLlmProviderClient.resolveModelsEndpoint(profile));

        profile.setBaseUrl("https://api.openai.com/v1/models");
        assertEquals("https://api.openai.com/v1/models", OpenAiCompatibleLlmProviderClient.resolveModelsEndpoint(profile));
    }

    @Test
    public void defaultProfileAndLegacyProfilesUseOpenAiProvider() {
        LlmProfile profile = new LlmProfile();
        profile.setId("legacy");
        profile.setName("Legacy");
        profile.setBaseUrl(null);
        profile.setApiKey("");
        profile.setModel("");

        com.fulinlin.storage.GitCommitMessageHelperSettings.checkDefaultLlmProfile(profile);

        assertEquals(LlmProvider.OPENAI_COMPATIBLE, profile.getProvider());
        assertEquals("https://api.openai.com/v1", profile.getBaseUrl());
        assertEquals(ThinkingLevel.DEFAULT, profile.getThinkingLevel());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void legacyReasoningCompatibilityFlagMigratesToDisabledThinkingLevel() {
        LlmProfile legacyOff = new LlmProfile();
        legacyOff.setId("legacy-off");
        legacyOff.setName("Legacy");
        legacyOff.setReasoningCompatibilityEnabled(Boolean.TRUE);

        GitCommitMessageHelperSettings.checkDefaultLlmProfile(legacyOff);

        assertEquals(ThinkingLevel.DISABLED, legacyOff.getThinkingLevel());
        assertNull("legacy flag is cleared after migration", legacyOff.getReasoningCompatibilityEnabled());

        LlmProfile legacyAbsent = new LlmProfile();
        legacyAbsent.setId("legacy-absent");
        legacyAbsent.setName("Legacy");
        legacyAbsent.setReasoningCompatibilityEnabled(Boolean.FALSE);

        GitCommitMessageHelperSettings.checkDefaultLlmProfile(legacyAbsent);

        assertEquals(ThinkingLevel.DEFAULT, legacyAbsent.getThinkingLevel());

        LlmProfile explicitLevel = new LlmProfile();
        explicitLevel.setId("explicit");
        explicitLevel.setName("Explicit");
        explicitLevel.setThinkingLevel(ThinkingLevel.MAX);
        explicitLevel.setReasoningCompatibilityEnabled(Boolean.TRUE);

        GitCommitMessageHelperSettings.checkDefaultLlmProfile(explicitLevel);

        assertEquals("an explicit level wins over the legacy flag", ThinkingLevel.MAX, explicitLevel.getThinkingLevel());
    }

    @Test
    public void unsupportedThinkingParameterErrorsCanRetryWithoutThinking() {
        assertTrue(AbstractHttpLlmProviderClient.shouldRetryWithoutThinkingParameters(
                "{\"error\":{\"message\":\"unknown parameter: enable_thinking\"}}"
        ));
        assertTrue(AbstractHttpLlmProviderClient.shouldRetryWithoutThinkingParameters(
                "{\"error\":{\"message\":\"unsupported parameter: thinking\"}}"
        ));
        assertTrue(AbstractHttpLlmProviderClient.shouldRetryWithoutThinkingParameters(
                "{\"error\":{\"message\":\"Extra inputs are not permitted\",\"param\":\"reasoning\"}}"
        ));
        assertFalse(AbstractHttpLlmProviderClient.shouldRetryWithoutThinkingParameters(
                "{\"error\":{\"message\":\"bad api key\"}}"
        ));
    }

    @Test
    public void capabilityCacheRemembersUnsupportedModelFeatures() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://compatible.example.com/v1");
        profile.setModel("reasoning-model");
        profile.setProvider(LlmProvider.OPENAI_COMPATIBLE);

        LlmCapabilityCache.clearForTests();
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
        assertFalse(LlmCapabilityCache.shouldSkipStreaming(profile));

        LlmCapabilityCache.markThinkingParametersUnsupported(profile);
        LlmCapabilityCache.markStreamingUnsupported(profile);

        assertTrue(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
        assertTrue(LlmCapabilityCache.shouldSkipStreaming(profile));
        LlmCapabilityCache.clearForTests();
    }

    @Test
    public void requestDiagnosticsSummarizesRequestWithoutApiKey() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setApiKey("secret-key");
        profile.setModel("o3-mini");
        profile.setProvider(LlmProvider.OPENAI_COMPATIBLE);
        profile.setThinkingLevel(ThinkingLevel.LOW);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        LlmRequestDiagnostics diagnostics = new LlmRequestDiagnostics();
        diagnostics.recordRequest(profile, false, true, false, requestBody);
        String summary = diagnostics.toUserSummary();

        assertTrue(summary.contains("o3-mini"));
        assertTrue(summary.contains("max_completion_tokens"));
        assertTrue(summary.contains("reasoning_effort"));
        assertTrue(summary.contains("low, applied"));
        assertFalse(summary.contains("secret-key"));
    }

    @Test
    public void anthropicEndpointAcceptsBaseUrlWithOrWithoutV1() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.anthropic.com");
        assertEquals("https://api.anthropic.com/v1/messages", AnthropicLlmProviderClient.resolveMessagesEndpoint(profile));

        profile.setBaseUrl("https://api.anthropic.com/v1");
        assertEquals("https://api.anthropic.com/v1/messages", AnthropicLlmProviderClient.resolveMessagesEndpoint(profile));

        profile.setBaseUrl("https://api.anthropic.com/v1/messages");
        assertEquals("https://api.anthropic.com/v1/messages", AnthropicLlmProviderClient.resolveMessagesEndpoint(profile));
    }

    @Test
    public void anthropicModelsEndpointAcceptsBaseUrlWithOrWithoutMessagesPath() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.anthropic.com");
        assertEquals("https://api.anthropic.com/v1/models", AnthropicLlmProviderClient.resolveModelsEndpoint(profile));

        profile.setBaseUrl("https://api.anthropic.com/v1");
        assertEquals("https://api.anthropic.com/v1/models", AnthropicLlmProviderClient.resolveModelsEndpoint(profile));

        profile.setBaseUrl("https://api.anthropic.com/v1/messages");
        assertEquals("https://api.anthropic.com/v1/models", AnthropicLlmProviderClient.resolveModelsEndpoint(profile));

        profile.setBaseUrl("https://api.anthropic.com/v1/models");
        assertEquals("https://api.anthropic.com/v1/models", AnthropicLlmProviderClient.resolveModelsEndpoint(profile));
    }

    @Test
    public void anthropicDetectsEventStreamResponses() {
        assertTrue(AnthropicLlmProviderClient.isEventStream("text/event-stream", "{\"content\":[]}"));
        assertTrue(AnthropicLlmProviderClient.isEventStream("application/json", "data:{\"content\":[]}"));
        assertEquals("event: message_start", AnthropicLlmProviderClient.normalizeEventStreamLine("data:event: message_start"));
    }

    @Test
    public void commitResponseSanitizerRemovesThinkingBlocksAndDanglingFences() {
        String response = "<think>\n"
                + "analysis\n"
                + "```\n"
                + "chore(config): switch Nacos namespace from dev to prd\n"
                + "```\n"
                + "</think>\n\n"
                + "chore(config): switch Nacos namespace from dev to prd\n"
                + "```";

        assertEquals(
                "chore(config): switch Nacos namespace from dev to prd",
                LlmCommitService.sanitizeCommitResponse(response)
        );
    }

    @Test
    public void commitResponseSanitizerExtractsFencedCommitMessage() {
        String response = "Here is the commit message:\n\n"
                + "```text\n"
                + "fix(ui): keep LLM reasoning out of commit message\n"
                + "```\n";

        assertEquals(
                "fix(ui): keep LLM reasoning out of commit message",
                LlmCommitService.sanitizeCommitResponse(response)
        );
    }

    @Test
    public void lowQualityCommitMessageDetectorFlagsVagueSubjects() {
        assertTrue(LlmCommitService.isLowQualityCommitMessage("chore: update files"));
        assertTrue(LlmCommitService.isLowQualityCommitMessage("fix: bug"));
        assertFalse(LlmCommitService.isLowQualityCommitMessage(
                "fix(llm): retry without unsupported reasoning parameters"
        ));
    }

    @Test
    public void jsonNormalizerIgnoresThinkingBlocksBeforeStructuredResponse() {
        String response = "<think>analysis</think>\n"
                + "```json\n"
                + "{\"type\":\"fix\",\"scope\":\"llm\",\"subject\":\"clean generated commit output\"}\n"
                + "```";

        assertEquals("fix", LlmCommitService.parseTemplateResponse(response).getType());
        assertEquals("llm", LlmCommitService.parseTemplateResponse(response).getScope());
        assertEquals("clean generated commit output", LlmCommitService.parseTemplateResponse(response).getSubject());
    }

    @Test
    public void templateRenderingConvertsParagraphBodyIntoBulletList() {
        GitCommitMessageHelperSettings settings = new GitCommitMessageHelperSettings();
        CommitTemplate commitTemplate = new CommitTemplate();
        commitTemplate.setType("docs");
        commitTemplate.setSubject("补充仓库说明和报告基础类型");
        commitTemplate.setBody("更新仓库 README 文档，补充结构说明和快速开始指南。新增监督检查报告相关常量和异常类型。");

        String rendered = LlmCommitService.renderCommitTemplate(settings, commitTemplate);

        assertTrue(rendered, rendered.startsWith("docs: 补充仓库说明和报告基础类型"));
        assertTrue(rendered, rendered.contains("\n\n- 更新仓库 README 文档，补充结构说明和快速开始指南\n"
                + "- 新增监督检查报告相关常量和异常类型"));
    }

    @Test
    public void templateResponseParserConvertsArrayBodyIntoBulletList() {
        String response = "{\"type\":\"docs\",\"subject\":\"补充仓库说明\","
                + "\"body\":[\"更新 README 文档\",\"新增监督检查报告相关常量\"]}";

        CommitTemplate commitTemplate = LlmCommitService.parseTemplateResponse(response);

        assertEquals("- 更新 README 文档\n- 新增监督检查报告相关常量", commitTemplate.getBody());
    }

    @Test
    public void templateResponseParserSalvagesUnterminatedBodyString() {
        String response = "{\"type\":\"docs\",\"scope\":\"readme\",\"subject\":\"补充仓库说明\","
                + "\"body\":\"更新 README 文档，补充结构说明和快速开始指南";

        CommitTemplate commitTemplate = LlmCommitService.parseTemplateResponse(response);

        assertEquals("docs", commitTemplate.getType());
        assertEquals("readme", commitTemplate.getScope());
        assertEquals("补充仓库说明", commitTemplate.getSubject());
        assertEquals("更新 README 文档，补充结构说明和快速开始指南", commitTemplate.getBody());
    }

    @Test
    public void templateResponseParserReportsNullJsonAsFriendlyParseFailure() {
        try {
            LlmCommitService.parseTemplateResponse("null");
        } catch (IllegalArgumentException ex) {
            assertEquals("LLM response is not a commit template JSON object", ex.getMessage());
            return;
        }
        throw new AssertionError("Expected null response to fail with a friendly parse error");
    }

    @Test
    public void templateResponseParserRejectsTypeOnlyPartialJson() {
        try {
            LlmCommitService.parseTemplateResponse("{\"type\":\"docs\"");
        } catch (IllegalArgumentException ex) {
            assertEquals("LLM response is not a commit template JSON object", ex.getMessage());
            return;
        }
        throw new AssertionError("Expected type-only partial JSON to fail");
    }

    @Test
    public void largeDiffTrimKeepsFileAndHunkHeaders() {
        StringBuilder diff = new StringBuilder();
        diff.append("diff --git a/src/A.java b/src/A.java\n");
        diff.append("--- a/src/A.java\n");
        diff.append("+++ b/src/A.java\n");
        diff.append("@@ -1,80 +1,80 @@\n");
        for (int i = 0; i < 120; i++) {
            diff.append("+line ").append(i).append("\n");
        }

        String trimmed = GitContextService.trimDiffForPrompt(diff.toString(), 500);

        assertTrue(trimmed.contains("diff --git a/src/A.java b/src/A.java"));
        assertTrue(trimmed.contains("@@ -1,80 +1,80 @@"));
        assertTrue(trimmed.contains("...[diff summarized: omitted "));
    }

    @Test
    public void profileTokenLimitDefaultsTo4096AndHonoursConfiguredValue() {
        LlmProfile profile = new LlmProfile();
        profile.setModel("gpt-4.1");
        LlmSettings settings = new LlmSettings();

        JsonObject defaultRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals(4096, defaultRequest.get("max_tokens").getAsInt());

        profile.setMaxResponseTokens(16000);
        JsonObject configuredRequest = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals(16000, configuredRequest.get("max_tokens").getAsInt());

        JsonObject anthropicRequest = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals(16000, anthropicRequest.get("max_tokens").getAsInt());

        JsonObject responsesRequest = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        assertEquals(16000, responsesRequest.get("max_output_tokens").getAsInt());
    }

    @Test
    public void profileTokenLimitIsClampedToSupportedRange() {
        assertEquals(4096, LlmProfile.resolveMaxResponseTokens(null));
        assertEquals(256, LlmProfile.resolveMaxResponseTokens(1));
        assertEquals(128000, LlmProfile.resolveMaxResponseTokens(999999));
        assertEquals(8192, LlmProfile.resolveMaxResponseTokens(8192));
    }

    @Test
    public void reasoningRetryKeepsConfiguredTokenLimit() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("o3-mini");
        profile.setMaxResponseTokens(32768);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false,
                false, true, true
        );

        assertEquals(32768, requestBody.get("max_completion_tokens").getAsInt());
    }

    @Test
    public void diffLengthSettingDefaultsTo12000AndIsClamped() {
        assertEquals(12000, LlmSettings.resolveMaxDiffLength(null));
        assertEquals(2000, LlmSettings.resolveMaxDiffLength(10));
        assertEquals(200000, LlmSettings.resolveMaxDiffLength(9999999));
        assertEquals(48000, LlmSettings.resolveMaxDiffLength(48000));
    }

    @Test
    public void diagnosticsReportConfiguredTokenLimit() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("gpt-4.1");
        profile.setMaxResponseTokens(24000);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );
        LlmRequestDiagnostics diagnostics = new LlmRequestDiagnostics();
        diagnostics.recordRequest(profile, false, false, false, requestBody);

        assertTrue(diagnostics.toUserSummary().contains("Token limit: 24000"));
    }

    @NotNull
    private static JsonObject createRequestBodyFor(@NotNull LlmProvider provider,
                                                   @NotNull LlmProfile profile,
                                                   @NotNull LlmSettings settings) {
        switch (provider) {
            case ANTHROPIC:
                return AnthropicLlmProviderClient.createRequestBody(
                        profile, settings, "system prompt", "user prompt", false);
            case OPENAI_RESPONSES:
                return OpenAiResponsesLlmProviderClient.createRequestBody(
                        profile, settings, "system prompt", "user prompt", false);
            default:
                return OpenAiCompatibleLlmProviderClient.createRequestBody(
                        profile, settings, "system prompt", "user prompt", false);
        }
    }
}
