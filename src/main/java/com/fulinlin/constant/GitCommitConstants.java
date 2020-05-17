package com.fulinlin.constant;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-08 11:37
 **/
public class GitCommitConstants {
    public static final String DEFAULT_TEMPLATE = "#if($type)${type}#end#if($scope)(${scope})#end: #if($subject)${subject}${newline}#end\n" +
            "#if($body)${body}${newline}#end\n" +
            "#if($changes)BREAKING CHANGE: ${changes}${newline}#end\n" +
            "#if($closes)Closes ${closes}#end\n";
}
