package com.fulinlin.ui.central;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.model.enums.ThinkingParameterPreset;
import com.fulinlin.service.ThinkingParameterMappings;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.*;

public class ThinkingConfigurationEditorTest {

    @Test
    public void editingJsonRetainsTheSelectedLevelAndBecomesCustom() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            editor.getMappingArea().setText("{\"HIGH\":{\"reasoning_effort\":\"xhigh\"}}");
            assertEquals(ThinkingParameterPreset.CUSTOM, editor.getPresetComboBox().getSelectedItem());
            assertEquals(ThinkingLevel.HIGH, editor.getLevelComboBox().getSelectedItem());
            assertEquals(2, editor.getLevelComboBox().getItemCount());

            LlmProfile updated = new LlmProfile();
            editor.applyTo(updated);
            assertEquals(ThinkingLevel.HIGH, updated.getThinkingLevel());
            assertTrue(updated.getThinkingParameterMapping().contains("xhigh"));
        });
    }

    @Test
    public void selectingCustomKeepsTheCurrentTemplate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            String original = editor.getMappingArea().getText();
            editor.getPresetComboBox().setSelectedItem(ThinkingParameterPreset.CUSTOM);

            assertEquals(original, editor.getMappingArea().getText());
            assertEquals(ThinkingLevel.HIGH, editor.getLevelComboBox().getSelectedItem());
        });
    }

    @Test
    public void incompleteJsonDoesNotResetTheCurrentChoice() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            editor.getMappingArea().setText("{\"HIGH\":");
            assertEquals(ThinkingLevel.HIGH, editor.getLevelComboBox().getSelectedItem());
            editor.getMappingArea().setText("{\"HIGH\":{\"reasoning_effort\":\"high\"}}");
            assertEquals(ThinkingLevel.HIGH, editor.getLevelComboBox().getSelectedItem());
        });
    }

    @Test
    public void removingALevelUsesDefaultUntilThatLevelIsRestored() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            editor.getMappingArea().setText("{\"LOW\":{\"reasoning_effort\":\"low\"}}");
            assertEquals(ThinkingLevel.DEFAULT, editor.getLevelComboBox().getSelectedItem());
            editor.getMappingArea().setText("{\"HIGH\":{\"reasoning_effort\":\"high\"}}");
            assertEquals(ThinkingLevel.HIGH, editor.getLevelComboBox().getSelectedItem());
        });
    }

    @Test
    public void choosingAToggleTemplateDoesNotInventGradedLevels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            editor.getPresetComboBox().setSelectedItem(ThinkingParameterPreset.ENABLE_THINKING);
            assertEquals(3, editor.getLevelComboBox().getItemCount());
            assertEquals(ThinkingLevel.DEFAULT, editor.getLevelComboBox().getItemAt(0));
            assertEquals(ThinkingLevel.DISABLED, editor.getLevelComboBox().getItemAt(1));
            assertEquals(ThinkingLevel.ENABLED, editor.getLevelComboBox().getItemAt(2));
        });
    }

    @Test
    public void selectingModelDefaultDisablesTheMappingEditor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            editor.getPresetComboBox().setSelectedItem(ThinkingParameterPreset.MODEL_DEFAULT);
            assertEquals(1, editor.getLevelComboBox().getItemCount());
            assertFalse(editor.getMappingArea().isEditable());
            assertFalse(editor.getMappingPanel().isVisible());
        });
    }

    @Test
    public void advancedMappingStartsCollapsedAndCustomMappingExpandsIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            assertTrue(editor.getMappingPanel().isVisible());
            assertFalse(editor.isAdvancedExpanded());

            editor.getPresetComboBox().setSelectedItem(ThinkingParameterPreset.CUSTOM);

            assertTrue(editor.isAdvancedExpanded());
        });
    }

    @Test
    public void expandedMappingGetsSpaceInTheActualProfileFieldLayout() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ThinkingConfigurationEditor editor = effortEditor();
            JPanel host = profileFieldLayout(editor);
            int collapsedHeight = host.getPreferredSize().height;

            editor.showAdvancedMapping();
            layout(host);

            assertTrue(host.getPreferredSize().height > collapsedHeight);
            JScrollPane scroll = (JScrollPane) editor.getMappingPanel().getComponent(1);
            assertTrue("expanded JSON editor must have usable height", scroll.getHeight() > 0);
            assertTrue(editor.getMappingArea().getHeight() > 0);
            assertFalse("the parent field must not fix the mapping panel's height",
                    editor.getMappingPanel().isPreferredSizeSet());
        });
    }

    @Test
    public void mappingCanExpandAfterStartingWithModelDefault() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LlmProfile profile = new LlmProfile();
            profile.setThinkingParameterPreset(ThinkingParameterPreset.MODEL_DEFAULT);
            ThinkingConfigurationEditor editor = new ThinkingConfigurationEditor(profile);
            JPanel host = profileFieldLayout(editor);

            editor.getPresetComboBox().setSelectedItem(ThinkingParameterPreset.OPENAI_EFFORT);
            editor.getPresetComboBox().setSelectedItem(ThinkingParameterPreset.CUSTOM);
            layout(host);

            JScrollPane scroll = (JScrollPane) editor.getMappingPanel().getComponent(1);
            assertTrue(editor.isAdvancedExpanded());
            assertTrue(scroll.getHeight() > 0);
            int expandedHeight = host.getPreferredSize().height;
            ((JButton) editor.getMappingPanel().getComponent(0)).doClick();
            assertTrue(host.getPreferredSize().height < expandedHeight);
        });
    }

    @Test
    public void enabledOnlyMappingDoesNotOfferOffOrInventEffortLevels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LlmProfile profile = new LlmProfile();
            profile.setModel("MiniMax-M3.1-Flash");
            profile.setThinkingParameterPreset(ThinkingParameterPreset.CUSTOM);
            profile.setThinkingParameterMapping("{\"ENABLED\":{\"thinking\":{\"type\":\"adaptive\"}}}");
            profile.setThinkingLevel(ThinkingLevel.ENABLED);
            ThinkingConfigurationEditor editor = new ThinkingConfigurationEditor(profile);

            assertEquals(2, editor.getLevelComboBox().getItemCount());
            assertEquals(ThinkingLevel.DEFAULT, editor.getLevelComboBox().getItemAt(0));
            assertEquals(ThinkingLevel.ENABLED, editor.getLevelComboBox().getItemAt(1));
            assertEquals(ThinkingLevel.ENABLED, editor.getLevelComboBox().getSelectedItem());
        });
    }

    private static JPanel profileFieldLayout(ThinkingConfigurationEditor editor) {
        JPanel host = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        int row = LlmProfileEditor.addField(host, "Thinking", editor.getMappingPanel(), constraints, 0);
        LlmProfileEditor.addField(host, "Tokens", new JSpinner(), constraints, row);
        return host;
    }

    private static void layout(Container container) {
        if (container.getParent() == null) {
            container.setSize(640, container.getPreferredSize().height);
        }
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }

    private static ThinkingConfigurationEditor effortEditor() {
        LlmProfile profile = new LlmProfile();
        profile.setThinkingParameterPreset(ThinkingParameterPreset.OPENAI_EFFORT);
        profile.setThinkingParameterMapping(ThinkingParameterMappings.template(ThinkingParameterPreset.OPENAI_EFFORT));
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        return new ThinkingConfigurationEditor(profile);
    }
}
