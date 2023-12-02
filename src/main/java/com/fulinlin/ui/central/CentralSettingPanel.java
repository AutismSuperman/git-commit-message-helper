package com.fulinlin.ui.central;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.DataSettings;
import com.fulinlin.model.enums.TypeDisplayStyleEnum;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBIntSpinner;

import javax.swing.*;
import java.util.List;

public class CentralSettingPanel {
    protected GitCommitMessageHelperSettings settings;
    private JPanel mainPanel;
    private JPanel hiddenPanel;
    private JPanel typePanel;
    private JPanel skipCiPanel;
    private JRadioButton typeDropDownRadioButton;
    private JRadioButton typeSelectionRadioButton;
    private JRadioButton typeMixingRadioButton;
    private JBIntSpinner typeDisplayNumberSpinner;
    private JCheckBox skipCiEnableCheckBox;
    private JComboBox<String> skipCiComboBox;
    private JCheckBox skipCiDefaultCheckedCheckedBox;

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
    private JLabel skipEnableSelectionLabel;



    public CentralSettingPanel(GitCommitMessageHelperSettings settings) {
        //Get setting
        this.settings = settings.clone();
        // Init  description
        typePanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.type.panel.title"), true));
        skipCiPanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.skip.ci.panel.title"), true));
        hiddenPanel.setBorder(IdeBorderFactory.createTitledBorder(PluginBundle.get("setting.central.hidden.panel.title"), true));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(typeDropDownRadioButton);
        buttonGroup.add(typeSelectionRadioButton);
        buttonGroup.add(typeMixingRadioButton);
        typeDiskPlayStyleLabel.setText(PluginBundle.get("setting.central.type.style"));
        typeDisplayNumberLabel.setText(PluginBundle.get("setting.central.type.number"));
        typeDropDownRadioButton.setText(PluginBundle.get("setting.central.type.dropdown.button"));
        typeSelectionRadioButton.setText(PluginBundle.get("setting.central.type.selection.button"));
        typeMixingRadioButton.setText(PluginBundle.get("setting.central.type.mixing.button"));
        // Init  skip ci option
        skipCiDefaultValueLabel.setText(PluginBundle.get("setting.central.skip.ci.enable.default"));
        skipEnableSelectionLabel.setText(PluginBundle.get("setting.central.skip.ci.enable.selection"));
        skipCiEnableCheckBox.setText(PluginBundle.get("setting.central.skip.ci.enable.checkbox"));
        skipCiDefaultCheckedCheckedBox.setText(PluginBundle.get("setting.central.skip.ci.default.checked.checkbox"));
        DataSettings dateSettings = settings.getDateSettings();
        List<String> skipCis = dateSettings.getSkipCis();
        for (String skipCi : skipCis) {
            skipCiComboBox.addItem(skipCi);
        }
        // Init  Component

        initComponent(settings);
        // Init
        typeSelectionRadioButton.addChangeListener(e -> {
            typeDisplayNumberSpinner.setEnabled(false);
        });
        typeDropDownRadioButton.addChangeListener(e -> {
            typeDisplayNumberSpinner.setEnabled(true);
        });
        typeMixingRadioButton.addChangeListener(e -> {
            typeDisplayNumberSpinner.setEnabled(true);
        });

    }

    public GitCommitMessageHelperSettings getSettings() {
        // Type Display Style Option
        int number = typeDisplayNumberSpinner.getNumber();
        if (typeDropDownRadioButton.isSelected()) {
            settings.getCentralSettings().setTypeDisplayStyle(TypeDisplayStyleEnum.DROP_DOWN);
        } else if (typeSelectionRadioButton.isSelected()) {
            settings.getCentralSettings().setTypeDisplayStyle(TypeDisplayStyleEnum.SELECTION);
        } else if (typeMixingRadioButton.isSelected()) {
            settings.getCentralSettings().setTypeDisplayStyle(TypeDisplayStyleEnum.MIXING);
        }
        settings.getCentralSettings().setTypeDisplayNum(number);
        // Skip CI Option
        settings.getCentralSettings().setSkipCiDefaultChecked(skipCiDefaultCheckedCheckedBox.isSelected());
        if (skipCiComboBox.getSelectedItem() != null) {
            settings.getCentralSettings().setSkipCiDefaultValue(skipCiComboBox.getSelectedItem().toString());
        }
        settings.getCentralSettings().setSkipCiSelectionEnable(skipCiEnableCheckBox.isSelected());
        // Hidden Option
        settings.getCentralSettings().getHidden().setType(typeCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setScope(scopeCheckBox.isSelected());
        settings.getCentralSettings().getHidden().setSubject(subjectCheckBox.isSelected());
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
        if (settings.getCentralSettings().getTypeDisplayStyle().equals(TypeDisplayStyleEnum.DROP_DOWN)) {
            typeDropDownRadioButton.setSelected(true);
        } else if (settings.getCentralSettings().getTypeDisplayStyle().equals(TypeDisplayStyleEnum.SELECTION)) {
            typeSelectionRadioButton.setSelected(true);
        } else if (settings.getCentralSettings().getTypeDisplayStyle().equals(TypeDisplayStyleEnum.MIXING)) {
            typeMixingRadioButton.setSelected(true);
        } else {
            typeDropDownRadioButton.setSelected(true);
        }
        typeDisplayNumberSpinner.setNumber(settings.getCentralSettings().getTypeDisplayNum());
        // Skip CI Option
        skipCiDefaultCheckedCheckedBox.setSelected(settings.getCentralSettings().getSkipCiDefaultChecked());
        skipCiComboBox.setSelectedItem(settings.getCentralSettings().getSkipCiDefaultValue());
        skipCiEnableCheckBox.setSelected(settings.getCentralSettings().getSkipCiSelectionEnable());
        // Hidden Option
        typeCheckBox.setSelected(settings.getCentralSettings().getHidden().getType());
        scopeCheckBox.setSelected(settings.getCentralSettings().getHidden().getScope());
        subjectCheckBox.setSelected(settings.getCentralSettings().getHidden().getSubject());
        bodyCheckBox.setSelected(settings.getCentralSettings().getHidden().getBody());
        changesCheckBox.setSelected(settings.getCentralSettings().getHidden().getChanges());
        closedCheckBox.setSelected(settings.getCentralSettings().getHidden().getClosed());
        skipCiCheckBox.setSelected(settings.getCentralSettings().getHidden().getSkipCi());
    }


    public boolean isModified(GitCommitMessageHelperSettings data) {
        boolean isModified = false;
        // Type Display Style Option
        if (typeDropDownRadioButton.isSelected() != data.getCentralSettings().getTypeDisplayStyle()
                .equals(TypeDisplayStyleEnum.DROP_DOWN)) {
            isModified = true;
        } else if (typeSelectionRadioButton.isSelected() != data.getCentralSettings().getTypeDisplayStyle()
                .equals(TypeDisplayStyleEnum.SELECTION)) {
            isModified = true;
        } else if (typeMixingRadioButton.isSelected() != data.getCentralSettings().getTypeDisplayStyle()
                .equals(TypeDisplayStyleEnum.MIXING)) {
            isModified = true;
        } else if (typeDisplayNumberSpinner.getNumber() != data.getCentralSettings().getTypeDisplayNum()) {
            isModified = true;
        }
        // Skip CI Option
        else if (skipCiDefaultCheckedCheckedBox.isSelected() != data.getCentralSettings().getSkipCiDefaultChecked()) {
            isModified = true;
        } else if (skipCiComboBox.getSelectedItem() != null && !skipCiComboBox.getSelectedItem().toString()
                .equals(data.getCentralSettings().getSkipCiDefaultValue())) {
            isModified = true;
        } else if (skipCiEnableCheckBox.isSelected() != data.getCentralSettings().getSkipCiSelectionEnable()) {
            isModified = true;
        }
        // Hidden Option
        else if (typeCheckBox.isSelected() != data.getCentralSettings().getHidden().getType()) {
            isModified = true;
        } else if (scopeCheckBox.isSelected() != data.getCentralSettings().getHidden().getScope()) {
            isModified = true;
        } else if (subjectCheckBox.isSelected() != data.getCentralSettings().getHidden().getSubject()) {
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
}
