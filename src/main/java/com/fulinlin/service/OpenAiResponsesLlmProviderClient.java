package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Client for the OpenAI Responses API ({@code POST /v1/responses}), the third protocol alongside
 * Chat Completions and Anthropic Messages. The request carries the system prompt in
 * {@code instructions}, the user turn in {@code input}, the response budget in
 * {@code max_output_tokens}, and reasoning depth in {@code reasoning.effort}.
 */
class OpenAiResponsesLlmProviderClient extends AbstractHttpLlmProviderClient {

    private static final String RESPONSES_PATH = "/responses";
    private static final String MODELS_PATH = "/models";
    private static final Gson GSON = new Gson();

    @Override
    @NotNull
    public String chat(@NotNull LlmProfile profile,
                       @NotNull LlmSettings settings,
                       @NotNull String systemPrompt,
                       @NotNull String userPrompt) throws IOException {
        return chat(profile, settings, systemPrompt, userPrompt, new LlmRequestDiagnostics());
    }

    @Override
    @NotNull
    public String chat(@NotNull LlmProfile profile,
                       @NotNull LlmSettings settings,
                       @NotNull String systemPrompt,
                       @NotNull String userPrompt,
                       @NotNull LlmRequestDiagnostics diagnostics) throws IOException {
        boolean thinkingRequested = LlmThinkingParameters.isRequested(profile);
        boolean thinkingSkippedByCache = thinkingRequested
                && LlmCapabilityCache.shouldSkipThinkingParameters(profile);
        boolean thinkingEnabled = thinkingRequested && !thinkingSkippedByCache;
        HttpURLConnection connection = createConnection(profile);
        JsonObject requestBody = createRequestBody(
                profile, settings, systemPrompt, userPrompt, false,
                thinkingEnabled, thinkingRequested, thinkingSkippedByCache, diagnostics
        );
        write(connection, GSON.toJson(requestBody));

        int responseCode = getResponseCode(connection);
        InputStream inputStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (responseCode < 200 || responseCode >= 300) {
            String errorBody = readAll(inputStream);
            connection.disconnect();
            if (shouldRetryWithAdjustedParameters(profile, thinkingEnabled, errorBody, diagnostics)) {
                connection = createConnection(profile);
                requestBody = createRequestBodyForRetry(
                        profile, settings, systemPrompt, userPrompt, false,
                        thinkingRequested, diagnostics
                );
                write(connection, GSON.toJson(requestBody));
                responseCode = getResponseCode(connection);
                inputStream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                if (responseCode < 200 || responseCode >= 300) {
                    String retryErrorBody = readAll(inputStream);
                    connection.disconnect();
                    throw new IOException(extractErrorMessage(retryErrorBody));
                }
            } else {
                throw new IOException(extractErrorMessage(errorBody));
            }
        }

        try {
            String responseBody = readAll(inputStream);
            String contentType = connection.getHeaderField("Content-Type");
            if (isEventStream(contentType, responseBody)) {
                return extractChatResponseFromEventStream(responseBody);
            }
            return extractChatResponse(responseBody);
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void streamChat(@NotNull LlmProfile profile,
                           @NotNull LlmSettings settings,
                           @NotNull String systemPrompt,
                           @NotNull String userPrompt,
                           @NotNull Consumer<String> onDelta) throws IOException {
        streamChat(profile, settings, systemPrompt, userPrompt, onDelta, new LlmRequestDiagnostics());
    }

    @Override
    public void streamChat(@NotNull LlmProfile profile,
                           @NotNull LlmSettings settings,
                           @NotNull String systemPrompt,
                           @NotNull String userPrompt,
                           @NotNull Consumer<String> onDelta,
                           @NotNull LlmRequestDiagnostics diagnostics) throws IOException {
        boolean thinkingRequested = LlmThinkingParameters.isRequested(profile);
        boolean thinkingSkippedByCache = thinkingRequested
                && LlmCapabilityCache.shouldSkipThinkingParameters(profile);
        boolean thinkingEnabled = thinkingRequested && !thinkingSkippedByCache;
        HttpURLConnection connection = createConnection(profile);
        JsonObject requestBody = createRequestBody(
                profile, settings, systemPrompt, userPrompt, true,
                thinkingEnabled, thinkingRequested, thinkingSkippedByCache, diagnostics
        );
        write(connection, GSON.toJson(requestBody));

        int responseCode = getResponseCode(connection);
        InputStream inputStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (responseCode < 200 || responseCode >= 300) {
            String errorBody = readAll(inputStream);
            connection.disconnect();
            if (shouldRetryWithAdjustedParameters(profile, thinkingEnabled, errorBody, diagnostics)) {
                connection = createConnection(profile);
                requestBody = createRequestBodyForRetry(
                        profile, settings, systemPrompt, userPrompt, true,
                        thinkingRequested, diagnostics
                );
                write(connection, GSON.toJson(requestBody));
                responseCode = getResponseCode(connection);
                inputStream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                if (responseCode < 200 || responseCode >= 300) {
                    String retryErrorBody = readAll(inputStream);
                    connection.disconnect();
                    throw new IOException(extractErrorMessage(retryErrorBody));
                }
            } else {
                throw new IOException(extractErrorMessage(errorBody));
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            String currentEvent = "";
            boolean emittedDelta = false;
            String completedText = "";
            while ((line = readLine(reader)) != null) {
                String normalizedLine = line.trim();
                if (normalizedLine.isEmpty()) {
                    continue;
                }
                if (normalizedLine.startsWith("event:")) {
                    currentEvent = normalizedLine.substring(6).trim();
                    continue;
                }
                if (!normalizedLine.startsWith("data:")) {
                    continue;
                }
                String payload = normalizedLine.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    continue;
                }
                if (isCompletionEvent(currentEvent, payload)) {
                    completedText = extractStreamDelta(currentEvent, payload);
                    continue;
                }
                String delta = extractStreamDelta(currentEvent, payload);
                if (!delta.isEmpty()) {
                    emittedDelta = true;
                    onDelta.accept(delta);
                }
            }
            // Some gateways omit the delta events and send only the final response; use it
            // instead of the deltas, never in addition to them.
            if (!emittedDelta && !completedText.isEmpty()) {
                onDelta.accept(completedText);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean isCompletionEvent(@NotNull String eventType, @NotNull String payload) {
        JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
        String type = jsonObject.has("type") && !jsonObject.get("type").isJsonNull()
                ? jsonObject.get("type").getAsString()
                : eventType;
        return "response.completed".equals(type) || "response.done".equals(type);
    }

    @Override
    @NotNull
    public java.util.List<String> listModels(@NotNull LlmProfile profile) throws IOException {
        HttpURLConnection connection = createModelListConnection(profile);
        try {
            int responseCode = getResponseCode(connection);
            InputStream inputStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readAll(inputStream);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(extractErrorMessage(responseBody));
            }
            return OpenAiCompatibleLlmProviderClient.extractModelIds(responseBody);
        } catch (ProcessCanceledException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IOException("Failed to parse model list response.", ex);
        } finally {
            connection.disconnect();
        }
    }

    @NotNull
    static JsonObject createRequestBody(@NotNull LlmProfile profile,
                                        @NotNull LlmSettings settings,
                                        @NotNull String systemPrompt,
                                        @NotNull String userPrompt,
                                        boolean stream) {
        return createRequestBody(profile, settings, systemPrompt, userPrompt, stream,
                LlmThinkingParameters.isRequested(profile), false);
    }

    @NotNull
    static JsonObject createRequestBody(@NotNull LlmProfile profile,
                                        @NotNull LlmSettings settings,
                                        @NotNull String systemPrompt,
                                        @NotNull String userPrompt,
                                        boolean stream,
                                        boolean thinkingEnabled,
                                        boolean omitTemperature) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", profile.getModel().trim());
        requestBody.addProperty("stream", stream);
        requestBody.addProperty("instructions", systemPrompt);
        requestBody.addProperty("input", userPrompt);
        requestBody.addProperty("max_output_tokens", profile.resolveMaxResponseTokens());
        if (thinkingEnabled) {
            LlmThinkingParameters.apply(requestBody, profile);
        }
        // The Responses API drops sampling parameters for reasoning models, so temperature is
        // only sent for requests that do not ask for thinking.
        if (!omitTemperature && !thinkingEnabled && settings.getTemperature() != null) {
            requestBody.addProperty("temperature", settings.getTemperature());
        }
        return requestBody;
    }

    @NotNull
    private static JsonObject createRequestBody(@NotNull LlmProfile profile,
                                                @NotNull LlmSettings settings,
                                                @NotNull String systemPrompt,
                                                @NotNull String userPrompt,
                                                boolean stream,
                                                boolean thinkingEnabled,
                                                boolean thinkingRequested,
                                                boolean thinkingSkippedByCache,
                                                @NotNull LlmRequestDiagnostics diagnostics) {
        JsonObject requestBody = createRequestBody(profile, settings, systemPrompt, userPrompt, stream,
                thinkingEnabled, false);
        diagnostics.recordRequest(profile, stream, thinkingRequested, thinkingSkippedByCache, requestBody);
        return requestBody;
    }

    /**
     * Retry request after the provider rejected the original one: thinking parameters are
     * dropped and temperature is omitted, since gateways that reject reasoning usually reject
     * sampling parameters on reasoning models too.
     */
    @NotNull
    private static JsonObject createRequestBodyForRetry(@NotNull LlmProfile profile,
                                                        @NotNull LlmSettings settings,
                                                        @NotNull String systemPrompt,
                                                        @NotNull String userPrompt,
                                                        boolean stream,
                                                        boolean thinkingRequested,
                                                        @NotNull LlmRequestDiagnostics diagnostics) {
        JsonObject requestBody = createRequestBody(profile, settings, systemPrompt, userPrompt, stream,
                false, true);
        diagnostics.recordRequest(profile, stream, thinkingRequested, false, requestBody);
        return requestBody;
    }

    private static boolean shouldRetryWithAdjustedParameters(@NotNull LlmProfile profile,
                                                             boolean thinkingEnabled,
                                                             @NotNull String errorBody,
                                                             @NotNull LlmRequestDiagnostics diagnostics) {
        boolean temperatureRejected = errorIndicatesUnsupportedTemperature(errorBody);
        boolean thinkingRejected = thinkingEnabled && shouldRetryWithoutThinkingParameters(errorBody);
        if (thinkingRejected) {
            LlmCapabilityCache.markThinkingParametersUnsupported(profile);
            diagnostics.markThinkingFallbackUsed();
        }
        if (temperatureRejected) {
            diagnostics.markTemperatureFallbackUsed();
        }
        return thinkingRejected || temperatureRejected;
    }

    @NotNull
    static String extractChatResponse(@NotNull String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        return extractOutputText(jsonObject);
    }

    /**
     * Text of a complete SSE transcript. Deltas are preferred; the final
     * {@code response.completed} payload is used only when a gateway skipped the deltas, so the
     * same text is never counted twice.
     */
    @NotNull
    static String extractChatResponseFromEventStream(@NotNull String responseBody) {
        StringBuilder builder = new StringBuilder();
        String currentEvent = "";
        String completedText = "";
        for (String rawLine : responseBody.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
                continue;
            }
            if (!line.startsWith("data:")) {
                continue;
            }
            String payload = line.substring(5).trim();
            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                continue;
            }
            if (isCompletionEvent(currentEvent, payload)) {
                completedText = extractStreamDelta(currentEvent, payload);
                continue;
            }
            builder.append(extractStreamDelta(currentEvent, payload));
        }
        return builder.length() > 0 ? builder.toString() : completedText;
    }

    /**
     * Streaming delta for one SSE payload. The final {@code response.completed} event carries the
     * whole response and is used to recover text when a gateway omits the delta events.
     */
    @NotNull
    static String extractStreamDelta(@NotNull String eventType, @NotNull String payload) {
        JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
        String type = jsonObject.has("type") && !jsonObject.get("type").isJsonNull()
                ? jsonObject.get("type").getAsString()
                : eventType;
        if ("response.output_text.delta".equals(type)) {
            return jsonObject.has("delta") && !jsonObject.get("delta").isJsonNull()
                    ? jsonObject.get("delta").getAsString()
                    : "";
        }
        if ("response.completed".equals(type) || "response.done".equals(type)) {
            JsonObject response = jsonObject.has("response") && jsonObject.get("response").isJsonObject()
                    ? jsonObject.getAsJsonObject("response")
                    : jsonObject;
            return extractOutputText(response);
        }
        return "";
    }

    /**
     * Text carried by a non-streaming response: {@code output_text} when present, otherwise the
     * {@code output[].content[].text} parts of completed message items.
     */
    @NotNull
    private static String extractOutputText(@NotNull JsonObject response) {
        if (response.has("output_text") && !response.get("output_text").isJsonNull()) {
            return response.get("output_text").getAsString();
        }
        StringBuilder builder = new StringBuilder();
        JsonArray output = response.getAsJsonArray("output");
        if (output == null) {
            return "";
        }
        for (JsonElement itemElement : output) {
            if (!itemElement.isJsonObject()) {
                continue;
            }
            JsonObject item = itemElement.getAsJsonObject();
            if (item.has("type") && !item.get("type").isJsonNull()
                    && !"message".equals(item.get("type").getAsString())) {
                continue;
            }
            JsonElement content = item.get("content");
            if (content == null || content.isJsonNull()) {
                continue;
            }
            builder.append(new OpenAiResponsesLlmProviderClient().extractTextParts(content));
        }
        return builder.toString();
    }

    @Override
    @NotNull
    public String extractErrorMessage(@NotNull String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject error = jsonObject.has("error") && jsonObject.get("error").isJsonObject()
                    ? jsonObject.getAsJsonObject("error")
                    : null;
            if (error != null && error.has("message") && !error.get("message").isJsonNull()) {
                return error.get("message").getAsString();
            }
        } catch (RuntimeException ignored) {
            // Fall back to the raw body for non-JSON errors.
        }
        return responseBody;
    }

    @NotNull
    private HttpURLConnection createConnection(@NotNull LlmProfile profile) throws IOException {
        HttpURLConnection connection = openPostConnection(resolveResponsesEndpoint(profile));
        connection.setRequestProperty("Authorization", "Bearer " + profile.getApiKey().trim());
        return connection;
    }

    @NotNull
    private HttpURLConnection createModelListConnection(@NotNull LlmProfile profile) throws IOException {
        HttpURLConnection connection = openGetConnection(resolveModelsEndpoint(profile));
        String apiKey = profile.getApiKey() == null ? "" : profile.getApiKey().trim();
        if (!apiKey.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        return connection;
    }

    @NotNull
    static String resolveResponsesEndpoint(@NotNull LlmProfile profile) {
        String baseUrl = new OpenAiResponsesLlmProviderClient().stripTrailingSlash(profile.getBaseUrl().trim());
        if (baseUrl.endsWith(RESPONSES_PATH)) {
            return baseUrl;
        }
        return new OpenAiResponsesLlmProviderClient().resolveEndpoint(profile, RESPONSES_PATH);
    }

    @NotNull
    static String resolveModelsEndpoint(@NotNull LlmProfile profile) {
        String baseUrl = new OpenAiResponsesLlmProviderClient().stripTrailingSlash(profile.getBaseUrl().trim());
        if (baseUrl.endsWith(MODELS_PATH)) {
            return baseUrl;
        }
        if (baseUrl.endsWith(RESPONSES_PATH)) {
            return baseUrl.substring(0, baseUrl.length() - RESPONSES_PATH.length()) + MODELS_PATH;
        }
        return new OpenAiResponsesLlmProviderClient().resolveEndpoint(profile, MODELS_PATH);
    }

    static boolean isEventStream(@Nullable String contentType, @NotNull String responseBody) {
        return contentType != null && contentType.contains("text/event-stream")
                || responseBody.startsWith("data:")
                || responseBody.startsWith("event:");
    }
}
