package top.kkoishi.ideacloudmusicplayer.io

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

@Service(Service.Level.APP)
@Suppress("MemberVisibilityCanBePrivate", "unused")
@State(name = "songs_cache", storages = [Storage(value = "songs_cache.xml")])
class CacheConfig : PersistentStateComponent<CacheConfig> {
    var cacheDir: String = System.getProperty("user.home") + "/.kkoishi/IDEACloudMusic_cache"

    init {
        val dir = Path.of(cacheDir)
        if (dir.notExists())
            dir.createDirectories()
    }

    override fun getState(): CacheConfig {
        return this
    }

    override fun loadState(config: CacheConfig) {
        XmlSerializerUtil.copyBean(config, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(): CacheConfig = ApplicationManager.getApplication().getService(CacheConfig::class.java)
    }
}