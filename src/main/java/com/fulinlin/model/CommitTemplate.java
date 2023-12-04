package com.fulinlin.model;

/**
 * @program: git-commit-message-helper
 * @author: fulin
 * @create: 2019-12-08 11:36
 **/
public class CommitTemplate {

    private String type;
    private String scope;
    private String subject;
    private String body;
    private String changes;
    private String closes;
    private String skipCi;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public String getCloses() {
        return closes;
    }

    public void setCloses(String closes) {
        this.closes = closes;
    }

    public String getSkipCi() {
        return skipCi;
    }

    public void setSkipCi(String skipCi) {
        this.skipCi = skipCi;
    }
}
