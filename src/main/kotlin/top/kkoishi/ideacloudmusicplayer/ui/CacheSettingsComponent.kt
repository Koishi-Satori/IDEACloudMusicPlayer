package top.kkoishi.ideacloudmusicplayer.ui

import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import top.kkoishi.ideacloudmusicplayer.Bundles
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


class CacheSettingsComponent {
    private val cacheDirInput = JBTextField()
    private val clearCacheBtn = JButton(Bundles.message("settings.cache.clear"))
    private val mainPanel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(
            Bundles.message("settings.cache.title"),
            cacheDirInput,
            1,
            false,
        ).addComponent(clearCacheBtn, 1)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    fun getPanel(): JPanel = mainPanel

    fun getPreferredFocusedComponent(): JComponent = cacheDirInput

    fun getCacheDir() = cacheDirInput.text

    fun setCacheDir(nText: String) {
        cacheDirInput.text = nText
    }
}