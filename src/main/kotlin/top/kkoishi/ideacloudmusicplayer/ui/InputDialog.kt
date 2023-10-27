package top.kkoishi.ideacloudmusicplayer.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel


class InputDialog(private val text: String = "Input") : DialogWrapper(true) {
    private val field = JBTextField()

    init {
        title = "Input Dialog"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())

        val label = JBLabel(text)
        dialogPanel.add(label, BorderLayout.WEST)
        dialogPanel.add(field, BorderLayout.CENTER)

        return dialogPanel
    }

    fun getInput() = field.text
}