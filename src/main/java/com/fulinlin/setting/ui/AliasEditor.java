package com.fulinlin.setting.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AliasEditor extends DialogWrapper {
    private JPanel myPanel;
    private JTextField titleField;
    private JTextField descriptionField;


    public interface Validator {
        boolean isOK(String name, String value);
    }

    public AliasEditor(String title, String macroName, String value) {
        super(true);
        setTitle(title);
        titleField.setText(macroName);
        descriptionField.setText(value);
        init();
    }

    public String getTitle() {
        return titleField.getText().trim();
    }

    public String getDescription() {
        return descriptionField.getText().trim();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

}