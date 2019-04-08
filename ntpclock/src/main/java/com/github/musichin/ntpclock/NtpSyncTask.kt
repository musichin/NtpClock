package com.github.musichin.ntpclock

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

        private fun MutableList<(NtpStamp) -> Unit>.dispatch(stamp: NtpStamp, clear: Boolean) {
            val copy = toList()
            if (clear) clear()
            copy.forEach { it(stamp) }
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

    private val firstCallbacks = mutableListOf<(NtpStamp) -> Unit>()
    private val nextCallbacks = mutableListOf<(NtpStamp) -> Unit>()
    private val successCallbacks = mutableListOf<(NtpStamp) -> Unit>()
    private val errorCallbacks = mutableListOf<(Exception) -> Unit>()
    private val completeCallbacks = mutableListOf<(NtpStamp?, Exception?) -> Unit>()

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

//    @Synchronized
//    private fun onDone() {
//        val stamp = this.stamp
//
//        if (stamp != null) {
//            firstCallbacks.dispatch(stamp)
//            lastCallbacks.dispatch(stamp)
//        }
//        firstCallbacks.clear()
//        nextCallbacks.clear()
//        lastCallbacks.clear()
//        // FIXME complete callbacks
//    }

    @Synchronized
    private fun next(stamp: NtpStamp? = this.stamp, done: Boolean = false, cause: Exception? = null) {
        if (this.stamp != stamp && stamp != null) {
            this.stamp = stamp
            firstCallbacks.dispatch(stamp, true)
            nextCallbacks.dispatch(stamp, false)
        }

        val error = cause ?: if (done && stamp == null) errors.values.firstOrNull() else null

        if (done) {
            complete(stamp, error)
        }
    }

    @Synchronized
    private fun complete(stamp: NtpStamp?, cause: Exception?) {
        this.complete = true
        this.error = cause

        if (stamp == null) {
            errorCallbacks.forEach { it(requireNotNull(cause)) }
            errorCallbacks.clear()
            successCallbacks.clear()
        } else {
            successCallbacks.dispatch(stamp, false)
            successCallbacks.clear()
            errorCallbacks.clear()
        }

        completeCallbacks.forEach {
            it(stamp, cause)
        }

        completeCallbacks.clear()
        successCallbacks.clear()
        errorCallbacks.clear()
        firstCallbacks.clear()
        nextCallbacks.clear()
    }

    @Synchronized
    fun onReady(cb: (NtpStamp) -> Unit): NtpSyncTask {
        val stamp = this.stamp
        if (stamp != null) {
            cb(stamp)
        } else if (!complete) {
            firstCallbacks.add(cb)
        }

        return this
    }

    @Synchronized
    fun onNext(cb: (NtpStamp) -> Unit): NtpSyncTask {
        if (!complete) {
            nextCallbacks.add(cb)
        }
        stamp?.let(cb)

        return this
    }

    @Synchronized
    fun onComplete(cb: (NtpStamp?, Exception?) -> Unit): NtpSyncTask {
        if (complete) {
            cb(stamp, error)
        } else {
            completeCallbacks.add(cb)
        }

        return this
    }

    @Synchronized
    fun onSuccess(cb: (NtpStamp) -> Unit): NtpSyncTask {
        if (!complete) {
            successCallbacks.add(cb)
        } else {
            stamp?.let(cb)
        }

        return this
    }

    @Synchronized
    fun onError(cb: (Exception) -> Unit): NtpSyncTask {
        if (!complete) {
            errorCallbacks.add(cb)
        } else {
            error?.let(cb)
        }
        return this
    }
}
