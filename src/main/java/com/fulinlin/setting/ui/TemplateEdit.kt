package com.fulinlin.setting.ui

import com.fulinlin.pojo.TemplateLanguage
import com.intellij.openapi.editor.Editor
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import javax.swing.JPanel

class TemplateEdit(private val splitPane: JPanel,
                   private val templateContent: String,
                   private val templateLanguage: () -> TemplateLanguage,
                   private val dividerPosWhenShown: Int = 150
) {
    var testInputShown = false
        private set

    private var templateEditor: Editor = newTemplateEditor(templateContent, templateLanguage())

    private val templateEditPanel: JPanel = splitPane

    val templateText
        get() = templateEditor.document.text

    init {
        splitPane.isEnabled = false
        refreshTemplateEditor()
    }

    fun toggleTestInputPane() {
        if (testInputShown) {
            hideTestInputPane()
        } else {
            showTestInputPane()
        }
    }

    fun refreshTemplateEditor() {
        if (templateEditPanel.componentCount > 0) {
            Editors.release(templateEditor)
            templateEditPanel.remove(0)
        }
        val editor = newTemplateEditor(templateText, templateLanguage())
        templateEditPanel.add(editor.component, BorderLayout.CENTER)
        templateEditor = editor
    }

    private fun newTemplateEditor(text: String, lang: TemplateLanguage): Editor {
        return Editors.createSourceEditor(null, lang.fileType, text, true)
    }


    private fun hideTestInputPane() {
        testInputShown = false
        splitPane.isEnabled = false
    }

    private fun showTestInputPane() {
        testInputShown = true
        splitPane.isEnabled = true
    }

}
