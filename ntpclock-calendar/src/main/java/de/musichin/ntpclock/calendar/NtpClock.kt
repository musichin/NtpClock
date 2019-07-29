package de.musichin.ntpclock.calendar

import de.musichin.ntpclock.NtpClock
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun NtpClock.calendar(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = calendarLocale()
): Calendar = stamp().calendar(zone, locale)

fun NtpClock.calendarOrNull(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = calendarLocale()
): Calendar? = stampOrNull()?.calendar(zone, locale)

fun NtpClock.calendarOrDefault(
    zone: TimeZone = TimeZone.getDefault(),
    locale: Locale = calendarLocale(),
    default: Calendar
): Calendar = stampOrNull()?.calendar(zone, locale) ?: default

fun NtpClock.calendarOrDefault(
    zone: TimeZone = TimeZone.getDefault(),
    locale: Locale = calendarLocale(),
    default: (zone: TimeZone, locale: Locale) -> Calendar
): Calendar = stampOrNull()?.calendar(zone, locale) ?: default(zone, locale)

fun NtpClock.calendarOrSystemValue(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = calendarLocale()
): Calendar = stampOrNull()?.calendar(zone, locale) ?: Calendar.getInstance(zone, locale)
