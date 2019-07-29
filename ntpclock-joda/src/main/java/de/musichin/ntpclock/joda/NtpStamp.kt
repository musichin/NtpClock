package de.musichin.ntpclock.joda

import de.musichin.ntpclock.NtpStamp
import org.joda.time.Chronology
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

fun NtpStamp.dateTime(): DateTime = DateTime(millis())

fun NtpStamp.dateTime(chronology: Chronology): DateTime = DateTime(millis(), chronology)

fun NtpStamp.dateTime(zone: DateTimeZone): DateTime = DateTime(millis(), zone)
