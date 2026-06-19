package com.fulinlin.ui.setting;

import com.fulinlin.constant.GitCommitConstants;
import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.ui.setting.description.DescriptionRead;
import com.fulinlin.utils.VelocityUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Optional;


public class TemplateEditPanel implements Disposable {
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
        myDescriptionComponent.setEditorKit(new JBHtmlEditorKit());
        myDescriptionComponent.setEditable(false);
        myDescriptionComponent.setOpaque(false);
        myDescriptionComponent.addHyperlinkListener(new BrowserHyperlinkListener());
        myDescriptionComponent.setCaretPosition(0);
        descriptionPanel.add(createDescriptionScrollPane(), BorderLayout.CENTER);

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
        templateEditorSettings.setLineMarkerAreaShown(true);
        templateEditorSettings.setLineNumbersShown(true);
        templateEditorSettings.setUseSoftWraps(true);
        templateEditorSettings.setVirtualSpace(false);
        if (templateEditor instanceof EditorEx) {
            ((EditorEx) templateEditor).setHorizontalScrollbarVisible(false);
        }
        templatePanel.add(templateEditor.getComponent(), BorderLayout.CENTER);

        // Init previewPanel
        previewEditor = new EditorTextField();
        previewEditor.setViewer(true);
        previewEditor.setOneLineMode(false);
        previewEditor.ensureWillComputePreferredSize();
        previewEditor.addSettingsProvider(uEditor -> {
            uEditor.setVerticalScrollbarVisible(false);
            uEditor.setHorizontalScrollbarVisible(false);
            uEditor.setBorder(null);
            uEditor.getSettings().setUseSoftWraps(true);
        });
        JBScrollPane previewScrollPane = new JBScrollPane(previewEditor.getComponent());
        previewScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        previewScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewPanel.add(previewScrollPane, BorderLayout.CENTER);
        templateEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                showPreview();
            }
        }, this);
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
                        .addExtraActions
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
        relayoutTemplateTab();
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
            String previewTemplate = templateEditor.getDocument().getText();
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
        showPreview();
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

    private void relayoutTemplateTab() {
        JComponent templateTab = (JComponent) tabbedPane.getComponentAt(0);
        templateTab.removeAll();
        templateTab.setLayout(new BorderLayout());
        templateTab.setBorder(JBUI.Borders.empty());

        description.setForeground(UIUtil.getContextHelpForeground());
        description.setBorder(JBUI.Borders.emptyBottom(4));

        JPanel contentPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                Container parent = getParent();
                if (parent instanceof JViewport && parent.getWidth() > 0) {
                    preferredSize.width = Math.max(preferredSize.width, parent.getWidth());
                }
                return preferredSize;
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(JBUI.Borders.empty(12, 16, 16, 16));

        contentPanel.add(description);
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(8)));
        contentPanel.add(createTemplateWorkspacePanel());
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(12)));
        contentPanel.add(createSectionPanel(descriptionLabel, descriptionPanel, null, JBUI.scale(270)));

        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
        scrollPane.getHorizontalScrollBar().setUnitIncrement(JBUI.scale(16));

        templateTab.add(scrollPane, BorderLayout.CENTER);
        templateTab.revalidate();
        templateTab.repaint();
    }

    private JComponent createTemplateWorkspacePanel() {
        JPanel templateSection = createSectionPanel(templateLabel, templatePanel, restoreDefaultsButton, 0);
        JPanel previewSection = createPreviewSection();
        templateSection.setMinimumSize(new Dimension(JBUI.scale(480), JBUI.scale(260)));
        previewSection.setMinimumSize(new Dimension(JBUI.scale(280), JBUI.scale(260)));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, templateSection, previewSection);
        splitPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        splitPane.setBorder(JBUI.Borders.empty());
        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(0.62);
        splitPane.setDividerSize(JBUI.scale(7));
        splitPane.setOpaque(false);
        splitPane.setPreferredSize(new Dimension(0, JBUI.scale(460)));
        splitPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(460)));
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.62));
        return splitPane;
    }

    private JPanel createPreviewSection() {
        JPanel previewOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), JBUI.scale(3)));
        previewOptions.setOpaque(false);
        previewOptions.setBorder(JBUI.Borders.emptyBottom(6));
        addPreviewCheckBox(previewOptions, typeCheckBox);
        addPreviewCheckBox(previewOptions, scopeCheckBox);
        addPreviewCheckBox(previewOptions, subjectCheckBox);
        addPreviewCheckBox(previewOptions, bodyCheckBox);
        addPreviewCheckBox(previewOptions, changesCheckBox);
        addPreviewCheckBox(previewOptions, closedCheckBox);
        addPreviewCheckBox(previewOptions, skipCiCheckBox);
        return createSectionPanel(previewLabel, previewPanel, previewOptions, 0);
    }

    private JComponent createDescriptionScrollPane() {
        JBScrollPane scrollPane = new JBScrollPane(myDescriptionComponent);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setViewportBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
        return scrollPane;
    }

    private void addPreviewCheckBox(JPanel container, JCheckBox checkBox) {
        removeFromParent(checkBox);
        checkBox.setOpaque(false);
        container.add(checkBox);
    }

    private JPanel createSectionPanel(JLabel titleLabel, JComponent content, JComponent trailingOrTopContent, int preferredHeight) {
        removeFromParent(titleLabel);
        removeFromParent(content);
        if (trailingOrTopContent != null) {
            removeFromParent(trailingOrTopContent);
        }

        JPanel sectionPanel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        sectionPanel.setBorder(IdeBorderFactory.createTitledBorder(titleLabel.getText(), false));
        sectionPanel.setOpaque(false);

        if (trailingOrTopContent instanceof JButton) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.add(trailingOrTopContent);
            sectionPanel.add(buttonPanel, BorderLayout.NORTH);
        }

        JComponent centerContent = content;
        if (trailingOrTopContent != null && !(trailingOrTopContent instanceof JButton)) {
            JPanel contentPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
            contentPanel.setOpaque(false);
            contentPanel.add(trailingOrTopContent, BorderLayout.NORTH);
            contentPanel.add(content, BorderLayout.CENTER);
            centerContent = contentPanel;
        }

        centerContent.setBorder(JBUI.Borders.empty(6, 8, 8, 8));
        sectionPanel.add(centerContent, BorderLayout.CENTER);
        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (preferredHeight > 0) {
            sectionPanel.setPreferredSize(new Dimension(0, preferredHeight));
            sectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));
        } else {
            sectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
        return sectionPanel;
    }

    private void removeFromParent(Component component) {
        Container parent = component.getParent();
        if (parent != null) {
            parent.remove(component);
        }
    }

    @Override
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(templateEditor);
    }
}
