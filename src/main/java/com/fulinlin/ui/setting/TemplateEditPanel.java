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
import com.intellij.util.ui.JBUI;
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
        myDescriptionComponent.setOpaque(false);
        myDescriptionComponent.addHyperlinkListener(new BrowserHyperlinkListener());
        myDescriptionComponent.setCaretPosition(0);
        descriptionPanel.add(createAutoHeightDescriptionPanel(), BorderLayout.CENTER);

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
        templateEditor.getComponent().setBorder(JBUI.Borders.empty(4));
        JBScrollPane templateScrollPane = new JBScrollPane(templateEditor.getComponent());
        templatePanel.add(templateScrollPane, BorderLayout.CENTER);

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

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(JBUI.Borders.empty(12));

        contentPanel.add(description);
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(8)));
        contentPanel.add(createSectionPanel(descriptionLabel, descriptionPanel, null, 0));
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(12)));
        contentPanel.add(createSectionPanel(templateLabel, templatePanel, restoreDefaultsButton, JBUI.scale(320)));
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(12)));
        contentPanel.add(createPreviewSection());

        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
        scrollPane.getHorizontalScrollBar().setUnitIncrement(JBUI.scale(16));

        templateTab.add(scrollPane, BorderLayout.CENTER);
        templateTab.revalidate();
        templateTab.repaint();
    }

    private JPanel createPreviewSection() {
        JPanel previewOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(12), JBUI.scale(8)));
        previewOptions.setOpaque(false);
        previewOptions.setBorder(JBUI.Borders.emptyBottom(8));
        addPreviewCheckBox(previewOptions, typeCheckBox);
        addPreviewCheckBox(previewOptions, scopeCheckBox);
        addPreviewCheckBox(previewOptions, subjectCheckBox);
        addPreviewCheckBox(previewOptions, bodyCheckBox);
        addPreviewCheckBox(previewOptions, changesCheckBox);
        addPreviewCheckBox(previewOptions, closedCheckBox);
        addPreviewCheckBox(previewOptions, skipCiCheckBox);
        return createSectionPanel(previewLabel, previewPanel, previewOptions, JBUI.scale(280));
    }

    private JComponent createAutoHeightDescriptionPanel() {
        JPanel autoHeightPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                int width = resolveDescriptionWidth();
                myDescriptionComponent.setSize(new Dimension(width, Integer.MAX_VALUE));
                Dimension preferredSize = myDescriptionComponent.getPreferredSize();
                Insets insets = getInsets();
                return new Dimension(width, preferredSize.height + insets.top + insets.bottom);
            }
        };
        autoHeightPanel.setOpaque(false);
        autoHeightPanel.add(myDescriptionComponent, BorderLayout.CENTER);
        return autoHeightPanel;
    }

    private int resolveDescriptionWidth() {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, myDescriptionComponent);
        if (viewport != null && viewport.getWidth() > 0) {
            return Math.max(JBUI.scale(480), viewport.getWidth());
        }
        int panelWidth = descriptionPanel.getWidth();
        if (panelWidth > 0) {
            return Math.max(JBUI.scale(480), panelWidth);
        }
        return JBUI.scale(680);
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

        JPanel sectionPanel = new JPanel(new BorderLayout(0, JBUI.scale(10)));
        sectionPanel.setBorder(IdeBorderFactory.createRoundedBorder());
        sectionPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(JBUI.Borders.empty(12, 12, 0, 12));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        if (trailingOrTopContent instanceof JButton) {
            titlePanel.add(trailingOrTopContent, BorderLayout.EAST);
        }
        sectionPanel.add(titlePanel, BorderLayout.NORTH);

        JComponent centerContent = content;
        if (trailingOrTopContent != null && !(trailingOrTopContent instanceof JButton)) {
            JPanel contentPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
            contentPanel.setOpaque(false);
            contentPanel.add(trailingOrTopContent, BorderLayout.NORTH);
            contentPanel.add(content, BorderLayout.CENTER);
            centerContent = contentPanel;
        }

        centerContent.setBorder(JBUI.Borders.empty(0, 12, 12, 12));
        sectionPanel.add(centerContent, BorderLayout.CENTER);
        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        if (preferredHeight > 0) {
            sectionPanel.setPreferredSize(new Dimension(0, preferredHeight));
        }
        return sectionPanel;
    }

    private void removeFromParent(Component component) {
        Container parent = component.getParent();
        if (parent != null) {
            parent.remove(component);
        }
    }
}
