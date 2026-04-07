package com.fulinlin.ui.central;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.LlmProfile;
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
    private final String profileId;

    public LlmProfileEditor(@NotNull String title, @NotNull LlmProfile profile) {
        super(true);
        setTitle(title);
        profileId = profile.getId();
        mainPanel = new JPanel(new GridBagLayout());
        nameField = new JTextField(profile.getName());
        baseUrlField = new JTextField(profile.getBaseUrl());
        apiKeyField = new JPasswordField(profile.getApiKey());
        modelField = new JTextField(profile.getModel());
        buildPanel();
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
        row = addField(PluginBundle.get("setting.llm.profile.name"), nameField, gbc, row);
        row = addField(PluginBundle.get("setting.central.llm.base.url"), baseUrlField, gbc, row);
        row = addField(PluginBundle.get("setting.central.llm.api.key"), apiKeyField, gbc, row);
        addField(PluginBundle.get("setting.central.llm.model"), modelField, gbc, row);
    }

    private int addField(String labelText, JComponent component, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        mainPanel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        component.setPreferredSize(new Dimension(JBUI.scale(420), component.getPreferredSize().height));
        mainPanel.add(component, gbc);
        return row + 1;
    }
}
