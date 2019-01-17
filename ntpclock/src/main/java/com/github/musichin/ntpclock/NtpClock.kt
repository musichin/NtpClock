package com.github.musichin.ntpclock

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.util.Date

object NtpClock {

    private val lock = Any()

    private var stamp: NtpStamp? = null

    private var sync: NtpSync? = null

    @JvmStatic
    fun sync(
        pool: String,
        version: Int = 3,
        servers: Int = 4,
        samples: Int = 4,
        storage: NtpStorage? = null,
        executor: (block: () -> Unit) -> Unit = { block -> block() },
        onDone: ((pool: String, samples: List<Int>, stamp: NtpStamp?) -> Unit)? = null,
        onChange: ((stamp: NtpStamp?) -> Unit)? = null,
        onReady: ((stamp: NtpStamp?) -> Unit)? = null
    ) {
        synchronized(lock) {
            if (sync != null) throw IllegalStateException("Synchronization already in progress")

            var ready: (stamp: NtpStamp?) -> Unit = { stamp ->
                onReady?.invoke(stamp)
            }

            val change: (stamp: NtpStamp?) -> Unit = { stamp ->
                this@NtpClock.stamp = stamp
                ready(stamp)
                ready = {}
                onChange?.invoke(stamp)
            }

            val done: (List<Int>, stamp: NtpStamp?) -> Unit = { samples, stamp ->
                change(stamp)
                storage?.set(stamp)
                onDone?.invoke(pool, samples, stamp)
            }

            storage?.get()?.let { stamp ->
                change(stamp)
            }

            sync = NtpSync(pool, version, servers, samples, executor, { stamp ->
                synchronized(lock) { change(stamp) }
            }, { samples, stamp ->
                synchronized(lock) { done(samples, stamp) }
            })
        }
    }

    @JvmStatic
    fun stamp(): NtpStamp? = stamp ?: synchronized(lock) { stamp }

    @JvmStatic
    fun requireStamp(): NtpStamp = stamp ?: throwNotSynced()

    @JvmStatic
    fun now(): Long? = stamp?.now()

    @JvmStatic
    fun requireNow(): Long = now() ?: throwNotSynced()

    @JvmStatic
    fun date(): Date? = now()?.let { Date(it) }

    @JvmStatic
    fun requireDate(): Date = date() ?: throwNotSynced()

    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.O)
    fun instant(): Instant? = now()?.let { Instant.ofEpochMilli(it) }

    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.O)
    fun requireInstant(): Instant = instant() ?: throwNotSynced()

    private fun throwNotSynced(): Nothing =
        throw IllegalStateException("NtpClock is not synchronized")
}
