package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

class OpenAiCompatibleLlmProviderClient extends AbstractHttpLlmProviderClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
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
            RetryPlan retryPlan = planRetry(profile, thinkingEnabled, errorBody, diagnostics);
            if (retryPlan.retry) {
                connection = createConnection(profile);
                requestBody = createRequestBodyForRetry(
                        profile, settings, systemPrompt, userPrompt, false,
                        thinkingRequested, retryPlan.thinkingEnabled, retryPlan.omitTemperature,
                        retryPlan.useCompletionTokens, diagnostics
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
            return extractChatResponse(readAll(inputStream));
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
            RetryPlan retryPlan = planRetry(profile, thinkingEnabled, errorBody, diagnostics);
            if (retryPlan.retry) {
                connection = createConnection(profile);
                requestBody = createRequestBodyForRetry(
                        profile, settings, systemPrompt, userPrompt, true,
                        thinkingRequested, retryPlan.thinkingEnabled, retryPlan.omitTemperature,
                        retryPlan.useCompletionTokens, diagnostics
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
            while ((line = readLine(reader)) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                String deltaText = extractStreamDelta(payload);
                if (!deltaText.isEmpty()) {
                    onDelta.accept(deltaText);
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    @Override
    @NotNull
    public List<String> listModels(@NotNull LlmProfile profile) throws IOException {
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
            return extractModelIds(responseBody);
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
        boolean thinkingEnabled = LlmThinkingParameters.isRequested(profile);
        return createRequestBody(profile, settings, systemPrompt, userPrompt, stream,
                thinkingEnabled,
                LlmThinkingParameters.isOpenAiReasoningModel(profile),
                LlmThinkingParameters.needsCompletionTokenLimit(profile));
    }

    @NotNull
    static JsonObject createRequestBody(@NotNull LlmProfile profile,
                                        @NotNull LlmSettings settings,
                                        @NotNull String systemPrompt,
                                        @NotNull String userPrompt,
                                        boolean stream,
                                        boolean thinkingEnabled,
                                        boolean omitTemperature,
                                        boolean useCompletionTokens) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", profile.getModel().trim());
        requestBody.addProperty("stream", stream);
        int maxResponseTokens = profile.resolveMaxResponseTokens();
        if (useCompletionTokens) {
            requestBody.addProperty("max_completion_tokens", maxResponseTokens);
        } else {
            requestBody.addProperty("max_tokens", maxResponseTokens);
        }
        if (thinkingEnabled) {
            LlmThinkingParameters.apply(requestBody, profile);
        }
        if (!omitTemperature && settings.getTemperature() != null) {
            requestBody.addProperty("temperature", settings.getTemperature());
        }
        JsonArray messages = new JsonArray();
        messages.add(createMessage("system", systemPrompt));
        messages.add(createMessage("user", userPrompt));
        requestBody.add("messages", messages);
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
                thinkingEnabled,
                LlmThinkingParameters.isOpenAiReasoningModel(profile),
                resolveCompletionTokens(profile));
        diagnostics.recordRequest(profile, stream, thinkingRequested, thinkingSkippedByCache, requestBody);
        return requestBody;
    }

    /**
     * Whether this request uses {@code max_completion_tokens}: only when the model calls for it
     * and a previous attempt has not shown the gateway rejects that field.
     */
    private static boolean resolveCompletionTokens(@NotNull LlmProfile profile) {
        return LlmThinkingParameters.needsCompletionTokenLimit(profile)
                && !LlmCapabilityCache.shouldSkipCompletionTokens(profile);
    }

    /**
     * Retry request after the provider rejected one of its parameters, retaining fields that
     * were not implicated in the error.
     */
    @NotNull
    private static JsonObject createRequestBodyForRetry(@NotNull LlmProfile profile,
                                                        @NotNull LlmSettings settings,
                                                        @NotNull String systemPrompt,
                                                        @NotNull String userPrompt,
                                                        boolean stream,
                                                        boolean thinkingRequested,
                                                        boolean thinkingEnabled,
                                                        boolean omitTemperature,
                                                        boolean useCompletionTokens,
                                                        @NotNull LlmRequestDiagnostics diagnostics) {
        JsonObject requestBody = createRequestBody(profile, settings, systemPrompt, userPrompt, stream,
                thinkingEnabled,
                LlmThinkingParameters.isOpenAiReasoningModel(profile) || omitTemperature,
                useCompletionTokens);
        diagnostics.recordRequest(profile, stream, thinkingRequested, false, requestBody);
        return requestBody;
    }

    /**
     * Decides how to retry a rejected request. {@code max_completion_tokens} and thinking
     * parameters are independent capabilities, and a model may need either field shape dropped
     * or kept; the decision is recorded per model so later requests go straight to the working
     * combination.
     */
    @NotNull
    private static RetryPlan planRetry(@NotNull LlmProfile profile,
                                       boolean thinkingEnabled,
                                       @NotNull String errorBody,
                                       @NotNull LlmRequestDiagnostics diagnostics) {
        boolean temperatureRejected = errorIndicatesUnsupportedTemperature(errorBody);
        boolean thinkingRejected = thinkingEnabled && shouldRetryWithoutThinkingParameters(profile, errorBody);
        boolean completionTokenRejected = LlmThinkingParameters.needsCompletionTokenLimit(profile)
                && errorIndicatesUnsupportedCompletionTokens(errorBody);
        if (thinkingRejected) {
            LlmCapabilityCache.markThinkingParametersUnsupported(profile);
            diagnostics.markThinkingFallbackUsed();
        }
        if (completionTokenRejected) {
            LlmCapabilityCache.markCompletionTokensUnsupported(profile);
            diagnostics.markCompletionTokenFallbackUsed();
        }
        if (temperatureRejected) {
            diagnostics.markTemperatureFallbackUsed();
        }
        if (!thinkingRejected && !completionTokenRejected && !temperatureRejected) {
            return RetryPlan.none();
        }
        boolean useCompletionTokens = LlmThinkingParameters.needsCompletionTokenLimit(profile)
                && !LlmCapabilityCache.shouldSkipCompletionTokens(profile);
        return new RetryPlan(
                true,
                useCompletionTokens,
                thinkingEnabled && !thinkingRejected,
                LlmThinkingParameters.isOpenAiReasoningModel(profile) || temperatureRejected
        );
    }

    static boolean errorIndicatesUnsupportedCompletionTokens(@NotNull String responseBody) {
        String lower = responseBody.toLowerCase(Locale.ROOT);
        return lower.contains("max_completion_tokens")
                && (lower.contains("unsupported")
                || lower.contains("unknown parameter")
                || lower.contains("unrecognized")
                || lower.contains("invalid")
                || lower.contains("not support")
                || lower.contains("not_supported"));
    }

    /**
     * Outcome of interpreting a provider error: whether to retry at all, and which token-limit
     * field the retry should use.
     */
    private static final class RetryPlan {
        private final boolean retry;
        private final boolean useCompletionTokens;
        private final boolean thinkingEnabled;
        private final boolean omitTemperature;

        private RetryPlan(boolean retry, boolean useCompletionTokens,
                          boolean thinkingEnabled, boolean omitTemperature) {
            this.retry = retry;
            this.useCompletionTokens = useCompletionTokens;
            this.thinkingEnabled = thinkingEnabled;
            this.omitTemperature = omitTemperature;
        }

        @NotNull
        private static RetryPlan none() {
            return new RetryPlan(false, false, false, false);
        }
    }

    @NotNull
    static String extractChatResponse(@NotNull String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray choices = jsonObject.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            return "";
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject message = choice.has("message") ? choice.getAsJsonObject("message") : null;
        if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
            return extractContent(message);
        }
        return "";
    }

    @NotNull
    static String extractStreamDelta(@NotNull String payload) {
        JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
        JsonArray choices = jsonObject.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            return "";
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject delta = choice.has("delta") ? choice.getAsJsonObject("delta") : null;
        if (delta != null && delta.has("content") && !delta.get("content").isJsonNull()) {
            return delta.get("content").getAsString();
        }
        JsonObject message = choice.has("message") ? choice.getAsJsonObject("message") : null;
        if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
            return extractContent(message);
        }
        return "";
    }

    @NotNull
    static List<String> extractModelIds(@NotNull String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        Set<String> modelIds = new LinkedHashSet<>();
        collectModelIds(jsonObject.getAsJsonArray("data"), modelIds);
        if (modelIds.isEmpty() && jsonObject.has("models") && jsonObject.get("models").isJsonArray()) {
            collectModelIds(jsonObject.getAsJsonArray("models"), modelIds);
        }
        return new ArrayList<>(modelIds);
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
        HttpURLConnection connection = openPostConnection(resolveEndpoint(profile, "/chat/completions"));
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
    static String resolveModelsEndpoint(@NotNull LlmProfile profile) {
        String baseUrl = new OpenAiCompatibleLlmProviderClient().stripTrailingSlash(profile.getBaseUrl().trim());
        if (baseUrl.endsWith("/models")) {
            return baseUrl;
        }
        if (baseUrl.endsWith(CHAT_COMPLETIONS_PATH)) {
            return baseUrl.substring(0, baseUrl.length() - CHAT_COMPLETIONS_PATH.length()) + "/models";
        }
        return new OpenAiCompatibleLlmProviderClient().resolveEndpoint(profile, "/models");
    }

    @NotNull
    private static JsonObject createMessage(@NotNull String role, @NotNull String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    @NotNull
    private static String extractContent(@NotNull JsonObject message) {
        return message.get("content").isJsonPrimitive()
                ? message.get("content").getAsString()
                : new OpenAiCompatibleLlmProviderClient().extractTextParts(message.get("content"));
    }

    private static void collectModelIds(JsonArray models, @NotNull Set<String> modelIds) {
        if (models == null) {
            return;
        }
        for (int i = 0; i < models.size(); i++) {
            String modelId = extractModelId(models.get(i));
            if (!modelId.isEmpty()) {
                modelIds.add(modelId);
            }
        }
    }

    @NotNull
    private static String extractModelId(@NotNull com.google.gson.JsonElement modelElement) {
        if (modelElement.isJsonPrimitive()) {
            return modelElement.getAsString().trim();
        }
        if (!modelElement.isJsonObject()) {
            return "";
        }
        JsonObject model = modelElement.getAsJsonObject();
        if (model.has("id") && !model.get("id").isJsonNull()) {
            return model.get("id").getAsString().trim();
        }
        if (model.has("name") && !model.get("name").isJsonNull()) {
            return model.get("name").getAsString().trim();
        }
        return "";
    }
}
