package com.github.musichin.ntpclock

import android.app.Application
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NtpClock.storage = SharedPreferencesNtpStorage(this).cached()
        }
    }
}
