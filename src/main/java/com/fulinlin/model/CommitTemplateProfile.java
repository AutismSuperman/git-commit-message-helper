package com.fulinlin.model;

import java.util.Objects;

public class CommitTemplateProfile {

    private String id;

    private String name;

    private String template;

    private Boolean defaultTemplate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Boolean getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Boolean defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public boolean isDefaultTemplate() {
        return Boolean.TRUE.equals(defaultTemplate);
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommitTemplateProfile)) return false;
        CommitTemplateProfile that = (CommitTemplateProfile) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(template, that.template)
                && Objects.equals(defaultTemplate, that.defaultTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, template, defaultTemplate);
    }
}
