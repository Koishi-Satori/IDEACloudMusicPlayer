package top.kkoishi.ideacloudmusicplayer.ui

import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.util.io.toNioPath
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import top.kkoishi.ideacloudmusicplayer.Bundles
import top.kkoishi.ideacloudmusicplayer.io.CacheConfig
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.io.path.exists
import kotlin.io.path.isDirectory


class CacheSettingsComponent {
    private val cacheDirInput = JBTextField()
    private val clearCacheBtn = JButton(Bundles.message("settings.cache.clear")).apply {
        TODO("clear caches")
    }
    private val mainPanel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(
            Bundles.message("settings.cache.title"),
            cacheDirInput,
            1,
            false,
        ).addComponent(JButton(Bundles.message("settings.cache.confirm")).apply {
            val config = CacheConfig.getInstance()
            val nDir = cacheDirInput.text.toNioPathOrNull()
            if (nDir == null || !nDir.exists() || nDir.isDirectory())
                return@apply

            config.cacheDir = nDir.toCanonicalPath()
        })
        .addComponent(clearCacheBtn, 1)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    fun getPanel(): JPanel = mainPanel

    fun getPreferredFocusedComponent(): JComponent = cacheDirInput

    fun getCacheDir() = cacheDirInput.text

    fun setCacheDir(nText: String) {
        cacheDirInput.text = nText
    }
}