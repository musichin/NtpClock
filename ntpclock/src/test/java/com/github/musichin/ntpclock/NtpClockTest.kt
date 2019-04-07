package com.github.musichin.ntpclock

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 21)
class NtpClockTest {

    @Test
    fun testSync() {
        val latch = CountDownLatch(1)
        Thread {
            NtpClock.sync("de.pool.ntp.org", executor = { block -> Thread(block).start() }) {
                latch.countDown()
            }
        }.start()

//        assertTrue(latch.await(30, TimeUnit.SECONDS))
        Thread.sleep(9999999)
        assertNotNull(NtpClock.stamp())
    }
}
