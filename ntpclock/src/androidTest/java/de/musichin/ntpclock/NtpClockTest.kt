package de.musichin.ntpclock

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NtpClockTest {

    @Test
    fun testSync() {
        val latch = CountDownLatch(1)
        Thread {
            NtpClock.sync("de.pool.ntp.org") { _, _ ->
                latch.countDown()
            }
        }.start()

        assertTrue(latch.await(30, TimeUnit.SECONDS))
        assertNotNull(NtpClock.stamp())
    }
}
