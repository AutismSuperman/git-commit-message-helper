package com.chivenh.action;

import com.chivenh.storage.GitCommitMsgHelperSettings;
import com.chivenh.ui.CommitDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.vcs.commit.ChangesViewCommitPanel;
import org.jetbrains.annotations.Nullable;

/**
 * @author fulin
 */
public class CreateCommitAction extends AnAction implements DumbAware {
	private static final Logger LOG = Logger.getInstance(CreateCommitAction.class);
    private final GitCommitMsgHelperSettings settings;

    public CreateCommitAction() {
        this.settings = ServiceManager.getService(GitCommitMsgHelperSettings.class);
    }

	/**
	 * @author Chivenh
	 * @since 2023-08-20 18:28
	 * @param actionEvent -
	 */
    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        final CommitMessageI commitPanel = getCommitPanel(actionEvent);
        if (commitPanel == null) {
            return;
        }
        String currentMsg=null;
        Object context= actionEvent.getData(DataKey.create("contextComponent"));
        if(context instanceof CommitMessage){
        	currentMsg=((CommitMessage) context).getComment();
		}else if(context instanceof ChangesViewCommitPanel){
        	currentMsg=((ChangesViewCommitPanel) context).getCommitMessageUi().getText();
		} else{
			LOG.warn((context!=null? context.toString() : "NULL")+"\n------\ncan't get msg!");
		}
		CommitDialog dialog = new CommitDialog(currentMsg,actionEvent.getProject(),settings);
        dialog.show();
        if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            commitPanel.setCommitMessage(dialog.getCommitMessage(settings).toString());
        }
    }

    @Nullable
    private static CommitMessageI getCommitPanel(@Nullable AnActionEvent e) {
        if (e == null) {
            return null;
        }
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (data instanceof CommitMessageI) {
            return (CommitMessageI) data;
        }
        return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());
    }
}