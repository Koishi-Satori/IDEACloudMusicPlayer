package top.kkoishi.ideacloudmusicplayer.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ListSelectionDialog(private val text: String, vararg data: String) : DialogWrapper(true) {
    private val listData = data.toList()
    private val list = JBList<String>()
    var selectedValue: String? = null

    init {
        title = "Selection"
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel(BorderLayout())

        list.setListData(listData.toTypedArray())
        list.addListSelectionListener {
            selectedValue = list.selectedValue
        }
        panel.add(JBLabel(text))
        panel.add(list, BorderLayout.CENTER)

        return panel
    }
}