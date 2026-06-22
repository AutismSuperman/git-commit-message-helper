package com.fulinlin.service;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.utils.VelocityUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.progress.ProcessCanceledException;
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
    private static final Pattern COMMIT_SUBJECT_PARTS_PATTERN = Pattern.compile("^([a-zA-Z][\\w-]*)(?:\\(([^\\r\\n()]+)\\))?!?:\\s+(.+)$");
    private static final Pattern BULLET_LINE_PATTERN = Pattern.compile("(?m)^\\s*[-*+]\\s+\\S.*$");
    private static final String GENERATE_SYSTEM_PROMPT = "You are a senior engineer and Git maintainer. "
            + "Analyze the selected changes carefully, identify the primary intent, and fill commit template fields. "
            + "Keep your analysis internal and return only the requested JSON.";
    private static final String FORMAT_SYSTEM_PROMPT = "You are a senior engineer and Git maintainer. "
            + "Analyze the current commit message and selected changes, then fill commit template fields. "
            + "Keep your analysis internal and return only the requested JSON.";
    private static final String PARSE_SYSTEM_PROMPT = "You convert git commit messages into structured commit template fields. "
            + "Keep your analysis internal and return only the requested JSON.";
    private final GitContextService gitContextService = new GitContextService();
    private final LlmClient llmClient = new LlmClient();

    public void generateCommitMessage(@NotNull Project project,
                                      @NotNull GitCommitMessageHelperSettings settings,
                                      @NotNull Collection<Change> selectedChanges,
                                      @NotNull Collection<File> selectedFiles,
                                      @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project, selectedChanges, selectedFiles);
        generateCommitMessage(project, settings, gitContext, onDelta);
    }

    public void generateCommitMessageForCommit(@NotNull Project project,
                                               @NotNull GitCommitMessageHelperSettings settings,
                                               @NotNull String commitHash,
                                               @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collectCommitted(project, commitHash);
        generateCommitMessage(project, settings, gitContext, onDelta);
    }

    private void generateCommitMessage(@NotNull Project project,
                                       @NotNull GitCommitMessageHelperSettings settings,
                                       @NotNull GitContextService.GitContext gitContext,
                                       @NotNull Consumer<String> onDelta) throws IOException {
        LlmSettings llmSettings = getLlmSettings(settings);
        LlmProfile profile = llmSettings.getActiveProfile();
        String template = settings.getActiveCommitTemplate(project);
        String systemPrompt = GENERATE_SYSTEM_PROMPT;
        String userPrompt = buildGeneratePrompt(settings, llmSettings, gitContext, template);
        onDelta.accept(completeTemplatedCommitMessage(template, profile, llmSettings, systemPrompt, userPrompt));
    }

    public void formatCommitMessage(@NotNull Project project,
                                    @NotNull GitCommitMessageHelperSettings settings,
                                    @NotNull Collection<Change> selectedChanges,
                                    @NotNull Collection<File> selectedFiles,
                                    @NotNull String currentMessage,
                                    @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project, selectedChanges, selectedFiles);
        formatCommitMessage(project, settings, gitContext, currentMessage, onDelta);
    }

    public void formatCommitMessageForCommit(@NotNull Project project,
                                             @NotNull GitCommitMessageHelperSettings settings,
                                             @NotNull String commitHash,
                                             @NotNull String currentMessage,
                                             @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collectCommitted(project, commitHash);
        formatCommitMessage(project, settings, gitContext, currentMessage, onDelta);
    }

    private void formatCommitMessage(@NotNull Project project,
                                     @NotNull GitCommitMessageHelperSettings settings,
                                     @NotNull GitContextService.GitContext gitContext,
                                     @NotNull String currentMessage,
                                     @NotNull Consumer<String> onDelta) throws IOException {
        LlmSettings llmSettings = getLlmSettings(settings);
        LlmProfile profile = llmSettings.getActiveProfile();
        String template = settings.getActiveCommitTemplate(project);
        String systemPrompt = FORMAT_SYSTEM_PROMPT;
        String userPrompt = buildFormatPrompt(settings, llmSettings, gitContext, currentMessage, template);
        onDelta.accept(completeTemplatedCommitMessage(template, profile, llmSettings, systemPrompt, userPrompt));
    }

    @NotNull
    public CommitTemplate parseCommitMessageToTemplate(@NotNull Project project,
                                                       @NotNull GitCommitMessageHelperSettings settings,
                                                       @NotNull Collection<Change> selectedChanges,
                                                       @NotNull Collection<File> selectedFiles,
                                                       @NotNull String currentMessage) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project, selectedChanges, selectedFiles);
        return parseCommitMessageToTemplate(project, settings, gitContext, currentMessage);
    }

    @NotNull
    public CommitTemplate parseCommitMessageToTemplateForCommit(@NotNull Project project,
                                                                @NotNull GitCommitMessageHelperSettings settings,
                                                                @NotNull String commitHash,
                                                                @NotNull String currentMessage) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collectCommitted(project, commitHash);
        return parseCommitMessageToTemplate(project, settings, gitContext, currentMessage);
    }

    @NotNull
    private CommitTemplate parseCommitMessageToTemplate(@NotNull Project project,
                                                       @NotNull GitCommitMessageHelperSettings settings,
                                                       @NotNull GitContextService.GitContext gitContext,
                                                       @NotNull String currentMessage) throws IOException {
        LlmSettings llmSettings = getLlmSettings(settings);
        LlmProfile profile = llmSettings.getActiveProfile();
        String template = settings.getActiveCommitTemplate(project);
        String userPrompt = buildParsePrompt(settings, gitContext, currentMessage, template);
        String response = llmClient.chat(
                profile,
                llmSettings,
                PARSE_SYSTEM_PROMPT,
                userPrompt
        );
        return parseTemplateResponseWithRetry(profile, llmSettings, PARSE_SYSTEM_PROMPT, userPrompt, response);
    }

    private void streamSanitized(@NotNull LlmProfile profile,
                                 @NotNull LlmSettings llmSettings,
                                 @NotNull String systemPrompt,
                                 @NotNull String userPrompt,
                                 @NotNull Consumer<String> onDelta) throws IOException {
        StringBuilder rawResponse = new StringBuilder();
        StringBuilder emittedResponse = new StringBuilder();
        try {
            llmClient.streamChat(profile, llmSettings, systemPrompt, userPrompt, delta -> {
                rawResponse.append(delta);
                String sanitized = sanitizeCommitResponse(rawResponse.toString());
                if (isAcceptableCommitMessage(sanitized)) {
                    emitSanitizedDelta(sanitized, emittedResponse, onDelta);
                }
            });
        } catch (IOException | RuntimeException streamException) {
            if (!shouldFallbackFromStreaming(streamException)) {
                throw streamException;
            }
            String fallbackResponse = chatSanitizedWithQualityRetry(profile, llmSettings, systemPrompt, userPrompt);
            LlmCapabilityCache.markStreamingUnsupported(profile);
            onDelta.accept(fallbackResponse);
            return;
        }

        String finalResponse = sanitizeCommitResponse(rawResponse.toString());
        if (isLowQualityCommitMessage(finalResponse) && emittedResponse.length() == 0) {
            finalResponse = chatSanitizedWithQualityRetry(profile, llmSettings, systemPrompt, userPrompt);
        }
        emitSanitizedDelta(finalResponse, emittedResponse, onDelta);
    }

    @NotNull
    private String completeTemplatedCommitMessage(@NotNull String template,
                                                 @NotNull LlmProfile profile,
                                                 @NotNull LlmSettings llmSettings,
                                                 @NotNull String systemPrompt,
                                                 @NotNull String userPrompt) throws IOException {
        CommitTemplate commitTemplate = requestCommitTemplate(profile, llmSettings, systemPrompt, userPrompt);
        String rendered = renderCommitTemplate(template, commitTemplate);
        if (!isLowQualityCommitMessage(rendered) && isBodyFormatAcceptable(commitTemplate)) {
            return rendered;
        }

        String retryPrompt = buildQualityRetryPrompt(userPrompt, rendered);
        CommitTemplate retryTemplate = requestCommitTemplate(profile, llmSettings, systemPrompt, retryPrompt);
        String retryRendered = renderCommitTemplate(template, retryTemplate);
        return retryRendered.isEmpty() ? rendered : retryRendered;
    }

    @NotNull
    private CommitTemplate requestCommitTemplate(@NotNull LlmProfile profile,
                                                @NotNull LlmSettings llmSettings,
                                                @NotNull String systemPrompt,
                                                @NotNull String userPrompt) throws IOException {
        String response;
        if (isStreamingResponseEnabled(llmSettings) && !LlmCapabilityCache.shouldSkipStreaming(profile)) {
            StringBuilder rawResponse = new StringBuilder();
            try {
                llmClient.streamChat(profile, llmSettings, systemPrompt, userPrompt, rawResponse::append);
                response = rawResponse.toString();
                if (isIncompleteTemplateJson(response)) {
                    LlmCapabilityCache.markStreamingUnsupported(profile);
                    return requestCommitTemplateWithoutStreaming(profile, llmSettings, systemPrompt, userPrompt, response);
                }
            } catch (IOException | RuntimeException streamException) {
                if (!shouldFallbackFromStreaming(streamException)) {
                    throw streamException;
                }
                LlmCapabilityCache.markStreamingUnsupported(profile);
                return requestCommitTemplateWithoutStreaming(profile, llmSettings, systemPrompt, userPrompt, rawResponse.toString());
            }
        } else {
            return requestCommitTemplateWithoutStreaming(profile, llmSettings, systemPrompt, userPrompt, "");
        }
        return parseTemplateResponseWithRetry(profile, llmSettings, systemPrompt, userPrompt, response);
    }

    @NotNull
    private CommitTemplate requestCommitTemplateWithoutStreaming(@NotNull LlmProfile profile,
                                                                 @NotNull LlmSettings llmSettings,
                                                                 @NotNull String systemPrompt,
                                                                 @NotNull String userPrompt,
                                                                 @NotNull String previousInvalidResponse) throws IOException {
        String prompt = previousInvalidResponse.trim().isEmpty()
                ? userPrompt
                : buildJsonRepairPrompt(userPrompt, previousInvalidResponse, "stream response was incomplete");
        String response = llmClient.chat(profile, llmSettings, systemPrompt, prompt);
        return parseTemplateResponseWithRetry(profile, llmSettings, systemPrompt, userPrompt, response);
    }

    @NotNull
    private CommitTemplate parseTemplateResponseWithRetry(@NotNull LlmProfile profile,
                                                         @NotNull LlmSettings llmSettings,
                                                         @NotNull String systemPrompt,
                                                         @NotNull String userPrompt,
                                                         @NotNull String response) throws IOException {
        try {
            if (!isIncompleteTemplateJson(response)) {
                return parseTemplateResponse(response);
            }
        } catch (RuntimeException ignored) {
            // Retry once below with a stricter repair prompt.
        }

        String retryPrompt = buildJsonRepairPrompt(userPrompt, response, "response was not a complete JSON object");
        String retryResponse = llmClient.chat(profile, llmSettings, systemPrompt, retryPrompt);
        try {
            return parseTemplateResponse(retryResponse);
        } catch (RuntimeException retryException) {
            try {
                return parseTemplateResponseLenient(response);
            } catch (RuntimeException ignored) {
                throw new IOException("LLM returned invalid commit template JSON after retry. "
                        + "Try testing the model connection, increasing model output stability, or disabling streaming.");
            }
        }
    }

    @NotNull
    private String chatSanitizedWithQualityRetry(@NotNull LlmProfile profile,
                                                @NotNull LlmSettings llmSettings,
                                                @NotNull String systemPrompt,
                                                @NotNull String userPrompt) throws IOException {
        String sanitized = sanitizeCommitResponse(llmClient.chat(profile, llmSettings, systemPrompt, userPrompt));
        if (!isLowQualityCommitMessage(sanitized)) {
            return sanitized;
        }
        String retryPrompt = buildQualityRetryPrompt(userPrompt, sanitized);
        String retryResponse = sanitizeCommitResponse(llmClient.chat(profile, llmSettings, systemPrompt, retryPrompt));
        return retryResponse.isEmpty() ? sanitized : retryResponse;
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
                                              @NotNull GitContextService.GitContext gitContext,
                                              @NotNull String template) {
        return "Generate git commit template fields for this project.\n\n"
                + buildInternalAnalysisInstructions()
                + "\n\n"
                + buildTemplateJsonOutputContract(llmSettings)
                + "\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template Velocity Source:\n" + template + "\n\n"
                + "Commit Template Preview:\n" + buildTemplatePreview(template) + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String buildFormatPrompt(@NotNull GitCommitMessageHelperSettings settings,
                                            @NotNull LlmSettings llmSettings,
                                            @NotNull GitContextService.GitContext gitContext,
                                            @NotNull String currentMessage,
                                            @NotNull String template) {
        return "Format the current git commit message into the project's commit template fields.\n\n"
                + buildInternalAnalysisInstructions()
                + "\n\n"
                + "Formatting Instructions:\n"
                + "1. Preserve the original intent when it matches the selected changes.\n"
                + "2. If the current message conflicts with the diff, trust the diff and rewrite the message.\n"
                + "3. Choose the closest valid type from the allowed types.\n\n"
                + buildTemplateJsonOutputContract(llmSettings)
                + "\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template Velocity Source:\n" + template + "\n\n"
                + "Commit Template Preview:\n" + buildTemplatePreview(template) + "\n\n"
                + "Current Commit Message:\n" + currentMessage + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String buildParsePrompt(@NotNull GitCommitMessageHelperSettings settings,
                                           @NotNull GitContextService.GitContext gitContext,
                                           @NotNull String currentMessage,
                                           @NotNull String template) {
        return "Parse the current git commit message into the project's commit template fields.\n\n"
                + buildInternalAnalysisInstructions()
                + "\n\n"
                + "JSON Output Contract:\n"
                + "1. Preserve the original intent when it matches the selected changes.\n"
                + "2. If the current message conflicts with the diff, trust the diff.\n"
                + "3. Map the message into these fields only: type, scope, subject, body, changes, closes, skipCi.\n"
                + "4. Choose the closest valid type from the allowed types.\n"
                + "5. Return valid JSON only, without markdown fences or extra explanation.\n"
                + "6. Use empty strings for missing fields.\n"
                + "7. If body contains multiple details, format it as Markdown bullet lines that each start with \"- \".\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template Preview:\n" + buildTemplatePreview(template) + "\n\n"
                + "Expected JSON Shape:\n"
                + "{\"type\":\"\",\"scope\":\"\",\"subject\":\"\",\"body\":\"\",\"changes\":\"\",\"closes\":\"\",\"skipCi\":\"\"}\n\n"
                + "Current Commit Message:\n" + currentMessage + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String buildInternalAnalysisInstructions() {
        return "Internal Analysis Instructions (do not output this analysis):\n"
                + "1. Inspect selected file status, diff hunks, tests, and recent commit style.\n"
                + "2. Identify the primary user-visible or technical intent, not just the changed filenames.\n"
                + "3. Separate the main change from incidental formatting, generated files, lockfiles, and mechanical churn.\n"
                + "4. Determine whether the change is a feature, bug fix, refactor, test, docs, build, ci, chore, perf, style, or revert.\n"
                + "5. Use recent commits only for style and scope hints; never copy their content.";
    }

    @NotNull
    private static String buildQualityRetryPrompt(@NotNull String userPrompt, @NotNull String previousOutput) {
        return userPrompt
                + "\n\nQuality Correction:\n"
                + "The previous output was too vague or invalid:\n"
                + previousOutput
                + "\n\nRewrite the JSON fields once. Use a concrete subject that names the actual code or behavior change. "
                + "If body has more than one detail, return body as Markdown bullet lines starting with \"- \". "
                + "Return valid JSON only.";
    }

    @NotNull
    private static String buildJsonRepairPrompt(@NotNull String userPrompt,
                                                @NotNull String previousOutput,
                                                @NotNull String reason) {
        return userPrompt
                + "\n\nJSON Repair Required:\n"
                + "The previous model response could not be used because " + reason + ".\n"
                + "Previous response:\n"
                + previousOutput
                + "\n\nReturn one complete valid JSON object only, using exactly this shape:\n"
                + "{\"type\":\"\",\"scope\":\"\",\"subject\":\"\",\"body\":\"\",\"changes\":\"\",\"closes\":\"\",\"skipCi\":\"\"}\n"
                + "Do not truncate strings. Escape newlines inside JSON strings as \\n. Do not output markdown fences.";
    }

    @NotNull
    private static String buildCommitOutputContract(@NotNull LlmSettings llmSettings) {
        return "Commit Message Output Contract:\n"
                + "1. Return commit message text only; no markdown fences, no JSON, no labels, no explanation, no analysis.\n"
                + "2. Follow the project template exactly.\n"
                + "3. The first line must be a concrete Conventional Commit subject using one allowed type.\n"
                + "4. Keep type and scope as lowercase English identifiers; write subject and body in "
                + getResponseLanguage(llmSettings) + ".\n"
                + "5. Use a scope only when the changed area is clear from paths or symbols; otherwise omit it.\n"
                + "6. Prefer an imperative, specific subject under 72 characters, without a trailing period.\n"
                + "7. Do not use vague subjects like \"update files\", \"modify code\", \"change logic\", \"adjust configuration\", or filenames alone.\n"
                + "8. Include a body only when it adds important why, impact, migration, or testing details not obvious from the subject.\n"
                + "9. Include BREAKING CHANGE or closing issue lines only when the selected changes clearly require them.";
    }

    @NotNull
    private static String buildTemplateJsonOutputContract(@NotNull LlmSettings llmSettings) {
        return "JSON Output Contract:\n"
                + "1. Return valid JSON only; no markdown fences, labels, explanation, or analysis.\n"
                + "2. Use this exact shape: {\"type\":\"\",\"scope\":\"\",\"subject\":\"\",\"body\":\"\",\"changes\":\"\",\"closes\":\"\",\"skipCi\":\"\"}.\n"
                + "3. The plugin will render these fields with the Velocity template; do not include template syntax like <body> or ${body}.\n"
                + "4. Keep type and scope as lowercase English identifiers; write subject and body in "
                + getResponseLanguage(llmSettings) + ".\n"
                + "5. Subject must be concrete, imperative, under 72 characters, and must not end with punctuation.\n"
                + "6. Do not use vague subjects like \"update files\", \"modify code\", \"change logic\", \"adjust configuration\", or filenames alone.\n"
                + "7. Body is the detailed description field. Use empty string only when the subject is sufficient.\n"
                + "8. When body includes more than one detail, format body as Markdown bullet lines. Each bullet must start with \"- \".\n"
                + "9. Put one changed area, behavior, or impact per body bullet; do not write a long paragraph body.\n"
                + "10. changes contains only breaking-change text without the \"BREAKING CHANGE:\" prefix.\n"
                + "11. closes contains only issue references without the \"Closes\" prefix.\n"
                + "12. skipCi is empty unless the selected changes clearly require a skip-ci marker.";
    }

    @NotNull
    private static String buildTemplatePreview(@NotNull String template) {
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
    static String renderCommitTemplate(@NotNull GitCommitMessageHelperSettings settings,
                                       @NotNull CommitTemplate commitTemplate) {
        return renderCommitTemplate(settings.getActiveCommitTemplate(), commitTemplate);
    }

    @NotNull
    static String renderCommitTemplate(@NotNull String template,
                                       @NotNull CommitTemplate commitTemplate) {
        normalizeCommitTemplate(commitTemplate);
        try {
            return sanitizeCommitResponse(VelocityUtils.convert(template, commitTemplate));
        } catch (RuntimeException ignored) {
            return sanitizeCommitResponse(buildFallbackCommitMessage(commitTemplate));
        }
    }

    private static void normalizeCommitTemplate(@NotNull CommitTemplate commitTemplate) {
        String type = normalizeIdentifier(commitTemplate.getType());
        commitTemplate.setType(type.isEmpty() ? "chore" : type);
        commitTemplate.setScope(emptyToNull(normalizeIdentifier(commitTemplate.getScope())));
        commitTemplate.setSubject(emptyToNull(trimTrailingPunctuation(safe(commitTemplate.getSubject()).trim())));
        commitTemplate.setBody(emptyToNull(normalizeBody(commitTemplate.getBody())));
        commitTemplate.setChanges(emptyToNull(stripPrefix(safe(commitTemplate.getChanges()).trim(), "BREAKING CHANGE:")));
        commitTemplate.setCloses(emptyToNull(stripPrefix(safe(commitTemplate.getCloses()).trim(), "Closes ")));
        commitTemplate.setSkipCi(emptyToNull(safe(commitTemplate.getSkipCi()).trim()));
    }

    @NotNull
    static String normalizeBody(String body) {
        String value = safe(body).trim();
        if (value.isEmpty() || BULLET_LINE_PATTERN.matcher(value).find()) {
            return value;
        }
        String[] parts = value.split("(?<=[。.!?])\\s*|[；;]\\s*");
        List<String> details = java.util.Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
        if (details.size() <= 1) {
            return value;
        }
        return details.stream()
                .map(detail -> "- " + trimTrailingPunctuation(detail))
                .collect(Collectors.joining("\n"));
    }

    private static boolean isBodyFormatAcceptable(@NotNull CommitTemplate commitTemplate) {
        String body = safe(commitTemplate.getBody()).trim();
        return body.isEmpty() || !body.contains("\n") || BULLET_LINE_PATTERN.matcher(body).find();
    }

    @NotNull
    private static String buildFallbackCommitMessage(@NotNull CommitTemplate commitTemplate) {
        StringBuilder builder = new StringBuilder();
        String type = safe(commitTemplate.getType()).trim();
        String scope = safe(commitTemplate.getScope()).trim();
        String subject = safe(commitTemplate.getSubject()).trim();
        builder.append(type.isEmpty() ? "chore" : type);
        if (!scope.isEmpty()) {
            builder.append('(').append(scope).append(')');
        }
        builder.append(": ").append(subject.isEmpty() ? "update project changes" : subject);
        appendSection(builder, safe(commitTemplate.getBody()).trim());
        String changes = safe(commitTemplate.getChanges()).trim();
        if (!changes.isEmpty()) {
            appendSection(builder, "BREAKING CHANGE: " + changes);
        }
        String closes = safe(commitTemplate.getCloses()).trim();
        if (!closes.isEmpty()) {
            appendSection(builder, "Closes " + closes);
        }
        appendSection(builder, safe(commitTemplate.getSkipCi()).trim());
        return builder.toString();
    }

    private static void appendSection(@NotNull StringBuilder builder, @NotNull String section) {
        if (!section.isEmpty()) {
            builder.append("\n\n").append(section);
        }
    }

    @NotNull
    private static String normalizeIdentifier(String value) {
        return safe(value)
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_.-]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    @NotNull
    private static String trimTrailingPunctuation(@NotNull String value) {
        return value.trim().replaceAll("[。.!?；;，,\\s]+$", "");
    }

    @NotNull
    private static String stripPrefix(@NotNull String value, @NotNull String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length())
                ? value.substring(prefix.length()).trim()
                : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    static boolean isLowQualityCommitMessage(@NotNull String message) {
        String sanitized = sanitizeCommitResponse(message);
        Matcher matcher = COMMIT_SUBJECT_PATTERN.matcher(sanitized);
        if (!matcher.find()) {
            return true;
        }
        String subjectLine = sanitized.substring(matcher.start(), matcher.end()).trim();
        int separator = subjectLine.indexOf(": ");
        if (separator < 0 || separator + 2 >= subjectLine.length()) {
            return true;
        }
        String subject = subjectLine.substring(separator + 2).trim().toLowerCase();
        return subject.length() < 12
                || subject.matches("^(update|modify|change|adjust|fix|improve|enhance)( files?| code| logic| project| implementation| stuff| things?)?$")
                || subject.matches("^(fix issue|bug fixes?|misc changes?|various changes?|code cleanup|minor changes?)$")
                || subject.matches("^[\\w./-]+$");
    }

    private static boolean isAcceptableCommitMessage(@NotNull String message) {
        return COMMIT_SUBJECT_PATTERN.matcher(message).find() && !isLowQualityCommitMessage(message);
    }

    private static boolean shouldFallbackFromStreaming(@NotNull Throwable throwable) {
        if (throwable instanceof ProcessCanceledException) {
            return false;
        }
        if (throwable instanceof RuntimeException) {
            return true;
        }
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("stream")
                || lower.contains("sse")
                || lower.contains("event")
                || lower.contains("data:")
                || lower.contains("json")
                || lower.contains("malformed")
                || lower.contains("unexpected")
                || lower.contains("not support")
                || lower.contains("unsupported");
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
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @NotNull
    static CommitTemplate parseTemplateResponse(@NotNull String response) {
        String normalized = normalizeJson(response);
        try {
            JsonElement element = JsonParser.parseString(normalized);
            if (element == null || !element.isJsonObject()) {
                return parseTemplateResponseLenient(response);
            }
            return parseTemplateJsonObject(element.getAsJsonObject());
        } catch (RuntimeException ignored) {
            return parseTemplateResponseLenient(response);
        }
    }

    @NotNull
    private static CommitTemplate parseTemplateJsonObject(@NotNull JsonObject jsonObject) {
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
    private static CommitTemplate parseTemplateResponseLenient(@NotNull String response) {
        String normalized = normalizeJson(response);
        CommitTemplate partial = parsePartialTemplateFields(normalized);
        if (hasTemplateSignal(partial)) {
            return partial;
        }
        CommitTemplate fromText = parseCommitTextFallback(response);
        if (hasTemplateSignal(fromText)) {
            return fromText;
        }
        throw new IllegalArgumentException("LLM response is not a commit template JSON object");
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

    private static boolean isIncompleteTemplateJson(@NotNull String response) {
        String normalized = normalizeJson(response);
        String trimmed = normalized.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (!trimmed.startsWith("{")) {
            return false;
        }
        try {
            JsonElement element = JsonParser.parseString(trimmed);
            return element == null || !element.isJsonObject();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    @NotNull
    private static CommitTemplate parsePartialTemplateFields(@NotNull String response) {
        CommitTemplate commitTemplate = new CommitTemplate();
        commitTemplate.setType(extractJsonStringField(response, "type"));
        commitTemplate.setScope(extractJsonStringField(response, "scope"));
        commitTemplate.setSubject(extractJsonStringField(response, "subject"));
        commitTemplate.setBody(extractJsonStringField(response, "body"));
        commitTemplate.setChanges(extractJsonStringField(response, "changes"));
        commitTemplate.setCloses(extractJsonStringField(response, "closes"));
        commitTemplate.setSkipCi(extractJsonStringField(response, "skipCi"));
        return commitTemplate;
    }

    @NotNull
    private static String extractJsonStringField(@NotNull String response, @NotNull String field) {
        String quotedField = Pattern.quote("\"" + field + "\"");
        Pattern closedString = Pattern.compile(quotedField + "\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher closedStringMatcher = closedString.matcher(response);
        if (closedStringMatcher.find()) {
            return unescapeJsonString(closedStringMatcher.group(1));
        }
        Pattern array = Pattern.compile(quotedField + "\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher arrayMatcher = array.matcher(response);
        if (arrayMatcher.find()) {
            return java.util.Arrays.stream(arrayMatcher.group(1).split(","))
                    .map(String::trim)
                    .map(value -> value.replaceAll("^\"|\"$", ""))
                    .map(LlmCommitService::unescapeJsonString)
                    .filter(value -> !value.isEmpty())
                    .map(value -> "body".equals(field) && !value.startsWith("- ") ? "- " + value : value)
                    .collect(Collectors.joining("body".equals(field) ? "\n" : ", "));
        }
        Pattern openString = Pattern.compile(quotedField + "\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)$", Pattern.DOTALL);
        Matcher openStringMatcher = openString.matcher(response);
        if (openStringMatcher.find()) {
            return unescapeJsonString(openStringMatcher.group(1));
        }
        return "";
    }

    @NotNull
    private static String unescapeJsonString(@NotNull String value) {
        try {
            return JsonParser.parseString("\"" + value + "\"").getAsString();
        } catch (RuntimeException ignored) {
            return value.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
    }

    @NotNull
    private static CommitTemplate parseCommitTextFallback(@NotNull String response) {
        String sanitized = sanitizeCommitResponse(response);
        Matcher subjectMatcher = COMMIT_SUBJECT_PATTERN.matcher(sanitized);
        if (!subjectMatcher.find()) {
            return new CommitTemplate();
        }
        String subjectLine = sanitized.substring(subjectMatcher.start(), subjectMatcher.end()).trim();
        Matcher partsMatcher = COMMIT_SUBJECT_PARTS_PATTERN.matcher(subjectLine);
        CommitTemplate commitTemplate = new CommitTemplate();
        if (partsMatcher.find()) {
            commitTemplate.setType(partsMatcher.group(1));
            commitTemplate.setScope(partsMatcher.group(2));
            commitTemplate.setSubject(partsMatcher.group(3));
            String body = sanitized.substring(subjectMatcher.end()).trim();
            commitTemplate.setBody(body);
        }
        return commitTemplate;
    }

    private static boolean hasTemplateSignal(@NotNull CommitTemplate commitTemplate) {
        return notBlank(commitTemplate.getSubject())
                || notBlank(commitTemplate.getBody());
    }

    @NotNull
    private static String getString(@NotNull JsonObject jsonObject, @NotNull String field) {
        if (!jsonObject.has(field) || jsonObject.get(field).isJsonNull()) {
            return "";
        }
        JsonElement element = jsonObject.get(field);
        if (element.isJsonArray()) {
            return java.util.stream.StreamSupport.stream(element.getAsJsonArray().spliterator(), false)
                    .map(LlmCommitService::jsonElementToString)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(value -> "body".equals(field) && !value.startsWith("- ") ? "- " + value : value)
                    .collect(Collectors.joining("body".equals(field) ? "\n" : ", "));
        }
        return element.isJsonPrimitive() ? GSON.fromJson(element, String.class) : element.toString();
    }

    @NotNull
    private static String jsonElementToString(@NotNull JsonElement element) {
        return element.isJsonPrimitive() ? element.getAsString() : element.toString();
    }
}
