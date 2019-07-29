package de.musichin.ntpclock

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RequiresApi(Build.VERSION_CODES.O)
fun NtpStamp.instant(): Instant = Instant.ofEpochMilli(time)

@RequiresApi(Build.VERSION_CODES.O)
fun NtpStamp.clock(zone: ZoneId): Clock = RealtimeClock(zone, this)

@RequiresApi(Build.VERSION_CODES.O)
fun NtpStamp.clockUTC(): Clock = RealtimeClock(ZoneOffset.UTC, this)

@RequiresApi(Build.VERSION_CODES.O)
fun NtpStamp.clockDefaultZone(): Clock = RealtimeClock(ZoneId.systemDefault(), this)
