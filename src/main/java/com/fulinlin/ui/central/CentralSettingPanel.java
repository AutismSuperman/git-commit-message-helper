package com.fulinlin.ui.central;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.DataSettings;
import com.fulinlin.model.enums.TypeDisplayStyleEnum;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBIntSpinner;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;

public class CentralSettingPanel {
    protected GitCommitMessageHelperSettings settings;
    private JPanel mainPanel;
    private JPanel hiddenPanel;
    private JPanel typePanel;
    private JPanel skipCiPanel;
    private JPanel llmPanel;
    private JPanel actionPanel;
    private JRadioButton typeComboboxRadioButton;
    private JRadioButton typeRadioRadioButton;
    private JRadioButton typeMixingRadioButton;
    private JBIntSpinner typeDisplayNumberSpinner;
    private JCheckBox skipCiEnableCheckBox;
    private JComboBox<String> skipCiComboBox;
    private JCheckBox skipCiDefaultApproveCheckedBox;

    //********************* hidden *********************//
    private JCheckBox typeCheckBox;
    private JCheckBox scopeCheckBox;
    private JCheckBox subjectCheckBox;
    private JCheckBox bodyCheckBox;
    private JCheckBox changesCheckBox;
    private JCheckBox closedCheckBox;
    private JCheckBox skipCiCheckBox;
    private JLabel typeDiskPlayStyleLabel;
    private JLabel typeDisplayNumberLabel;
    private JLabel skipCiDefaultValueLabel;
    private JLabel skipEnableComboboxLabel;
    private JTextField llmBaseUrlField;
    private JTextField llmModelField;
    private JTextField llmTemperatureField;
    private JTextField llmResponseLanguageField;
    private JPasswordField llmApiKeyField;
    private JLabel llmBaseUrlLabel;
    private JLabel llmApiKeyLabel;
    private JLabel llmModelLabel;
    private JLabel llmTemperatureLabel;
    private JLabel llmResponseLanguageLabel;
    private JCheckBox createCommitActionVisibleCheckBox;
    private JCheckBox generateCommitActionVisibleCheckBox;
    private JCheckBox formatCommitActionVisibleCheckBox;


    public CentralSettingPanel(GitCommitMessageHelperSettings settings) {
        //Get setting
        this.settings = settings.clone();
        // Init  description
        typePanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.type.panel.title"), true));
        skipCiPanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.skip.ci.panel.title"), true));
        hiddenPanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.hidden.panel.title"), true));
        llmPanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.llm.panel.title"), true));
        actionPanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.action.panel.title"), true));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(typeComboboxRadioButton);
        buttonGroup.add(typeRadioRadioButton);
        buttonGroup.add(typeMixingRadioButton);
        typeDiskPlayStyleLabel.setText(PluginBundle.get("setting.central.type.style"));
        typeDisplayNumberLabel.setText(PluginBundle.get("setting.central.type.number"));
        typeDisplayNumberSpinner.setToolTipText(PluginBundle.get("setting.central.type.number.tooltip"));
        typeComboboxRadioButton.setText(PluginBundle.get("setting.central.type.combobox.button"));
        typeRadioRadioButton.setText(PluginBundle.get("setting.central.type.radio.button"));
        typeMixingRadioButton.setText(PluginBundle.get("setting.central.type.mixing.button"));
        // Init  skip ci option
        skipCiDefaultValueLabel.setText(PluginBundle.get("setting.central.skip.ci.enable.default"));
        skipEnableComboboxLabel.setText(PluginBundle.get("setting.central.skip.ci.enable.selection"));
        skipCiEnableCheckBox.setText(PluginBundle.get("setting.central.skip.ci.enable.checkbox"));
        skipCiDefaultApproveCheckedBox.setText(PluginBundle.get("setting.central.skip.ci.default.checked.checkbox"));
        llmBaseUrlLabel.setText(PluginBundle.get("setting.central.llm.base.url"));
        llmApiKeyLabel.setText(PluginBundle.get("setting.central.llm.api.key"));
        llmModelLabel.setText(PluginBundle.get("setting.central.llm.model"));
        llmTemperatureLabel.setText(PluginBundle.get("setting.central.llm.temperature"));
        llmResponseLanguageLabel.setText(PluginBundle.get("setting.central.llm.response.language"));
        createCommitActionVisibleCheckBox.setText(PluginBundle.get("setting.central.action.create.visible"));
        generateCommitActionVisibleCheckBox.setText(PluginBundle.get("setting.central.action.generate.visible"));
        formatCommitActionVisibleCheckBox.setText(PluginBundle.get("setting.central.action.format.visible"));
        DataSettings dateSettings = settings.getDateSettings();
        List<String> skipCis = dateSettings.getSkipCis();
        for (String skipCi : skipCis) {
            skipCiComboBox.addItem(skipCi);
        }
        // Init
        typeComboboxRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                typeDisplayNumberSpinner.setEnabled(false);
            }
        });
        typeRadioRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                typeDisplayNumberSpinner.setEnabled(true);
            }
        });
        typeMixingRadioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                typeDisplayNumberSpinner.setEnabled(true);
            }
        });
        // Init  Component
        initComponent(settings);

    }

    public GitCommitMessageHelperSettings getSettings() {
        // Type Display Style Option
        int number = typeDisplayNumberSpinner.getNumber();
        if (typeComboboxRadioButton.isSelected()) {
            settings.getCentralSettings().setTypeDisplayStyle(TypeDisplayStyleEnum.CHECKBOX);
        } else if (typeRadioRadioButton.isSelected()) {
            settings.getCentralSettings().setTypeDisplayStyle(TypeDisplayStyleEnum.RADIO);
        } else if (typeMixingRadioButton.isSelected()) {
            settings.getCentralSettings().setTypeDisplayStyle(TypeDisplayStyleEnum.MIXING);
        }
        settings.getCentralSettings().setTypeDisplayNumber(number);
        // Skip CI Option
        settings.getCentralSettings().setSkipCiDefaultApprove(skipCiDefaultApproveCheckedBox.isSelected());
        if (skipCiComboBox.getSelectedItem() != null) {
            settings.getCentralSettings().setSkipCiDefaultValue(skipCiComboBox.getSelectedItem().toString());
        }
        settings.getCentralSettings().setSkipCiComboboxEnable(skipCiEnableCheckBox.isSelected());
        settings.getCentralSettings().getLlmSettings().setBaseUrl(llmBaseUrlField.getText().trim());
        settings.getCentralSettings().getLlmSettings().setApiKey(new String(llmApiKeyField.getPassword()).trim());
        settings.getCentralSettings().getLlmSettings().setModel(llmModelField.getText().trim());
        settings.getCentralSettings().getLlmSettings().setTemperature(parseTemperature(llmTemperatureField.getText()));
        settings.getCentralSettings().getLlmSettings().setResponseLanguage(llmResponseLanguageField.getText().trim());
        settings.getCentralSettings().getActionSettings().setCreateCommitActionVisible(createCommitActionVisibleCheckBox.isSelected());
        settings.getCentralSettings().getActionSettings().setGenerateCommitActionVisible(generateCommitActionVisibleCheckBox.isSelected());
        settings.getCentralSettings().getActionSettings().setFormatCommitActionVisible(formatCommitActionVisibleCheckBox.isSelected());
        // Hidden Option
        // settings.getCentralSettings().getHidden().setSubject(subjectCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setType(typeCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setScope(scopeCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setBody(bodyCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setChanges(changesCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setClosed(closedCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setSkipCi(skipCiCheckBox.isSelected());
        return settings;
    }


    public void reset(GitCommitMessageHelperSettings settings) {
        this.settings = settings.clone();
        initComponent(settings);
    }

    private void initComponent(GitCommitMessageHelperSettings settings) {
        // Type Display Style Option
        if (settings.getCentralSettings().getTypeDisplayStyle().equals(TypeDisplayStyleEnum.CHECKBOX)) {
            typeComboboxRadioButton.setSelected(true);
        } else if (settings.getCentralSettings().getTypeDisplayStyle().equals(TypeDisplayStyleEnum.RADIO)) {
            typeRadioRadioButton.setSelected(true);
        } else if (settings.getCentralSettings().getTypeDisplayStyle().equals(TypeDisplayStyleEnum.MIXING)) {
            typeMixingRadioButton.setSelected(true);
        } else {
            typeComboboxRadioButton.setSelected(true);
        }
        typeDisplayNumberSpinner.setNumber(settings.getCentralSettings().getTypeDisplayNumber());
        // Skip CI Option
        skipCiDefaultApproveCheckedBox.setSelected(settings.getCentralSettings().getSkipCiDefaultApprove());
        skipCiComboBox.setSelectedItem(settings.getCentralSettings().getSkipCiDefaultValue());
        skipCiEnableCheckBox.setSelected(settings.getCentralSettings().getSkipCiComboboxEnable());
        llmBaseUrlField.setText(settings.getCentralSettings().getLlmSettings().getBaseUrl());
        llmApiKeyField.setText(settings.getCentralSettings().getLlmSettings().getApiKey());
        llmModelField.setText(settings.getCentralSettings().getLlmSettings().getModel());
        llmTemperatureField.setText(String.valueOf(settings.getCentralSettings().getLlmSettings().getTemperature()));
        llmResponseLanguageField.setText(settings.getCentralSettings().getLlmSettings().getResponseLanguage());
        createCommitActionVisibleCheckBox.setSelected(settings.getCentralSettings().getActionSettings().getCreateCommitActionVisible());
        generateCommitActionVisibleCheckBox.setSelected(settings.getCentralSettings().getActionSettings().getGenerateCommitActionVisible());
        formatCommitActionVisibleCheckBox.setSelected(settings.getCentralSettings().getActionSettings().getFormatCommitActionVisible());
        // Hidden Option
        typeCheckBox.setSelected(settings.getCentralSettings().getHidden().getType());
        scopeCheckBox.setSelected(settings.getCentralSettings().getHidden().getScope());
        //subjectCheckBox.setSelected(settings.getCentralSettings().getHidden().getSubject());
        subjectCheckBox.setEnabled(false);
        bodyCheckBox.setSelected(settings.getCentralSettings().getHidden().getBody());
        changesCheckBox.setSelected(settings.getCentralSettings().getHidden().getChanges());
        closedCheckBox.setSelected(settings.getCentralSettings().getHidden().getClosed());
        skipCiCheckBox.setSelected(settings.getCentralSettings().getHidden().getSkipCi());
    }


    public boolean isModified(GitCommitMessageHelperSettings data) {
        boolean isModified = false;
        // Type Display Style Option
        if (typeComboboxRadioButton.isSelected() != data.getCentralSettings().getTypeDisplayStyle()
                .equals(TypeDisplayStyleEnum.CHECKBOX)) {
            isModified = true;
        } else if (typeRadioRadioButton.isSelected() != data.getCentralSettings().getTypeDisplayStyle()
                .equals(TypeDisplayStyleEnum.RADIO)) {
            isModified = true;
        } else if (typeMixingRadioButton.isSelected() != data.getCentralSettings().getTypeDisplayStyle()
                .equals(TypeDisplayStyleEnum.MIXING)) {
            isModified = true;
        } else if (typeDisplayNumberSpinner.getNumber() != data.getCentralSettings().getTypeDisplayNumber()) {
            isModified = true;
        }
        // Skip CI Option
        else if (skipCiDefaultApproveCheckedBox.isSelected() != data.getCentralSettings().getSkipCiDefaultApprove()) {
            isModified = true;
        } else if (skipCiComboBox.getSelectedItem() != null && !skipCiComboBox.getSelectedItem().toString()
                .equals(data.getCentralSettings().getSkipCiDefaultValue())) {
            isModified = true;
        } else if (skipCiEnableCheckBox.isSelected() != data.getCentralSettings().getSkipCiComboboxEnable()) {
            isModified = true;
        }
        // LLM and action option
        else if (!Objects.equals(llmBaseUrlField.getText().trim(), data.getCentralSettings().getLlmSettings().getBaseUrl())) {
            isModified = true;
        } else if (!Objects.equals(new String(llmApiKeyField.getPassword()).trim(), data.getCentralSettings().getLlmSettings().getApiKey())) {
            isModified = true;
        } else if (!Objects.equals(llmModelField.getText().trim(), data.getCentralSettings().getLlmSettings().getModel())) {
            isModified = true;
        } else if (!Objects.equals(parseTemperature(llmTemperatureField.getText()), data.getCentralSettings().getLlmSettings().getTemperature())) {
            isModified = true;
        } else if (!Objects.equals(llmResponseLanguageField.getText().trim(), data.getCentralSettings().getLlmSettings().getResponseLanguage())) {
            isModified = true;
        } else if (createCommitActionVisibleCheckBox.isSelected() != data.getCentralSettings().getActionSettings().getCreateCommitActionVisible()) {
            isModified = true;
        } else if (generateCommitActionVisibleCheckBox.isSelected() != data.getCentralSettings().getActionSettings().getGenerateCommitActionVisible()) {
            isModified = true;
        } else if (formatCommitActionVisibleCheckBox.isSelected() != data.getCentralSettings().getActionSettings().getFormatCommitActionVisible()) {
            isModified = true;
        }
        // Hidden Option
        else if (typeCheckBox.isSelected() != data.getCentralSettings().getHidden().getType()) {
            isModified = true;
        } else if (scopeCheckBox.isSelected() != data.getCentralSettings().getHidden().getScope()) {
            isModified = true;
        } else if (bodyCheckBox.isSelected() != data.getCentralSettings().getHidden().getBody()) {
            isModified = true;
        } else if (changesCheckBox.isSelected() != data.getCentralSettings().getHidden().getChanges()) {
            isModified = true;
        } else if (closedCheckBox.isSelected() != data.getCentralSettings().getHidden().getClosed()) {
            isModified = true;
        } else if (skipCiCheckBox.isSelected() != data.getCentralSettings().getHidden().getSkipCi()) {
            isModified = true;
        }
        return isModified;
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }


    private void createUIComponents() {
        typeDisplayNumberSpinner = new JBIntSpinner(1, -1, 999);
    }

    private static Double parseTemperature(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return 0.2D;
        }
    }
}
