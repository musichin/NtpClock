package de.musichin.ntpclock.threetenbp

import de.musichin.ntpclock.NtpStamp
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset

fun NtpStamp.instant(): Instant = Instant.ofEpochMilli(millis())

fun NtpStamp.clock(zone: ZoneId): Clock = RealtimeClock(zone, this)

fun NtpStamp.clockUTC(): Clock =
    RealtimeClock(ZoneOffset.UTC, this)

fun NtpStamp.clockDefaultZone(): Clock =
    RealtimeClock(ZoneId.systemDefault(), this)
