package com.fulinlin;

import com.fulinlin.setting.ui.TemplateEditPane;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * My Component
 * User: cdai
 * Date: 13-11-4
 * Time: 上午10:08
 */
public class MyComponent implements Configurable {

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitCommitMessageHelper";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return new TemplateEditPane().getTemplateEdit();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }
}