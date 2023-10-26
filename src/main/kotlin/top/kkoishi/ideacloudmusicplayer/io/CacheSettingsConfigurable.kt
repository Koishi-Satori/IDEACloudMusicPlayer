package top.kkoishi.ideacloudmusicplayer.io

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.io.toCanonicalPath
import top.kkoishi.ideacloudmusicplayer.ui.CacheSettingsComponent
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class CacheSettingsConfigurable : Configurable {
    private var component = CacheSettingsComponent()

    override fun createComponent(): JComponent = component.getPanel()

    override fun isModified(): Boolean {
        val config = CacheConfig.getInstance()
        return component.getCacheDir() == config.cacheDir
    }

    override fun apply() {
        val config = CacheConfig.getInstance()
        val cacheDir = component.getCacheDir()
        with(Path.of(cacheDir)) {
            if (notExists())
                createDirectories()
            config.cacheDir = toCanonicalPath()
        }
    }

    override fun getDisplayName(): String = "CloudMusicPlayer: Cache"

    override fun reset() {
        val config = CacheConfig.getInstance()
        component.setCacheDir(config.cacheDir)
    }

    override fun getPreferredFocusedComponent(): JComponent = component.getPreferredFocusedComponent()
}