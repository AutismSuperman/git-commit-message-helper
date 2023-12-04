package com.fulinlin.ui.setting;

import com.fulinlin.constant.GitCommitConstants;
import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.ui.setting.description.DescriptionRead;
import com.fulinlin.utils.VelocityUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Optional;


public class TemplateEditPanel {
    private final AliasTable aliasTable;
    private final Editor templateEditor;
    private final EditorTextField previewEditor;
    private final JEditorPane myDescriptionComponent;
    protected GitCommitMessageHelperSettings settings;
    private JPanel mainPanel;
    private JPanel templatePanel;
    private JPanel typeEditPanel;
    private JTabbedPane tabbedPane;
    private JLabel description;
    private JLabel descriptionLabel;
    private JLabel templateLabel;
    private JPanel descriptionPanel;
    private JLabel previewLabel;
    private JPanel previewPanel;
    private JCheckBox typeCheckBox;
    private JCheckBox scopeCheckBox;
    private JCheckBox subjectCheckBox;
    private JCheckBox bodyCheckBox;
    private JCheckBox changesCheckBox;
    private JCheckBox closedCheckBox;
    private JCheckBox skipCiCheckBox;
    private JButton restoreDefaultsButton;


    public TemplateEditPanel(GitCommitMessageHelperSettings settings) {
        //Get setting
        this.settings = settings.clone();

        // Init  description 
        description.setText(PluginBundle.get("setting.description"));
        descriptionLabel.setText(PluginBundle.get("setting.template.description"));
        templateLabel.setText(PluginBundle.get("setting.template.edit"));
        previewLabel.setText(PluginBundle.get("setting.template.preview"));
        tabbedPane.setTitleAt(0, PluginBundle.get("setting.tabbed.panel.template"));
        tabbedPane.setTitleAt(1, PluginBundle.get("setting.tabbed.panel.type"));
        restoreDefaultsButton.setText(PluginBundle.get("setting.template.restore.defaults"));

        // Init descriptionPanel
        myDescriptionComponent = new JEditorPane();
        myDescriptionComponent.setEditorKit(UIUtil.getHTMLEditorKit());
        myDescriptionComponent.setEditable(false);
        myDescriptionComponent.addHyperlinkListener(new BrowserHyperlinkListener());
        myDescriptionComponent.setCaretPosition(0);
        JBScrollPane descriptionScrollPanel = new JBScrollPane(myDescriptionComponent);
        descriptionScrollPanel.setMaximumSize(new Dimension(150, 50));
        descriptionPanel.add(descriptionScrollPanel);

        // Init  templatePanel
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
        JBScrollPane templateScrollPane = new JBScrollPane(templateEditor.getComponent());
        templateScrollPane.setMaximumSize(new Dimension(150, 50));
        templatePanel.add(templateScrollPane);

        // Init previewPanel
        previewEditor = new EditorTextField();
        previewEditor.setViewer(true);
        previewEditor.setOneLineMode(false);
        previewEditor.ensureWillComputePreferredSize();
        previewEditor.addSettingsProvider(uEditor -> {
            uEditor.setVerticalScrollbarVisible(true);
            uEditor.setHorizontalScrollbarVisible(true);
            uEditor.setBorder(null);
        });
        JBScrollPane previewScrollPane = new JBScrollPane(previewEditor.getComponent());
        previewScrollPane.setMaximumSize(new Dimension(150, 50));
        previewPanel.add(previewScrollPane);
        templateEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                showPreview();
            }
        });
        typeCheckBox.addChangeListener(e -> showPreview());
        scopeCheckBox.addChangeListener(e -> showPreview());
        subjectCheckBox.addChangeListener(e -> showPreview());
        bodyCheckBox.addChangeListener(e -> showPreview());
        changesCheckBox.addChangeListener(e -> showPreview());
        closedCheckBox.addChangeListener(e -> showPreview());
        skipCiCheckBox.addChangeListener(e -> showPreview());
        // Init  typeEditPanel
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

        // Init data
        ApplicationManager.getApplication().runWriteAction(() -> {
            templateEditor.getDocument().setText(template);
            myDescriptionComponent.setText(DescriptionRead.readHtmlFile());
        });
        restoreDefaultsButton.addActionListener(e -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                templateEditor.getDocument().setText(GitCommitConstants.DEFAULT_TEMPLATE);
            });
        });
        // Add  DoubleClickListener
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent e) {
                return aliasTable.editAlias();
            }
        }.installOn(aliasTable);
    }

    private void showPreview() {
        CommitTemplate commitTemplate = new CommitTemplate();
        if (typeCheckBox.isSelected()) {
            commitTemplate.setType("<type>");
        }
        if (scopeCheckBox.isSelected()) {
            commitTemplate.setScope("<scope>");
        }
        if (subjectCheckBox.isSelected()) {
            commitTemplate.setSubject("<subject>");
        }
        if (bodyCheckBox.isSelected()) {
            commitTemplate.setBody("<body>");
        }
        if (changesCheckBox.isSelected()) {
            commitTemplate.setChanges("<changes>");
        }
        if (closedCheckBox.isSelected()) {
            commitTemplate.setCloses("<closes>");
        }
        if (skipCiCheckBox.isSelected()) {
            commitTemplate.setSkipCi("<skipCi>");
        }
        ApplicationManager.getApplication().runWriteAction(() -> {
            String previewTemplate = templateEditor.getDocument().getText().replaceAll("\\n", "");
            previewEditor.getDocument().setText(VelocityUtils.convert(previewTemplate, commitTemplate));
        });
    }


    public GitCommitMessageHelperSettings getSettings() {
        aliasTable.commit(settings);
        settings.getDateSettings().setTemplate(templateEditor.getDocument().getText());
        return settings;
    }

    public void reset(GitCommitMessageHelperSettings settings) {
        this.settings = settings.clone();
        aliasTable.reset(settings);
        ApplicationManager.getApplication().runWriteAction(() ->
                templateEditor.getDocument().setText(settings.getDateSettings().getTemplate())
        );
        myDescriptionComponent.setText(DescriptionRead.readHtmlFile());
    }

    public boolean isSettingsModified(GitCommitMessageHelperSettings settings) {
        if (aliasTable.isModified(settings)) return true;
        return isModified(settings);
    }

    public boolean isModified(GitCommitMessageHelperSettings data) {
        if (!StringUtil.equals(settings.getDateSettings().getTemplate(), templateEditor.getDocument().getText())) {
            return true;
        }
        return settings.getDateSettings().getTypeAliases() == data.getDateSettings().getTypeAliases();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

}
