package de.musichin.ntpclock.date

import de.musichin.ntpclock.NtpStamp
import java.util.Date

fun NtpStamp.date(): Date = Date(millis())
