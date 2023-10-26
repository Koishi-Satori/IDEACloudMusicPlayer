package top.kkoishi.ideacloudmusicplayer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JComponent

class InputDialog(project: Project, parent: Component): DialogWrapper(project, parent, false, IdeModalityType.IDE) {
    init {
        title = "Input Dialog"
        init()
    }

    private val field = JBTextField()

    override fun createCenterPanel(): JComponent? = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(field, BorderLayout.CENTER)
    }

    fun getInput() = field.text
}