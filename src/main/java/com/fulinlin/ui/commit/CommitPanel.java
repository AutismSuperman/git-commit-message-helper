package com.fulinlin.ui.commit;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.TypeAlias;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.utils.I18nUtil;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.util.List;


public class CommitPanel {
    private JPanel mainPanel;
    private JComboBox<TypeAlias> changeType;
    private JTextField changeScope;
    private JTextField shortDescription;
    private JTextArea longDescription;
    private JTextField closedIssues;
    private JTextArea breakingChanges;
    private JLabel typeDescriptionLabel;
    private JLabel scopeDescriptionLabel;
    private JLabel subjectDescriptionLabel;
    private JLabel bodyDescriptionLabel;
    private JLabel closedDescriptionLabel;
    private JLabel changeDescriptionLabel;
    private JScrollPane longDescriptionScrollPane;
    private JScrollPane breakingChangesScrollPane;

    public CommitPanel(Project project, GitCommitMessageHelperSettings settings, CommitTemplate commitMessageTemplate) {
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
        typeDescriptionLabel.setText(I18nUtil.getInfo("commit.type.field"));
        scopeDescriptionLabel.setText(I18nUtil.getInfo("commit.scope.field"));
        subjectDescriptionLabel.setText(I18nUtil.getInfo("commit.subject.field"));
        bodyDescriptionLabel.setText(I18nUtil.getInfo("commit.body.field"));
        closedDescriptionLabel.setText(I18nUtil.getInfo("commit.closes.field"));
        changeDescriptionLabel.setText(I18nUtil.getInfo("commit.changes.field"));
        longDescriptionScrollPane.setBorder(BorderFactory.createEmptyBorder());
        breakingChangesScrollPane.setBorder(BorderFactory.createEmptyBorder());
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
