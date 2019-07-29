package de.musichin.ntpclock.date

import de.musichin.ntpclock.NtpClock
import java.util.Date

fun NtpClock.date(): Date? = stampOrNull()?.date()

fun NtpClock.dateOrNull(): Date? = stampOrNull()?.date()

fun NtpClock.dateOrElse(default: Date): Date = stampOrNull()?.date() ?: default

fun NtpClock.dateOrElse(default: () -> Date): Date = stampOrNull()?.date() ?: default()

fun NtpClock.dateOrSystemValue(): Date = stampOrNull()?.date() ?: Date(System.currentTimeMillis())
