package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

abstract class AbstractHttpLlmProviderClient implements LlmProviderClient {

    protected static final int MAX_RESPONSE_TOKENS = 4096;

    @NotNull
    protected HttpURLConnection openPostConnection(@NotNull String endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(0);
        connection.setRequestProperty("Content-Type", "application/json");
        return connection;
    }

    @NotNull
    protected HttpURLConnection openGetConnection(@NotNull String endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    protected void write(@NotNull HttpURLConnection connection, @NotNull String body) throws IOException {
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    @NotNull
    protected String readAll(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "Request failed";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    @NotNull
    protected String resolveEndpoint(@NotNull LlmProfile profile, @NotNull String expectedPath) {
        String baseUrl = profile.getBaseUrl().trim();
        return baseUrl.endsWith(expectedPath) ? baseUrl : stripTrailingSlash(baseUrl) + expectedPath;
    }

    @NotNull
    protected String stripTrailingSlash(@NotNull String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @NotNull
    protected String extractTextParts(@NotNull JsonElement content) {
        if (content.isJsonPrimitive()) {
            return content.getAsString();
        }
        if (!content.isJsonArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonElement element : content.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject part = element.getAsJsonObject();
            if (part.has("text") && !part.get("text").isJsonNull()) {
                builder.append(part.get("text").getAsString());
            }
        }
        return builder.toString();
    }

    protected static boolean isReasoningCompatibilityEnabled(@NotNull LlmProfile profile) {
        return Boolean.TRUE.equals(profile.getReasoningCompatibilityEnabled());
    }

    protected static boolean shouldUseCompletionTokenLimit(@NotNull LlmProfile profile) {
        return isReasoningCompatibilityEnabled(profile)
                && (isOpenAiReasoningModel(profile) || containsProfileText(profile, "mimo", "xiaomimimo", "token-plan"));
    }

    protected static void applyReasoningCompatibility(@NotNull JsonObject requestBody, @NotNull LlmProfile profile) {
        applyReasoningCompatibility(requestBody, profile, isReasoningCompatibilityEnabled(profile));
    }

    protected static void applyReasoningCompatibility(@NotNull JsonObject requestBody,
                                                      @NotNull LlmProfile profile,
                                                      boolean enabled) {
        if (!enabled) {
            return;
        }
        if (isQwenCompatible(profile)) {
            requestBody.addProperty("enable_thinking", false);
            return;
        }
        if (isOpenAiReasoningModel(profile)) {
            requestBody.addProperty("reasoning_effort", "low");
            return;
        }
        if (isThinkingObjectCompatible(profile)) {
            requestBody.add("thinking", createThinkingDisabled());
        }
    }

    protected static boolean shouldRetryWithoutReasoningCompatibility(@NotNull String responseBody) {
        String lower = responseBody.toLowerCase(Locale.ROOT);
        return lower.contains("unsupported")
                || lower.contains("unknown parameter")
                || lower.contains("unrecognized")
                || lower.contains("invalid parameter")
                || lower.contains("extra_forbidden")
                || lower.contains("not support")
                || lower.contains("not_supported");
    }

    @NotNull
    private static JsonObject createThinkingDisabled() {
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", "disabled");
        return thinking;
    }

    private static boolean isOpenAiReasoningModel(@NotNull LlmProfile profile) {
        String model = normalize(profile.getModel());
        return model.startsWith("o1")
                || model.startsWith("o3")
                || model.startsWith("o4")
                || model.startsWith("o5")
                || model.startsWith("gpt-5");
    }

    private static boolean isQwenCompatible(@NotNull LlmProfile profile) {
        return containsProfileText(profile, "qwen", "dashscope", "aliyuncs", "alibabacloud");
    }

    private static boolean isThinkingObjectCompatible(@NotNull LlmProfile profile) {
        if (LlmProvider.ANTHROPIC == LlmProvider.fromNullable(profile.getProvider())) {
            return !containsProfileText(profile, "api.anthropic.com");
        }
        return containsProfileText(profile, "mimo", "xiaomimimo", "token-plan", "zhipu", "bigmodel", "glm", "moonshot", "kimi");
    }

    private static boolean containsProfileText(@NotNull LlmProfile profile, @NotNull String... needles) {
        String text = normalize(profile.getBaseUrl()) + " " + normalize(profile.getModel());
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
