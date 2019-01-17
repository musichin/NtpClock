package com.github.musichin.ntpclock

import android.os.SystemClock
import java.io.Serializable

data class NtpStamp(
    val pool: String,
    val ntpTime: Long,
    val realtime: Long
) : Serializable {
    fun now(): Long = ntpTime + SystemClock.elapsedRealtime() - realtime
}
