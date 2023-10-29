package top.kkoishi.ideacloudmusicplayer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import top.kkoishi.cloudmusic.CloudMusic
import top.kkoishi.cloudmusic.Config

private const val BUNDLE = "messages.all"

object Bundles : DynamicBundle(BUNDLE) {
    @JvmStatic
    val GSON: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

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

    @JvmStatic
    fun Long.msToFormattedTime(): String {
        val totalSec = this / 1000
        val hour = totalSec / (60 * 60)
        val hourRest = totalSec - hour * (60 * 60)
        val min = hourRest / 60
        val sec = hourRest - min * 60
        val sb = StringBuilder()

        if (hour < 10)
            sb.append('0').append(hour)
        else
            sb.append(hour)
        sb.append(':')
        if (min < 10)
            sb.append('0').append(min)
        else
            sb.append(min)
        sb.append(':')
        if (sec < 10)
            sb.append('0').append(sec)
        else
            sb.append(sec)
        return sb.toString()
    }
}