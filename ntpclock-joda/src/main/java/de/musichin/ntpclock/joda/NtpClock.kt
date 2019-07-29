package de.musichin.ntpclock.joda

import de.musichin.ntpclock.NtpClock
import org.joda.time.DateTime

fun NtpClock.dateTime(): DateTime? =
    stamp().dateTime()

fun NtpClock.dateTimeOrNull(): DateTime? =
    stampOrNull()?.dateTime()

fun NtpClock.dateTimeOrElse(default: DateTime): DateTime =
    stampOrNull()?.dateTime() ?: default

fun NtpClock.dateTimeOrElse(default: () -> DateTime): DateTime =
    stampOrNull()?.dateTime() ?: default()

fun NtpClock.dateTimeOrSystemValue(): DateTime =
    stampOrNull()?.dateTime() ?: DateTime.now()
