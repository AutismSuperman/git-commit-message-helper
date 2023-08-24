package com.chivenh.ui;

import com.chivenh.model.CommitTemplate;
import com.chivenh.model.TypeAlias;
import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.chivenh.utils.BundleHelper;
import com.chivenh.utils.VelocityUtils;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fulin
 */
public class CommitMessage {
	private static final int MAX_LINE_LENGTH = 72; // https://stackoverflow.com/a/2120040/5138796
	private final String content;

	public CommitMessage(GitCommitMsgHelperSettings settings, TypeAlias typeAlias, String changeScope, String shortDescription, String longDescription, String closedIssues, String breakingChanges,
			String deprecated) {
		this.content = buildContent(settings, typeAlias, changeScope, shortDescription, longDescription, breakingChanges, closedIssues, deprecated);
	}

	private String buildContent(GitCommitMsgHelperSettings settings, TypeAlias typeAlias, String changeScope, String shortDescription, String longDescription, String breakingChanges,
			String closedIssues, String deprecated) {

		CommitTemplate commitTemplate = new CommitTemplate();
		int valid = 0, willValid = 3;
		if (typeAlias != null) {
			if (StringUtils.isNotBlank(typeAlias.getTitle())) {
				commitTemplate.setType(typeAlias.getTitle());
				valid++;
			}
		}
		if (StringUtils.isNotBlank(changeScope)) {
			commitTemplate.setScope(changeScope);
			valid++;
		}
		if (StringUtils.isNotBlank(shortDescription)) {
			commitTemplate.setSubject(shortDescription);
			valid++;
		}
		if (valid < willValid) {
			return "";
		}
		if (StringUtils.isNotBlank(longDescription)) {
			String[] splitStr = longDescription.split("\n+");
			StringBuilder longDesc = new StringBuilder();
			for (String si : splitStr) {
				if (StringUtils.isBlank(si)) {
					continue;
				}
				if (longDesc.length() > 0) {
					longDesc.append('\n');
				}
				if (!si.startsWith("-")) {
					longDesc.append("- ");
				}
				longDesc.append(si.trim());
			}
			if (longDesc.length() > 0) {
				commitTemplate.setBody(longDesc.toString());
			}
		}
		boolean hasFooter = false;
		breakingChanges= breakingChanges.replace(BundleHelper.message("commitPanel.msgBreakingChanges.defaultText"),"");
		if (StringUtils.isNotBlank(breakingChanges)) {
			String[] splitStr = breakingChanges.split("\n+");
			StringBuilder longDesc = new StringBuilder();
			for (String si : splitStr) {
				if (StringUtils.isBlank(si)) {
					continue;
				}
				if (longDesc.length() > 0) {
					if (!si.startsWith("-")) {
						longDesc.append("\n- ");
					} else {
						longDesc.append("\n");
					}
				}
				longDesc.append(si.trim());
			}
			commitTemplate.setChanges(longDesc.toString());
			hasFooter = true;
		}
		deprecated=deprecated.replace(BundleHelper.message("commitPanel.msgDeprecated.defaultText"),"");
		if (StringUtils.isNotBlank(deprecated)) {
			String[] splitStr = deprecated.split("\n+");
			StringBuilder longDesc = new StringBuilder();
			for (String si : splitStr) {
				if (StringUtils.isBlank(si)) {
					continue;
				}
				if (longDesc.length() > 0) {
					if (!si.startsWith("-")) {
						longDesc.append("\n- ");
					} else {
						longDesc.append("\n");
					}
				}
				longDesc.append(si);
			}
			commitTemplate.setDeprecated(longDesc.toString());
			hasFooter = true;
		}
		if (StringUtils.isNotBlank(closedIssues)) {
			commitTemplate.setCloses(closedIssues);
			hasFooter = true;
		}
		String template = settings.getDateSettings().getTemplate();
		return VelocityUtils.convert(template, commitTemplate, hasFooter);
	}

	static Pattern HEADER_MATCH = Pattern.compile("([^(]+)\\((\\S+)\\):\\s([^(]*)", Pattern.MULTILINE);

	public static CommitTemplate defaultParse(String msg) {
		CommitTemplate commitTemplate = new CommitTemplate();
		String[] contentList = msg.split(CommitTemplate.SECTION, 3);
		int length = contentList.length;
		if (length > 0) {
			String header, body, changes, deprecated, issues;
			header = contentList[0];
			Matcher matcher = HEADER_MATCH.matcher(header);
			if (matcher.matches()) {
				body = null;
				commitTemplate.setType(matcher.group(1));
				commitTemplate.setScope(matcher.group(2));
				commitTemplate.setSubject(matcher.group(3));
			} else {
				body = header;
			}
			if (length > 1) {
				String cBody = contentList[1];
				String footer = null;

				if (cBody.contains("BREAKING") || cBody.contains("DEPRECATED") || cBody.contains("ISSUES")) {
					footer = cBody;
				} else {
					if (body != null) {
						body += cBody;
					} else {
						body = cBody;
					}
					if (length > 2) {
						footer = contentList[2];
					}
				}
				if (footer != null) {
					int bc = footer.indexOf("BREAKING");
					int dp = footer.indexOf("DEPRECATED");
					int is = footer.indexOf("ISSUES");
					if (bc > -1) {
						bc += 17;
						int end = Math.min(dp>-1?dp:is, is>-1?is:dp);
						if (end > bc) {
							changes = footer.substring(bc, end);
							footer = footer.substring(end);
							dp -= end;
							is -= end;
						} else {
							changes = footer.substring(bc);
							dp = is = -1;
						}
						commitTemplate.setChanges(changes);
					}
					if (dp > -1) {
						dp += 12;
						if (is > dp) {
							deprecated = footer.substring(dp, is);
							footer = footer.substring(is);
							is = 0;
						} else {
							deprecated = footer.substring(dp);
							is = -1;
						}
						commitTemplate.setDeprecated(deprecated);
					}
					if (is > -1) {
						issues = footer.substring(is + 8);
						commitTemplate.setCloses(issues);
					}
				}

			}
			if (body != null) {
				commitTemplate.setBody(body.trim());
			}
		}
		return commitTemplate;
	}

	@Override
	public String toString() {
		return content;
	}
}