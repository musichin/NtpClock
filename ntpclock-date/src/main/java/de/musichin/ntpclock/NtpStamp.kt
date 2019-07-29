package de.musichin.ntpclock

import java.util.Date

fun NtpStamp.date(): Date = Date(time)
