package com.github.musichin.ntpclock

abstract class NtpStorage {
    abstract var stamp: NtpStamp?

    fun cached(): NtpStorage = if (this is CachedNtpStorage) this else CachedNtpStorage(this)
}
