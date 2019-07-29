package de.musichin.ntpclock.calendar

import de.musichin.ntpclock.NtpStamp
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun NtpStamp.calendar(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = calendarLocale()
): Calendar = Calendar.getInstance(zone, locale).apply { timeInMillis = millis() }
