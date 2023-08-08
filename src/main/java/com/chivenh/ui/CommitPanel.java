package com.chivenh.ui;

import com.chivenh.model.TypeAlias;
import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.util.List;


public class CommitPanel {
    private JPanel mainPanel;
    private JComboBox<TypeAlias> msgChangeType;
    private JTextField msgChangeScope;
    private JTextField msgShortDescription;
    private JTextArea msgLongDescription;
    private JTextField msgClosedIssues;
    private JTextArea msgBreakingChanges;

    public CommitPanel(Project project, GitCommitMsgHelperSettings settings) {
        //parameter
        List<TypeAlias> typeAliases = settings.getDateSettings().getTypeAliases();
        for (TypeAlias type : typeAliases) {
            msgChangeType.addItem(type);
        }
       /* fix fulin  File workingDirectory = VfsUtil.virtualToIoFile(project.getBaseDir());
        Command.Result result = new Command(workingDirectory, "git log --all --format=%s | grep -Eo '^[a-z]+(\\(.*\\)):.*$' | sed 's/^.*(\\(.*\\)):.*$/\\1/' | sort -n | uniq").execute();
        if (result.isSuccess()) {
            result.getOutput().forEach(msgChangeScope::addItem);
        }*/
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    CommitMessage getCommitMessage(GitCommitMsgHelperSettings settings) {
        return new CommitMessage(
                settings,
                (TypeAlias) msgChangeType.getSelectedItem(),
                msgChangeScope.getText().trim(),
                msgShortDescription.getText().trim(),
                msgLongDescription.getText().trim(),
                msgClosedIssues.getText().trim(),
                msgBreakingChanges.getText().trim()
        );
    }

}
