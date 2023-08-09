package com.chivenh.constant;

/**
 * @author Chivenh
 * @program: git-commit-message-helper
 * @create: 2019-12-08 11:37
 **/
public class GitCommitConstants {
	/**
	 * 默认模板
	 */
	public static final String DEFAULT_TEMPLATE =
			// @formatter:off
			"#if($type)${type}#end#if($scope)(${scope})#end: #if($subject)${subject}#end\n"
					+ "#if($body)${breakLine}${section}${body}#end\n"
					+ "#if($hasFooter)${section}#end\n"
					+ "#if($changes)${breakLine}BREAKING CHANGE: ${changes}#end\n"
					+ "#if($deprecated)${breakLine}DEPRECATED: ${deprecated}#end\n"
					+ "#if($closes)${breakLine}ISSUES: ${closes}#end\n";
	// @formatter:on
}
