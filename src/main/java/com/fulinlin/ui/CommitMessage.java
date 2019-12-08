package com.fulinlin.ui;

import com.fulinlin.model.ChangeType;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.utils.VelocityUtils;
import org.apache.commons.lang.WordUtils;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * @author fulin
 */
public class CommitMessage {
    private static final int MAX_LINE_LENGTH = 72; // https://stackoverflow.com/a/2120040/5138796
    private final String content;

    public CommitMessage(GitCommitMessageHelperSettings settings, TypeAlias typeAlias, String changeScope, String shortDescription, String longDescription, String closedIssues, String breakingChanges) {
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

    private String buildContent(GitCommitMessageHelperSettings settings,
                                TypeAlias typeAlias,
                                String changeScope,
                                String shortDescription,
                                String longDescription,
                                String breakingChanges,
                                String closedIssues
    ) {

        CommitTemplate commitTemplate = new CommitTemplate();
        commitTemplate.setType(typeAlias.toString());
        commitTemplate.setScope(changeScope);
        commitTemplate.setSubject(shortDescription);
        commitTemplate.setBody(longDescription);
        commitTemplate.setChanges(breakingChanges);
        commitTemplate.setCloses(closedIssues);
        String template = settings.getDateSettings().getTemplate();
        return VelocityUtils.convert(template, commitTemplate);
    }

    @Override
    public String toString() {
        return content;
    }
}