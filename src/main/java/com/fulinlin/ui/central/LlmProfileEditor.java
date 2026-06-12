package com.fulinlin.ui.central;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class LlmProfileEditor extends DialogWrapper {

    private final JPanel mainPanel;
    private final JTextField nameField;
    private final JTextField baseUrlField;
    private final JPasswordField apiKeyField;
    private final JTextField modelField;
    private final JComboBox<LlmProvider> providerComboBox;
    private final JCheckBox reasoningCompatibilityCheckBox;
    private final String profileId;
    private LlmProvider previousProvider;

    public LlmProfileEditor(@NotNull String title, @NotNull LlmProfile profile) {
        super(true);
        setTitle(title);
        profileId = profile.getId();
        mainPanel = new JPanel(new GridBagLayout());
        nameField = new JTextField(profile.getName());
        baseUrlField = new JTextField(profile.getBaseUrl());
        apiKeyField = new JPasswordField(profile.getApiKey());
        modelField = new JTextField(profile.getModel());
        providerComboBox = new JComboBox<>(LlmProvider.values());
        reasoningCompatibilityCheckBox = new JCheckBox(PluginBundle.get("setting.llm.reasoning.compatibility"));
        reasoningCompatibilityCheckBox.setToolTipText(PluginBundle.get("setting.llm.reasoning.compatibility.tooltip"));
        providerComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Object displayValue = value instanceof LlmProvider ? ((LlmProvider) value).getDisplayName() : value;
                return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            }
        });
        previousProvider = LlmProvider.fromNullable(profile.getProvider());
        providerComboBox.setSelectedItem(previousProvider);
        reasoningCompatibilityCheckBox.setSelected(Boolean.TRUE.equals(profile.getReasoningCompatibilityEnabled()));
        buildPanel();
        bindListeners();
        init();
    }

    @NotNull
    public LlmProfile getProfile() {
        LlmProfile profile = new LlmProfile();
        profile.setId(profileId);
        profile.setName(nameField.getText().trim());
        profile.setBaseUrl(baseUrlField.getText().trim());
        profile.setApiKey(new String(apiKeyField.getPassword()).trim());
        profile.setModel(modelField.getText().trim());
        profile.setProvider((LlmProvider) providerComboBox.getSelectedItem());
        profile.setReasoningCompatibilityEnabled(reasoningCompatibilityCheckBox.isSelected());
        GitCommitMessageHelperSettings.checkDefaultLlmProfile(profile);
        return profile;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    private void buildPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        row = addField(mainPanel, PluginBundle.get("setting.llm.profile.name"), nameField, gbc, row);
        row = addField(mainPanel, PluginBundle.get("setting.llm.profile.provider"), providerComboBox, gbc, row);
        row = addField(mainPanel, PluginBundle.get("setting.central.llm.base.url"), baseUrlField, gbc, row);
        row = addField(mainPanel, PluginBundle.get("setting.central.llm.api.key"), apiKeyField, gbc, row);
        row = addField(mainPanel, PluginBundle.get("setting.central.llm.model"), modelField, gbc, row);
        addCheckBox(reasoningCompatibilityCheckBox, gbc, row);
    }

    private int addField(@NotNull JPanel panel, String labelText, JComponent component, GridBagConstraints gbc, int row) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        component.setPreferredSize(new Dimension(JBUI.scale(420), component.getPreferredSize().height));
        panel.add(component, gbc);
        return row + 1;
    }

    private void addCheckBox(@NotNull JCheckBox checkBox, GridBagConstraints gbc, int row) {
        JPanel checkBoxPanel = new JPanel(new BorderLayout());
        checkBoxPanel.add(checkBox, BorderLayout.WEST);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(checkBoxPanel, gbc);
    }

    private void bindListeners() {
        providerComboBox.addActionListener(e -> {
            LlmProvider currentProvider = (LlmProvider) providerComboBox.getSelectedItem();
            if (currentProvider == null) {
                return;
            }
            String currentBaseUrl = baseUrlField.getText().trim();
            if (currentBaseUrl.isEmpty() || currentBaseUrl.equals(previousProvider.getDefaultBaseUrl())) {
                baseUrlField.setText(currentProvider.getDefaultBaseUrl());
            }
            previousProvider = currentProvider;
        });
    }
}
