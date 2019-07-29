package de.musichin.ntpclock.threetenbp

import de.musichin.ntpclock.NtpStamp
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId

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
