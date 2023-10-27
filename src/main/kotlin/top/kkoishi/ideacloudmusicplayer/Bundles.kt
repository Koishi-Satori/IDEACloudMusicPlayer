package top.kkoishi.ideacloudmusicplayer

import com.google.gson.GsonBuilder
import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import top.kkoishi.cloudmusic.CloudMusic
import top.kkoishi.cloudmusic.Config

private const val BUNDLE = "messages.all"

object Bundles : DynamicBundle(BUNDLE) {
    @JvmStatic
    val GSON = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    @JvmStatic
    val CLOUD_MUSIC_API = CloudMusic(
        GSON.fromJson(
            (this.javaClass.getResourceAsStream("config.json") ?: throw ExceptionInInitializerError())
                .bufferedReader()
                .readText(),
            Config::class.java
        )
    )

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)
}