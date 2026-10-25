package com.fulinlin.ui.central;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.model.enums.ThinkingParameterPreset;
import com.fulinlin.service.ThinkingParameterMappings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

final class ThinkingConfigurationEditor {

    private final JComboBox<ThinkingLevel> levelComboBox = new JComboBox<>();
    private final JComboBox<ThinkingParameterPreset> presetComboBox =
            new JComboBox<>(ThinkingParameterPreset.values());
    private final JTextArea mappingArea = new JTextArea(8, 42);
    private final JScrollPane mappingScrollPane = new JScrollPane(mappingArea);
    private final JButton mappingToggleButton = new JButton();
    private final JPanel mappingPanel = new JPanel(new BorderLayout(0, 4));
    private ThinkingLevel preferredLevel;
    private boolean updating;

    ThinkingConfigurationEditor(@NotNull LlmProfile profile) {
        levelComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                           boolean isSelected, boolean cellHasFocus) {
                Object label = value instanceof ThinkingLevel ? ((ThinkingLevel) value).getDisplayName() : value;
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        presetComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                           boolean isSelected, boolean cellHasFocus) {
                Object label = value instanceof ThinkingParameterPreset
                        ? ((ThinkingParameterPreset) value).getDisplayName() : value;
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        levelComboBox.setToolTipText(PluginBundle.get("setting.llm.thinking.level.tooltip"));
        presetComboBox.setToolTipText(PluginBundle.get("setting.llm.thinking.preset.tooltip"));
        mappingArea.setToolTipText(PluginBundle.get("setting.llm.thinking.mapping.tooltip"));
        mappingArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, mappingArea.getFont().getSize()));
        mappingArea.setTabSize(2);
        mappingPanel.add(mappingToggleButton, BorderLayout.NORTH);
        mappingPanel.add(mappingScrollPane, BorderLayout.CENTER);
        ThinkingParameterPreset preset = ThinkingParameterPreset.fromNullable(profile.getThinkingParameterPreset());
        presetComboBox.setSelectedItem(preset);
        mappingArea.setText(profile.getThinkingParameterMapping() == null
                ? ThinkingParameterMappings.template(preset) : profile.getThinkingParameterMapping());
        setAdvancedExpanded(preset == ThinkingParameterPreset.CUSTOM);
        preferredLevel = ThinkingLevel.fromNullable(profile.getThinkingLevel());
        refreshLevels();
        bindListeners();
    }

    JComboBox<ThinkingLevel> getLevelComboBox() {
        return levelComboBox;
    }

    JComboBox<ThinkingParameterPreset> getPresetComboBox() {
        return presetComboBox;
    }

    JTextArea getMappingArea() {
        return mappingArea;
    }

    JPanel getMappingPanel() {
        return mappingPanel;
    }

    boolean isAdvancedExpanded() {
        return mappingScrollPane.isVisible();
    }

    void showAdvancedMapping() {
        if (mappingPanel.isVisible()) {
            setAdvancedExpanded(true);
        }
    }

    void applyTo(@NotNull LlmProfile profile) {
        profile.setThinkingLevel((ThinkingLevel) levelComboBox.getSelectedItem());
        profile.setThinkingParameterPreset((ThinkingParameterPreset) presetComboBox.getSelectedItem());
        profile.setThinkingParameterMapping(mappingArea.getText());
    }

    private void bindListeners() {
        mappingToggleButton.addActionListener(e -> setAdvancedExpanded(!mappingScrollPane.isVisible()));
        levelComboBox.addActionListener(e -> {
            if (!updating && levelComboBox.getSelectedItem() instanceof ThinkingLevel) {
                preferredLevel = (ThinkingLevel) levelComboBox.getSelectedItem();
            }
        });
        presetComboBox.addActionListener(e -> {
            if (updating) {
                return;
            }
            ThinkingParameterPreset preset = (ThinkingParameterPreset) presetComboBox.getSelectedItem();
            if (preset == null) {
                return;
            }
            updating = true;
            // Selecting Custom keeps the current template as the starting point.
            if (preset != ThinkingParameterPreset.CUSTOM) {
                mappingArea.setText(ThinkingParameterMappings.template(preset));
                mappingArea.setCaretPosition(0);
            } else {
                setAdvancedExpanded(true);
            }
            updating = false;
            refreshLevels();
        });
        mappingArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                mappingChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mappingChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                mappingChanged();
            }

            private void mappingChanged() {
                if (updating) {
                    return;
                }
                updating = true;
                presetComboBox.setSelectedItem(ThinkingParameterPreset.CUSTOM);
                updating = false;
                setAdvancedExpanded(true);
                // An incomplete JSON edit must not discard the user's selected level.
                if (ThinkingParameterMappings.validate(ThinkingParameterPreset.CUSTOM, mappingArea.getText()) == null) {
                    refreshLevels();
                }
            }
        });
    }

    private void refreshLevels() {
        ThinkingParameterPreset preset = (ThinkingParameterPreset) presetComboBox.getSelectedItem();
        if (preset == null) {
            return;
        }
        updating = true;
        List<ThinkingLevel> levels = ThinkingParameterMappings.availableLevels(preset, mappingArea.getText());
        levelComboBox.setModel(new DefaultComboBoxModel<>(levels.toArray(new ThinkingLevel[0])));
        levelComboBox.setSelectedItem(levels.contains(preferredLevel) ? preferredLevel : ThinkingLevel.DEFAULT);
        boolean editable = preset != ThinkingParameterPreset.AUTO && preset != ThinkingParameterPreset.MODEL_DEFAULT;
        mappingArea.setEditable(editable);
        mappingArea.setEnabled(editable);
        mappingPanel.setVisible(editable);
        mappingToggleButton.setEnabled(editable);
        updating = false;
        refreshLayout();
    }

    private void setAdvancedExpanded(boolean expanded) {
        mappingScrollPane.setVisible(expanded);
        mappingToggleButton.setText(PluginBundle.get(expanded
                ? "setting.llm.thinking.mapping.hide"
                : "setting.llm.thinking.mapping.show"));
        refreshLayout();
    }

    private void refreshLayout() {
        mappingPanel.revalidate();
        mappingPanel.repaint();
        Window window = SwingUtilities.getWindowAncestor(mappingPanel);
        if (window != null) {
            Dimension preferred = window.getPreferredSize();
            if (preferred.width > window.getWidth() || preferred.height > window.getHeight()) {
                window.setSize(Math.max(window.getWidth(), preferred.width),
                        Math.max(window.getHeight(), preferred.height));
            }
        }
    }
}
