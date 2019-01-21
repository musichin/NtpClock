package com.github.musichin.ntpclock

import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class NtpSyncTask private constructor(
    private val pool: String,
    private val version: Int,
    private val servers: Int,
    private val samples: Int,
    private val executor: Executor
) {
    companion object {
        private fun NtpResponse.isCandidate(): Boolean {
            return stratum < 15
                    && rootDelayInMillis <= 1_500
                    && leapIndicator != NtpResponse.LI_ALARM_CONDITION
        }

        private fun MutableList<(NtpStamp) -> Unit>.dispatch(stamp: NtpStamp, clear: Boolean = true) {
            val copy = toList()
            if (clear) clear()
            copy.forEach { it(stamp) }
        }

        @JvmOverloads
        @JvmStatic
        fun sync(
            pool: String,
            version: Int = 3,
            servers: Int = 4,
            samples: Int = 4,
            executor: Executor = Executors.newFixedThreadPool(servers)
        ): NtpSyncTask {
            return NtpSyncTask(
                pool = pool,
                version = version,
                servers = servers,
                samples = samples,
                executor = executor
            )
        }
    }

    @get:Synchronized
    var stamp: NtpStamp? = null
        private set

    @get:Synchronized
    var complete: Boolean = false
        private set

    private val firstCallbacks = mutableListOf<(NtpStamp) -> Unit>()
    private val lastCallbacks = mutableListOf<(NtpStamp) -> Unit>()
    private val nextCallbacks = mutableListOf<(NtpStamp) -> Unit>()

    private var success = 0
    private var executing = 0
    private var results = mutableMapOf<InetAddress, MutableList<NtpResponse>>()
    private val remainingServers = mutableListOf<InetAddress>()


    init {
        execute()
    }

    @Throws(UnknownHostException::class)
    private fun execute() {
        val addresses = InetAddress.getAllByName(pool)

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

        if (remainingServers.isNotEmpty() && (success + executing) < servers) {
            val address = remainingServers.removeAt(0)
            executing++
            executor.run {
                NtpClient.request(
                    address = address,
                    version = version,
                    samples = samples
                ) {
                    onResponse(address, it)
                }

                onComplete(address)
            }
        }

        loop()
    }

    @Synchronized
    private fun onResponse(address: InetAddress, response: NtpResponse) {
        results.getOrPut(address) { mutableListOf() }.add(response)

        next(calculateStamp())
    }

    @Synchronized
    private fun onComplete(address: InetAddress) {
        if (results[address]?.size == samples) {
            success++
        }

        executing--

        loop()
    }

    private fun calculateStamp(): NtpStamp? {
        return results.values
            .fold(emptyList<NtpResponse>()) { acc, res -> acc.plus(res) }
            .filter { it.isCandidate() }
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
    private fun next(stamp: NtpStamp? = this.stamp, done: Boolean = false) {
        if (stamp == null) return
        if (this.stamp == stamp) return

        this.stamp = stamp

        firstCallbacks.dispatch(stamp)
        nextCallbacks.dispatch(stamp, false)
    }

    @Synchronized
    fun onFirst(cb: (NtpStamp) -> Unit): NtpSyncTask {
        val stamp = this.stamp
        if (stamp != null) {
            cb(stamp)
        } else if (!complete) {
            firstCallbacks.add(cb)
        }

        return this
    }

    @Synchronized
    fun onLast(cb: (NtpStamp) -> Unit): NtpSyncTask {
        if (complete) {
            stamp?.let(cb)
        } else {
            lastCallbacks.add(cb)
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
    fun onComplete(cb: (NtpStamp, List<Int>) -> Unit): NtpSyncTask {
        // FIXME
        return this
    }

    @Synchronized
    fun onError(cb: (Exception) -> Unit): NtpSyncTask {
        // FIXME
        return this
    }
}
