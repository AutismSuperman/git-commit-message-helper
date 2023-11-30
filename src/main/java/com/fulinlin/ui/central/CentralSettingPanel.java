package com.fulinlin.ui.central;

import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;

public class CentralSettingPanel {
    private JPanel mainPanel;
    private JPanel hiddenPanel;
    private JPanel typeStylePanel;
    private JPanel skipCiPanel;
    private JCheckBox typeCheckBox;
    private JCheckBox scopeCheckBox;
    private JCheckBox subjectCheckBox;
    private JCheckBox bodyCheckBox;
    private JCheckBox changesCheckBox;
    private JCheckBox closedCheckBox;
    private JRadioButton dropDownRadioButton;
    private JRadioButton selectRadioButton;
    private JRadioButton mixingTheTwoRadioButton;
    private JCheckBox skipCiCheckBox;
    private JComboBox skipCiComboBox;
    private JCheckBox defaultCheckCheckBox;
    private JCheckBox selectCheckBox;


    public CentralSettingPanel() {
        hiddenPanel.setBorder(IdeBorderFactory.createTitledBorder("Hidden", true));
        typeStylePanel.setBorder(IdeBorderFactory.createTitledBorder("Setting Type Display Style", true));
        skipCiPanel.setBorder(IdeBorderFactory.createTitledBorder("Setting Skip CI", true));
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }


}
