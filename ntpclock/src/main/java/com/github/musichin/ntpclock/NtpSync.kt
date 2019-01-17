package com.github.musichin.ntpclock

import java.net.InetAddress
import java.net.UnknownHostException

internal class NtpSync(
    private val pool: String,
    private val version: Int,
    private val servers: Int,
    private val samples: Int,
    private val executor: (block: () -> Unit) -> Unit,
    private val onChange: (stamp: NtpStamp) -> Unit,
    private val onDone: (samples: List<Int>, stamp: NtpStamp?) -> Unit
) {
    private var success = 0
    private var executing = 0
    private var results = mutableMapOf<InetAddress, MutableList<NtpResponse>>()
    private val remainingServers = mutableListOf<InetAddress>()

    private var stamp: NtpStamp? = null

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
            onDone(results.values.map { it.size }, stamp)
        }

        if (executing == servers) return
        if (remainingServers.isEmpty()) return

        if (remainingServers.isNotEmpty() && (success + executing) < servers) {
            val address = remainingServers.removeAt(0)
            executing++
            executor {
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
    private fun onComplete(address: InetAddress) {
        if (results[address]?.size == samples) {
            success++
        }

        executing--

        loop()
    }

    @Synchronized
    private fun onResponse(address: InetAddress, response: NtpResponse) {
        results.getOrPut(address) { mutableListOf() }.add(response)

        val oldStamp = stamp
        stamp = calculateStamp()
        if (oldStamp != stamp) {
            stamp?.let { onChange(it) }
        }
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

    private fun NtpResponse.isCandidate(): Boolean {
        return stratum < 15
                && rootDelayInMillis <= 1_500
                && leapIndicator != NtpResponse.LI_ALARM_CONDITION
    }
}
