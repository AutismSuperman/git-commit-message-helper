package com.chivenh.constant;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-08 11:37
 **/
public class GitCommitConstants {
	public static final String DEFAULT_TEMPLATE = "#if($type)${type}#end#if($scope)(${scope})#end: #if($subject)${subject}#end\n" + "#if($body)${section}${body}#end\n" + "#if($changes)${section}不兼容变更: ${changes}#end\n" + "#if($closes)${section}}解决工单: ${closes}#end\n";
}
