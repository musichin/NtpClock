package de.musichin.ntpclock

import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NtpClock {
    private var runningTask: NtpSyncTask? = null

    private val syncListeners = mutableListOf<(NtpSyncTask) -> Unit>()

    @get:Synchronized
    @set:Synchronized
    @get:JvmStatic
    @set:JvmStatic
    var storage: NtpStorage = InMemoryNtpStorage().cached()

    @Synchronized
    @JvmOverloads
    @JvmStatic
    fun sync(
        pool: String = "pool.ntp.org",
        version: Int = 3,
        servers: Int = 4,
        samples: Int = 4,
        timeout: Int = 15_000,
        onComplete: (stamp: NtpStamp?, case: Throwable?) -> Unit
    ) {
        sync(pool, version, servers, samples, timeout).onComplete(onComplete)
    }

    @Synchronized
    @JvmOverloads
    @JvmStatic
    fun sync(
        pool: String = "pool.ntp.org",
        version: Int = 3,
        servers: Int = 4,
        samples: Int = 4,
        timeout: Int = 15_000
    ): NtpSyncTask {
        return runningTask ?: createTask(pool, version, servers, samples, timeout)
    }

    private fun createTask(
        pool: String = "pool.ntp.org",
        version: Int = 3,
        servers: Int = 4,
        samples: Int = 4,
        timeout: Int = 15_000
    ): NtpSyncTask = synchronized(this) {
        if (runningTask != null) throw IllegalStateException("Sync already in progress")
        val task = NtpSyncTask.sync(pool, version, servers, samples, timeout)
        runningTask = task
        task.onComplete(null) { _, _ ->
            synchronized(this) {
                runningTask = null
            }
        }
        syncListeners.toList().forEach { listener -> listener(task) }
        task
    }

    /**
     * Return `true` when sync is in progress otherwise `false`.
     */
    @Synchronized
    @JvmStatic
    fun isSyncing(): Boolean = runningTask != null

    /**
     * Returns currently running [NtpSyncTask] or `null` when no sync is in progress.
     */
    @Synchronized
    @JvmStatic
    fun syncTask(): NtpSyncTask? = runningTask

    @Synchronized
    @JvmStatic
    fun addOnSyncListener(listener: (NtpSyncTask) -> Unit) {
        addOnSyncListener(Handler(), listener)
    }

    @Synchronized
    @JvmStatic
    fun addOnSyncListener(handler: Handler?, listener: (NtpSyncTask) -> Unit) {
        val realListener: (NtpSyncTask) -> Unit =
            if (handler == null)
                listener
            else
                { task -> handler.post { listener(task) } }
        syncListeners.add(realListener)
        runningTask?.let(listener)
    }

    @JvmStatic
    @Synchronized
    fun removeOnSyncListener(listener: (NtpSyncTask) -> Unit) {
        syncListeners.remove(listener)
    }

    @Synchronized
    @JvmStatic
    fun reset() {
        if (runningTask != null) throwSyncInProgress()
        storage.stamp = null
    }

    @JvmStatic
    fun stamp(): NtpStamp = stampOrNull() ?: throwNotSynced()

    @JvmStatic
    fun stampOrNull(): NtpStamp? = storage.stamp ?: synchronized(this) { storage.stamp }

    @JvmStatic
    fun millis(): Long? = stamp().millis()

    @JvmStatic
    fun millisOrNull(): Long? = stampOrNull()?.millis()

    @JvmStatic
    fun millisOrElse(default: Long): Long = stampOrNull()?.millis() ?: default

    @JvmStatic
    fun millisOrElse(default: () -> Long): Long = stampOrNull()?.millis() ?: default()

    @JvmStatic
    fun millisOrSystemValue(): Long = stampOrNull()?.millis() ?: System.currentTimeMillis()

    @JvmStatic
    fun millisAt(elapsedRealtime: Long): Long = stamp().millisAt(elapsedRealtime)

    @JvmStatic
    fun millisAtOrNull(elapsedRealtime: Long): Long? = stampOrNull()?.millisAt(elapsedRealtime)

    private fun throwNotSynced(): Nothing = throw IllegalStateException("Not synchronized")
    private fun throwSyncInProgress(): Nothing = throw IllegalStateException("Sync in progress")
}
