package com.fulinlin.ui.setting;

import com.fulinlin.constant.GitCommitConstants;
import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.CommitTemplateProfile;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.storage.GitCommitMessageStorage;
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


public class TemplateEditPanel implements Disposable {
    private final AliasTable aliasTable;
    private final Editor templateEditor;
    private final JTextArea previewTextArea;
    private final JEditorPane myDescriptionComponent;
    private final DefaultListModel<CommitTemplateProfile> templateListModel;
    private final JBList<CommitTemplateProfile> templateList;
    private final JComboBox<CommitTemplateProfile> globalDefaultTemplateComboBox;
    private final JComboBox<CommitTemplateProfile> projectDefaultTemplateComboBox;
    private final Project currentProject;
    protected GitCommitMessageHelperSettings settings;
    private CommitTemplateProfile displayedTemplate;
    private boolean loadingTemplateSelection;
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
        templateListModel = new DefaultListModel<>();
        templateList = new JBList<>(templateListModel);
        globalDefaultTemplateComboBox = new JComboBox<>();
        projectDefaultTemplateComboBox = new JComboBox<>();
        currentProject = findCurrentProject();

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
        configureTemplateList();
        configureDefaultTemplateControls();

        // Init  templatePanel
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
        previewTextArea = new JTextArea();
        previewTextArea.setEditable(false);
        previewTextArea.setFocusable(false);
        previewTextArea.setLineWrap(true);
        previewTextArea.setWrapStyleWord(false);
        previewTextArea.setRows(9);
        previewTextArea.setBorder(JBUI.Borders.empty(6, 8));
        previewTextArea.setFont(templateEditor.getContentComponent().getFont());
        JBScrollPane previewScrollPane = new JBScrollPane(previewTextArea);
        previewScrollPane.setBorder(JBUI.Borders.empty());
        previewScrollPane.setViewportBorder(JBUI.Borders.empty());
        previewScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        previewScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewScrollPane.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
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
            myDescriptionComponent.setText(DescriptionRead.readHtmlFile());
        });
        reloadTemplateList();
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
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent e) {
                renameSelectedTemplate();
                return true;
            }
        }.installOn(templateList);
    }

    private void configureTemplateList() {
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateList.setCellRenderer(new TemplateListCellRenderer());
        templateList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || loadingTemplateSelection) {
                return;
            }
            commitDisplayedTemplate();
            loadSelectedTemplate();
        });
    }

    private void configureDefaultTemplateControls() {
        TemplateListCellRenderer renderer = new TemplateListCellRenderer();
        globalDefaultTemplateComboBox.setRenderer(renderer);
        projectDefaultTemplateComboBox.setRenderer(renderer);
        projectDefaultTemplateComboBox.setEnabled(currentProject != null);
    }

    private void reloadTemplateList() {
        loadingTemplateSelection = true;
        templateListModel.clear();
        List<CommitTemplateProfile> templates = settings.getDateSettings().getTemplates();
        for (CommitTemplateProfile template : templates) {
            templateListModel.addElement(template);
        }
        CommitTemplateProfile activeTemplate = settings.getActiveCommitTemplateProfile();
        int activeIndex = 0;
        for (int i = 0; i < templateListModel.size(); i++) {
            if (Objects.equals(templateListModel.getElementAt(i).getId(), activeTemplate.getId())) {
                activeIndex = i;
                break;
            }
        }
        if (!templateListModel.isEmpty()) {
            templateList.setSelectedIndex(activeIndex);
        }
        loadingTemplateSelection = false;
        refreshDefaultTemplateComboModels();
        loadSelectedTemplate();
    }

    private void refreshDefaultTemplateComboModels() {
        String globalTemplateId = getSelectedTemplateId(globalDefaultTemplateComboBox);
        if (globalTemplateId == null) {
            globalTemplateId = settings.getDateSettings().getActiveTemplateId();
        }
        String projectTemplateId = getSelectedTemplateId(projectDefaultTemplateComboBox);
        if (projectTemplateId == null) {
            projectTemplateId = getEffectiveProjectTemplateId();
        }

        globalDefaultTemplateComboBox.removeAllItems();
        projectDefaultTemplateComboBox.removeAllItems();
        for (int i = 0; i < templateListModel.size(); i++) {
            CommitTemplateProfile template = templateListModel.getElementAt(i);
            globalDefaultTemplateComboBox.addItem(template);
            projectDefaultTemplateComboBox.addItem(template);
        }

        selectTemplate(globalDefaultTemplateComboBox, globalTemplateId);
        selectTemplate(projectDefaultTemplateComboBox, projectTemplateId);
    }

    private String getSelectedTemplateId(JComboBox<CommitTemplateProfile> comboBox) {
        CommitTemplateProfile selectedTemplate = (CommitTemplateProfile) comboBox.getSelectedItem();
        return selectedTemplate == null ? null : selectedTemplate.getId();
    }

    private void selectTemplate(JComboBox<CommitTemplateProfile> comboBox, String templateId) {
        if (templateId != null) {
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                CommitTemplateProfile template = comboBox.getItemAt(i);
                if (Objects.equals(templateId, template.getId())) {
                    comboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }

    private String getProjectTemplateId() {
        GitCommitMessageStorage storage = getProjectStorage();
        if (storage == null || storage.getState() == null || storage.getState().getMessageStorage() == null) {
            return null;
        }
        return storage.getState().getMessageStorage().getProjectTemplateId();
    }

    private String getEffectiveProjectTemplateId() {
        String projectTemplateId = getProjectTemplateId();
        if (projectTemplateId != null && findTemplateInList(projectTemplateId) != null) {
            return projectTemplateId;
        }
        return templateListModel.isEmpty() ? null : templateListModel.getElementAt(0).getId();
    }

    private CommitTemplateProfile findTemplateInList(String templateId) {
        if (templateId == null) {
            return null;
        }
        for (int i = 0; i < templateListModel.size(); i++) {
            CommitTemplateProfile template = templateListModel.getElementAt(i);
            if (Objects.equals(templateId, template.getId())) {
                return template;
            }
        }
        return null;
    }

    private GitCommitMessageStorage getProjectStorage() {
        return currentProject == null ? null : currentProject.getService(GitCommitMessageStorage.class);
    }

    private Project findCurrentProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        return openProjects.length == 0 ? null : openProjects[0];
    }

    private void loadSelectedTemplate() {
        CommitTemplateProfile selectedTemplate = templateList.getSelectedValue();
        if (selectedTemplate == null) {
            displayedTemplate = null;
            return;
        }
        displayedTemplate = selectedTemplate;
        settings.getDateSettings().setActiveTemplateId(selectedTemplate.getId());
        ApplicationManager.getApplication().runWriteAction(() ->
                templateEditor.getDocument().setText(defaultString(selectedTemplate.getTemplate()))
        );
        showPreview();
    }

    private void commitDisplayedTemplate() {
        if (displayedTemplate != null) {
            displayedTemplate.setTemplate(templateEditor.getDocument().getText());
        }
    }

    private void addTemplate() {
        commitDisplayedTemplate();
        int index = templateListModel.size();
        CommitTemplateProfile template = GitCommitMessageHelperSettings.createCommitTemplateProfile(
                GitCommitMessageHelperSettings.createTemplateId(index),
                createTemplateName(PluginBundle.get("setting.template.new.name")),
                GitCommitConstants.DEFAULT_TEMPLATE,
                false
        );
        templateListModel.addElement(template);
        refreshDefaultTemplateComboModels();
        templateList.setSelectedIndex(templateListModel.size() - 1);
    }

    private void removeSelectedTemplate() {
        CommitTemplateProfile selectedTemplate = templateList.getSelectedValue();
        if (selectedTemplate == null) {
            return;
        }
        if (selectedTemplate.isDefaultTemplate()) {
            Messages.showWarningDialog(
                    mainPanel,
                    PluginBundle.get("setting.template.default.remove.warning"),
                    PluginBundle.get("setting.configurable.template")
            );
            return;
        }
        commitDisplayedTemplate();
        int selectedIndex = templateList.getSelectedIndex();
        loadingTemplateSelection = true;
        templateListModel.remove(selectedIndex);
        int nextIndex = Math.min(selectedIndex, templateListModel.size() - 1);
        if (nextIndex >= 0) {
            templateList.setSelectedIndex(nextIndex);
        }
        loadingTemplateSelection = false;
        refreshDefaultTemplateComboModels();
        loadSelectedTemplate();
    }

    private void renameSelectedTemplate() {
        CommitTemplateProfile selectedTemplate = templateList.getSelectedValue();
        if (selectedTemplate == null) {
            return;
        }
        if (selectedTemplate.isDefaultTemplate()) {
            Messages.showWarningDialog(
                    mainPanel,
                    PluginBundle.get("setting.template.default.rename.warning"),
                    PluginBundle.get("setting.configurable.template")
            );
            return;
        }
        String name = Messages.showInputDialog(
                mainPanel,
                PluginBundle.get("setting.template.rename.message"),
                PluginBundle.get("setting.template.rename.title"),
                null,
                selectedTemplate.getName(),
                null
        );
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        selectedTemplate.setName(createTemplateName(name.trim(), selectedTemplate));
        refreshDefaultTemplateComboModels();
        templateList.repaint();
    }

    private String createTemplateName(String baseName) {
        return createTemplateName(baseName, null);
    }

    private String createTemplateName(String baseName, CommitTemplateProfile ignoredTemplate) {
        String name = baseName;
        int counter = 2;
        while (containsTemplateName(name, ignoredTemplate)) {
            name = baseName + " " + counter++;
        }
        return name;
    }

    private boolean containsTemplateName(String name, CommitTemplateProfile ignoredTemplate) {
        for (int i = 0; i < templateListModel.size(); i++) {
            CommitTemplateProfile template = templateListModel.getElementAt(i);
            if (template != ignoredTemplate && name.equals(template.getName())) {
                return true;
            }
        }
        return false;
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
        String previewTemplate = templateEditor.getDocument().getText();
        String previewText = VelocityUtils.convert(previewTemplate, commitTemplate);
        previewTextArea.setText(previewText);
        previewTextArea.setCaretPosition(0);
        previewPanel.revalidate();
    }


    public GitCommitMessageHelperSettings getSettings() {
        aliasTable.commit(settings);
        syncTemplateListToSettings(true);
        return settings;
    }

    public void reset(GitCommitMessageHelperSettings settings) {
        this.settings = settings.clone();
        aliasTable.reset(settings);
        myDescriptionComponent.setText(DescriptionRead.readHtmlFile());
        reloadTemplateList();
    }

    public boolean isSettingsModified(GitCommitMessageHelperSettings settings) {
        if (aliasTable.isModified(settings)) return true;
        return isModified(settings);
    }

    public boolean isModified(GitCommitMessageHelperSettings data) {
        syncTemplateListToSettings(false);
        if (!Objects.equals(settings.getDateSettings().getTemplates(), data.getDateSettings().getTemplates())) {
            return true;
        }
        if (!Objects.equals(settings.getDateSettings().getActiveTemplateId(), data.getDateSettings().getActiveTemplateId())) {
            return true;
        }
        return currentProject != null
                && !Objects.equals(getSelectedTemplateId(projectDefaultTemplateComboBox), getEffectiveProjectTemplateId());
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void syncTemplateListToSettings(boolean persistProjectDefault) {
        commitDisplayedTemplate();
        List<CommitTemplateProfile> templates = new LinkedList<>();
        for (int i = 0; i < templateListModel.size(); i++) {
            templates.add(templateListModel.getElementAt(i));
        }
        settings.getDateSettings().setTemplates(templates);
        CommitTemplateProfile globalDefaultTemplate = (CommitTemplateProfile) globalDefaultTemplateComboBox.getSelectedItem();
        if (globalDefaultTemplate == null && !templates.isEmpty()) {
            globalDefaultTemplate = templates.get(0);
        }
        if (globalDefaultTemplate != null) {
            settings.getDateSettings().setActiveTemplateId(globalDefaultTemplate.getId());
            settings.getDateSettings().setTemplate(defaultString(globalDefaultTemplate.getTemplate()));
        }
        CommitTemplateProfile projectDefaultTemplate = (CommitTemplateProfile) projectDefaultTemplateComboBox.getSelectedItem();
        GitCommitMessageStorage projectStorage = getProjectStorage();
        if (persistProjectDefault && projectStorage != null && projectStorage.getState() != null && projectStorage.getState().getMessageStorage() != null) {
            if (projectDefaultTemplate == null && !templates.isEmpty()) {
                projectDefaultTemplate = templates.get(0);
            }
            projectStorage.getState().getMessageStorage().setProjectTemplateId(
                    projectDefaultTemplate == null ? null : projectDefaultTemplate.getId()
            );
        }
    }

    private void relayoutTemplateTab() {
        JComponent templateTab = (JComponent) tabbedPane.getComponentAt(0);
        templateTab.removeAll();
        templateTab.setLayout(new BorderLayout());
        templateTab.setBorder(JBUI.Borders.empty());

        description.setForeground(UIUtil.getContextHelpForeground());
        description.setBorder(JBUI.Borders.emptyBottom(4));

        JPanel contentPanel = new JPanel(new BorderLayout(0, JBUI.scale(8))) {
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
        contentPanel.setOpaque(false);
        contentPanel.setBorder(JBUI.Borders.empty(12, 16, 16, 16));
        contentPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        contentPanel.add(createTemplateWorkspacePanel(), BorderLayout.CENTER);

        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setViewportBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
        templateTab.add(scrollPane, BorderLayout.CENTER);
        templateTab.revalidate();
        templateTab.repaint();
    }

    private JComponent createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        headerPanel.setOpaque(false);
        headerPanel.add(description, BorderLayout.NORTH);
        headerPanel.add(createDefaultTemplatePanel(), BorderLayout.CENTER);
        return headerPanel;
    }

    private JComponent createDefaultTemplatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(0, 0, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(PluginBundle.get("setting.template.global.default")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(globalDefaultTemplateComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(PluginBundle.get("setting.template.project.default")), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(projectDefaultTemplateComboBox, gbc);

        gbc.gridx = 4;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);
        return panel;
    }

    private JComponent createTemplateWorkspacePanel() {
        JPanel templateDirectorySection = createTemplateDirectorySection();
        JPanel templateSection = createSectionPanel(templateLabel, templatePanel, restoreDefaultsButton, 0);
        JPanel previewSection = createPreviewSection();
        JPanel descriptionSection = createSectionPanel(descriptionLabel, descriptionPanel, null, 0);
        int leftWidth = JBUI.scale(320);
        templateDirectorySection.setMinimumSize(new Dimension(leftWidth, JBUI.scale(320)));
        templateDirectorySection.setPreferredSize(new Dimension(leftWidth, JBUI.scale(760)));
        templateSection.setMinimumSize(new Dimension(JBUI.scale(480), JBUI.scale(260)));
        previewSection.setMinimumSize(new Dimension(JBUI.scale(320), JBUI.scale(320)));
        descriptionSection.setMinimumSize(new Dimension(JBUI.scale(320), JBUI.scale(260)));

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 0;
        rightGbc.fill = GridBagConstraints.BOTH;
        rightGbc.weightx = 1;
        rightGbc.insets = JBUI.insets(0);

        rightGbc.gridy = 0;
        rightGbc.weighty = 0.35;
        rightPanel.add(templateSection, rightGbc);

        rightGbc.gridy = 1;
        rightGbc.weighty = 0.42;
        rightGbc.insets = JBUI.insetsTop(14);
        rightPanel.add(previewSection, rightGbc);

        rightGbc.gridy = 2;
        rightGbc.weighty = 0.23;
        rightGbc.insets = JBUI.insetsTop(10);
        rightPanel.add(descriptionSection, rightGbc);

        JPanel workspacePanel = new JPanel(new GridBagLayout());
        workspacePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.insets = JBUI.insets(0);
        workspacePanel.add(templateDirectorySection, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = JBUI.insetsLeft(16);
        workspacePanel.add(rightPanel, gbc);

        workspacePanel.setPreferredSize(new Dimension(0, JBUI.scale(980)));
        return workspacePanel;
    }

    private JPanel createTemplateDirectorySection() {
        JComponent templateListPanel = ToolbarDecorator.createDecorator(templateList)
                .setAddAction(button -> addTemplate())
                .setRemoveAction(button -> removeSelectedTemplate())
                .setEditAction(button -> renameSelectedTemplate())
                .disableUpDownActions()
                .createPanel();

        JPanel sectionPanel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        sectionPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(PluginBundle.get("setting.template.list"));
        titleLabel.setBorder(JBUI.Borders.emptyBottom(4));
        sectionPanel.add(titleLabel, BorderLayout.NORTH);
        sectionPanel.add(templateListPanel, BorderLayout.CENTER);
        return sectionPanel;
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

        JPanel sectionPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        sectionPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.WEST);
        if (trailingOrTopContent instanceof JButton) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.add(trailingOrTopContent);
            titlePanel.add(buttonPanel, BorderLayout.EAST);
        }
        sectionPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel contentBox = new JPanel(new BorderLayout());
        contentBox.setOpaque(false);
        contentBox.setBorder(IdeBorderFactory.createBorder());
        contentBox.add(content, BorderLayout.CENTER);

        JComponent centerContent = contentBox;
        if (trailingOrTopContent != null && !(trailingOrTopContent instanceof JButton)) {
            JPanel contentPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
            contentPanel.setOpaque(false);
            contentPanel.add(trailingOrTopContent, BorderLayout.NORTH);
            contentPanel.add(contentBox, BorderLayout.CENTER);
            centerContent = contentPanel;
        }

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

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static class TemplateListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Object displayValue = value;
            if (value instanceof CommitTemplateProfile) {
                displayValue = ((CommitTemplateProfile) value).getName();
            }
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
        }
    }

    @Override
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(templateEditor);
    }
}
