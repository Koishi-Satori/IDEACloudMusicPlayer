package top.kkoishi.ideacloudmusicplayer

import com.intellij.util.io.createFile
import com.intellij.util.io.isFile
import top.kkoishi.cloudmusic.response.SongSearchResponse
import top.kkoishi.cloudmusic.util.StringExtension.isDigit
import top.kkoishi.ideacloudmusicplayer.Bundles.msToFormattedTime
import top.kkoishi.ideacloudmusicplayer.io.CacheConfig
import top.kkoishi.ideacloudmusicplayer.io.IOUtil.createIfNotExists
import java.nio.file.Path
import kotlin.io.path.*

data class PlayList(val name: String, var songs: LongArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayList) return false

        if (name != other.name) return false
        return songs.contentEquals(other.songs)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + songs.contentHashCode()
        return result
    }

    override fun toString(): String = name

    fun getDisplayText(): String {
        val sb = StringBuilder("name: ").append(name)
        val config = CacheConfig.getInstance()
        val songIndex = Path.of("${config.cacheDir}/song_indexes.json")
        if (songIndex.notExists()) {
            if (songIndex.parent.notExists())
                songIndex.parent.createDirectories()
            songIndex.createFile()
            return sb.toString()
        }

        val json = songIndex.readText()
        if (json.isEmpty())
            return sb.toString()
        val indexes =
            Bundles.GSON.fromJson(json, Array<SongSearchResponse.SongInfo>::class.java)

        songs.forEach {
            if (indexes.any { info -> info.id == it }) {
                indexes.find { info -> info.id == it }?.let { info ->
                    sb.append("\nName: ").append(info.name).append("\nArtist: ")
                    with(info.artists.iterator()) {
                        while (hasNext()) {
                            sb.append(next().name)
                            if (!hasNext())
                                break
                            sb.append(", ")
                        }
                    }
                    sb.append("\nDuration: ").append(info.duration.toLong().msToFormattedTime())
                }
            } else
                sb.append("\nid: ").append(it)
            sb.append("\n---------------------------------")
        }
        return sb.toString()
    }

    companion object {
        @JvmStatic
        fun getDefault(): PlayList {
            val config = CacheConfig.getInstance()
            val songCacheDir = Path.of("${config.cacheDir}/songs/")

            if (songCacheDir.notExists() || !songCacheDir.isDirectory())
                return PlayList("default", longArrayOf())
            return PlayList(
                "default",
                songCacheDir.toFile().listFiles()!!
                    .filter { it.nameWithoutExtension.isDigit() }
                    .map { it.nameWithoutExtension.toLong() }
                    .toLongArray()
            )
        }

        @JvmStatic
        fun singleList(id: Long): PlayList {
            return PlayList("single", longArrayOf(id))
        }

        @JvmStatic
        fun getCachedLists(): MutableList<PlayList> {
            val config = CacheConfig.getInstance()
            val songListCacheFile = Path.of("${config.cacheDir}/.lists")
            if (songListCacheFile.notExists() || !songListCacheFile.isFile())
                return mutableListOf()
            return Bundles.GSON.fromJson(songListCacheFile.readText(), Array<PlayList>::class.java)
                .toMutableList()
        }

        @JvmStatic
        fun storeCache(lists: MutableList<PlayList>) {
            val config = CacheConfig.getInstance()
            val songListCacheFile = Path.of("${config.cacheDir}/.lists")
                .createIfNotExists()

            songListCacheFile.writeText(Bundles.GSON.toJson(lists.toTypedArray()))
        }
    }
}