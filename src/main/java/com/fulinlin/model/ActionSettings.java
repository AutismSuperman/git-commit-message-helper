package com.fulinlin.model;

public class ActionSettings {

    private Boolean createCommitActionVisible;

    private Boolean generateCommitActionVisible;

    private Boolean formatCommitActionVisible;

    private Boolean generateCommitWithAdditionalContextActionVisible;

    public Boolean getCreateCommitActionVisible() {
        return createCommitActionVisible;
    }

    public void setCreateCommitActionVisible(Boolean createCommitActionVisible) {
        this.createCommitActionVisible = createCommitActionVisible;
    }

    public Boolean getGenerateCommitActionVisible() {
        return generateCommitActionVisible;
    }

    public void setGenerateCommitActionVisible(Boolean generateCommitActionVisible) {
        this.generateCommitActionVisible = generateCommitActionVisible;
    }

    public Boolean getFormatCommitActionVisible() {
        return formatCommitActionVisible;
    }

    public void setFormatCommitActionVisible(Boolean formatCommitActionVisible) {
        this.formatCommitActionVisible = formatCommitActionVisible;
    }

    public Boolean getGenerateCommitWithAdditionalContextActionVisible() {
        return generateCommitWithAdditionalContextActionVisible;
    }

    public void setGenerateCommitWithAdditionalContextActionVisible(Boolean generateCommitWithAdditionalContextActionVisible) {
        this.generateCommitWithAdditionalContextActionVisible = generateCommitWithAdditionalContextActionVisible;
    }
}
