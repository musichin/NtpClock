package com.github.musichin.ntpclock

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private var syncing = false

    private val _time = MutableLiveData<NtpStamp?>()
    private val _loading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<Event<Throwable>?>()
    private val _timeChanges = TimeChangedLiveData(application)

    val clock: LiveData<Long> = combineLatest(_time, _timeChanges).switchMap { (stamp) -> stamp?.let(::TimeLiveData) }

    val offset: LiveData<Long?> = combineLatest(_time, _timeChanges) { stamp, _ ->
        stamp?.offset()
    }

    val loading: LiveData<Boolean> = _loading
    val error: LiveData<Event<Throwable>?> = _error

    init {
        _loading.value = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NtpClock.storage = SharedPreferencesNtpStorage(application).cached()

            NtpClock.stampOrNull()?.let { _time.value = it }
        }
    }

    fun sync() {
        if (syncing) return
        syncing = true
        _loading.value = true
        NtpClock.sync("pool.ntp.org")
            .onReady { if (_time.value == null) _time.value = it }
            .onNext { println("offset ${it.offset()}") }
            .onComplete { stamp, cause ->
                stamp?.let { _time.value = it }
                cause?.let { _error.value = Event(it) }

                syncing = false
                _loading.value = false
                println("sync completed with: ${stamp ?: cause}")
            }
    }
}
