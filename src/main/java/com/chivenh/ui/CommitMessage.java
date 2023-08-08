package com.chivenh.ui;

import com.chivenh.model.CommitTemplate;
import com.chivenh.model.TypeAlias;
import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.chivenh.utils.VelocityUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author fulin
 */
public class CommitMessage {
    private static final int MAX_LINE_LENGTH = 72; // https://stackoverflow.com/a/2120040/5138796
    private final String content;

    public CommitMessage(GitCommitMsgHelperSettings settings, TypeAlias typeAlias, String changeScope, String shortDescription, String longDescription, String closedIssues, String breakingChanges) {
        this.content = buildContent(
                settings,
                typeAlias,
                changeScope,
                shortDescription,
                longDescription,
                breakingChanges,
                closedIssues
        );
    }

    private String buildContent(GitCommitMsgHelperSettings settings,
                                TypeAlias typeAlias,
                                String changeScope,
                                String shortDescription,
                                String longDescription,
                                String breakingChanges,
                                String closedIssues
    ) {

        CommitTemplate commitTemplate = new CommitTemplate();
        if (typeAlias != null) {
            if (StringUtils.isNotBlank(typeAlias.getTitle())) {
                commitTemplate.setType(typeAlias.getTitle());
            }
        }
        if (StringUtils.isNotBlank(changeScope)) {
            commitTemplate.setScope(changeScope);
        }
        if (StringUtils.isNotBlank(shortDescription)) {
            commitTemplate.setSubject(shortDescription);
        }
        if (StringUtils.isNotBlank(longDescription)) {
            commitTemplate.setBody(longDescription);
        }
        if (StringUtils.isNotBlank(breakingChanges)) {
            commitTemplate.setChanges(breakingChanges);
        }
        if (StringUtils.isNotBlank(closedIssues)) {
            commitTemplate.setCloses(closedIssues);
        }
        String template = settings.getDateSettings().getTemplate();
        return VelocityUtils.convert(template, commitTemplate);
    }

    @Override
    public String toString() {
        return content;
    }
}