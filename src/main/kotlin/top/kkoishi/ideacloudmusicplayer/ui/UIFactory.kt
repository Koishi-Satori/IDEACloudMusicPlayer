package top.kkoishi.ideacloudmusicplayer.ui

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.util.io.toNioPath
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.ui.util.preferredHeight
import com.intellij.util.io.readText
import top.kkoishi.cloudmusic.api.SearchApi
import top.kkoishi.cloudmusic.response.AccessSongResponse
import top.kkoishi.cloudmusic.response.SongSearchResponse
import top.kkoishi.cloudmusic.util.StringExtension.isDigit
import top.kkoishi.ideacloudmusicplayer.Bundles
import top.kkoishi.ideacloudmusicplayer.Bundles.msToFormattedTime
import top.kkoishi.ideacloudmusicplayer.PlayList
import top.kkoishi.ideacloudmusicplayer.Players
import top.kkoishi.ideacloudmusicplayer.ThreadPool
import top.kkoishi.ideacloudmusicplayer.io.CacheConfig
import top.kkoishi.ideacloudmusicplayer.io.IOUtil.createIfNotExists
import java.awt.BorderLayout
import java.awt.Point
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.BUTTON3
import java.io.IOException
import java.nio.file.Path
import java.util.*
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import kotlin.NullPointerException
import kotlin.collections.ArrayDeque
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class UIFactory : ToolWindowFactory {
    init {
        thisLogger().info("Finish init the UIFactory.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val main = MainToolWindow()
        val content = ContentFactory
            .getInstance()
            .createContent(main.content(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    class MainToolWindow {
        private val root = JBTabbedPane(4)
        private var info: SongSearchResponse.SongInfo? = null
        private val infoText = JBTextArea()

        private val itemsInSearch = listOf(
            Bundles.message("popup.info"),
            Bundles.message("popup.add.player"),
            Bundles.message("popup.add.list"),
        )

        private val searchCallBack = listOf(
            { _: String, info: SongSearchResponse.SongInfo ->
                info(info)
            },
            { _: String, info: SongSearchResponse.SongInfo ->
                try {
                    val p = downloadSong(info)
                    Players.getInstance().addAudio(p.toNioPath())
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            },
            { _: String, info: SongSearchResponse.SongInfo ->
                try {
                    downloadSong(info)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }

                val songLists = PlayList.getCachedLists()
                val songListTitles = songLists.map { it.name }.toTypedArray()
                val dialog =
                    ListSelectionDialog("Select Song List to Insert:", *songListTitles)
                if (dialog.showAndGet()) {
                    val selected = dialog.selectedValue
                    if (selected != null) {
                        val index = songListTitles.indexOf(selected)
                        if (index != -1) {
                            songLists[index].songs = songLists[index].songs + info.id
                            PlayList.storeCache(songLists)
                        }
                    }
                }
            }
        )

        private fun getInfoString(info: SongSearchResponse.SongInfo): String {
            val sb = StringBuilder("ID: ").append(info.id)
                .append("\nName: ").append(info.name)
                .append("\nArtists: ")

            with(info.artists.iterator()) {
                while (hasNext()) {
                    sb.append(next().name)
                    if (!hasNext())
                        break
                    sb.append(", ")
                }
            }

            sb.append("\nDuration: ").append(info.duration.toLong().msToFormattedTime())
                .append("\nAlias: ")

            with(info.alias.iterator()) {
                while (hasNext()) {
                    sb.append(next())
                    if (!hasNext())
                        break
                    sb.append(", ")
                }
            }
            return sb.toString()
        }

        private fun cachedSongsTab() = JBPanel<JBPanel<*>>(BorderLayout()).apply {

        }

        private fun playerTab() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            fun getDescFromPath(p: Path): String {
                val config = CacheConfig.getInstance()
                val indexesJson = Path.of("${config.cacheDir}/song_indexes.json")
                    .createIfNotExists().readText()
                if (indexesJson.isEmpty())
                    return p.toFile().name
                val realName = p.toFile().nameWithoutExtension
                if (realName.isDigit()) {
                    val indexes =
                        Bundles.GSON.fromJson(indexesJson, Array<SongSearchResponse.SongInfo>::class.java)
                    indexes.find { it.id == realName.toLong() }
                        ?.let { return it.name }
                }
                return p.toFile().name
            }

            fun getSongDesc(p: String, metaData: Map<String, String>): String {
                if (p.isEmpty())
                    return ""
                val config = CacheConfig.getInstance()
                val indexesJson = Path.of("${config.cacheDir}/song_indexes.json")
                    .createIfNotExists().readText()
                val sb = StringBuilder()
                if (indexesJson.isEmpty())
                    sb.append("Path: ").append(p)
                        .append("\nFrom: Unknown")
                        .append("\nError: Can not get Song Indexes.")
                else {
                    val realName = p.toNioPath().toFile().nameWithoutExtension
                    if (realName.isDigit()) {
                        val info =
                            Bundles.GSON.fromJson(indexesJson, Array<SongSearchResponse.SongInfo>::class.java)
                                .find { it.id == realName.toLong() }

                        if (info == null)
                            sb.append("ID: ").append(realName)
                                .append("\nFrom: Unknown")
                                .append("\nError: id is not in Song Indexes.")
                        else {
                            sb.append(getInfoString(info))
                        }
                    } else
                        sb.append("Path: ").append(p)
                            .append("\nFrom: Unknown")
                            .append("\nError: Non-number id.")
                }

                sb.append("\n--------------------------\nMetaData: ")
                metaData.forEach {
                    sb.append("\n\t").append(it.key).append(':').append(it.value)
                }
                return sb.toString()
            }

            val players = Players.getInstance()
            val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val progressPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val progressBar = JProgressBar(0, 0)
            val progressLabel = JBLabel(Bundles.message("lable.process", "null", "null"))
            val stopBtn = JButton(Bundles.message("button.play.continue")).apply {
                addActionListener {
                    if (players.isAdjusting()) {
                        if (!players.isStop())
                            players.stop()
                        else
                            players.replay()
                    }
                    text = Bundles.message("button.play.continue")
                }
            }
            progressPanel.add(progressLabel, BorderLayout.WEST)
            progressPanel.add(progressBar, BorderLayout.CENTER)
            progressPanel.add(JBPanel<JBPanel<*>>().apply {
                add(stopBtn)
                add(JButton(Bundles.message("button.play.clear")).apply {
                    addActionListener {
                        players.end()
                        players.clear()
                        players.keep()
                    }
                })
            }, BorderLayout.EAST)

            var last = ""
            val list = JBList<String>()
            val current = JBTextArea().apply {
                columns = 40
                autoscrolls = true
                isEditable = false
            }
            // update info at 20 tps, if needed
            // 260026(api, ms), 260 * 1000 = 260000(actual, ms), 260075102(ffmpeg, ns)
            ThreadPool.task(50L, "info_update") {
                updateUI()
                val progress = players.progress()
                val length = players.length()
                val isAdjusting = players.isAdjusting()
                val stop = players.isStop()
                val currentSong = players.current()
                if (length != -1L)
                    progressBar.maximum = (length % Int.MAX_VALUE).toInt()
                progressBar.value = if (progress.isNaN()) 0 else (progress % Int.MAX_VALUE).toInt()
                players.playList()
                    .map { getDescFromPath(it) }
                    .takeIf {
                        if (list.model.size != it.size)
                            return@takeIf true
                        it.forEachIndexed { index, item ->
                            if (list.model.getElementAt(index) != item)
                                return@takeIf true
                        }
                        return@takeIf false
                    }?.let {
                        if (list.selectedIndex >= it.size)
                            list.selectedIndex = it.size - 1
                        list.setListData(it.toTypedArray())
                    }
                if (last != currentSong) {
                    last = currentSong

                    synchronized(current) {
                        current.text = getSongDesc(last, players.currentMetaData())
                    }
                }

                if (isAdjusting) {
                    progressLabel.text = Bundles.message(
                        "lable.process",
                        (progress.toLong() / 1000L).msToFormattedTime(),
                        (length / 1000L).msToFormattedTime()
                    )
                    if (!stop)
                        stopBtn.text = Bundles.message("button.play.stop")
                } else if (progressLabel.text.isEmpty() || progressLabel.text[0] != 'n')
                    progressLabel.text = Bundles.message("lable.process", "null", "null")
            }
            mainPanel.add(JBScrollPane(list), BorderLayout.CENTER)
            mainPanel.add(JBScrollPane(current), BorderLayout.EAST)

            add(progressPanel, BorderLayout.SOUTH)
            add(mainPanel, BorderLayout.CENTER)
            updateUI()
        }

        private fun info(info: SongSearchResponse.SongInfo) {
            this.info = info
            infoText.text = getInfoString(info)
            root.selectedIndex = 1
        }

        private fun downloadSong(info: SongSearchResponse.SongInfo): String {
            val checkJson = Bundles.CLOUD_MUSIC_API.checkMusicRequest(info.id)
            if (Bundles.CLOUD_MUSIC_API.checkMusicRequest(info.id).contains("ok")) {
                val accessJson = Bundles.CLOUD_MUSIC_API.accessMusicRequest(info.id)
                try {
                    val response =
                        Bundles.GSON.fromJson(accessJson, AccessSongResponse::class.java).data[0]
                    val config = CacheConfig.getInstance()
                    val cache =
                        Path.of("${config.cacheDir}/songs/${info.id}.${response.type}")
                    val songIndex = Path.of("${config.cacheDir}/song_indexes.json")
                        .createIfNotExists()

                    var indexJson = songIndex.readText()
                    indexJson = if (indexJson.isEmpty())
                        Bundles.GSON.toJson(arrayOf(info))
                    else {
                        val indexes =
                            Bundles.GSON.fromJson(indexJson, Array<SongSearchResponse.SongInfo>::class.java)
                        if (indexes.any { it.id == info.id })
                            indexJson
                        else
                            Bundles.GSON.toJson(indexes + info)
                    }
                    songIndex.writeText(indexJson)
                    response.download(cache.toCanonicalPath(), false)
                    return cache.toCanonicalPath()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                } catch (e: Exception) {
                    throw RuntimeException("Bad json response: $accessJson", e)
                }
            } else
                throw RuntimeException(checkJson)
        }

        private fun songInfoTab() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val topPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val infoPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val bottomPanel = JBPanel<JBPanel<*>>()
            val player = Players.getInstance()

            infoPanel.add(infoText, BorderLayout.CENTER)
            bottomPanel.add(JButton(Bundles.message("button.download.cache")).apply {
                addActionListener {
                    val info = this@MainToolWindow.info
                    if (info != null)
                        downloadSong(info)
                }
            })
            bottomPanel.add(JButton(Bundles.message("button.add")).apply {
                addActionListener {
                    val info = this@MainToolWindow.info
                    if (info != null) {
                        val path = Path.of(downloadSong(info))
                        if (!player.contains(path))
                            player.addAudio(path)
                    }
                }
            })
            bottomPanel.add(JButton(Bundles.message("button.clear")).apply {
                addActionListener {
                    player.end()
                    player.clear()
                    player.keep()
                }
            })
            bottomPanel.add(JButton(Bundles.message("button.copy.id")).apply {
                addActionListener {
                    val info = this@MainToolWindow.info
                    if (info != null) {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(
                            StringSelection(info.id.toString()),
                            null
                        )
                    }
                }
            })

            add(topPanel, BorderLayout.NORTH)
            add(infoPanel, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }

        private fun songListsTab() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val topPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val songListInfoPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val bottomPanel = JBPanel<JBPanel<*>>(BorderLayout())
            var index = -1
            val songIDInput = JBTextField()
            val textArea = JBTextArea()
                .apply { autoscrolls = true }
            val playLists = PlayList.getCachedLists()
            val list = JBList(playLists)
                .apply {
                    selectionMode = ListSelectionModel.SINGLE_SELECTION
                    visibleRowCount = 1
                    autoscrolls = true
                }

            fun updateDataWithoutCheck() {
                textArea.text = playLists[index].getDisplayText()
                textArea.moveCaretPosition(0)
                list.setListData(playLists.toTypedArray())
            }

            topPanel.add(JBScrollPane(list), BorderLayout.CENTER)
            topPanel.add(JButton(Bundles.message("button.refresh")).apply {
                addActionListener {
                    playLists.clear()
                    playLists.addAll(PlayList.getCachedLists())
                    list.setListData(playLists.toTypedArray())
                    if (index >= playLists.size || index < 0)
                        index = 0
                    if (playLists.isNotEmpty())
                        updateDataWithoutCheck()
                }
            }, BorderLayout.EAST)
            topPanel.add(JButton(Bundles.message("button.list.new")).apply {
                addActionListener {
                    val input = InputDialog("Input Song List Name: ")
                    if (input.showAndGet()) {
                        val text = input.getInput()
                        if (text.isNotEmpty()) {
                            playLists.add(PlayList(text, longArrayOf()))
                            index = playLists.lastIndex
                            updateDataWithoutCheck()
                            PlayList.storeCache(playLists)
                        }
                    }
                }
            }, BorderLayout.WEST)
            list.addListSelectionListener {
                index = list.selectedIndex
                if (index < 0 || index > playLists.size)
                    index = 0
                if (playLists.isNotEmpty())
                    textArea.text = playLists[index].getDisplayText()
            }

            textArea.autoscrolls = true
            songListInfoPanel.add(JBScrollPane(textArea), BorderLayout.CENTER)

            val bottomSubPanel = JBPanel<JBPanel<*>>()
            val bottomMainPanel = JBPanel<JBPanel<*>>(BorderLayout())
            bottomSubPanel.add(JButton(Bundles.message("button.list.delete")).apply {
                addActionListener {
                    if (playLists.isNotEmpty() && index >= 0 && index < playLists.size) {
                        val cur = playLists[index]
                        println("remove play list: $cur")
                        playLists.removeAt(index)
                        PlayList.storeCache(playLists)
                        index = 0

                        if (playLists.isNotEmpty())
                            updateDataWithoutCheck()
                    }
                }
            })
            bottomSubPanel.add(JButton(Bundles.message("button.list.manage")).apply {
                // TODO: add actions
            })
            bottomMainPanel.add(songIDInput, BorderLayout.CENTER)
            bottomMainPanel.add(JButton(Bundles.message("button.list.add")).apply {
                addActionListener {
                    val id = songIDInput.text
                    if (id.isDigit()) {
                        if (playLists.isNotEmpty() && index < playLists.size && index >= 0) {
                            val cur = playLists[index]
                            cur.songs = cur.songs + id.toLong()

                            updateDataWithoutCheck()
                            PlayList.storeCache(playLists)
                        }
                    }
                }
            }, BorderLayout.EAST)
            bottomMainPanel.add(JButton(Bundles.message("button.list.play")).apply {
                addActionListener {
                    if (playLists.isNotEmpty() && index < playLists.size && index >= 0) {
                        val cur = playLists[index]
                        val config = CacheConfig.getInstance()
                        val cachedSongDir = Path.of("${config.cacheDir}/songs/")
                        val players = Players.getInstance()
                        if (cachedSongDir.notExists() || !cachedSongDir.isDirectory())
                            return@addActionListener

                        cachedSongDir.toFile().listFiles()!!
                            .filter {
                                val shortName = it.nameWithoutExtension
                                if (!shortName.isDigit())
                                    return@filter false
                                return@filter cur.songs.contains(it.nameWithoutExtension.toLong())
                            }
                            .map { it.canonicalPath.toNioPath() }
                            .shuffled()
                            .forEach { players.addAudio(it) }
                    }
                }
            }, BorderLayout.WEST)

            bottomPanel.add(bottomSubPanel, BorderLayout.WEST)
            bottomPanel.add(bottomMainPanel, BorderLayout.CENTER)

            add(topPanel, BorderLayout.NORTH)
            add(songListInfoPanel, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }

        private fun searchPageTab() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            var keywords = ""
            var currentPage = 0
            val label = JBLabel(Bundles.message("lable.search"))
            val input = JBTextField(40)
            val searchBtn = JButton(Bundles.message("button.search"))
            val songData = ArrayDeque<SongSearchResponse.SongInfo>(30)
            val songsModel = object : DataTableModel(arrayOf(
                Bundles.message("table.search.header.0"),
                Bundles.message("table.search.header.1"),
                Bundles.message("table.search.header.2"),
                Bundles.message("table.search.header.3"),
                Bundles.message("table.search.header.4")
            ),
                { rowIndex, columnIndex ->
                    val data = songData[rowIndex - 1]
                    when (columnIndex) {
                        0 -> data.name
                        1 -> data.id.toString()
                        2 -> data.artists.map { it.name }.toString()
                        3 -> data.duration.toLong().msToFormattedTime()
                        4 -> data.alias.contentToString()
                        else -> data.id.toString()
                    }
                }) {
                override fun dataRowCount(): Int = songData.size
            }
            val table = JBTable(songsModel)

            table.tableHeader.preferredHeight = 0
            searchBtn.addActionListener {
                keywords = input.text
                if (keywords.isNotEmpty()) {
                    currentPage = 0
                    val json = Bundles.CLOUD_MUSIC_API.searchRequest(keywords)
                    try {
                        val result = Bundles.GSON.fromJson(json, SongSearchResponse::class.java)
                        songData.clear()
                        songData.addAll(result.result.songs)
                    } catch (npe: NullPointerException) {
                        thisLogger().warn(npe)
                        // do nothing
                    } finally {
                        table.updateUI()
                    }
                }
            }
            table.addMouseListener(object : MouseInputAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.button == BUTTON3) {
                        val row = table.rowAtPoint(Point(e.x, e.y))
                        if (row == 0)
                            return

                        JBPopupFactory.getInstance()
                            .createPopupChooserBuilder(itemsInSearch)
                            .setItemChosenCallback {
                                itemsInSearch.indexOf(it).takeIf { index -> index != -1 }?.let { index ->
                                    searchCallBack[index].invoke(it, songData[row - 1])
                                }
                            }
                            .createPopup()
                            .show(RelativePoint(e))
                    }
                }
            })
            table.autoscrolls = true
            table.cellSelectionEnabled = true
            table.fillsViewportHeight = true
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

            val topSubPanel = JBPanel<JBPanel<*>>(BorderLayout())
            topSubPanel.add(label, BorderLayout.WEST)
            topSubPanel.add(input, BorderLayout.CENTER)
            topSubPanel.add(searchBtn, BorderLayout.EAST)

            val bottomSubPanel = JBPanel<JBPanel<*>>()
            val firstPage = JButton(Bundles.message("button.page.first")).apply {
                addActionListener {
                    if (keywords.isNotEmpty()) {
                        currentPage = 0
                        val json =
                            Bundles.CLOUD_MUSIC_API.searchRequest(
                                keywords,
                                SearchApi.SearchType.SINGLE_SONG,
                                30,
                                currentPage
                            )
                        try {
                            val result = Bundles.GSON.fromJson(json, SongSearchResponse::class.java)
                            songData.clear()
                            songData.addAll(result.result.songs)
                        } catch (npe: NullPointerException) {
                            thisLogger().warn(npe)
                            // do nothing
                        } finally {
                            table.updateUI()
                        }
                    }
                }
            }
            val prevPage = JButton(Bundles.message("button.page.prev")).apply {
                addActionListener {
                    if (keywords.isNotEmpty()) {
                        if (currentPage == 0)
                            return@addActionListener
                        val json =
                            Bundles.CLOUD_MUSIC_API.searchRequest(
                                keywords,
                                SearchApi.SearchType.SINGLE_SONG,
                                30,
                                --currentPage
                            )
                        try {
                            val result = Bundles.GSON.fromJson(json, SongSearchResponse::class.java)
                            songData.clear()
                            songData.addAll(result.result.songs)
                        } catch (npe: NullPointerException) {
                            thisLogger().warn(npe)
                            // do nothing
                        } finally {
                            table.updateUI()
                        }
                    }
                }
            }
            val nextPage = JButton(Bundles.message("button.page.next")).apply {
                addActionListener {
                    if (keywords.isNotEmpty()) {
                        val json =
                            Bundles.CLOUD_MUSIC_API.searchRequest(
                                keywords,
                                SearchApi.SearchType.SINGLE_SONG,
                                30,
                                ++currentPage
                            )
                        try {
                            val result = Bundles.GSON.fromJson(json, SongSearchResponse::class.java)
                            songData.clear()
                            songData.addAll(result.result.songs)
                        } catch (npe: NullPointerException) {
                            thisLogger().warn(npe)
                            // do nothing
                        } finally {
                            table.updateUI()
                        }
                    }
                }
            }
            bottomSubPanel.add(firstPage)
            bottomSubPanel.add(prevPage)
            bottomSubPanel.add(nextPage)

            add(topSubPanel, BorderLayout.NORTH)
            add(
                JBScrollPane(
                    table,
                    JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                ), BorderLayout.CENTER
            )
            add(bottomSubPanel, BorderLayout.SOUTH)
            updateUI()
        }

        fun content() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            root.addTab(Bundles.message("tab.search"), searchPageTab())
            root.setToolTipTextAt(0, Bundles.message("tab.search.tooltip"))
            root.addTab(Bundles.message("tab.info"), songInfoTab())
            root.setToolTipTextAt(1, Bundles.message("tab.info.tooltip"))
            root.addTab(Bundles.message("tab.lists"), songListsTab())
            root.setToolTipTextAt(2, Bundles.message("tab.lists.tooltip"))
            root.addTab(Bundles.message("tab.player"), playerTab())
            root.setToolTipTextAt(3, Bundles.message("tab.player.tooltip"))
            root.addTab(Bundles.message("tab.cache.songs"), cachedSongsTab())
            root.setToolTipTextAt(4, Bundles.message("tab.cache.songs.tooltip"))

            add(root, BorderLayout.CENTER)
        }
    }
}