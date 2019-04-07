package com.github.musichin.ntpclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetAddress

@RunWith(RobolectricTestRunner::class)
class NtpClientTest {

    @Test
    fun testRequestTimeout() {
        val responses = NtpClient.request(InetAddress.getByName("localhost"))
        assertTrue(responses.isEmpty())
    }

    @Test
    fun testRequest() {
        val address = InetAddress.getByName("de.pool.ntp.org")
        val res = NtpClient.request(address, samples = 10)
        assertTrue(res.isNotEmpty())
        assertEquals(10, res.size)
    }
}
