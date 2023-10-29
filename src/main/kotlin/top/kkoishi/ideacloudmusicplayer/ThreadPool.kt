package top.kkoishi.ideacloudmusicplayer

import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayDeque

object ThreadPool {
    @JvmStatic
    private val threadIndex = AtomicInteger(0)

    @JvmStatic
    private val scheduledTasks = ArrayDeque<String>()

    @JvmStatic
    private val POOL = ScheduledThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 2
    ) { task ->
        Thread(
            Thread.currentThread().threadGroup,
            task,
            "kkoishi.cm.player-${threadIndex.getAndIncrement()}"
        )
    }

    @JvmStatic
    fun task(name: String = UUID.randomUUID().toString(), task: Runnable) {
        if (!scheduledTasks.contains(name)) {
            scheduledTasks.add(name)
            POOL.schedule(task, 0L, TimeUnit.MICROSECONDS)
        }
    }

    @JvmStatic
    fun task(period: Long, name: String = UUID.randomUUID().toString(), task: Runnable) {
        task(name) {
            while (true) {
                task.run()
                Thread.sleep(period)
            }
        }
    }
}