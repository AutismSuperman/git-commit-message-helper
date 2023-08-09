package com.chivenh.ui;

import com.chivenh.model.CommitTemplate;
import com.chivenh.model.TypeAlias;
import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import org.apache.commons.lang.StringUtils;

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
	private JTextArea msgDeprecated;

	public CommitPanel(String currentMsg,Project project, GitCommitMsgHelperSettings settings) {
		String selectedType=null;
		if(StringUtils.isNotBlank(currentMsg)){
			CommitTemplate currentTemplate = CommitMessage.defaultParse(currentMsg);
			selectedType=currentTemplate.getType();
			msgChangeScope.setText(currentTemplate.getScope());
			msgShortDescription.setText(currentTemplate.getSubject());
			msgLongDescription.setText(currentTemplate.getBody());
			if(StringUtils.isNotBlank(currentTemplate.getChanges())){
				msgBreakingChanges.setText(currentTemplate.getChanges());
			}
			if(StringUtils.isNotBlank(currentTemplate.getDeprecated())){
				msgDeprecated.setText(currentTemplate.getDeprecated());
			}
			msgClosedIssues.setText(currentTemplate.getCloses());
		}
        //parameter
        List<TypeAlias> typeAliases = settings.getDateSettings().getTypeAliases();
        for (TypeAlias type : typeAliases) {
            msgChangeType.addItem(type);
            if(type.getTitle().equals(selectedType)){
            	msgChangeType.setSelectedItem(type);
			}
        }
       /* fix fulin  File workingDirectory = VfsUtil.virtualToIoFile(project.getBaseDir());
        Command.Result result = new Command(workingDirectory, "git log --all --format=%s | grep -Eo '^[a-z]+(\\(.*\\)):.*$' | sed 's/^.*(\\(.*\\)):.*$/\\1/' | sort -n | uniq").execute();
        if (result.isSuccess()) {
            result.getOutput().forEach(msgChangeScope::addItem);
        }*/
    }

	/**
	 * 类型，影响范围，标题必填
	 * @return -
	 */
    ValidationInfo doValidate(){
		String scope = msgChangeScope.getText().trim();
		if(StringUtils.isBlank(scope)){
			return new ValidationInfo(msgChangeScope.getName()+ "不能为空!",msgChangeScope);
		}
		String subject = msgShortDescription.getText().trim();
		if(StringUtils.isBlank(subject)){
			return new ValidationInfo(msgShortDescription.getName()+ "不能为空!",msgShortDescription);
		}
		return null;
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
                msgBreakingChanges.getText().trim(),
                msgDeprecated.getText().trim()
        );
    }

}
