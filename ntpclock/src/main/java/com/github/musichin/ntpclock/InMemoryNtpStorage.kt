package com.github.musichin.ntpclock

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class InMemoryNtpStorage : NtpStorage() {
    private var stamp: NtpStamp? = null
    private val lock = ReentrantReadWriteLock()

    override fun set(stamp: NtpStamp?) {
        lock.writeLock().withLock {
            this.stamp = stamp
        }
    }

    override fun get(): NtpStamp? {
        return lock.readLock().withLock {
            this.stamp
        }
    }
}
