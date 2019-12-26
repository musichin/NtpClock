package de.musichin.ntpclock

internal object Defaults {
    const val POOL = "pool.ntp.org"
    const val PORT = 123
    const val VERSION = NtpResponse.VERSION_3
    const val TIMEOUT = 15_000
    const val SAMPLES = 4
    const val SERVERS = 4
}
