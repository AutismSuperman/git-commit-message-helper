package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.model.enums.ThinkingParameterPreset;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ThinkingMappingRetryTest {

    private static final String CHAT_OK = "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}";
    private static final String TEMPERATURE_ERROR =
            "{\"error\":{\"message\":\"Unsupported parameter: temperature\",\"param\":\"temperature\"}}";

    @After
    public void clearCache() {
        LlmCapabilityCache.clearForTests();
    }

    @Test
    public void thinkingRejectionRetriesAndCorrectedMappingIsSentOnTheNextRequest() throws Exception {
        Transport transport = new Transport();
        transport.reply(400, "{\"error\":{\"message\":\"Unsupported parameter: reasoning_effort\"}}");
        transport.reply(200, CHAT_OK);
        transport.reply(200, CHAT_OK);
        LlmProfile profile = profile(LlmProvider.OPENAI_COMPATIBLE,
                "{\"HIGH\":{\"reasoning_effort\":\"max\"}}");
        LlmRequestDiagnostics diagnostics = new LlmRequestDiagnostics();
        ChatClient client = new ChatClient(transport);

        assertEquals("OK", client.chat(profile, settings(), "system", "user", diagnostics));
        assertTrue(transport.body(0).has("reasoning_effort"));
        assertFalse(transport.body(1).has("reasoning_effort"));
        assertEquals(0.5D, transport.body(1).get("temperature").getAsDouble(), 0.0D);
        assertTrue(diagnostics.toUserSummary().contains("Thinking fallback"));
        assertTrue(LlmCapabilityCache.shouldSkipThinkingParameters(profile));

        profile.setThinkingParameterMapping("{\"HIGH\":{\"reasoning_effort\":\"high\"}}");
        assertEquals("OK", client.chat(profile, settings(), "system", "user"));
        assertEquals("high", transport.body(2).get("reasoning_effort").getAsString());
        assertEquals(3, transport.attempts);
    }

    @Test
    public void chatTemperatureRetryKeepsTheConfiguredReasoningMapping() throws Exception {
        Transport transport = new Transport();
        transport.reply(400, TEMPERATURE_ERROR);
        transport.reply(200, CHAT_OK);
        LlmProfile profile = profile(LlmProvider.OPENAI_COMPATIBLE,
                "{\"HIGH\":{\"reasoning_effort\":\"high\"}}");
        assertEquals("OK", new ChatClient(transport).chat(profile, settings(), "system", "user"));

        assertFalse(transport.body(1).has("temperature"));
        assertEquals("high", transport.body(1).get("reasoning_effort").getAsString());
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    @Test
    public void responsesTemperatureRetryKeepsTheConfiguredReasoningMapping() throws Exception {
        Transport transport = new Transport();
        transport.reply(400, TEMPERATURE_ERROR);
        transport.reply(200, "{\"output_text\":\"OK\"}");
        LlmProfile profile = profile(LlmProvider.OPENAI_RESPONSES,
                "{\"HIGH\":{\"reasoning\":{\"effort\":\"high\"}}}");
        assertEquals("OK", new ResponsesClient(transport).chat(profile, settings(), "system", "user"));

        assertFalse(transport.body(1).has("temperature"));
        assertEquals("high", transport.body(1).getAsJsonObject("reasoning").get("effort").getAsString());
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    @Test
    public void anthropicTemperatureRetryKeepsTheConfiguredReasoningMapping() throws Exception {
        Transport transport = new Transport();
        transport.reply(400, TEMPERATURE_ERROR);
        transport.reply(200, "{\"content\":[{\"type\":\"text\",\"text\":\"OK\"}]}");
        LlmProfile profile = profile(LlmProvider.ANTHROPIC,
                "{\"HIGH\":{\"thinking\":{\"type\":\"adaptive\"},\"output_config\":{\"effort\":\"high\"}}}");
        assertEquals("OK", new AnthropicClient(transport).chat(profile, settings(), "system", "user"));

        assertEquals("adaptive", transport.body(1).getAsJsonObject("thinking").get("type").getAsString());
        assertEquals("high", transport.body(1).getAsJsonObject("output_config").get("effort").getAsString());
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    @Test
    public void customFieldRejectionRetriesForEveryProtocolAndStreamingMode() throws Exception {
        for (LlmProvider provider : LlmProvider.values()) {
            for (boolean stream : new boolean[]{false, true}) {
                LlmCapabilityCache.clearForTests();
                Transport transport = new Transport();
                transport.reply(400, "{\"error\":{\"message\":\"Extra inputs are not permitted\","
                        + "\"param\":\"vendor_reasoning.effort\"}}");
                transport.reply(200, successResponse(provider, stream));
                LlmProfile profile = profile(provider,
                        "{\"HIGH\":{\"vendor_reasoning\":{\"effort\":\"high\"}}}");

                assertEquals(provider.name(), "OK", request(client(provider, transport), profile, stream));
                assertEquals(2, transport.attempts);
                assertTrue(transport.body(0).has("vendor_reasoning"));
                assertFalse(transport.body(1).has("vendor_reasoning"));
                assertTrue(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
            }
        }
    }

    @Test
    public void thinkingRetryNeverIntroducesTemperatureForAnyProtocol() throws Exception {
        for (LlmProvider provider : LlmProvider.values()) {
            for (boolean stream : new boolean[]{false, true}) {
                LlmCapabilityCache.clearForTests();
                Transport transport = new Transport();
                transport.reply(400, "{\"error\":{\"message\":\"Unsupported parameter: thinking\","
                        + "\"param\":\"thinking\"}}");
                transport.reply(200, successResponse(provider, stream));
                LlmProfile profile = profile(provider,
                        "{\"HIGH\":{\"thinking\":{\"type\":\"adaptive\"}}}");
                if (provider != LlmProvider.ANTHROPIC) {
                    profile.setModel("gpt-5-mini");
                }

                assertEquals("OK", request(client(provider, transport), profile, stream));
                assertFalse(transport.body(0).has("temperature"));
                assertFalse("retry must preserve omitted temperature", transport.body(1).has("temperature"));
                assertFalse(transport.body(1).has("thinking"));
            }
        }
    }

    @Test
    public void colonDelimitedThinkingErrorsCanRetry() throws Exception {
        Transport transport = new Transport();
        transport.reply(400, "{\"error\":{\"message\":\"thinking: Extra inputs are not permitted\"}}");
        transport.reply(200, CHAT_OK);
        LlmProfile profile = profile(LlmProvider.OPENAI_COMPATIBLE,
                "{\"HIGH\":{\"thinking\":{\"type\":\"adaptive\"}}}");

        assertEquals("OK", new ChatClient(transport).chat(profile, settings(), "system", "user"));
        assertFalse(transport.body(1).has("thinking"));
    }

    @Test
    public void unrelatedThinkingFieldRejectionDoesNotPoisonTheMappingCache() throws Exception {
        for (LlmProvider provider : LlmProvider.values()) {
            Transport transport = new Transport();
            transport.reply(400, "{\"error\":{\"message\":\"Unsupported parameter: thinking\","
                    + "\"param\":\"thinking\"}}");
            LlmProfile profile = profile(provider,
                    "{\"HIGH\":{\"vendor_reasoning\":{\"effort\":\"high\"}}}");

            assertThrows(IOException.class, () -> request(client(provider, transport), profile, false));
            assertEquals(1, transport.attempts);
            assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
        }
    }

    @Test
    public void similarFieldNamesAreNotMistakenForTheConfiguredThinkingField() throws Exception {
        Transport transport = new Transport();
        transport.reply(400, "{\"error\":{\"message\":\"Unsupported parameter: reasoning_effort_extra\"}}");
        LlmProfile profile = profile(LlmProvider.OPENAI_COMPATIBLE,
                "{\"HIGH\":{\"reasoning_effort\":\"high\"}}");

        assertThrows(IOException.class, () -> new ChatClient(transport).chat(
                profile, settings(), "system", "user"));
        assertEquals(1, transport.attempts);
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    @Test
    public void unrelatedTokenErrorsDoNotDiscardThinkingMappings() throws Exception {
        Transport transport = new Transport();
        transport.reply(400, "{\"error\":{\"message\":\"Unsupported parameter: max_completion_tokens\","
                + "\"param\":\"max_completion_tokens\"}}");
        transport.reply(200, CHAT_OK);
        LlmProfile profile = profile(LlmProvider.OPENAI_COMPATIBLE,
                "{\"HIGH\":{\"vendor_reasoning\":{\"effort\":\"high\"}}}");
        profile.setModel("gpt-5-mini");

        assertEquals("OK", new ChatClient(transport).chat(profile, settings(), "system", "user"));
        assertTrue(transport.body(1).has("vendor_reasoning"));
        assertFalse(transport.body(1).has("max_completion_tokens"));
        assertTrue(transport.body(1).has("max_tokens"));
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    @Test
    public void authenticationFailureIsNotRetriedWithoutReasoning() throws Exception {
        Transport transport = new Transport();
        transport.reply(401, "{\"error\":{\"message\":\"Invalid API key\"}}");
        LlmProfile profile = profile(LlmProvider.OPENAI_COMPATIBLE,
                "{\"HIGH\":{\"reasoning_effort\":\"high\"}}");
        assertThrows(IOException.class, () -> new ChatClient(transport).chat(
                profile, settings(), "system", "user"));
        assertEquals(1, transport.attempts);
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    private static LlmProviderClient client(LlmProvider provider, Transport transport) {
        switch (provider) {
            case ANTHROPIC:
                return new AnthropicClient(transport);
            case OPENAI_RESPONSES:
                return new ResponsesClient(transport);
            default:
                return new ChatClient(transport);
        }
    }

    private static String request(LlmProviderClient client, LlmProfile profile, boolean stream) throws IOException {
        if (!stream) {
            return client.chat(profile, settings(), "system", "user");
        }
        StringBuilder text = new StringBuilder();
        client.streamChat(profile, settings(), "system", "user", text::append);
        return text.toString();
    }

    private static String successResponse(LlmProvider provider, boolean stream) {
        switch (provider) {
            case ANTHROPIC:
                return stream
                        ? "event: content_block_delta\ndata: {\"delta\":{\"type\":\"text_delta\",\"text\":\"OK\"}}\n\n"
                        : "{\"content\":[{\"type\":\"text\",\"text\":\"OK\"}]}";
            case OPENAI_RESPONSES:
                return stream
                        ? "data: {\"type\":\"response.output_text.delta\",\"delta\":\"OK\"}\n\n"
                        : "{\"output_text\":\"OK\"}";
            default:
                return stream
                        ? "data: {\"choices\":[{\"delta\":{\"content\":\"OK\"}}]}\n\ndata: [DONE]\n\n"
                        : CHAT_OK;
        }
    }

    private static LlmProfile profile(LlmProvider provider, String mapping) {
        LlmProfile profile = new LlmProfile();
        profile.setProvider(provider);
        profile.setBaseUrl("https://mock.example/v1");
        profile.setApiKey("test-key");
        profile.setModel("custom-model");
        profile.setThinkingParameterPreset(ThinkingParameterPreset.CUSTOM);
        profile.setThinkingParameterMapping(mapping);
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        return profile;
    }

    private static LlmSettings settings() {
        LlmSettings settings = new LlmSettings();
        settings.setTemperature(0.5D);
        return settings;
    }

    private static final class Transport {
        private final List<Connection> connections = new ArrayList<>();
        private int attempts;

        void reply(int status, String body) throws Exception {
            connections.add(new Connection(status, body));
        }

        HttpURLConnection open() {
            return connections.get(attempts++);
        }

        JsonObject body(int index) {
            return JsonParser.parseString(connections.get(index).request.toString(StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static final class Connection extends HttpURLConnection {
        private final byte[] response;
        private final ByteArrayOutputStream request = new ByteArrayOutputStream();

        Connection(int status, String body) throws Exception {
            super(new URL("https://mock.example"));
            responseCode = status;
            response = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public OutputStream getOutputStream() {
            return request;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(response);
        }

        @Override
        public InputStream getErrorStream() {
            return getInputStream();
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }
    }

    private static final class ChatClient extends OpenAiCompatibleLlmProviderClient {
        private final Transport transport;

        ChatClient(Transport transport) {
            this.transport = transport;
        }

        @Override
        protected HttpURLConnection openPostConnection(String endpoint) {
            return transport.open();
        }

        @Override
        protected int getResponseCode(HttpURLConnection connection) throws IOException {
            return connection.getResponseCode();
        }

        @Override
        protected String readLine(BufferedReader reader) throws IOException {
            return reader.readLine();
        }
    }

    private static final class ResponsesClient extends OpenAiResponsesLlmProviderClient {
        private final Transport transport;

        ResponsesClient(Transport transport) {
            this.transport = transport;
        }

        @Override
        protected HttpURLConnection openPostConnection(String endpoint) {
            return transport.open();
        }

        @Override
        protected int getResponseCode(HttpURLConnection connection) throws IOException {
            return connection.getResponseCode();
        }

        @Override
        protected String readLine(BufferedReader reader) throws IOException {
            return reader.readLine();
        }
    }

    private static final class AnthropicClient extends AnthropicLlmProviderClient {
        private final Transport transport;

        AnthropicClient(Transport transport) {
            this.transport = transport;
        }

        @Override
        protected HttpURLConnection openPostConnection(String endpoint) {
            return transport.open();
        }

        @Override
        protected int getResponseCode(HttpURLConnection connection) throws IOException {
            return connection.getResponseCode();
        }

        @Override
        protected String readLine(BufferedReader reader) throws IOException {
            return reader.readLine();
        }
    }
}
