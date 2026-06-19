package com.fulinlin.ui.central;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.service.LlmClient;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LlmProfileEditor extends DialogWrapper {

    private final JPanel mainPanel;
    private final JTextField nameField;
    private final JTextField baseUrlField;
    private final JPasswordField apiKeyField;
    private final JComboBox<String> modelComboBox;
    private final JButton fetchModelsButton;
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
        modelComboBox = new JComboBox<>();
        modelComboBox.setEditable(true);
        modelComboBox.setSelectedItem(profile.getModel());
        fetchModelsButton = new JButton(PluginBundle.get("setting.llm.model.fetch"));
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
        profile.setModel(getModelText());
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
        row = addField(mainPanel, PluginBundle.get("setting.central.llm.model"), createModelPanel(), gbc, row);
        addCheckBox(reasoningCompatibilityCheckBox, gbc, row);
    }

    @NotNull
    private JComponent createModelPanel() {
        JPanel modelPanel = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        modelPanel.add(modelComboBox, BorderLayout.CENTER);
        modelPanel.add(fetchModelsButton, BorderLayout.EAST);
        return modelPanel;
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
        fetchModelsButton.addActionListener(e -> fetchModels());
    }

    private void fetchModels() {
        LlmProfile profile = createProfileFromFields();
        if (profile.getBaseUrl().trim().isEmpty()) {
            Messages.showWarningDialog(
                    mainPanel,
                    PluginBundle.get("setting.llm.model.fetch.not.configured"),
                    PluginBundle.get("setting.llm.model.fetch.title")
            );
            return;
        }

        String currentModel = getModelText();
        fetchModelsButton.setEnabled(false);
        fetchModelsButton.setText(PluginBundle.get("setting.llm.model.fetching"));
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<String> models = new LlmClient().listModels(profile);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!mainPanel.isDisplayable()) {
                        return;
                    }
                    resetFetchButton();
                    if (models.isEmpty()) {
                        Messages.showInfoMessage(
                                mainPanel,
                                PluginBundle.get("setting.llm.model.fetch.empty"),
                                PluginBundle.get("setting.llm.model.fetch.title")
                        );
                        return;
                    }
                    applyFetchedModels(models, currentModel);
                }, ModalityState.any());
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!mainPanel.isDisplayable()) {
                        return;
                    }
                    resetFetchButton();
                    String message = ex.getMessage() == null
                            ? PluginBundle.get("setting.llm.model.fetch.failed")
                            : ex.getMessage();
                    Messages.showErrorDialog(mainPanel, message, PluginBundle.get("setting.llm.model.fetch.title"));
                }, ModalityState.any());
            }
        });
    }

    @NotNull
    private LlmProfile createProfileFromFields() {
        LlmProfile profile = new LlmProfile();
        profile.setId(profileId);
        profile.setName(nameField.getText().trim());
        profile.setBaseUrl(baseUrlField.getText().trim());
        profile.setApiKey(new String(apiKeyField.getPassword()).trim());
        profile.setModel(getModelText());
        profile.setProvider((LlmProvider) providerComboBox.getSelectedItem());
        profile.setReasoningCompatibilityEnabled(reasoningCompatibilityCheckBox.isSelected());
        return profile;
    }

    @NotNull
    private String getModelText() {
        Object item = modelComboBox.isEditable()
                ? modelComboBox.getEditor().getItem()
                : modelComboBox.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    private void applyFetchedModels(@NotNull List<String> models, @NotNull String previousModel) {
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        for (String model : models) {
            comboBoxModel.addElement(model);
        }
        modelComboBox.setModel(comboBoxModel);
        modelComboBox.setEditable(true);
        if (!previousModel.isEmpty()) {
            modelComboBox.setSelectedItem(previousModel);
        } else {
            modelComboBox.setSelectedIndex(0);
        }
        modelComboBox.showPopup();
    }

    private void resetFetchButton() {
        fetchModelsButton.setEnabled(true);
        fetchModelsButton.setText(PluginBundle.get("setting.llm.model.fetch"));
    }
}
