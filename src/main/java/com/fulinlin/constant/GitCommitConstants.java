package com.fulinlin.constant;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-08 11:37
 **/
public class GitCommitConstants {
    public static final String DEFAULT_TEMPLATE = "${type}(${scope}): ${subject}\n" +
            "\n" +
            "${body}\n" +
            "\n" +
            "BREAKING CHANGE: ${changes}\n" +
            "\n" +
            "Closes ${closes}\n";

}
