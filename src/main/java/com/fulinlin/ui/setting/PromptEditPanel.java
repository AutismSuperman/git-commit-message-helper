package com.fulinlin.ui.setting;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.DataSettings;
import com.fulinlin.model.PromptProfile;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.storage.GitCommitMessageStorage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class PromptEditPanel {
    private final JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
    private final DefaultListModel<PromptProfile> listModel = new DefaultListModel<>();
    private final JBList<PromptProfile> list = new JBList<>(listModel);
    private final JTextArea editor = new JTextArea(12, 60);
    private final JComboBox<PromptProfile> globalCombo = new JComboBox<>();
    private final JComboBox<PromptProfile> projectCombo = new JComboBox<>();
    private final GitCommitMessageHelperSettings settings;
    private final Project project;
    private String originalProjectPromptId;
    private PromptProfile displayed;
    private boolean loading;

    public PromptEditPanel(@NotNull GitCommitMessageHelperSettings source) {
        settings = source.clone();
        project = findProject();
        originalProjectPromptId = projectPromptId();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && !loading) { commit(); load(); } });
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        JPanel defaults = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), JBUI.scale(4)));
        defaults.add(new JLabel(PluginBundle.get("setting.prompt.global.default")));
        defaults.add(globalCombo);
        defaults.add(new JLabel(PluginBundle.get("setting.prompt.project.default")));
        defaults.add(projectCombo);
        projectCombo.setEnabled(project != null);
        panel.add(defaults, BorderLayout.NORTH);
        panel.add(createContentSplitPane(), BorderLayout.CENTER);
        reload();
    }

    private JComponent createContentSplitPane() {
        JComponent listPanel = createToolbar();
        JBScrollPane editorScrollPane = new JBScrollPane(editor);
        listPanel.setMinimumSize(new Dimension(JBUI.scale(180), 0));
        editorScrollPane.setMinimumSize(new Dimension(JBUI.scale(320), 0));

        OnePixelSplitter splitPane = new OnePixelSplitter(false, 0.29f, 0.18f, 0.55f);
        splitPane.setOpaque(false);
        splitPane.setSplitterProportionKey("GitCommitMessageHelper.PromptSettings.Splitter");
        splitPane.setFirstComponent(listPanel);
        splitPane.setSecondComponent(editorScrollPane);
        return splitPane;
    }

    private JComponent createToolbar() {
        return ToolbarDecorator.createDecorator(list)
                .setAddAction(b -> add())
                .setRemoveAction(b -> remove())
                .setEditAction(b -> rename())
                .createPanel();
    }

    private void reload() {
        loading = true;
        listModel.clear();
        for (PromptProfile p : settings.getDateSettings().getPrompts()) listModel.addElement(p);
        globalCombo.removeAllItems(); projectCombo.removeAllItems();
        for (int i = 0; i < listModel.size(); i++) { globalCombo.addItem(listModel.get(i)); projectCombo.addItem(listModel.get(i)); }
        select(globalCombo, settings.getDateSettings().getActivePromptId());
        select(projectCombo, projectPromptId());
        if (!listModel.isEmpty()) list.setSelectedIndex(0);
        loading = false;
        load();
    }

    private void load() {
        displayed = list.getSelectedValue();
        editor.setText(displayed == null ? "" : displayed.getPrompt());
        editor.setCaretPosition(0);
    }

    private void commit() { if (displayed != null) displayed.setPrompt(editor.getText()); }

    private void add() {
        commit();
        PromptProfile p = GitCommitMessageHelperSettings.createPromptProfile(
                GitCommitMessageHelperSettings.createPromptId(listModel.size()),
                uniqueName(PluginBundle.get("setting.prompt.new.name")), "", false);
        listModel.addElement(p); list.setSelectedIndex(listModel.size() - 1); refreshCombos();
    }

    private void remove() {
        PromptProfile p = list.getSelectedValue();
        if (p == null || p.isDefaultPrompt()) { if (p != null) Messages.showWarningDialog(panel, PluginBundle.get("setting.prompt.default.remove.warning"), PluginBundle.get("setting.configurable.prompt")); return; }
        commit(); int i = list.getSelectedIndex(); listModel.remove(i); if (!listModel.isEmpty()) list.setSelectedIndex(Math.min(i, listModel.size()-1)); refreshCombos();
    }

    private void rename() {
        PromptProfile p = list.getSelectedValue();
        if (p == null || p.isDefaultPrompt()) { if (p != null) Messages.showWarningDialog(panel, PluginBundle.get("setting.prompt.default.rename.warning"), PluginBundle.get("setting.configurable.prompt")); return; }
        String name = Messages.showInputDialog(panel, PluginBundle.get("setting.prompt.rename.message"), PluginBundle.get("setting.prompt.rename.title"), null, p.getName(), null);
        if (name != null && !name.trim().isEmpty()) { p.setName(uniqueName(name.trim(), p)); refreshCombos(); list.repaint(); }
    }

    private String uniqueName(String base) { return uniqueName(base, null); }
    private String uniqueName(String base, PromptProfile ignored) { String n=base; int i=2; while (contains(n, ignored)) n=base+" "+i++; return n; }
    private boolean contains(String n, PromptProfile ignored) { for(int i=0;i<listModel.size();i++){ PromptProfile p=listModel.get(i); if(p!=ignored && n.equals(p.getName())) return true;} return false; }
    private void refreshCombos() { String g=id((PromptProfile)globalCombo.getSelectedItem()); String p=id((PromptProfile)projectCombo.getSelectedItem()); globalCombo.removeAllItems(); projectCombo.removeAllItems(); for(int i=0;i<listModel.size();i++){globalCombo.addItem(listModel.get(i));projectCombo.addItem(listModel.get(i));} select(globalCombo,g); select(projectCombo,p); }
    private void select(JComboBox<PromptProfile> c, String id) { for(int i=0;i<c.getItemCount();i++) if(Objects.equals(id,c.getItemAt(i).getId())) {c.setSelectedIndex(i);return;} if(c.getItemCount()>0)c.setSelectedIndex(0); }
    private String id(PromptProfile p){return p==null?null:p.getId();}
    private String projectPromptId(){ if(project==null)return null; GitCommitMessageStorage s=project.getService(GitCommitMessageStorage.class); return s==null||s.getState()==null||s.getState().getMessageStorage()==null?null:s.getState().getMessageStorage().getProjectPromptId(); }
    private Project findProject(){ Project[] ps=ProjectManager.getInstance().getOpenProjects(); return ps.length==0?null:ps[0]; }

    public JPanel getMainPanel(){return panel;}
    public GitCommitMessageHelperSettings getSettings(){ sync(false); persistProjectSelection(); return settings; }
    public void reset(GitCommitMessageHelperSettings source){ settings.setDateSettings(source.clone().getDateSettings()); originalProjectPromptId=projectPromptId(); reload(); }
    public boolean isModified(GitCommitMessageHelperSettings source){ sync(false); return !Objects.equals(settings.getDateSettings().getPrompts(), source.getDateSettings().getPrompts()) || !Objects.equals(settings.getDateSettings().getActivePromptId(), source.getDateSettings().getActivePromptId()) || (project!=null&&!Objects.equals(originalProjectPromptId, selectedProjectId())); }
    private String selectedProjectId(){return id((PromptProfile)projectCombo.getSelectedItem());}
    private void sync(boolean ignored){ commit(); List<PromptProfile> ps=new LinkedList<>(); for(int i=0;i<listModel.size();i++)ps.add(listModel.get(i)); DataSettings d=settings.getDateSettings(); d.setPrompts(ps); PromptProfile g=(PromptProfile)globalCombo.getSelectedItem(); if(g!=null)d.setActivePromptId(g.getId()); }
    private void persistProjectSelection(){ if(project==null)return; GitCommitMessageStorage s=project.getService(GitCommitMessageStorage.class); if(s!=null&&s.getState()!=null&&s.getState().getMessageStorage()!=null){s.getState().getMessageStorage().setProjectPromptId(selectedProjectId());originalProjectPromptId=selectedProjectId();} }
}
