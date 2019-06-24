package com.github.musichin.ntpclock

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.LiveData
import de.musichin.ntpclock.NtpStamp

class TimeLiveData(
    private val elapsedRealtime: Long,
    private val time: Long
) : LiveData<Long>() {

    constructor(stamp: NtpStamp) : this(stamp.elapsedRealtime, stamp.time)

    private val handler = Handler(Looper.getMainLooper())
    private var active = false

    private fun millis() = time + (SystemClock.elapsedRealtime() - elapsedRealtime)

    override fun onActive() {
        active = true
        update()
    }

    override fun onInactive() {
        active = false
        handler.removeCallbacks(::update)
    }

    private fun update() {
        if (!active) return
        value = millis()
        handler.removeCallbacks(::update)
        val delay = 1_000L - (millis() % 1_000L)
        handler.postDelayed(::update, delay)
    }
}
