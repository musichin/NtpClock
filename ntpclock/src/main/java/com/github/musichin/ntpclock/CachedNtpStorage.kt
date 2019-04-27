package com.github.musichin.ntpclock

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class CachedNtpStorage(private val delegate: NtpStorage) : NtpStorage() {
    private val lock = ReentrantReadWriteLock()

    override var stamp: NtpStamp? = lock.readLock().withLock { delegate.stamp }
        get() = lock.readLock().withLock { field }
        set(value) = lock.writeLock().withLock {
            field = value
            delegate.stamp = value
        }
}
