package com.fulinlin.setting.ui;

import com.fulinlin.pojo.TemplateLanguage;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.util.text.StringUtil;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;


public class TemplateEditPane {
    private JPanel mainPenel;
    private JTabbedPane tabbedPane;
    private JTable table;
    private JPanel templateEditPenel;

    //my  attribute
    private String template;
    private Map<String, String> typeMap;
    private TemplateEdit templateEdit;


    public TemplateEditPane(GitCommitMessageHelperSettings settings) {
        this.typeMap = new LinkedHashMap<>(settings.getDateSettings().getTypeMap());
        this.template = StringUtil.isEmpty(settings.getDateSettings().getTemplate()) ? "" : settings.getDateSettings().getTemplate();
        templateEdit = new TemplateEdit(
                templateEditPenel,
                this.template,
                this::getTemplateLanguage,
                150);
    }

    public JPanel getMainPenel() {
        return mainPenel;
    }

    public TemplateLanguage getTemplateLanguage() {
        return TemplateLanguage.valueOf(String.valueOf(TemplateLanguage.vm.fileType));
    }


}
