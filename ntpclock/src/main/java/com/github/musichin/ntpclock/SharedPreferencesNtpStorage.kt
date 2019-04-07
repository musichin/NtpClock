package com.github.musichin.ntpclock

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class SharedPreferencesNtpStorage(
    private val context: Context,
    val name: String = "com.github.musichin.ntpclock"
) : NtpStorage() {
    companion object {
        private const val KEY_BOOT_COUNT = "boot_id"
        private const val KEY_POOL = "pool"
        private const val KEY_REALTIME = "elapsedRealtime"
        private const val KEY_NTP_TIME = "ntp_time"

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.N)
        fun bootCount(context: Context): Int {
            return bootCount(context.contentResolver)
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.N)
        fun bootCount(contentResolver: ContentResolver): Int {
            return Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT)
        }
    }

    private val sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private val bootCount by lazy {
        bootCount(context)
    }

    override fun set(stamp: NtpStamp?) {
        sharedPreferences.edit()
            .clear()
            .apply {
                if (stamp != null) {
                    putLong(KEY_REALTIME, stamp.elapsedRealtime)
                    putLong(KEY_NTP_TIME, stamp.time)
                    putString(KEY_POOL, stamp.pool)
                    putInt(KEY_BOOT_COUNT, bootCount)
                }
            }
            .apply()
    }

    override fun get(): NtpStamp? {
        val bootCount =
            if (sharedPreferences.contains(KEY_BOOT_COUNT))
                sharedPreferences.getInt(KEY_BOOT_COUNT, 0)
            else
                return null
        if (bootCount != this.bootCount) return null

        val pool = sharedPreferences.getString(KEY_POOL, null) ?: return null
        val realtime =
            if (sharedPreferences.contains(KEY_REALTIME))
                sharedPreferences.getLong(KEY_REALTIME, 0)
            else
                return null

        val ntpTime =
            if (sharedPreferences.contains(KEY_NTP_TIME))
                sharedPreferences.getLong(KEY_NTP_TIME, 0)
            else
                return null

        return NtpStamp(pool, ntpTime, realtime)
    }
}
