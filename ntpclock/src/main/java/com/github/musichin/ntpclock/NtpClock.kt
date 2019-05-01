package com.github.musichin.ntpclock

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NtpClock {
    private var runningTask: NtpSyncTask? = null

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
    ): NtpSyncTask {
        val task = NtpSyncTask.sync(pool, version, servers, samples, timeout).onComplete { _, _ ->
            synchronized(this) {
                runningTask = null
            }
        }

        runningTask = task

        return task
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
    fun date(): Date? = stampOrNull()?.date()

    @JvmStatic
    fun dateOrNull(): Date? = stampOrNull()?.date()

    @JvmStatic
    fun dateOrElse(default: Date): Date = stampOrNull()?.date() ?: default

    @JvmStatic
    fun dateOrElse(default: () -> Date): Date = stampOrNull()?.date() ?: default()

    @JvmStatic
    fun dateOrSystemValue(): Date = stampOrNull()?.date() ?: Date(System.currentTimeMillis())


    @JvmStatic
    @JvmOverloads
    fun calendar(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Locale.getDefault(Locale.Category.FORMAT)
            else Locale.getDefault()
    ): Calendar = stamp().calendar()

    @JvmStatic
    @JvmOverloads
    fun calendarOrNull(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Locale.getDefault(Locale.Category.FORMAT)
            else Locale.getDefault()
    ): Calendar? = stampOrNull()?.calendar()

    @JvmStatic
    @JvmOverloads
    fun calendarOrDefault(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Locale.getDefault(Locale.Category.FORMAT)
            else Locale.getDefault(),
        default: Calendar
    ): Calendar = stampOrNull()?.calendar() ?: default

    @JvmStatic
    @JvmOverloads
    fun calendarOrDefault(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Locale.getDefault(Locale.Category.FORMAT)
            else Locale.getDefault(),
        default: () -> Calendar
    ): Calendar = stampOrNull()?.calendar() ?: default()

    @JvmStatic
    @JvmOverloads
    fun calendarOrSystemValue(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Locale.getDefault(Locale.Category.FORMAT)
            else Locale.getDefault()
    ): Calendar = stampOrNull()?.calendar() ?: Calendar.getInstance(zone, locale)


    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun instant(): Instant? = stamp().instant()

    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun instantOrNull(): Instant? = stampOrNull()?.instant()

    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun instantOrElse(default: Instant): Instant = stampOrNull()?.instant() ?: default

    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun instantOrElse(default: () -> Instant): Instant = stampOrNull()?.instant() ?: default()

    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun instantOrSystemValue(): Instant = stampOrNull()?.instant() ?: Instant.ofEpochMilli(System.currentTimeMillis())


    @JvmStatic
    fun millisAt(elapsedRealtime: Long): Long = stamp().millisAt(elapsedRealtime)

    @JvmStatic
    fun millisAtOrNull(elapsedRealtime: Long): Long? = stampOrNull()?.millisAt(elapsedRealtime)


//    fun atOrNull(realtime: Long): Long? = stampOrNull()?.calculateAt(realtime)
//    fun atOrElse(realtime: Long, default: Long): Long = stampOrNull()?.calculateAt(realtime) ?: default
//    fun atOrElse(realtime: Long, default: () -> Long): Long = stampOrNull()?.calculateAt(realtime) ?: default()
//
//    fun atDateOrNull(realtime: Long): Date? = stampOrNull()?.calculateAt(realtime)?.let(::Date)
//    fun atDateOrElse(realtime: Long, default: Date): Date = stampOrNull()?.calculateAt(realtime)?.let(::Date) ?: default
//    fun atDateOrElse(realtime: Long, default: () -> Date): Date =
//        stampOrNull()?.calculateAt(realtime)?.let(::Date) ?: default()
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun atInstantOrNull(realtime: Long): Instant? = stampOrNull()?.calculateAt(realtime)?.let(Instant::ofEpochMilli)
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun atInstantOrElse(realtime: Long, default: Instant): Instant =
//        stampOrNull()?.calculateAt(realtime)?.let(Instant::ofEpochMilli) ?: default
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun atInstantOrNull(realtime: Long, default: () -> Instant): Instant =
//        stampOrNull()?.calculateAt(realtime)?.let(Instant::ofEpochMilli) ?: default()

    private fun throwNotSynced(): Nothing = throw IllegalStateException("Not synchronized")
    private fun throwSyncInProgress(): Nothing = throw IllegalStateException("Sync in progress")
}
