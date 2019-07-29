package de.musichin.ntpclock.time

import android.os.Build
import androidx.annotation.RequiresApi
import de.musichin.ntpclock.NtpClock
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
fun NtpClock.instant(): Instant? =
    stamp().instant()

@RequiresApi(Build.VERSION_CODES.O)
fun NtpClock.instantOrNull(): Instant? =
    stampOrNull()?.instant()

@RequiresApi(Build.VERSION_CODES.O)
fun NtpClock.instantOrElse(default: Instant): Instant =
    stampOrNull()?.instant() ?: default

@RequiresApi(Build.VERSION_CODES.O)
fun NtpClock.instantOrElse(default: () -> Instant): Instant =
    stampOrNull()?.instant() ?: default()

@RequiresApi(Build.VERSION_CODES.O)
fun NtpClock.instantOrSystemValue(): Instant =
    stampOrNull()?.instant() ?: Instant.ofEpochMilli(System.currentTimeMillis())
