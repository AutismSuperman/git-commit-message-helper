package com.fulinlin.service;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.utils.VelocityUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LlmCommitService {

    private static final Gson GSON = new Gson();
    private static final Pattern THINK_BLOCK_PATTERN = Pattern.compile("(?is)<think\\b[^>]*>.*?</think>");
    private static final Pattern FENCED_BLOCK_PATTERN = Pattern.compile("(?s)```[^\\r\\n`]*\\R(.*?)```");
    private static final Pattern FENCE_LINE_PATTERN = Pattern.compile("(?m)^\\s*```[^\\r\\n`]*\\s*$\\R?");
    private static final Pattern COMMIT_SUBJECT_PATTERN = Pattern.compile("(?m)^[a-zA-Z][\\w-]*(?:\\([^\\r\\n()]+\\))?!?:\\s+\\S.*$");
    private final GitContextService gitContextService = new GitContextService();
    private final LlmClient llmClient = new LlmClient();

    public void generateCommitMessage(@NotNull Project project,
                                      @NotNull GitCommitMessageHelperSettings settings,
                                      @NotNull Collection<Change> selectedChanges,
                                      @NotNull Collection<File> selectedFiles,
                                      @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project, selectedChanges, selectedFiles);
        LlmSettings llmSettings = getLlmSettings(settings);
        LlmProfile profile = llmSettings.getActiveProfile();
        String systemPrompt = "You are a senior engineer who writes precise git commit messages.";
        String userPrompt = buildGeneratePrompt(settings, llmSettings, gitContext);
        if (isStreamingResponseEnabled(llmSettings)) {
            streamSanitized(profile, llmSettings, systemPrompt, userPrompt, onDelta);
        } else {
            onDelta.accept(sanitizeCommitResponse(llmClient.chat(profile, llmSettings, systemPrompt, userPrompt)));
        }
    }

    public void formatCommitMessage(@NotNull Project project,
                                    @NotNull GitCommitMessageHelperSettings settings,
                                    @NotNull Collection<Change> selectedChanges,
                                    @NotNull Collection<File> selectedFiles,
                                    @NotNull String currentMessage,
                                    @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project, selectedChanges, selectedFiles);
        LlmSettings llmSettings = getLlmSettings(settings);
        LlmProfile profile = llmSettings.getActiveProfile();
        String systemPrompt = "You rewrite git commit messages to match the requested project template exactly.";
        String userPrompt = buildFormatPrompt(settings, llmSettings, gitContext, currentMessage);
        if (isStreamingResponseEnabled(llmSettings)) {
            streamSanitized(profile, llmSettings, systemPrompt, userPrompt, onDelta);
        } else {
            onDelta.accept(sanitizeCommitResponse(llmClient.chat(profile, llmSettings, systemPrompt, userPrompt)));
        }
    }

    @NotNull
    public CommitTemplate parseCommitMessageToTemplate(@NotNull Project project,
                                                       @NotNull GitCommitMessageHelperSettings settings,
                                                       @NotNull Collection<Change> selectedChanges,
                                                       @NotNull Collection<File> selectedFiles,
                                                       @NotNull String currentMessage) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project, selectedChanges, selectedFiles);
        LlmSettings llmSettings = getLlmSettings(settings);
        LlmProfile profile = llmSettings.getActiveProfile();
        String response = llmClient.chat(
                profile,
                llmSettings,
                "You convert git commit messages into structured commit template fields.",
                buildParsePrompt(settings, gitContext, currentMessage)
        );
        return parseTemplateResponse(response);
    }

    private void streamSanitized(@NotNull LlmProfile profile,
                                 @NotNull LlmSettings llmSettings,
                                 @NotNull String systemPrompt,
                                 @NotNull String userPrompt,
                                 @NotNull Consumer<String> onDelta) throws IOException {
        StringBuilder rawResponse = new StringBuilder();
        StringBuilder emittedResponse = new StringBuilder();
        llmClient.streamChat(profile, llmSettings, systemPrompt, userPrompt, delta -> {
            rawResponse.append(delta);
            String sanitized = sanitizeCommitResponse(rawResponse.toString());
            if (COMMIT_SUBJECT_PATTERN.matcher(sanitized).find()) {
                emitSanitizedDelta(sanitized, emittedResponse, onDelta);
            }
        });
        emitSanitizedDelta(sanitizeCommitResponse(rawResponse.toString()), emittedResponse, onDelta);
    }

    private static void emitSanitizedDelta(@NotNull String sanitized,
                                           @NotNull StringBuilder emittedResponse,
                                           @NotNull Consumer<String> onDelta) {
        if (sanitized.startsWith(emittedResponse.toString())) {
            String nextDelta = sanitized.substring(emittedResponse.length());
            if (!nextDelta.isEmpty()) {
                emittedResponse.append(nextDelta);
                onDelta.accept(nextDelta);
            }
        }
    }

    @NotNull
    private static String buildGeneratePrompt(@NotNull GitCommitMessageHelperSettings settings,
                                              @NotNull LlmSettings llmSettings,
                                              @NotNull GitContextService.GitContext gitContext) {
        return "Generate a git commit message for this project.\n\n"
                + "Requirements:\n"
                + "1. Follow the project's commit template strictly.\n"
                + "2. Prefer one concise subject line and only include body/breaking/closes/skip ci sections when needed.\n"
                + "3. Choose the most suitable type from the allowed types.\n"
                + "4. Write the commit message in " + getResponseLanguage(llmSettings) + ".\n"
                + "5. Return commit message text only, no markdown fences, no explanation.\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template Preview:\n" + buildTemplatePreview(settings) + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String buildFormatPrompt(@NotNull GitCommitMessageHelperSettings settings,
                                            @NotNull LlmSettings llmSettings,
                                            @NotNull GitContextService.GitContext gitContext,
                                            @NotNull String currentMessage) {
        return "Format the current git commit message to match the project's template.\n\n"
                + "Requirements:\n"
                + "1. Preserve the original intent.\n"
                + "2. Follow the project's commit template strictly.\n"
                + "3. Choose the closest valid type from the allowed types.\n"
                + "4. Rewrite the commit message in " + getResponseLanguage(llmSettings) + ".\n"
                + "5. Return commit message text only, no markdown fences, no explanation.\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template Preview:\n" + buildTemplatePreview(settings) + "\n\n"
                + "Current Commit Message:\n" + currentMessage + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String buildParsePrompt(@NotNull GitCommitMessageHelperSettings settings,
                                           @NotNull GitContextService.GitContext gitContext,
                                           @NotNull String currentMessage) {
        return "Parse the current git commit message into the project's commit template fields.\n\n"
                + "Requirements:\n"
                + "1. Preserve the original intent.\n"
                + "2. Map the message into these fields only: type, scope, subject, body, changes, closes, skipCi.\n"
                + "3. Choose the closest valid type from the allowed types.\n"
                + "4. Return valid JSON only, without markdown fences or extra explanation.\n"
                + "5. Use empty strings for missing fields.\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template Preview:\n" + buildTemplatePreview(settings) + "\n\n"
                + "Expected JSON Shape:\n"
                + "{\"type\":\"\",\"scope\":\"\",\"subject\":\"\",\"body\":\"\",\"changes\":\"\",\"closes\":\"\",\"skipCi\":\"\"}\n\n"
                + "Current Commit Message:\n" + currentMessage + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String buildTemplatePreview(@NotNull GitCommitMessageHelperSettings settings) {
        String template = settings.getDateSettings().getTemplate();
        CommitTemplate commitTemplate = new CommitTemplate();
        commitTemplate.setType("<type>");
        commitTemplate.setScope("<scope>");
        commitTemplate.setSubject("<subject>");
        commitTemplate.setBody("<body>");
        commitTemplate.setChanges("<changes>");
        commitTemplate.setCloses("<closes>");
        commitTemplate.setSkipCi("<skipCi>");
        try {
            return VelocityUtils.convert(template, commitTemplate);
        } catch (RuntimeException ignored) {
            return template;
        }
    }

    @NotNull
    private static String getResponseLanguage(@NotNull LlmSettings llmSettings) {
        String responseLanguage = llmSettings.getResponseLanguage();
        return notBlank(responseLanguage) ? responseLanguage.trim() : "English";
    }

    @NotNull
    static String sanitizeCommitResponse(@NotNull String response) {
        String withoutThinking = removeThinkingBlocks(response).trim();
        String fencedContent = extractLastFencedBlock(withoutThinking);
        String cleaned = fencedContent != null ? fencedContent : withoutThinking;
        cleaned = FENCE_LINE_PATTERN.matcher(cleaned).replaceAll("").trim();

        Matcher commitSubjectMatcher = COMMIT_SUBJECT_PATTERN.matcher(cleaned);
        if (commitSubjectMatcher.find()) {
            cleaned = cleaned.substring(commitSubjectMatcher.start()).trim();
        }
        return cleaned;
    }

    @NotNull
    private static String removeThinkingBlocks(@NotNull String response) {
        String cleaned = THINK_BLOCK_PATTERN.matcher(response).replaceAll("");
        int openThink = cleaned.toLowerCase().lastIndexOf("<think");
        int closeThink = cleaned.toLowerCase().lastIndexOf("</think>");
        if (openThink >= 0 && openThink > closeThink) {
            cleaned = cleaned.substring(0, openThink);
        }
        return cleaned.replaceAll("(?is)</think>", "");
    }

    private static String extractLastFencedBlock(@NotNull String response) {
        Matcher matcher = FENCED_BLOCK_PATTERN.matcher(response);
        String fencedContent = null;
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (!candidate.isEmpty()) {
                fencedContent = candidate;
            }
        }
        return fencedContent;
    }

    @NotNull
    private static String formatTypes(@NotNull List<TypeAlias> typeAliases) {
        return typeAliases.stream()
                .map(typeAlias -> "- " + typeAlias.getTitle() + ": " + typeAlias.getDescription())
                .collect(Collectors.joining("\n"));
    }

    public static boolean isConfigured(@NotNull GitCommitMessageHelperSettings settings) {
        LlmProfile profile = getActiveProfile(settings);
        return notBlank(profile.getBaseUrl())
                && notBlank(profile.getApiKey())
                && notBlank(profile.getModel());
    }

    public static boolean isSmartEchoEnabled(@NotNull GitCommitMessageHelperSettings settings) {
        return Boolean.TRUE.equals(getLlmSettings(settings).getSmartEchoEnabled());
    }

    private static boolean isStreamingResponseEnabled(@NotNull LlmSettings llmSettings) {
        return Boolean.TRUE.equals(llmSettings.getStreamingResponseEnabled());
    }

    @NotNull
    private static LlmProfile getActiveProfile(@NotNull GitCommitMessageHelperSettings settings) {
        return getLlmSettings(settings).getActiveProfile();
    }

    @NotNull
    private static LlmSettings getLlmSettings(@NotNull GitCommitMessageHelperSettings settings) {
        LlmSettings llmSettings = settings.getCentralSettings().getLlmSettings();
        GitCommitMessageHelperSettings.checkDefaultLlmSettings(llmSettings);
        return llmSettings;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @NotNull
    static CommitTemplate parseTemplateResponse(@NotNull String response) {
        String normalized = normalizeJson(response);
        JsonObject jsonObject = JsonParser.parseString(normalized).getAsJsonObject();
        CommitTemplate commitTemplate = new CommitTemplate();
        commitTemplate.setType(getString(jsonObject, "type"));
        commitTemplate.setScope(getString(jsonObject, "scope"));
        commitTemplate.setSubject(getString(jsonObject, "subject"));
        commitTemplate.setBody(getString(jsonObject, "body"));
        commitTemplate.setChanges(getString(jsonObject, "changes"));
        commitTemplate.setCloses(getString(jsonObject, "closes"));
        commitTemplate.setSkipCi(getString(jsonObject, "skipCi"));
        return commitTemplate;
    }

    @NotNull
    private static String normalizeJson(@NotNull String response) {
        String trimmed = removeThinkingBlocks(response).trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak >= 0) {
                trimmed = trimmed.substring(firstLineBreak + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    @NotNull
    private static String getString(@NotNull JsonObject jsonObject, @NotNull String field) {
        return jsonObject.has(field) && !jsonObject.get(field).isJsonNull()
                ? GSON.fromJson(jsonObject.get(field), String.class)
                : "";
    }
}
