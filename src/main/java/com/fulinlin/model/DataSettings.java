package com.fulinlin.model;

import java.util.List;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-05 21:22
 **/
public class DataSettings {
    private String template;
    private List<CommitTemplateProfile> templates;
    private String activeTemplateId;
    private List<TypeAlias> typeAliases;
    private List<String> skipCis;

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<CommitTemplateProfile> getTemplates() {
        return templates;
    }

    public void setTemplates(List<CommitTemplateProfile> templates) {
        this.templates = templates;
    }

    public String getActiveTemplateId() {
        return activeTemplateId;
    }

    public void setActiveTemplateId(String activeTemplateId) {
        this.activeTemplateId = activeTemplateId;
    }

    public List<TypeAlias> getTypeAliases() {
        return typeAliases;
    }

    public void setTypeAliases(List<TypeAlias> typeAliases) {
        this.typeAliases = typeAliases;
    }

    public List<String> getSkipCis() {
        return skipCis;
    }

    public void setSkipCis(List<String> skipCis) {
        this.skipCis = skipCis;
    }
}
