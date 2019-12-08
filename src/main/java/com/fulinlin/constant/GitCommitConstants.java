package com.fulinlin.constant;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-08 11:37
 **/
public class GitCommitConstants {
    public static final String DEFAULT_TEMPLATE = "#if($type)${type}#end#if(${scope})(${scope})#end: #if(${subject})${subject}#end\n" +
            "${newline}\n" +
            "#if(${body})${body}#end\n" +
            "${newline}\n" +
            "#if(${changes})BREAKING CHANGE: ${changes}#end\n" +
            "${newline}\n" +
            "#if(${closes})Closes ${closes}#end\n";

}
