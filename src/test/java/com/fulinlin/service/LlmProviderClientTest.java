package com.fulinlin.service;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LlmProviderClientTest {

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
        assertEquals(0.6D, requestBody.get("temperature").getAsDouble(), 0.0D);
        JsonArray messages = requestBody.getAsJsonArray("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
    }

    @Test
    public void openAiReasoningCompatibilityUsesOpenAiReasoningParameters() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.openai.com/v1");
        profile.setModel("o3-mini");
        profile.setReasoningCompatibilityEnabled(Boolean.TRUE);
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.5D);

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals("o3-mini", requestBody.get("model").getAsString());
        assertEquals(4096, requestBody.get("max_completion_tokens").getAsInt());
        assertFalse(requestBody.has("max_tokens"));
        assertEquals("low", requestBody.get("reasoning_effort").getAsString());
    }

    @Test
    public void openAiReasoningCompatibilityUsesQwenThinkingSwitch() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        profile.setModel("qwen3-coder-plus");
        profile.setReasoningCompatibilityEnabled(Boolean.TRUE);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertFalse(requestBody.has("max_completion_tokens"));
        assertFalse(requestBody.get("enable_thinking").getAsBoolean());
    }

    @Test
    public void openAiReasoningCompatibilityDoesNotAddUnknownParametersForDeepSeekReasoner() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.deepseek.com/v1");
        profile.setModel("deepseek-reasoner");
        profile.setReasoningCompatibilityEnabled(Boolean.TRUE);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertFalse(requestBody.has("max_completion_tokens"));
        assertFalse(requestBody.has("enable_thinking"));
        assertFalse(requestBody.has("reasoning_effort"));
        assertFalse(requestBody.has("thinking"));
    }

    @Test
    public void openAiReasoningCompatibilityUsesThinkingObjectForCompatibleGateway() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://compatible.example.com/v1");
        profile.setModel("mimo-v2.5-pro");
        profile.setReasoningCompatibilityEnabled(Boolean.TRUE);
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
        JsonArray messages = requestBody.getAsJsonArray("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user prompt", messages.get(0).getAsJsonObject().get("content").getAsString());
    }

    @Test
    public void anthropicRequestBodyCanUseConfiguredReasoningParameter() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://compatible.example.com/anthropic");
        profile.setModel("compatible-reasoning-model");
        profile.setProvider(LlmProvider.ANTHROPIC);
        profile.setReasoningCompatibilityEnabled(Boolean.TRUE);
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.3D);

        JsonObject requestBody = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertEquals(4096, requestBody.get("max_tokens").getAsInt());
        assertEquals("disabled", requestBody.getAsJsonObject("thinking").get("type").getAsString());
    }

    @Test
    public void officialAnthropicRequestBodyDoesNotAddThinkingDisableParameter() {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://api.anthropic.com");
        profile.setModel("claude-3-7-sonnet-latest");
        profile.setProvider(LlmProvider.ANTHROPIC);
        profile.setReasoningCompatibilityEnabled(Boolean.TRUE);
        LlmSettings settings = new LlmSettings();

        JsonObject requestBody = AnthropicLlmProviderClient.createRequestBody(
                profile, settings, "system prompt", "user prompt", false
        );

        assertFalse(requestBody.has("thinking"));
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
        assertFalse(profile.getReasoningCompatibilityEnabled());
    }

    @Test
    public void unsupportedCompatibilityParameterErrorsCanRetryWithoutCompatibility() {
        assertTrue(AbstractHttpLlmProviderClient.shouldRetryWithoutReasoningCompatibility(
                "{\"error\":{\"message\":\"unknown parameter: enable_thinking\"}}"
        ));
        assertTrue(AbstractHttpLlmProviderClient.shouldRetryWithoutReasoningCompatibility(
                "{\"error\":{\"message\":\"unsupported parameter: thinking\"}}"
        ));
        assertFalse(AbstractHttpLlmProviderClient.shouldRetryWithoutReasoningCompatibility(
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
        assertFalse(LlmCapabilityCache.shouldSkipReasoningCompatibility(profile));
        assertFalse(LlmCapabilityCache.shouldSkipStreaming(profile));

        LlmCapabilityCache.markReasoningCompatibilityUnsupported(profile);
        LlmCapabilityCache.markStreamingUnsupported(profile);

        assertTrue(LlmCapabilityCache.shouldSkipReasoningCompatibility(profile));
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
        profile.setReasoningCompatibilityEnabled(Boolean.TRUE);
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
}
