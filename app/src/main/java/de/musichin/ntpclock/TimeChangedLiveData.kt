package de.musichin.ntpclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.lifecycle.LiveData

class TimeChangedLiveData(private val context: Context) : LiveData<Long>() {
    private val timeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            value = SystemClock.elapsedRealtime()
        }
    }

    private val timeBroadcastFilter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }

    override fun onActive() {
        value = SystemClock.elapsedRealtime()
        context.registerReceiver(timeBroadcastReceiver, timeBroadcastFilter)
    }

    override fun onInactive() {
        context.unregisterReceiver(timeBroadcastReceiver)
    }
}
