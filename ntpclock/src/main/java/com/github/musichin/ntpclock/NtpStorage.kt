package com.github.musichin.ntpclock

abstract class NtpStorage {
    abstract var stamp: NtpStamp?

    fun cached(): NtpStorage = CachedNtpStorage(this)
}
