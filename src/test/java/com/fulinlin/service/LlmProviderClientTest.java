package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.enums.LlmProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LlmProviderClientTest {

    @Test
    public void openAiRequestBodyUsesChatCompletionsShape() {
        LlmProfile profile = new LlmProfile();
        profile.setModel("gpt-4.1");
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.6D);

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", true
        );

        assertEquals("gpt-4.1", requestBody.get("model").getAsString());
        assertTrue(requestBody.get("stream").getAsBoolean());
        assertEquals(0.6D, requestBody.get("temperature").getAsDouble(), 0.0D);
        JsonArray messages = requestBody.getAsJsonArray("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
    }

    @Test
    public void anthropicRequestBodyUsesMessagesApiShape() {
        LlmProfile profile = new LlmProfile();
        profile.setModel("claude-3-7-sonnet-latest");
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.3D);

        JsonObject requestBody = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals("claude-3-7-sonnet-latest", requestBody.get("model").getAsString());
        assertEquals("system prompt", requestBody.get("system").getAsString());
        assertEquals(1024, requestBody.get("max_tokens").getAsInt());
        JsonArray messages = requestBody.getAsJsonArray("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user prompt", messages.get(0).getAsJsonObject().get("content").getAsString());
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
    public void jsonNormalizerIgnoresThinkingBlocksBeforeStructuredResponse() {
        String response = "<think>analysis</think>\n"
                + "```json\n"
                + "{\"type\":\"fix\",\"scope\":\"llm\",\"subject\":\"clean generated commit output\"}\n"
                + "```";

        assertEquals("fix", LlmCommitService.parseTemplateResponse(response).getType());
        assertEquals("llm", LlmCommitService.parseTemplateResponse(response).getScope());
        assertEquals("clean generated commit output", LlmCommitService.parseTemplateResponse(response).getSubject());
    }
}
