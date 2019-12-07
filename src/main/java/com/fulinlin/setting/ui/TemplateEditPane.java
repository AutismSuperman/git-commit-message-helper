package com.fulinlin.setting.ui;

import com.fulinlin.model.TemplateLanguage;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Optional;


public class TemplateEditPane {
    private JPanel mainPenel;
    private JPanel templateEditPenel;
    private JPanel typeEditPenel;

    //my  attribute
    protected GitCommitMessageHelperSettings settings;
    private TemplateEdit templateEdit;
    private AliasTable aliasTable;


    public TemplateEditPane(GitCommitMessageHelperSettings settings) {
        this.settings = settings;
        String template = Optional.of(settings.getDateSettings().getTemplate()).orElse("");
        templateEdit = new TemplateEdit(
                templateEditPenel,
                template,
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
        //init
    }

    public GitCommitMessageHelperSettings getSettings() {
        aliasTable.commit(settings);
        return settings;
    }

    public void importFrom(GitCommitMessageHelperSettings settings) {
        this.settings = settings;
        aliasTable.reset(settings);
    }

    public boolean isSettingsModified(GitCommitMessageHelperSettings settings) {
        if (aliasTable.isModified(settings)) return true;
        return !this.settings.equals(settings) || isModified(settings);
    }

    public boolean isModified(GitCommitMessageHelperSettings data) {
        if (!StringUtil.equals(settings.getDateSettings().getTemplate(), data.getDateSettings().getTemplate())) {
            return true;
        }
        if (settings.getDateSettings().getTypeAliases() != data.getDateSettings().getTypeAliases()) {
            return true;
        }
        return false;
    }


    public JPanel getMainPenel() {
        return mainPenel;
    }

    public TemplateLanguage getTemplateLanguage() {
        return TemplateLanguage.valueOf(String.valueOf(TemplateLanguage.vm.fileType));
    }


}
