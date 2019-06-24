package de.musichin.ntpclock

data class NtpResponse(
    val leapIndicator: Int,
    val versionNumber: Int,
    val mode: Int,
    val stratum: Int,
    val poll: Int,
    val precision: Int,
    val rootDelay: Int,
    val rootDispersion: Int,
    val referenceIdentifier: Int,
    val referenceTimestamp: Long,
    val originateTimestamp: Long, // T1
    val receiveTimestamp: Long, // T2
    val transmitTimestamp: Long, // T3
    val destinationTimestamp: Long, // T4
    val elapsedRealtime: Long
) {
    companion object {
        const val LI_NO_WARNING = 0
        const val LI_LAST_MINUTE_HAS_61_SECONDS = 1
        const val LI_LAST_MINUTE_HAS_59_SECONDS = 2
        const val LI_ALARM_CONDITION = 3

        const val VERSION_3 = 3
        const val VERSION_4 = 4

        const val MODE_RESERVED = 0
        const val MODE_SYMMETRIC_ACTIVE = 1
        const val MODE_SYMMETRIC_PASSIVE = 2
        const val MODE_CLIENT = 3
        const val MODE_SERVER = 4
        const val MODE_BROADCAST = 5
        const val MODE_CONTROL_MESSAGE = 6
        const val MODE_PRIVATE = 7

        const val MIN_POLL = 6
        const val MAX_POLL = 10
    }

    val rootDelayInMillis = (rootDelay * 1_000L) / 65_536
    val rootDispersionInMillis = (rootDispersion * 1_000L) / 65_536
    val transmissionDelay = (destinationTimestamp - originateTimestamp) - (transmitTimestamp - receiveTimestamp)
    val offset = ((receiveTimestamp - originateTimestamp) + (transmitTimestamp - destinationTimestamp)) / 2
    val duration = destinationTimestamp - originateTimestamp
}
