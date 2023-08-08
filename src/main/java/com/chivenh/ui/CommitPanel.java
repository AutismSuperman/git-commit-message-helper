package com.chivenh.ui;

import com.chivenh.model.TypeAlias;
import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.util.List;


public class CommitPanel {
    private JPanel mainPanel;
    private JComboBox changeType;
    private JTextField changeScope;
    private JTextField shortDescription;
    private JTextArea longDescription;
    private JTextField closedIssues;
    private JTextArea breakingChanges;

    public CommitPanel(Project project, GitCommitMsgHelperSettings settings) {
        //parameter
        List<TypeAlias> typeAliases = settings.getDateSettings().getTypeAliases();
        for (TypeAlias type : typeAliases) {
            changeType.addItem(type);
        }
       /* fix fulin  File workingDirectory = VfsUtil.virtualToIoFile(project.getBaseDir());
        Command.Result result = new Command(workingDirectory, "git log --all --format=%s | grep -Eo '^[a-z]+(\\(.*\\)):.*$' | sed 's/^.*(\\(.*\\)):.*$/\\1/' | sort -n | uniq").execute();
        if (result.isSuccess()) {
            result.getOutput().forEach(changeScope::addItem);
        }*/
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    CommitMessage getCommitMessage(GitCommitMsgHelperSettings settings) {
        return new CommitMessage(
                settings,
                (TypeAlias) changeType.getSelectedItem(),
                (String) changeScope.getText().trim(),
                shortDescription.getText().trim(),
                longDescription.getText().trim(),
                closedIssues.getText().trim(),
                breakingChanges.getText().trim()
        );
    }

}
