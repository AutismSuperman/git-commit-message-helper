package com.fulinlin.setting.ui;

import com.fulinlin.pojo.TemplateLanguage;
import com.fulinlin.pojo.TypeAlias;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;


public class TemplateEditPane {
    private JPanel mainPenel;
    private JTabbedPane tabbedPane;
    private JPanel templateEditPenel;
    private JPanel typeEditPenel;

    //my  attribute
    private String template;
    private List<TypeAlias> typeAliases;
    private TemplateEdit templateEdit;
    private AliasTable aliasTable;


    public TemplateEditPane(GitCommitMessageHelperSettings settings) {
        this.typeAliases = new LinkedList<>(settings.getDateSettings().getTypeAliases());
        this.template = StringUtil.isEmpty(settings.getDateSettings().getTemplate()) ? "" : settings.getDateSettings().getTemplate();

        templateEdit = new TemplateEdit(
                templateEditPenel,
                this.template,
                this::getTemplateLanguage,
                150);
        aliasTable = new AliasTable();
        typeEditPenel.add(
                ToolbarDecorator.createDecorator(aliasTable)
                        .setAddAction(button -> aliasTable.addAlias())
                        .setRemoveAction(button -> aliasTable.removeSelectedAliases())
                        .setEditAction(button -> aliasTable.editAlias())
                        .setMoveUpAction(anActionButton -> aliasTable.moveUp())
                        .setMoveDownAction(anActionButton -> aliasTable.moveDown())
                        .addExtraAction
                                (new AnActionButton("Reset Default Aliases", AllIcons.Actions.Rollback) {
                                    @Override
                                    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                                        aliasTable.resetDefaultAliases();
                                    }
                                }).createPanel(), BorderLayout.CENTER);
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                return aliasTable.editAlias();
            }
        }.installOn(aliasTable);


    }

    public JPanel getMainPenel() {
        return mainPenel;
    }

    public TemplateLanguage getTemplateLanguage() {
        return TemplateLanguage.valueOf(String.valueOf(TemplateLanguage.vm.fileType));
    }


}
