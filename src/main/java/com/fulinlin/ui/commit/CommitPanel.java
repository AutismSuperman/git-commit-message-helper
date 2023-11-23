package com.fulinlin.ui.commit;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import java.util.List;


public class CommitPanel {
    private JPanel mainPanel;
    private JComboBox<TypeAlias> changeType;
    private JTextField changeScope;
    private JTextField shortDescription;
    private EditorTextField longDescription;
    private EditorTextField breakingChanges;
    private JTextField closedIssues;
    private JLabel typeDescriptionLabel;
    private JLabel scopeDescriptionLabel;
    private JLabel subjectDescriptionLabel;
    private JLabel bodyDescriptionLabel;
    private JLabel closedDescriptionLabel;
    private JLabel changeDescriptionLabel;
    private JScrollPane longDescriptionScrollPane;
    private JScrollPane breakingChangesScrollPane;


    public CommitPanel(Project project, GitCommitMessageHelperSettings settings, CommitTemplate commitMessageTemplate) {
        // Personalized UI configuration
        typeDescriptionLabel.setText(PluginBundle.get("commit.type.field"));
        scopeDescriptionLabel.setText(PluginBundle.get("commit.scope.field"));
        subjectDescriptionLabel.setText(PluginBundle.get("commit.subject.field"));
        bodyDescriptionLabel.setText(PluginBundle.get("commit.body.field"));
        closedDescriptionLabel.setText(PluginBundle.get("commit.closes.field"));
        changeDescriptionLabel.setText(PluginBundle.get("commit.changes.field"));
        longDescriptionScrollPane.setBorder(BorderFactory.createEmptyBorder());
        breakingChangesScrollPane.setBorder(BorderFactory.createEmptyBorder());
        longDescription.setBorder(new DarculaEditorTextFieldBorder());
        breakingChanges.setBorder(new DarculaEditorTextFieldBorder());
        longDescription.setOneLineMode(false);
        longDescription.ensureWillComputePreferredSize();
        longDescription.addSettingsProvider(uEditor -> {
            uEditor.setVerticalScrollbarVisible(true);
            uEditor.setHorizontalScrollbarVisible(true);
            uEditor.setBorder(null);
        });
        breakingChanges.setOneLineMode(false);
        breakingChanges.ensureWillComputePreferredSize();
        breakingChanges.addSettingsProvider(uEditor -> {
            uEditor.setVerticalScrollbarVisible(true);
            uEditor.setHorizontalScrollbarVisible(true);
            uEditor.setBorder(null);
        });
        //parameter
        List<TypeAlias> typeAliases = settings.getDateSettings().getTypeAliases();
        for (TypeAlias type : typeAliases) {
            changeType.addItem(type);
        }
        if (commitMessageTemplate != null) {
            // with cache init
            typeAliases.stream()
                    .filter(typeAlias -> typeAlias.getTitle().equals(commitMessageTemplate.getType()))
                    .findFirst()
                    .ifPresent(typeAlias ->
                            changeType.setSelectedItem(typeAlias)
                    );
            changeScope.setText(commitMessageTemplate.getScope());
            shortDescription.setText(commitMessageTemplate.getSubject());
            longDescription.setText(commitMessageTemplate.getBody());
            breakingChanges.setText(commitMessageTemplate.getChanges());
            closedIssues.setText(commitMessageTemplate.getCloses());
        }
         /* //Todo Command check commit message template
            File workingDirectory = VfsUtil.virtualToIoFile(project.getBaseDir());
            Command.Result result =
            new Command(workingDirectory, "git log --all --format=%s | grep -Eo '^[a-z]+(\\(.*\\)):.*$' | sed 's/^.*(\\(.*\\)):.*$/\\1/' | sort -n | uniq")
                  .execute();
            if (result.isSuccess()) {
                result.getOutput().forEach(changeScope::addItem);
            }
          */
    }

    JPanel getMainPanel() {
        return mainPanel;
    }


    CommitMessage getCommitMessage(GitCommitMessageHelperSettings settings) {
        return new CommitMessage(
                settings,
                (TypeAlias) changeType.getSelectedItem(),
                changeScope.getText().trim(),
                shortDescription.getText().trim(),
                longDescription.getText().trim(),
                closedIssues.getText().trim(),
                breakingChanges.getText().trim()
        );
    }

    CommitTemplate getCommitMessageTemplate() {
        CommitTemplate commitTemplate = new CommitTemplate();
        TypeAlias selectedItem = (TypeAlias) changeType.getSelectedItem();
        assert selectedItem != null;
        commitTemplate.setType(selectedItem.getTitle());
        commitTemplate.setScope(changeScope.getText().trim());
        commitTemplate.setSubject(shortDescription.getText().trim());
        commitTemplate.setBody(longDescription.getText().trim());
        commitTemplate.setChanges(breakingChanges.getText().trim());
        commitTemplate.setCloses(closedIssues.getText().trim());
        return commitTemplate;
    }


}
