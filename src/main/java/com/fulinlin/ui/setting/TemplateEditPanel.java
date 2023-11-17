package com.fulinlin.ui.setting;

import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.utils.PropertiesUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Optional;


public class TemplateEditPanel {
    private final AliasTable aliasTable;
    private final Editor templateEditor;
    //my  attribute
    protected GitCommitMessageHelperSettings settings;
    private JPanel mainPanel;
    private JPanel templateEditPanel;
    private JPanel typeEditPanel;
    private JTabbedPane tabbedPane;
    private JLabel description;
    private JLabel explainDescriptionLabel;
    private JLabel typeExplainDescriptionLabel;
    private JLabel subjectExplainDescriptionLabel;
    private JLabel bodyExplainDescriptionLabel;
    private JLabel changesExplainDescriptionLabel;
    private JLabel closesExplainDescriptionLabel;
    private JLabel newLineExplainDescriptionLabel;
    private JLabel settingTemplateDescriptionLabel;
    private JLabel scopeExplainDescriptionLabel;


    public TemplateEditPanel(GitCommitMessageHelperSettings settings) {
        //get setting
        this.settings = settings.clone();
        //init  description
        description.setText(PropertiesUtils.getInfo("setting.description"));
        explainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.explainDescription"));
        typeExplainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.typeExplainDescription"));
        scopeExplainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.scopeExplainDescription"));
        subjectExplainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.subjectExplainDescription"));
        bodyExplainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.bodyExplainDescription"));
        changesExplainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.changesExplainDescription"));
        closesExplainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.closesExplainDescription"));
        newLineExplainDescriptionLabel.setText(PropertiesUtils.getInfo("setting.newLineExplainDescription"));
        settingTemplateDescriptionLabel.setText(PropertiesUtils.getInfo("setting.settingTemplateDescription"));

        //init  templateEditor
        String template = Optional.of(settings.getDateSettings().getTemplate()).orElse("");
        templateEditor = EditorFactory.getInstance().createEditor(
                EditorFactory.getInstance().createDocument(""),
                null,
                FileTypeManager.getInstance().getFileTypeByExtension("vm"),
                false);
        EditorSettings templateEditorSettings = templateEditor.getSettings();
        templateEditorSettings.setAdditionalLinesCount(0);
        templateEditorSettings.setAdditionalColumnsCount(0);
        templateEditorSettings.setLineMarkerAreaShown(false);
        templateEditorSettings.setVirtualSpace(false);
        JBScrollPane jbScrollPane = new JBScrollPane(templateEditor.getComponent());
        jbScrollPane.setMaximumSize(new Dimension(150, 50));
        templateEditPanel.add(jbScrollPane);
        ApplicationManager.getApplication().runWriteAction(() -> templateEditor.getDocument().setText(template));
        

        //init  typeEditPanel
        aliasTable = new AliasTable();
        typeEditPanel.add(
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


        // DoubleClickListener
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
        settings.getDateSettings().setTemplate(templateEditor.getDocument().getText());
        return settings;
    }

    public void reset(GitCommitMessageHelperSettings settings) {
        this.settings = settings.clone();
        aliasTable.reset(settings);
        ApplicationManager.getApplication().runWriteAction(() -> templateEditor.getDocument().setText(settings.getDateSettings().getTemplate()));
    }


    public boolean isSettingsModified(GitCommitMessageHelperSettings settings) {
        if (aliasTable.isModified(settings)) return true;
        return isModified(settings);
    }

    public boolean isModified(GitCommitMessageHelperSettings data) {
        if (!StringUtil.equals(settings.getDateSettings().getTemplate(), templateEditor.getDocument().getText())) {
            return true;
        }
        if (settings.getDateSettings().getTypeAliases() == data.getDateSettings().getTypeAliases()) {
            return true;
        }
        return false;
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }


}
