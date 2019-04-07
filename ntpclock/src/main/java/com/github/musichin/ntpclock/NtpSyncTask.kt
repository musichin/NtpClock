package com.github.musichin.ntpclock

import java.net.InetAddress
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class NtpSyncTask private constructor(
    private val pool: String,
    private val version: Int,
    private val servers: Int,
    private val samples: Int,
    private val executor: (() -> Unit) -> Unit
) {
    companion object {
        @JvmOverloads
        @JvmStatic
        fun sync(
            pool: String,
            version: Int = 3,
            servers: Int = 4,
            samples: Int = 4
        ): NtpSyncTask {
            val executor = Executors.newFixedThreadPool(servers)

            return sync(pool, version, servers, samples, executor)
                .onComplete { _, _ ->
                    executor.shutdown()
                }
        }

        @JvmOverloads
        @JvmStatic
        fun sync(
            pool: String,
            version: Int = 3,
            servers: Int = 4,
            samples: Int = 4,
            executor: Executor
        ) = sync(pool, version, servers, samples, executor::execute)

        @JvmOverloads
        @JvmStatic
        fun sync(
            pool: String,
            version: Int = 3,
            servers: Int = 4,
            samples: Int = 4,
            executor: (() -> Unit) -> Unit
        ) = NtpSyncTask(pool = pool, version = version, servers = servers, samples = samples, executor = executor)

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

    private var success = 0
    private var executing = 0
    private var results = mutableMapOf<InetAddress, MutableList<NtpResponse>>()
    private var errors = mutableMapOf<InetAddress, Exception>()
    private val remainingServers = mutableListOf<InetAddress>()


    init {
        execute()
    }

    private fun execute() {
        val addresses = try {
            InetAddress.getAllByName(pool)
        } catch (cause: Exception) {
            return next(cause = cause)
        }

        remainingServers.addAll(addresses)

        loop()
    }

    @Synchronized
    private fun loop() {
        if ((remainingServers.isEmpty() || success == servers) && executing == 0) {
            return next(done = true)
        }

        if (executing == servers) return
        if (remainingServers.isEmpty()) return

        while (remainingServers.isNotEmpty() && (success + executing) < servers) {
            val address = remainingServers.removeAt(0)
            executing++
            executor {
                request(address, {
                    onReqResponse(address, it)
                }, {
                    onReqComplete(address)
                }, {
                    onReqException(address, it)
                })
            }
        }
    }

    private fun request(
        address: InetAddress,
        next: (NtpResponse) -> Unit,
        done: () -> Unit,
        error: (Exception) -> Unit
    ) {
        try {
            NtpClient.request(
                address = address,
                version = version,
                samples = samples,
                onResponse = next
            )
        } catch (cause: Exception) {
            error(cause)
        }

        done()
    }

    @Synchronized
    private fun onReqResponse(address: InetAddress, response: NtpResponse) {
        results.getOrPut(address) { mutableListOf() }.add(response)

        next(stamp = calculateStamp())
    }

    @Synchronized
    private fun onReqComplete(address: InetAddress) {
        if (results[address]?.size == samples) {
            success++
        }

        executing--

        loop()
    }

    @Synchronized
    private fun onReqException(address: InetAddress, cause: Exception) {
        // FIXME
        errors[address] = cause
        executing--

        loop()
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
    private fun next(stamp: NtpStamp? = null, done: Boolean = false, cause: Exception? = null) {
        if (stamp == null) return
        if (this.stamp == stamp) return

        this.stamp = stamp

        firstCallbacks.dispatch(stamp, true)
        nextCallbacks.dispatch(stamp, done)

        if (done) {
            successCallbacks.dispatch(stamp, true)
        }
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
    fun onComplete(cb: (NtpStamp?, Exception) -> Unit): NtpSyncTask {
        TODO()
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
