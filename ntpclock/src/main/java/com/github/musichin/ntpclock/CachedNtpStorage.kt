package com.github.musichin.ntpclock

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class CachedNtpStorage(private val delegate: NtpStorage) : NtpStorage() {
    private var stamp: NtpStamp? = null
    private val lock = ReentrantReadWriteLock()

    init {
        lock.readLock().withLock {
            stamp = delegate.get()
        }
    }

    override fun set(stamp: NtpStamp?) {
        lock.writeLock().withLock {
            this.stamp = stamp
            delegate.set(stamp)
        }
    }

    override fun get(): NtpStamp? {
        return lock.readLock().withLock { stamp }
    }
}
