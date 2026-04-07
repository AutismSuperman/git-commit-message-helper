package com.fulinlin.ui.central;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.service.LlmClient;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class LlmSettingPanel {

    private final JPanel mainPanel;
    private final JComboBox<LlmProfile> activeProfileComboBox;
    private final JSpinner temperatureSpinner;
    private final JTextField responseLanguageField;
    private final JCheckBox smartEchoEnabledCheckBox;
    private final JCheckBox streamingResponseEnabledCheckBox;
    private final JButton testButton;
    private final JLabel testStatusLabel;
    private final LlmProfileTable profileTable;

    private GitCommitMessageHelperSettings settings;
    private boolean loading;

    public LlmSettingPanel(@NotNull GitCommitMessageHelperSettings settings) {
        this.settings = settings.clone();
        GitCommitMessageHelperSettings.checkDefaultLlmSettings(this.settings.getCentralSettings().getLlmSettings());

        mainPanel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
        mainPanel.setBorder(JBUI.Borders.empty(12));
        activeProfileComboBox = new JComboBox<>();
        activeProfileComboBox.setRenderer(new ProfileListCellRenderer());
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.5D, 0.0D, 2.0D, 0.1D));
        responseLanguageField = new JTextField();
        smartEchoEnabledCheckBox = new JCheckBox(PluginBundle.get("setting.central.llm.smart.echo"));
        streamingResponseEnabledCheckBox = new JCheckBox(PluginBundle.get("setting.llm.streaming.response"));
        testButton = new JButton(PluginBundle.get("setting.llm.test.connection"));
        testStatusLabel = new JLabel();
        profileTable = new LlmProfileTable();

        mainPanel.add(createActiveProfilePanel(), BorderLayout.NORTH);
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        bindListeners();
        reloadProfiles();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public GitCommitMessageHelperSettings getSettings() {
        commitGlobalSettings();
        GitCommitMessageHelperSettings.checkDefaultLlmSettings(settings.getCentralSettings().getLlmSettings());
        return settings;
    }

    public void reset(@NotNull GitCommitMessageHelperSettings settings) {
        this.settings = settings.clone();
        GitCommitMessageHelperSettings.checkDefaultLlmSettings(this.settings.getCentralSettings().getLlmSettings());
        reloadProfiles();
    }

    public boolean isModified(@NotNull GitCommitMessageHelperSettings data) {
        commitGlobalSettings();
        GitCommitMessageHelperSettings.checkDefaultLlmSettings(settings.getCentralSettings().getLlmSettings());
        return !Objects.equals(
                settings.getCentralSettings().getLlmSettings(),
                data.getCentralSettings().getLlmSettings()
        );
    }

    private JComponent createActiveProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel(PluginBundle.get("setting.llm.active.model")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(activeProfileComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(PluginBundle.get("setting.central.llm.temperature")), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(temperatureSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(PluginBundle.get("setting.central.llm.response.language")), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(responseLanguageField, gbc);

        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(12), 0));
        checkboxPanel.add(smartEchoEnabledCheckBox);
        checkboxPanel.add(streamingResponseEnabledCheckBox);
        checkboxPanel.add(testButton);
        checkboxPanel.add(testStatusLabel);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(checkboxPanel, gbc);
        return panel;
    }

    private JComponent createTablePanel() {
        JPanel panel = ToolbarDecorator.createDecorator(profileTable)
                .setAddAction(button -> addProfile())
                .setRemoveAction(button -> removeSelectedProfile())
                .setEditAction(button -> editSelectedProfile())
                .disableUpDownActions()
                .addExtraActions(new AnActionButton(PluginBundle.get("setting.llm.profile.duplicate"), AllIcons.Actions.Copy) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        duplicateSelectedProfile();
                    }
                })
                .createPanel();
        return new JBScrollPane(panel);
    }

    private void bindListeners() {
        activeProfileComboBox.addActionListener(e -> {
            if (loading) {
                return;
            }
            commitGlobalSettings();
            LlmProfile selectedProfile = (LlmProfile) activeProfileComboBox.getSelectedItem();
            if (selectedProfile != null) {
                settings.getCentralSettings().getLlmSettings().setActiveProfileId(selectedProfile.getId());
                profileTable.selectProfile(selectedProfile);
            }
        });
        testButton.addActionListener(e -> testActiveProfile());
    }

    private void reloadProfiles() {
        loading = true;
        LlmSettings llmSettings = settings.getCentralSettings().getLlmSettings();
        temperatureSpinner.setValue(llmSettings.getTemperature());
        responseLanguageField.setText(llmSettings.getResponseLanguage());
        smartEchoEnabledCheckBox.setSelected(Boolean.TRUE.equals(llmSettings.getSmartEchoEnabled()));
        streamingResponseEnabledCheckBox.setSelected(Boolean.TRUE.equals(llmSettings.getStreamingResponseEnabled()));
        profileTable.reset(llmSettings.getProfiles());
        activeProfileComboBox.removeAllItems();
        for (LlmProfile profile : llmSettings.getProfiles()) {
            activeProfileComboBox.addItem(profile);
        }
        LlmProfile activeProfile = llmSettings.getActiveProfile();
        if (activeProfile != null) {
            activeProfileComboBox.setSelectedItem(activeProfile);
            profileTable.selectProfile(activeProfile);
        }
        loading = false;
    }

    private void commitGlobalSettings() {
        LlmSettings llmSettings = settings.getCentralSettings().getLlmSettings();
        LlmProfile selectedProfile = (LlmProfile) activeProfileComboBox.getSelectedItem();
        if (selectedProfile != null) {
            llmSettings.setActiveProfileId(selectedProfile.getId());
        }
        llmSettings.setTemperature(((Number) temperatureSpinner.getValue()).doubleValue());
        llmSettings.setResponseLanguage(responseLanguageField.getText().trim());
        llmSettings.setSmartEchoEnabled(smartEchoEnabledCheckBox.isSelected());
        llmSettings.setStreamingResponseEnabled(streamingResponseEnabledCheckBox.isSelected());
    }

    private void testActiveProfile() {
        commitGlobalSettings();
        LlmProfile selectedProfile = (LlmProfile) activeProfileComboBox.getSelectedItem();
        LlmSettings llmSettings = settings.getCentralSettings().getLlmSettings();
        if (selectedProfile == null
                || !notBlank(selectedProfile.getBaseUrl())
                || !notBlank(selectedProfile.getApiKey())
                || !notBlank(selectedProfile.getModel())) {
            notifyTestResult(
                    PluginBundle.get("setting.llm.test.not.configured"),
                    NotificationType.WARNING
            );
            return;
        }

        testButton.setEnabled(false);
        testStatusLabel.setText(PluginBundle.get("setting.llm.test.testing"));
        testStatusLabel.setForeground(UIUtil.getContextHelpForeground());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                new LlmClient().chat(
                        selectedProfile,
                        llmSettings,
                        "You are a connectivity test assistant.",
                        "Reply with OK only."
                );
                ApplicationManager.getApplication().invokeLater(() -> {
                    testButton.setEnabled(true);
                    testStatusLabel.setText(PluginBundle.get("setting.llm.test.success"));
                    testStatusLabel.setForeground(JBColor.GREEN);
                    notifyTestResult(
                            PluginBundle.get("setting.llm.test.success"),
                            NotificationType.INFORMATION
                    );
                }, ModalityState.any());
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    testButton.setEnabled(true);
                    testStatusLabel.setText(PluginBundle.get("setting.llm.test.failed"));
                    testStatusLabel.setForeground(JBColor.RED);
                    notifyTestResult(
                            ex.getMessage(),
                            NotificationType.ERROR
                    );
                }, ModalityState.any());
            }
        });
    }

    private void notifyTestResult(String content, NotificationType type) {
        Notifications.Bus.notify(new Notification(
                "Git Commit Message Helper",
                PluginBundle.get("setting.llm.test.title"),
                content == null ? "" : content,
                type
        ));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void addProfile() {
        commitGlobalSettings();
        LlmProfile profile = GitCommitMessageHelperSettings.createDefaultLlmProfile();
        profile.setId(createProfileId());
        profile.setName(createProfileName("New Model"));
        LlmProfileEditor editor = new LlmProfileEditor(PluginBundle.get("setting.llm.profile.add.title"), profile);
        if (editor.showAndGet()) {
            profileTable.addProfile(editor.getProfile());
            syncAfterProfileListChanged(editor.getProfile());
        }
    }

    private void editSelectedProfile() {
        commitGlobalSettings();
        LlmProfile selectedProfile = profileTable.getSelectedProfile();
        if (selectedProfile == null) {
            return;
        }
        LlmProfileEditor editor = new LlmProfileEditor(PluginBundle.get("setting.llm.profile.edit.title"), selectedProfile);
        if (editor.showAndGet()) {
            profileTable.updateSelectedProfile(editor.getProfile());
            syncAfterProfileListChanged(editor.getProfile());
        }
    }

    private void duplicateSelectedProfile() {
        commitGlobalSettings();
        LlmProfile selectedProfile = profileTable.getSelectedProfile();
        if (selectedProfile == null) {
            return;
        }
        LlmProfile profile = copyProfile(selectedProfile);
        profile.setId(createProfileId());
        profile.setName(createProfileName(selectedProfile.getName() + " Copy"));
        profileTable.addProfile(profile);
        syncAfterProfileListChanged(profile);
    }

    private void removeSelectedProfile() {
        commitGlobalSettings();
        LlmProfile selectedProfile = profileTable.getSelectedProfile();
        if (selectedProfile == null || profileTable.getProfileCount() <= 1) {
            return;
        }
        profileTable.removeSelectedProfile();
        LlmProfile nextProfile = profileTable.getSelectedProfile();
        syncAfterProfileListChanged(nextProfile);
    }

    private void syncAfterProfileListChanged(LlmProfile activeProfile) {
        LlmSettings llmSettings = settings.getCentralSettings().getLlmSettings();
        llmSettings.setProfiles(profileTable.getProfiles());
        if (activeProfile != null) {
            llmSettings.setActiveProfileId(activeProfile.getId());
        }
        reloadProfiles();
    }

    private LlmProfile copyProfile(@NotNull LlmProfile source) {
        LlmProfile profile = new LlmProfile();
        profile.setName(source.getName());
        profile.setBaseUrl(source.getBaseUrl());
        profile.setApiKey(source.getApiKey());
        profile.setModel(source.getModel());
        return profile;
    }

    private String createProfileId() {
        return "profile-" + System.currentTimeMillis() + "-" + profileTable.getProfileCount();
    }

    private String createProfileName(String baseName) {
        String name = baseName;
        int counter = 2;
        while (containsProfileName(name)) {
            name = baseName + " " + counter++;
        }
        return name;
    }

    private boolean containsProfileName(String name) {
        for (LlmProfile profile : profileTable.getProfiles()) {
            if (name.equals(profile.getName())) {
                return true;
            }
        }
        return false;
    }

    private static class ProfileListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Object displayValue = value;
            if (value instanceof LlmProfile) {
                displayValue = ((LlmProfile) value).getName();
            }
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
        }
    }

    private static class LlmProfileTable extends JBTable {
        private static final int NAME_COLUMN = 0;
        private static final int BASE_URL_COLUMN = 1;
        private static final int MODEL_COLUMN = 2;

        private final LlmProfileTableModel tableModel = new LlmProfileTableModel();

        LlmProfileTable() {
            setModel(tableModel);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setColumnWidth(NAME_COLUMN, 160);
            setColumnWidth(BASE_URL_COLUMN, 260);
            setColumnWidth(MODEL_COLUMN, 180);
        }

        void reset(List<LlmProfile> profiles) {
            tableModel.reset(profiles);
            if (getRowCount() > 0) {
                setRowSelectionInterval(0, 0);
            }
        }

        void addProfile(LlmProfile profile) {
            tableModel.addProfile(profile);
            int index = getRowCount() - 1;
            setRowSelectionInterval(index, index);
        }

        void updateSelectedProfile(LlmProfile profile) {
            int row = getSelectedRow();
            if (row < 0) {
                return;
            }
            tableModel.updateProfile(row, profile);
            setRowSelectionInterval(row, row);
        }

        void removeSelectedProfile() {
            int row = getSelectedRow();
            if (row < 0 || getRowCount() <= 1) {
                return;
            }
            tableModel.removeProfile(row);
            int nextRow = Math.min(row, getRowCount() - 1);
            if (nextRow >= 0) {
                setRowSelectionInterval(nextRow, nextRow);
            }
        }

        LlmProfile getSelectedProfile() {
            int row = getSelectedRow();
            return row >= 0 ? tableModel.getProfile(row) : null;
        }

        int getProfileCount() {
            return tableModel.getRowCount();
        }

        List<LlmProfile> getProfiles() {
            return tableModel.getProfiles();
        }

        void selectProfile(LlmProfile profile) {
            if (profile == null) {
                return;
            }
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (profile.getId().equals(tableModel.getProfile(i).getId())) {
                    setRowSelectionInterval(i, i);
                    return;
                }
            }
        }

        private void setColumnWidth(int columnIndex, int preferredWidth) {
            TableColumn column = getColumnModel().getColumn(columnIndex);
            column.setPreferredWidth(preferredWidth);
        }
    }

    private static class LlmProfileTableModel extends AbstractTableModel {
        private final List<LlmProfile> profiles = new LinkedList<>();

        void reset(List<LlmProfile> sourceProfiles) {
            profiles.clear();
            profiles.addAll(sourceProfiles);
            fireTableDataChanged();
        }

        void addProfile(LlmProfile profile) {
            profiles.add(profile);
            fireTableDataChanged();
        }

        void updateProfile(int row, LlmProfile profile) {
            profiles.set(row, profile);
            fireTableRowsUpdated(row, row);
        }

        void removeProfile(int row) {
            profiles.remove(row);
            fireTableDataChanged();
        }

        LlmProfile getProfile(int row) {
            return profiles.get(row);
        }

        List<LlmProfile> getProfiles() {
            return new LinkedList<>(profiles);
        }

        @Override
        public int getRowCount() {
            return profiles.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case LlmProfileTable.NAME_COLUMN:
                    return PluginBundle.get("setting.llm.profile.name.column");
                case LlmProfileTable.BASE_URL_COLUMN:
                    return PluginBundle.get("setting.central.llm.base.url");
                case LlmProfileTable.MODEL_COLUMN:
                    return PluginBundle.get("setting.central.llm.model");
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LlmProfile profile = profiles.get(rowIndex);
            switch (columnIndex) {
                case LlmProfileTable.NAME_COLUMN:
                    return profile.getName();
                case LlmProfileTable.BASE_URL_COLUMN:
                    return profile.getBaseUrl();
                case LlmProfileTable.MODEL_COLUMN:
                    return profile.getModel();
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
