package de.musichin.ntpclock

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import java.io.Serializable

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pool)
        parcel.writeLong(time)
        parcel.writeLong(elapsedRealtime)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun offset(): Long = System.currentTimeMillis() - millis()

    fun millis(): Long = millisAt(SystemClock.elapsedRealtime())

    fun millisAt(elapsedRealtime: Long): Long = this.time + (elapsedRealtime - this.elapsedRealtime)
}
