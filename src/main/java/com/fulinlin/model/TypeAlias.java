package com.fulinlin.model;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-06 21:11
 **/
public class TypeAlias extends DomainObject {
    public String title;
    public String description;

    public TypeAlias() {
    }

    public TypeAlias(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("%s - %s", this.getTitle(), this.getDescription());
    }
}
