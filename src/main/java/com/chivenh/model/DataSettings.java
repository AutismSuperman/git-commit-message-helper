package com.chivenh.model;

import java.util.List;

/**
 * @program: git-commit-message-helper
 * @author Chivenh
 * @since 2023-08-20 18:30
 **/
public class DataSettings {
    private String template;
    private List<TypeAlias> typeAliases;

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<TypeAlias> getTypeAliases() {
        return typeAliases;
    }

    public void setTypeAliases(List<TypeAlias> typeAliases) {
        this.typeAliases = typeAliases;
    }


}
