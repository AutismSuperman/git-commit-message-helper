package com.fulinlin.setting.ui;

import javax.swing.*;

public class AliasEditor {
    private JTextField fromField;
    private JPanel myPanel;
    private final Validator myValidator;

    public interface Validator {
        boolean isOK(String name, String value);
    }

    public AliasEditor(String title, String macroName, String value, Validator validator) {
        myValidator = validator;
        fromField.setText(macroName);
    }


}