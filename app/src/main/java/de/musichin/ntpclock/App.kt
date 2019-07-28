package de.musichin.ntpclock

import android.app.Application
import android.os.Build
import de.musichin.ntpclock.NtpClock
import de.musichin.ntpclock.SharedPreferencesNtpStorage

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NtpClock.storage = SharedPreferencesNtpStorage(this).cached()
            NtpClock.syncTask()
        }
    }
}
