package com.fulinlin.service;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitContextService {

    private static final int MAX_DIFF_LENGTH = 12000;
    private static final int MAX_UNVERSIONED_FILE_LENGTH = 4000;

    @NotNull
    public GitContext collect(@NotNull Project project,
                              @NotNull Collection<Change> selectedChanges,
                              @NotNull Collection<File> selectedFiles) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.trim().isEmpty()) {
            return new GitContext("", "", "", "");
        }
        File baseDir = new File(basePath);
        String repositoryRoot = execute(baseDir, "git", "rev-parse", "--show-toplevel").trim();
        File workDir = repositoryRoot.isEmpty() ? baseDir : new File(repositoryRoot);
        String status = buildStatus(selectedChanges, selectedFiles, workDir);
        String selectedDiff = buildSelectedDiff(project, selectedChanges, workDir.toPath());
        String unversionedSnapshot = buildUnversionedSnapshot(selectedChanges, selectedFiles, workDir);
        String recentCommits = execute(workDir, "git", "log", "-5", "--pretty=format:%h %s");
        return new GitContext(
                workDir.getAbsolutePath(),
                trim(status, 4000),
                trim(combine(selectedDiff, unversionedSnapshot), MAX_DIFF_LENGTH),
                "",
                trim(recentCommits, 2000)
        );
    }

    @NotNull
    private static String buildStatus(@NotNull Collection<Change> selectedChanges,
                                      @NotNull Collection<File> selectedFiles,
                                      @NotNull File workDir) {
        List<String> lines = new ArrayList<>();
        Set<String> changePaths = new HashSet<>();
        for (Change change : selectedChanges) {
            String path = resolveChangePath(change, workDir);
            changePaths.add(path);
            lines.add(change.getType().name() + " " + path);
        }
        for (File file : selectedFiles) {
            String path = relativize(workDir, file);
            if (!changePaths.contains(path)) {
                lines.add("UNVERSIONED " + path);
            }
        }
        return String.join("\n", lines);
    }

    @NotNull
    private static String buildSelectedDiff(@NotNull Project project,
                                            @NotNull Collection<Change> selectedChanges,
                                            @NotNull Path basePath) {
        if (selectedChanges.isEmpty()) {
            return "";
        }
        try {
            List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(project, selectedChanges, basePath, false, false);
            if (patches.isEmpty()) {
                return "";
            }
            StringWriter writer = new StringWriter();
            UnifiedDiffWriter.write(project, basePath, patches, writer, "\n", null, null);
            return writer.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    @NotNull
    private static String buildUnversionedSnapshot(@NotNull Collection<Change> selectedChanges,
                                                   @NotNull Collection<File> selectedFiles,
                                                   @NotNull File workDir) {
        Set<String> changePaths = new HashSet<>();
        for (Change change : selectedChanges) {
            changePaths.add(resolveChangePath(change, workDir));
        }

        List<String> sections = new ArrayList<>();
        for (File file : selectedFiles) {
            String path = relativize(workDir, file);
            if (changePaths.contains(path) || !file.isFile()) {
                continue;
            }
            sections.add("### " + path + "\n" + trim(readFileContent(file), MAX_UNVERSIONED_FILE_LENGTH));
        }
        if (sections.isEmpty()) {
            return "";
        }
        return "Selected Unversioned Files:\n" + String.join("\n\n", sections);
    }

    @NotNull
    private static String combine(@NotNull String selectedDiff, @NotNull String unversionedSnapshot) {
        if (selectedDiff.isEmpty()) {
            return unversionedSnapshot;
        }
        if (unversionedSnapshot.isEmpty()) {
            return selectedDiff;
        }
        return selectedDiff + "\n\n" + unversionedSnapshot;
    }

    @NotNull
    private static String resolveChangePath(@NotNull Change change, @NotNull File workDir) {
        if (change.getAfterRevision() != null) {
            return relativize(workDir, change.getAfterRevision().getFile().getIOFile());
        }
        if (change.getBeforeRevision() != null) {
            return relativize(workDir, change.getBeforeRevision().getFile().getIOFile());
        }
        return "(unknown)";
    }

    @NotNull
    private static String relativize(@NotNull File workDir, @NotNull File file) {
        try {
            return workDir.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()).toString()
                    .replace('\\', '/');
        } catch (Exception ignored) {
            return file.getPath().replace('\\', '/');
        }
    }

    @NotNull
    private static String readFileContent(@NotNull File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "(unable to read file content)";
        }
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
            sections.add("Selected Files Status:\n" + emptyFallback(status));
            sections.add("Recent Commits:\n" + emptyFallback(recentCommits));
            if (!stagedDiff.trim().isEmpty()) {
                sections.add("Selected Changes Diff:\n" + stagedDiff);
            } else {
                sections.add("Selected Changes Diff:\n" + emptyFallback(workingTreeDiff));
            }
            return String.join("\n\n", sections);
        }

        @NotNull
        private static String emptyFallback(String value) {
            return value == null || value.trim().isEmpty() ? "(empty)" : value;
        }
    }
}
