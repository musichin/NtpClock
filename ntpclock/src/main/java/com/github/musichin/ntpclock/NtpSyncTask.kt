package com.github.musichin.ntpclock

import android.os.Handler
import java.net.InetAddress
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class NtpSyncTask private constructor(
    private val pool: String,
    private val version: Int,
    private val servers: Int,
    private val samples: Int,
    private val timeout: Int,
    private val executor: (() -> Unit) -> Unit
) {
    companion object {
        @JvmOverloads
        @JvmStatic
        fun sync(
            pool: String = "pool.ntp.org",
            version: Int = 3,
            servers: Int = 4,
            samples: Int = 4,
            timeout: Int = 15_000
        ): NtpSyncTask {
            val executor = Executors.newFixedThreadPool(servers)

            return sync(pool, version, servers, samples, timeout, executor).onComplete { _, _ -> executor.shutdown() }
        }

        @JvmOverloads
        @JvmStatic
        fun sync(
            pool: String,
            version: Int = 3,
            servers: Int = 4,
            samples: Int = 4,
            timeout: Int = 15_000,
            executor: Executor
        ) = NtpSyncTask(pool, version, servers, samples, timeout) { executor.execute(it) }


        private fun NtpResponse.isCandidate(): Boolean {
            return stratum < 15
                    && rootDelayInMillis <= 1_500
                    && leapIndicator != NtpResponse.LI_ALARM_CONDITION
        }

        private fun List<NtpResponse>.calculateStamp(pool: String): NtpStamp? {
            return filter { it.isCandidate() }
                .sortedBy { it.transmissionDelay }
                .firstOrNull()
                ?.let {
                    NtpStamp(
                        pool,
                        it.destinationTimestamp + it.offset,
                        it.elapsedRealtime
                    )
                }
        }
    }

    @get:Synchronized
    var stamp: NtpStamp? = null
        private set

    @get:Synchronized
    var complete: Boolean = false
        private set

    @get:Synchronized
    var error: Exception? = null
        private set

    private val listeners = mutableListOf<Listener>()

    private var results = mutableMapOf<InetAddress, MutableList<NtpResponse>>()
    private var errors = mutableMapOf<InetAddress, Exception>()

    private var addresses: MutableList<InetAddress> = mutableListOf()
    private var executing = 0

    init {
        execute()
    }

    private fun execute() = executor {
        try {
            InetAddress.getAllByName(pool).toCollection(addresses)

            repeat(kotlin.math.min(servers, addresses.size)) { executeNext() }
        } catch (cause: Exception) {
            next(done = true, cause = cause)
        }
    }

    @Synchronized
    private fun executeNext() {
        val address = addresses.removeAt(0)
        executing++
        request(address,
            {
                // received ntp packet
                onReqResponse(address, it)
            },
            {
                // request success
                onReqSuccess()
            },
            {
                // request failed
                onReqException(address, it)
            })
    }

    @Synchronized
    private fun onReqResponse(address: InetAddress, response: NtpResponse) {
        results.getOrPut(address) { mutableListOf() }.add(response)

        next(stamp = calculateStamp())
    }

    @Synchronized
    private fun onReqSuccess() {
        executing--

        if (executing == 0) {
            next(done = true)
        }
    }

    @Synchronized
    private fun onReqException(address: InetAddress, cause: Exception) {
        errors[address] = cause
        executing--

        if (addresses.isNotEmpty()) {
            executeNext()
        } else if (executing == 0) {
            next(done = true)
        }
    }

    private fun request(
        address: InetAddress,
        next: (NtpResponse) -> Unit,
        done: () -> Unit,
        error: (Exception) -> Unit
    ) = executor {
        try {
            NtpClient.request(
                address = address,
                version = version,
                samples = samples,
                timeout = timeout,
                onResponse = next
            )
        } catch (cause: Exception) {
            return@executor error(cause)
        }

        done()
    }

    private fun calculateStamp(): NtpStamp? =
        results.values.fold(emptyList<NtpResponse>()) { acc, res -> acc.plus(res) }.calculateStamp(pool)

    @Synchronized
    private fun next(stamp: NtpStamp? = this.stamp, done: Boolean = false, cause: Exception? = null) {
        if (this.complete) return

        val oldStamp = this.stamp
        val newStamp = stamp ?: oldStamp
        this.stamp = stamp ?: oldStamp
        val complete = done || cause != null
        this.complete = complete
        val error =
            if (complete && newStamp == null) cause ?: errors.values.firstOrNull() ?: IllegalStateException() else null
        this.error = error


        // dispatch listeners
        // first / ready
        if (oldStamp == null && newStamp != null) {
            listeners.forEach { it.onReady(newStamp) }
        }

        // next
        if (newStamp != null && oldStamp != newStamp) {
            listeners.forEach { it.onNext(newStamp) }
        }

        // success / error / complete
        if (complete) {
            if (newStamp != null) {
                listeners.forEach { it.onSuccess(newStamp) }
            }

            if (error != null) {
                listeners.forEach { it.onError(error) }
            }
            listeners.forEach { it.onComplete(newStamp, error) }
            listeners.clear()
        }
    }

    @Synchronized
    fun onReady(cb: (NtpStamp) -> Unit) = onReady(Handler(), cb)

    @Synchronized
    fun onReady(handler: Handler?, cb: (NtpStamp) -> Unit): NtpSyncTask {
        addListener(handler, object : Listener {
            override fun onReady(stamp: NtpStamp) = cb(stamp)
        })

        return this
    }

    @Synchronized
    fun onNext(cb: (NtpStamp) -> Unit) = onNext(Handler(), cb)

    @Synchronized
    fun onNext(handler: Handler?, cb: (NtpStamp) -> Unit): NtpSyncTask {
        addListener(handler, object : Listener {
            override fun onNext(stamp: NtpStamp) = cb(stamp)
        })

        return this
    }

    @Synchronized
    fun onComplete(cb: (NtpStamp?, Exception?) -> Unit) = onComplete(Handler(), cb)

    @Synchronized
    fun onComplete(handler: Handler?, cb: (NtpStamp?, Exception?) -> Unit): NtpSyncTask {
        addListener(handler, object : Listener {
            override fun onComplete(stamp: NtpStamp?, cause: Exception?) = cb(stamp, cause)
        })

        return this
    }

    @Synchronized
    fun onSuccess(cb: (NtpStamp) -> Unit) = onSuccess(Handler(), cb)

    @Synchronized
    fun onSuccess(handler: Handler?, cb: (NtpStamp) -> Unit): NtpSyncTask {
        addListener(handler, object : Listener {
            override fun onSuccess(stamp: NtpStamp) = cb(stamp)
        })

        return this
    }

    @Synchronized
    fun onError(cb: (Exception) -> Unit) = onError(Handler(), cb)

    @Synchronized
    fun onError(handler: Handler?, cb: (Exception) -> Unit): NtpSyncTask {
        addListener(handler, object : Listener {
            override fun onError(cause: Exception) = cb(cause)
        })

        return this
    }

    @Synchronized
    fun addListener(listener: Listener) = addListener(Handler(), listener)

    @Synchronized
    fun addListener(handler: Handler?, listener: Listener): NtpSyncTask {
        insertOrDispatchListener(if (handler == null) listener else ListenerWrapper(handler, listener))

        return this
    }

    @Synchronized
    private fun insertOrDispatchListener(listener: Listener) {
        stamp?.let(listener::onReady)
        stamp?.let(listener::onNext)

        if (complete) {
            error?.let(listener::onError)
            stamp?.let(listener::onSuccess)
            listener.onComplete(stamp, error)
        } else {
            listeners.add(listener)
        }
    }

    interface Listener {
        fun onError(cause: Exception) = Unit

        fun onSuccess(stamp: NtpStamp) = Unit

        fun onComplete(stamp: NtpStamp?, cause: Exception?) = Unit

        fun onNext(stamp: NtpStamp) = Unit

        fun onReady(stamp: NtpStamp) = Unit
    }

    private class ListenerWrapper(private val handler: Handler, private val delegate: Listener) : Listener {
        override fun onError(cause: Exception) {
            handler.post { delegate.onError(cause) }
        }

        override fun onSuccess(stamp: NtpStamp) {
            handler.post { delegate.onSuccess(stamp) }
        }

        override fun onComplete(stamp: NtpStamp?, cause: Exception?) {
            handler.post { delegate.onComplete(stamp, cause) }
        }

        override fun onNext(stamp: NtpStamp) {
            handler.post { delegate.onNext(stamp) }
        }

        override fun onReady(stamp: NtpStamp) {
            handler.post { delegate.onReady(stamp) }
        }
    }
}
