package com.fulinlin.service;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GitContextService {

    private static final int MAX_DIFF_LENGTH = 12000;

    @NotNull
    public GitContext collect(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.trim().isEmpty()) {
            return new GitContext("", "", "", "");
        }
        File baseDir = new File(basePath);
        String repositoryRoot = execute(baseDir, "git", "rev-parse", "--show-toplevel").trim();
        File workDir = repositoryRoot.isEmpty() ? baseDir : new File(repositoryRoot);
        String status = execute(workDir, "git", "status", "--short");
        String stagedDiff = execute(workDir, "git", "diff", "--cached", "--", ".");
        String workingTreeDiff = stagedDiff.trim().isEmpty()
                ? execute(workDir, "git", "diff", "--", ".")
                : "";
        String recentCommits = execute(workDir, "git", "log", "-5", "--pretty=format:%h %s");
        return new GitContext(
                workDir.getAbsolutePath(),
                trim(status, 4000),
                trim(stagedDiff, MAX_DIFF_LENGTH),
                trim(workingTreeDiff, MAX_DIFF_LENGTH),
                trim(recentCommits, 2000)
        );
    }

    @NotNull
    private static String trim(@NotNull String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...[truncated]";
    }

    @NotNull
    private static String execute(@NotNull File workDir, @NotNull String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workDir)
                    .redirectErrorStream(true)
                    .start();
            String output = read(process.getInputStream());
            process.waitFor();
            return process.exitValue() == 0 ? output : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    @NotNull
    private static String read(@NotNull InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    public static class GitContext {

        private final String repositoryRoot;
        private final String status;
        private final String stagedDiff;
        private final String workingTreeDiff;
        private final String recentCommits;

        public GitContext(String repositoryRoot, String status, String stagedDiff, String workingTreeDiff) {
            this(repositoryRoot, status, stagedDiff, workingTreeDiff, "");
        }

        public GitContext(String repositoryRoot, String status, String stagedDiff, String workingTreeDiff, String recentCommits) {
            this.repositoryRoot = repositoryRoot;
            this.status = status;
            this.stagedDiff = stagedDiff;
            this.workingTreeDiff = workingTreeDiff;
            this.recentCommits = recentCommits;
        }

        public String getRepositoryRoot() {
            return repositoryRoot;
        }

        public String getStatus() {
            return status;
        }

        public String getStagedDiff() {
            return stagedDiff;
        }

        public String getWorkingTreeDiff() {
            return workingTreeDiff;
        }

        public String getRecentCommits() {
            return recentCommits;
        }

        @NotNull
        public String toPromptText() {
            List<String> sections = new ArrayList<>();
            sections.add("Repository Root:\n" + repositoryRoot);
            sections.add("Git Status:\n" + emptyFallback(status));
            sections.add("Recent Commits:\n" + emptyFallback(recentCommits));
            if (!stagedDiff.trim().isEmpty()) {
                sections.add("Staged Diff:\n" + stagedDiff);
            } else {
                sections.add("Working Tree Diff:\n" + emptyFallback(workingTreeDiff));
            }
            return String.join("\n\n", sections);
        }

        @NotNull
        private static String emptyFallback(String value) {
            return value == null || value.trim().isEmpty() ? "(empty)" : value;
        }
    }
}
