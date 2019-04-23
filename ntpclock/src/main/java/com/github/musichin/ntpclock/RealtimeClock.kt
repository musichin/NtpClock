package com.github.musichin.ntpclock

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
internal class RealtimeClock(
    private val zone: ZoneId,
    private val stamp: NtpStamp
) : Clock() {
    override fun withZone(zone: ZoneId): Clock {
        return RealtimeClock(zone, stamp)
    }

    override fun getZone(): ZoneId {
        return zone
    }

    override fun instant(): Instant {
        return stamp.instant()
    }

    override fun millis(): Long {
        return stamp.millis()
    }
}
