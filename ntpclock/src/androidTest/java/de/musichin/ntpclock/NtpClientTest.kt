package de.musichin.ntpclock

import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertTrue

class NtpClientTest {

    @Test(expected = SocketTimeoutException::class)
    fun testRequestTimeout() {
        NtpClient.request(InetAddress.getByName("localhost"))
    }

    @Test
    fun testRequest() {
        val address = InetAddress.getByName("de.pool.ntp.org")
        val res = mutableListOf<NtpResponse>()
        try {
            NtpClient.request(address, samples = 10) {
                res.add(it)
            }
        } catch (cause: SocketTimeoutException) {
            // ignore
        }
        assertTrue(res.isNotEmpty())
    }
}
