package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

abstract class AbstractHttpLlmProviderClient implements LlmProviderClient {

    private static final int CANCELLABLE_READ_TIMEOUT_MS = 1000;

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

    protected int getResponseCode(@NotNull HttpURLConnection connection) throws IOException {
        if (isCancellationSupported()) {
            connection.setReadTimeout(CANCELLABLE_READ_TIMEOUT_MS);
        }
        while (true) {
            boolean cancellationSupported = checkCanceled();
            try {
                return connection.getResponseCode();
            } catch (SocketTimeoutException ex) {
                if (!cancellationSupported && !isCancellationSupported()) {
                    throw ex;
                }
                checkCanceled();
            }
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
            while ((line = readLine(reader)) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    @Nullable
    protected String readLine(@NotNull BufferedReader reader) throws IOException {
        while (true) {
            boolean cancellationSupported = checkCanceled();
            try {
                return reader.readLine();
            } catch (SocketTimeoutException ex) {
                if (!cancellationSupported && !isCancellationSupported()) {
                    throw ex;
                }
                checkCanceled();
            }
        }
    }

    protected boolean checkCanceled() {
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
            indicator.checkCanceled();
            return true;
        }
        return false;
    }

    private boolean isCancellationSupported() {
        return ProgressManager.getInstance().getProgressIndicator() != null;
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

    /**
     * Providers reject thinking parameters they do not know. Official APIs report unknown fields
     * with a small set of phrasings, from {@code unsupported} to Anthropic's
     * {@code Extra inputs are not permitted}.
     */
    static boolean shouldRetryWithoutThinkingParameters(@NotNull String responseBody) {
        String lower = responseBody.toLowerCase(Locale.ROOT);
        return lower.contains("unsupported")
                || lower.contains("unknown parameter")
                || lower.contains("unknown field")
                || lower.contains("unrecognized")
                || lower.contains("invalid parameter")
                || lower.contains("extra_forbidden")
                || lower.contains("extra inputs are not permitted")
                || lower.contains("not permitted")
                || lower.contains("not support")
                || lower.contains("not_supported");
    }

    static boolean errorIndicatesUnsupportedTemperature(@NotNull String responseBody) {
        String lower = responseBody.toLowerCase(Locale.ROOT);
        return lower.contains("temperature")
                && (lower.contains("unsupported")
                || lower.contains("unknown parameter")
                || lower.contains("unrecognized")
                || lower.contains("invalid")
                || lower.contains("is not a valid")
                || lower.contains("not support")
                || lower.contains("not_supported")
                || lower.contains("only the default"));
    }
}
