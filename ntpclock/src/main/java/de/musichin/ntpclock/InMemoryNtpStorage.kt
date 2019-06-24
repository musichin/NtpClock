package de.musichin.ntpclock

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class InMemoryNtpStorage : NtpStorage() {
    private val lock = ReentrantReadWriteLock()

    override var stamp: NtpStamp? = null
        get() = lock.readLock().withLock { field }
        set(value) = lock.writeLock().withLock {
            field = value
        }
}
