package com.fulinlin.model;


import org.apache.commons.text.StringEscapeUtils;


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
        this.title = StringEscapeUtils.escapeJava(title);
        this.description = StringEscapeUtils.escapeJava(description);
    }

    public String getTitle() {
        return StringEscapeUtils.unescapeJava(title);
    }

    public void setTitle(String title) {
        this.title = StringEscapeUtils.escapeJava(title);
    }

    public String getDescription() {
        return StringEscapeUtils.unescapeJava(description);
    }

    public void setDescription(String description) {
        this.description = StringEscapeUtils.escapeJava(description);
    }

    @Override
    public String toString() {
        return String.format("%s - %s", this.getTitle(), this.getDescription());
    }

}
