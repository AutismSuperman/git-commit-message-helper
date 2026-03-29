package com.fulinlin.service;

import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LlmCommitService {

    private final GitContextService gitContextService = new GitContextService();
    private final LlmClient llmClient = new LlmClient();

    public void generateCommitMessage(@NotNull Project project,
                                      @NotNull GitCommitMessageHelperSettings settings,
                                      @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project);
        llmClient.streamChat(
                settings.getCentralSettings().getLlmSettings(),
                "You are a senior engineer who writes precise git commit messages.",
                buildGeneratePrompt(settings, gitContext),
                onDelta
        );
    }

    public void formatCommitMessage(@NotNull Project project,
                                    @NotNull GitCommitMessageHelperSettings settings,
                                    @NotNull String currentMessage,
                                    @NotNull Consumer<String> onDelta) throws IOException {
        GitContextService.GitContext gitContext = gitContextService.collect(project);
        llmClient.streamChat(
                settings.getCentralSettings().getLlmSettings(),
                "You rewrite git commit messages to match the requested project template exactly.",
                buildFormatPrompt(settings, gitContext, currentMessage),
                onDelta
        );
    }

    @NotNull
    private static String buildGeneratePrompt(@NotNull GitCommitMessageHelperSettings settings,
                                              @NotNull GitContextService.GitContext gitContext) {
        return "Generate a git commit message for this project.\n\n"
                + "Requirements:\n"
                + "1. Follow the project's commit template strictly.\n"
                + "2. Prefer one concise subject line and only include body/breaking/closes/skip ci sections when needed.\n"
                + "3. Choose the most suitable type from the allowed types.\n"
                + "4. Write the commit message in " + getResponseLanguage(settings) + ".\n"
                + "5. Return commit message text only, no markdown fences, no explanation.\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template (Velocity syntax):\n" + settings.getDateSettings().getTemplate() + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String buildFormatPrompt(@NotNull GitCommitMessageHelperSettings settings,
                                            @NotNull GitContextService.GitContext gitContext,
                                            @NotNull String currentMessage) {
        return "Format the current git commit message to match the project's template.\n\n"
                + "Requirements:\n"
                + "1. Preserve the original intent.\n"
                + "2. Follow the project's commit template strictly.\n"
                + "3. Choose the closest valid type from the allowed types.\n"
                + "4. Rewrite the commit message in " + getResponseLanguage(settings) + ".\n"
                + "5. Return commit message text only, no markdown fences, no explanation.\n\n"
                + "Allowed Types:\n" + formatTypes(settings.getDateSettings().getTypeAliases()) + "\n\n"
                + "Commit Template (Velocity syntax):\n" + settings.getDateSettings().getTemplate() + "\n\n"
                + "Current Commit Message:\n" + currentMessage + "\n\n"
                + "Git Context:\n" + gitContext.toPromptText();
    }

    @NotNull
    private static String getResponseLanguage(@NotNull GitCommitMessageHelperSettings settings) {
        String responseLanguage = settings.getCentralSettings().getLlmSettings().getResponseLanguage();
        return notBlank(responseLanguage) ? responseLanguage.trim() : "English";
    }

    @NotNull
    private static String formatTypes(@NotNull List<TypeAlias> typeAliases) {
        return typeAliases.stream()
                .map(typeAlias -> "- " + typeAlias.getTitle() + ": " + typeAlias.getDescription())
                .collect(Collectors.joining("\n"));
    }

    public static boolean isConfigured(@NotNull GitCommitMessageHelperSettings settings) {
        LlmSettings llmSettings = settings.getCentralSettings().getLlmSettings();
        return llmSettings != null
                && notBlank(llmSettings.getBaseUrl())
                && notBlank(llmSettings.getApiKey())
                && notBlank(llmSettings.getModel());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
