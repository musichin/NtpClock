package de.musichin.ntpclock.threetenbp

import de.musichin.ntpclock.NtpClock
import org.threeten.bp.Instant

fun NtpClock.instant(): Instant? =
    stamp().instant()

fun NtpClock.instantOrNull(): Instant? =
    stampOrNull()?.instant()

fun NtpClock.instantOrElse(default: Instant): Instant =
    stampOrNull()?.instant() ?: default

fun NtpClock.instantOrElse(default: () -> Instant): Instant =
    stampOrNull()?.instant() ?: default()

fun NtpClock.instantOrSystemValue(): Instant =
    stampOrNull()?.instant() ?: Instant.ofEpochMilli(System.currentTimeMillis())
