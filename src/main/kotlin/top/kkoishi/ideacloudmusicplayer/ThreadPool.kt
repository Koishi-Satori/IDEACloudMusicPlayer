package top.kkoishi.ideacloudmusicplayer

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object ThreadPool {
    @JvmStatic
    private val threadIndex = AtomicInteger(0)

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
    fun task(task: Runnable) {
        POOL.schedule(task, 0L, TimeUnit.MICROSECONDS)
    }

    @JvmStatic
    fun task(period: Long, task: Runnable) {
        task {
            while (true) {
                task.run()
                Thread.sleep(period)
            }
        }
    }
}