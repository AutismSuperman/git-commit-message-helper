package com.fulinlin.model;

public class MessageStorage {

    private CommitTemplate commitTemplate;

    private String projectTemplateId;

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
}
