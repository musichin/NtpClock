package de.musichin.ntpclock.calendar

import android.os.Build
import java.util.Locale

internal fun calendarLocale() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Locale.getDefault(Locale.Category.FORMAT)
        else Locale.getDefault()
