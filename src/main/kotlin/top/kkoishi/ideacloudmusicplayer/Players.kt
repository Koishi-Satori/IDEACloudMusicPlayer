package top.kkoishi.ideacloudmusicplayer

import com.intellij.openapi.util.io.toCanonicalPath
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.io.InputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import kotlin.io.path.exists
import kotlin.io.path.inputStream

class Players private constructor() : Runnable {
    private val lock = Any()
    private val playQueue = ArrayDeque<Path>()
    private val player = AudioPlayer()
    private val end = AtomicBoolean(false)
    private var cur: String = ""

    fun addAudio(audioPath: Path) = synchronized(lock) {
        if (audioPath.exists()) {
            playQueue.addFirst(audioPath)
            println("Add Audio: $audioPath")
        }
    }

    fun clear() = synchronized(lock) {
        playQueue.clear()
    }

    fun contains(audioPath: Path): Boolean = synchronized(lock) {
        return playQueue.contains(audioPath)
    }

    fun end() = synchronized(lock) {
        player.end()
        end.set(true)
    }

    fun keep() {
        end.set(false)
    }

    fun replay() = synchronized(lock) {
        player.replay()
    }

    fun current(): String = synchronized(lock) {
        return cur
    }

    fun currentMetaData(): MutableMap<String, String> = synchronized(lock) {
        return player.metaData()
    }

    fun playList(): Array<Path> = synchronized(lock) {
        return playQueue.toTypedArray()
    }

    fun isStop(): Boolean = synchronized(lock) {
        return player.isStop()
    }

    fun stop() = synchronized(lock) {
        player.stop()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun isAdjusting(): Boolean = !progress().isNaN() || length() != -1L

    fun progress(): Double = synchronized(lock) {
        return player.progress()
    }

    fun length(): Long = synchronized(lock) {
        return player.length()
    }

    override fun run() {
        player.end()
        while (true) {
            if (end.get()) {
                player.end()
                continue
            }
            var p: Path? = null
            synchronized(lock) {
                if (!playQueue.isEmpty() && player.isEnd()) {
                    p = playQueue.removeFirst()
                    playQueue.addLast(p!!)
                    cur = p!!.toCanonicalPath()
                }
            }
            if (p != null)
                player.play(p!!.inputStream())
        }
    }

    companion object {
        @JvmStatic
        private val PLAYERS = Players()

        init {
            ThreadPool.task(task = PLAYERS)
        }

        fun getInstance() = PLAYERS
    }

    class AudioPlayer {
        private lateinit var grabber: FFmpegFrameGrabber
        private var format: AudioFormat? = null
        private lateinit var line: SourceDataLine
        private lateinit var info: DataLine.Info
        private lateinit var buf: Array<Buffer>
        private lateinit var leftData: FloatBuffer
        private lateinit var rightData: FloatBuffer
        private lateinit var iLeftData: ShortBuffer
        private lateinit var iRightData: ShortBuffer
        private lateinit var tLeftData: ByteBuffer
        private lateinit var tRightData: ByteBuffer
        private var volume = 0.75f
        private var sampleFormat: Int = 0
        private lateinit var tl: ByteArray
        private lateinit var tr: ByteArray
        private lateinit var combine: ByteArray
        private var stop = false
        private var end = false

        fun isEnd(): Boolean {
            return end
        }

        fun end() {
            if (this::grabber.isInitialized || end) {
                grabber.close()
                line.close()
            }
            end = true
        }

        fun stop() {
            stop = true
        }

        fun replay() {
            stop = false
        }

        fun isStop(): Boolean {
            return stop
        }

        fun length(): Long {
            return if (!this::grabber.isInitialized || end)
                -1L
            else
                try {
                    grabber.lengthInTime
                } catch (npe: NullPointerException) {
                    -1L
                }
        }

        fun progress(): Double {
            return if (!this::line.isInitialized || end)
                Double.NaN
            else
                line.longFramePosition * (1000000.0 / line.format.sampleRate)
        }

        fun metaData(): MutableMap<String, String> {
            return if (!this::grabber.isInitialized || end) {
                mutableMapOf()
            } else {
                grabber.metadata
            }
        }

        fun play(ins: InputStream) {
            grabber = FFmpegFrameGrabber(ins)
            val sec = 60
            var frame: Frame?

            end = false
            stop = false
            grabber.start()
            grabber.timestamp = sec * 1000000L
            sampleFormat = grabber.sampleFormat
            initializeSourceDataLine(grabber)
            grabber.restart()

            while (true) {
                if (isEnd()) {
                    break
                }
                if (isStop()) {
                    Thread.sleep(10)
                    continue
                }
                frame = grabber.grabSamples()
                if (frame == null) {
                    grabber.stop()
                    break
                }
                processAudio(frame.samples)
            }
            end()
            grabber.close()
            line.close()
            ins.close()
        }

        private fun initializeSourceDataLine(grabber: FFmpegFrameGrabber) {
            when (grabber.sampleFormat) {
                // most sound card might will not support this type.
                avutil.AV_SAMPLE_FMT_S32P -> {
                    format = AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        grabber.sampleRate.toFloat(),
                        32,
                        grabber.audioChannels,
                        grabber.audioChannels * 2,
                        grabber.sampleRate.toFloat(),
                        true
                    )
                }

                avutil.AV_SAMPLE_FMT_FLT, avutil.AV_SAMPLE_FMT_FLTP, avutil.AV_SAMPLE_FMT_S16P, avutil.AV_SAMPLE_FMT_S16 -> {
                    format = AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        grabber.sampleRate.toFloat(),
                        16,
                        grabber.audioChannels,
                        grabber.audioChannels * 2,
                        grabber.sampleRate.toFloat(),
                        true
                    )
                }

                avutil.AV_SAMPLE_FMT_U8, avutil.AV_SAMPLE_FMT_U8P, avutil.AV_SAMPLE_FMT_S32,
                avutil.AV_SAMPLE_FMT_DBL, avutil.AV_SAMPLE_FMT_DBLP,
                avutil.AV_SAMPLE_FMT_S64, avutil.AV_SAMPLE_FMT_S64P,
                -> {
                    // just do nothing.
                }

                else -> throw RuntimeException("unsupported audio sample format: ${grabber.sampleFormat}")
            }

            info = DataLine.Info(SourceDataLine::class.java, format, AudioSystem.NOT_SPECIFIED)
            try {
                line = AudioSystem.getLine(info) as SourceDataLine
                line.open(format)
                line.start()
            } catch (lue: LineUnavailableException) {
                lue.printStackTrace()
            }
        }

        private fun processAudio(samples: Array<Buffer>) {
            var k: Int
            buf = samples

            when (sampleFormat) {
                avutil.AV_SAMPLE_FMT_FLTP -> {
                    leftData = buf[0] as FloatBuffer
                    tLeftData = floatToByteBuffer(leftData, volume)
                    rightData = buf[1] as FloatBuffer
                    tRightData = floatToByteBuffer(rightData, volume)
                    tl = tLeftData.array()
                    tr = tRightData.array()
                    combine = ByteArray(tl.size + tr.size)
                    k = 0

                    // mix two sound channel
                    for (i in tl.indices step 2) {
                        // this will be faster than loop.
                        combine[4 * k] = tl[i]
                        combine[4 * k + 2] = tr[i]
                        combine[1 + 4 * k] = tl[i + 1]
                        combine[1 + 4 * k + 2] = tr[i + 1]
                        ++k
                    }
                    line.write(combine, 0, combine.size)
                }

                avutil.AV_SAMPLE_FMT_S16 -> {
                    iLeftData = buf[0] as ShortBuffer
                    tLeftData = shortToByteBuffer(iLeftData, volume)
                    tl = tLeftData.array()
                    line.write(tl, 0, tl.size)
                }

                avutil.AV_SAMPLE_FMT_FLT -> {
                    iLeftData = buf[0] as ShortBuffer
                    iRightData = buf[0] as ShortBuffer
                    tLeftData = shortToByteBuffer(iLeftData, volume)
                    tRightData = shortToByteBuffer(iRightData, volume)
                    tl = tLeftData.array()
                    tr = tRightData.array()

                    k = 0
                    // mix two sound channel
                    for (i in tl.indices step 2) {
                        // this will be faster than loop.
                        combine[4 * k] = tl[i]
                        combine[4 * k + 2] = tr[i]
                        combine[1 + 4 * k] = tl[i + 1]
                        combine[1 + 4 * k + 2] = tr[i + 1]
                        ++k
                    }
                }

                else -> throw RuntimeException("Unsupported audio format")
            }
        }

        companion object {
            @JvmStatic
            fun shortToByteBuffer(buf: ShortBuffer, vol: Float): ByteBuffer {
                val len = buf.capacity()
                val res = ByteBuffer.allocate(2 * len)

                (0 until len).forEach {
                    res.putShort(2 * it, (buf[it] * vol).toInt().toShort())
                }

                return res
            }

            @JvmStatic
            fun floatToByteBuffer(buf: FloatBuffer, vol: Float): ByteBuffer {
                val len = buf.capacity()
                var f: Float
                val v: Float = 32768 * vol
                val res = ByteBuffer.allocate(2 * len)

                (0 until len).forEach {
                    f = buf[it] * v
                    if (f > v)
                        f = v
                    if (f < -v)
                        f = v

                    // big endian
                    res.putShort(2 * it, f.toInt().toShort())
                }

                return res
            }
        }
    }
}