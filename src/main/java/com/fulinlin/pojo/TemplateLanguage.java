package com.fulinlin.pojo;

public enum TemplateLanguage {

    vm("vm");

    TemplateLanguage(String fileType) {
        this.fileType = fileType;
    }

    public final String fileType;
}
