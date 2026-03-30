package com.fulinlin.service;

import com.fulinlin.model.LlmSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class LlmClient {

    private static final Gson GSON = new Gson();

    @NotNull
    public String chat(@NotNull LlmSettings settings,
                       @NotNull String systemPrompt,
                       @NotNull String userPrompt) throws IOException {
        HttpURLConnection connection = createConnection(settings);
        JsonObject requestBody = createRequestBody(settings, systemPrompt, userPrompt, false);
        write(connection, GSON.toJson(requestBody));

        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException(readAll(inputStream));
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(readAll(inputStream)).getAsJsonObject();
            JsonArray choices = jsonObject.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return "";
            }
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.has("message") ? choice.getAsJsonObject("message") : null;
            if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
                return extractMessageContent(message.get("content"));
            }
            return "";
        } finally {
            connection.disconnect();
        }
    }

    public void streamChat(@NotNull LlmSettings settings,
                           @NotNull String systemPrompt,
                           @NotNull String userPrompt,
                           @NotNull Consumer<String> onDelta) throws IOException {
        HttpURLConnection connection = createConnection(settings);
        JsonObject requestBody = createRequestBody(settings, systemPrompt, userPrompt, true);
        write(connection, GSON.toJson(requestBody));

        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException(readAll(inputStream));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
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
                JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
                JsonArray choices = jsonObject.getAsJsonArray("choices");
                if (choices == null || choices.size() == 0) {
                    continue;
                }
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject delta = choice.has("delta") ? choice.getAsJsonObject("delta") : null;
                if (delta != null && delta.has("content") && !delta.get("content").isJsonNull()) {
                    onDelta.accept(delta.get("content").getAsString());
                    continue;
                }
                JsonObject message = choice.has("message") ? choice.getAsJsonObject("message") : null;
                if (message != null && message.has("content") && !message.get("content").isJsonNull()) {
                    onDelta.accept(extractMessageContent(message.get("content")));
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    @NotNull
    private static JsonObject createRequestBody(@NotNull LlmSettings settings,
                                                @NotNull String systemPrompt,
                                                @NotNull String userPrompt,
                                                boolean stream) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", settings.getModel().trim());
        requestBody.addProperty("stream", stream);
        if (settings.getTemperature() != null) {
            requestBody.addProperty("temperature", settings.getTemperature());
        }
        requestBody.add("messages", createMessages(systemPrompt, userPrompt));
        return requestBody;
    }

    @NotNull
    private static HttpURLConnection createConnection(@NotNull LlmSettings settings) throws IOException {
        String baseUrl = settings.getBaseUrl().trim();
        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : stripTrailingSlash(baseUrl) + "/chat/completions";
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(0);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + settings.getApiKey().trim());
        return connection;
    }

    @NotNull
    private static String stripTrailingSlash(@NotNull String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @NotNull
    private static JsonArray createMessages(@NotNull String systemPrompt, @NotNull String userPrompt) {
        JsonArray messages = new JsonArray();
        messages.add(createMessage("system", systemPrompt));
        messages.add(createMessage("user", userPrompt));
        return messages;
    }

    @NotNull
    private static JsonObject createMessage(@NotNull String role, @NotNull String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static void write(@NotNull HttpURLConnection connection, @NotNull String body) throws IOException {
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    @NotNull
    private static String readAll(InputStream inputStream) throws IOException {
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
    private static String extractMessageContent(@NotNull JsonElement content) {
        if (content.isJsonPrimitive()) {
            return content.getAsString();
        }
        if (content.isJsonArray()) {
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
        return "";
    }
}
