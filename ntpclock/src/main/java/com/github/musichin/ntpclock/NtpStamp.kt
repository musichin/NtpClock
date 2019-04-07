package com.github.musichin.ntpclock

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import androidx.annotation.RequiresApi
import java.io.Serializable
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class NtpStamp(
    val pool: String,
    val time: Long,
    val elapsedRealtime: Long
) : Serializable, Parcelable {
    constructor(parcel: Parcel) : this(
        requireNotNull(parcel.readString()),
        parcel.readLong(),
        parcel.readLong()
    )

    companion object CREATOR : Parcelable.Creator<NtpStamp> {
        override fun createFromParcel(parcel: Parcel): NtpStamp {
            return NtpStamp(parcel)
        }

        override fun newArray(size: Int): Array<NtpStamp?> {
            return arrayOfNulls(size)
        }
    }

    fun millis(): Long = millisAt(SystemClock.elapsedRealtime())

    fun date(): Date = Date(time)

    @JvmOverloads
    fun calendar(
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Locale.getDefault(Locale.Category.FORMAT)
            else Locale.getDefault()
    ): Calendar = Calendar.getInstance(zone, locale).apply { timeInMillis = millis() }

    @RequiresApi(Build.VERSION_CODES.O)
    fun instant(): Instant = Instant.ofEpochMilli(time)

    fun millisAt(elapsedRealtime: Long): Long = this.time + (elapsedRealtime - this.elapsedRealtime)

    //
//    fun atDate(realtime: Long): Date = Date(stamp().calculateAt(realtime))
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun atInstant(realtime: Long): Instant = Instant.ofEpochMilli(stamp().calculateAt(realtime))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pool)
        parcel.writeLong(time)
        parcel.writeLong(elapsedRealtime)
    }

    override fun describeContents(): Int {
        return 0
    }
}
