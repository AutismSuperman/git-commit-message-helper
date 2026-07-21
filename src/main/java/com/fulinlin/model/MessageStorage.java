package com.fulinlin.model;

public class MessageStorage {

    private CommitTemplate commitTemplate;

    private String projectTemplateId;

    private String projectPromptId;

    public CommitTemplate getCommitTemplate() {
        return commitTemplate;
    }

    public void setCommitTemplate(CommitTemplate commitTemplate) {
        this.commitTemplate = commitTemplate;
    }

    public String getProjectTemplateId() {
        return projectTemplateId;
    }

    public void setProjectTemplateId(String projectTemplateId) {
        this.projectTemplateId = projectTemplateId;
    }

    public String getProjectPromptId() {
        return projectPromptId;
    }

    public void setProjectPromptId(String projectPromptId) {
        this.projectPromptId = projectPromptId;
    }
}
