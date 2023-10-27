package top.kkoishi.ideacloudmusicplayer.io

import com.intellij.testFramework.utils.io.createDirectory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

object IOUtil {
    @JvmStatic
    fun Path.createIfNotExists(isDir: Boolean = false): Path {
        if (notExists()) {
            if (parent.notExists())
                parent.createDirectories()
            if (isDir)
                createDirectory()
            else
                createFile()
        }
        return this
    }
}