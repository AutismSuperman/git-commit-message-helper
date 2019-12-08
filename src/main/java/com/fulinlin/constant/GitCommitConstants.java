package com.fulinlin.constant;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-08 11:37
 **/
public class GitCommitConstants {
    public static final String DEFAULT_TEMPLATE = "#if($type)${type}#end#if(${scope})(${scope})#end: #if(${subject})${subject}#end\n" +
            "\n" +
            "#if(${body})${body}#end\n" +
            "\n" +
            "#if(${changes})BREAKING CHANGE: ${changes}#end\n" +
            "\n" +
            "#if(${closes})Closes ${closes}#end\n";

}
