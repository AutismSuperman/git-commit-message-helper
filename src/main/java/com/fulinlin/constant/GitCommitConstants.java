package com.fulinlin.constant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-08 11:37
 **/
public class GitCommitConstants {

    public static final String ACTION_PREFIX = "$APP_CONFIG$/GitCommitMessageHelperSettings";

    public static final String DEFAULT_TEMPLATE = loadDefaultTemplate();

    private static String loadDefaultTemplate() {
        try (InputStream inputStream = GitCommitConstants.class.getClassLoader()
                .getResourceAsStream("includes/defaultTemplate.vm")) {
            if (inputStream == null) {
                throw new IllegalStateException("Default template resource not found: includes/defaultTemplate.vm");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load default template resource", e);
        }
    }
}
