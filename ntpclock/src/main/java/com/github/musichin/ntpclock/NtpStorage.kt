package com.github.musichin.ntpclock

abstract class NtpStorage {
    abstract fun set(stamp: NtpStamp?)
    abstract fun get(): NtpStamp?
}
