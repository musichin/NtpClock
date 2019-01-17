package com.github.musichin.ntpclock

import android.os.SystemClock
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NtpClient {
    private const val TAG = "NtpClient"

    private const val NTP_PORT = 123
    private const val NTP_TIMEOUT = 15_000
    private const val NTP_PACKET_SIZE = 48

    private const val VERSION_3 = 3
    private const val VERSION_4 = 4
    private const val MODE_CLIENT = 3

    private const val DEFAULT_SAMPLES = 4

    private const val MSB_0_BASE = 2085978496000L
    private const val MSB_1_BASE = -2208988800000L

    @JvmStatic
    private fun Long.toNtpTime(): Long {
        val msb = this < MSB_0_BASE
        val baseTime = this - if (msb) MSB_1_BASE else MSB_0_BASE
        val seconds = (baseTime / 1000L) or if (msb) 0x0L else 0x80000000L
        val fraction = baseTime % 1000 * 0x100000000L / 1000

        return seconds shl 32 or fraction
    }

    @JvmStatic
    private fun Long.fromNtpTime(): Long {
        val seconds = (this ushr 32) and 0xffffffffL
        val fraction = this and 0xffffffffL
        val millis = Math.round(1000.0 * fraction / 0x100000000L)

        val msb = seconds and 0x80000000L
        val base = if (msb == 0L) MSB_0_BASE else MSB_1_BASE
        return base + seconds * 1000L + millis
    }

    @JvmOverloads
    @JvmStatic
    fun request(
        address: InetAddress,
        port: Int = NTP_PORT,
        version: Int = VERSION_3,
        samples: Int = DEFAULT_SAMPLES,
        timeout: Int = NTP_TIMEOUT,
        onResponse: (NtpResponse) -> Unit
    ) {
        if (version != VERSION_3 && version != VERSION_4)
            throw IllegalArgumentException("Version $version is unsupported")

        val socket = DatagramSocket().apply {
            soTimeout = timeout
        }

        val buffer = ByteArray(NTP_PACKET_SIZE)
        val byteBuffer = ByteBuffer.wrap(buffer)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        val req = DatagramPacket(buffer, buffer.size, address, port)

        // samples loop
        try {
            for (sample in 0 until samples) {
                (0 until NTP_PACKET_SIZE).forEach { index -> byteBuffer.put(index, 0) }
                byteBuffer.put(0, ((VERSION_3 shl 3) or MODE_CLIENT).toByte())

                val startedAt = System.currentTimeMillis()
                val initiatedTs = startedAt.toNtpTime()
                val started = SystemClock.elapsedRealtime()
                byteBuffer.putLong(40, initiatedTs)

                socket.send(req)
                socket.receive(req)
                val completed = SystemClock.elapsedRealtime()
                val duration = completed - started

                // read response data
                val leapIndicator = byteBuffer.get(0).toInt() ushr 6
                val versionNumber = (byteBuffer.get(0).toInt() ushr 3) and 0x07
                val mode = byteBuffer.get(0).toInt() and 0x07
                val stratum = byteBuffer.get(1).toInt()
                val poll = byteBuffer.get(2).toInt()
                val precision = byteBuffer.get(3).toInt()
                val rootDelay = byteBuffer.getInt(4)
                val rootDispersion = byteBuffer.getInt(8)
                val referenceIdentifier = byteBuffer.getInt(12)
                val referenceTs = byteBuffer.getLong(16)
                val originateTs = byteBuffer.getLong(24) // T1
                val receiveTs = byteBuffer.getLong(32) // T2
                val transmitTs = byteBuffer.getLong(40) // T3
                val destinationTs = startedAt + duration // T4

                if (originateTs != initiatedTs) {
                    Log.w(TAG, "Received NTP packet, but the originate timestamp didn't match.")
                    break
                }

                val response = NtpResponse(
                    leapIndicator,
                    versionNumber,
                    mode,
                    stratum,
                    poll,
                    precision,
                    rootDelay,
                    rootDispersion,
                    referenceIdentifier,
                    referenceTs.fromNtpTime(),
                    originateTs.fromNtpTime(),
                    receiveTs.fromNtpTime(),
                    transmitTs.fromNtpTime(),
                    destinationTs,
                    completed
                )
                onResponse(response)
            }
        } catch (cause: SocketTimeoutException) {
            Log.w(TAG, cause)
        }
        socket.close()
    }

    @JvmOverloads
    @JvmStatic
    fun request(
        address: InetAddress,
        port: Int = NTP_PORT,
        version: Int = VERSION_3,
        samples: Int = DEFAULT_SAMPLES,
        timeout: Int = NTP_TIMEOUT
    ): List<NtpResponse> {
        val results = mutableListOf<NtpResponse>()
        request(address, port, version, samples, timeout) {
            results.add(it)
        }

        return results
    }
}
