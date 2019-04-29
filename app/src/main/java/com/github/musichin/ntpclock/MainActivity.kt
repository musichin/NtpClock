package com.github.musichin.ntpclock

import android.os.Bundle
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ClockViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(ClockViewModel::class.java)

        refresh.setOnRefreshListener {
            viewModel.sync()
        }

        viewModel.clock.observe(this) { millis ->
            date.text = DateFormat.getLongDateFormat(this).format(Date(millis))

            val hour24 = DateFormat.is24HourFormat(this)
            val pattern = if (hour24) "HH:mm:ss" else "hh:mm:ss"
            time.text = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
        }

        viewModel.offset.observe(this) {
            offset.text = it.toString()
        }

        viewModel.loading.observe(this) {
            refresh.isRefreshing = it == true
        }

        viewModel.error.observe(this) {
            val cause = it?.consume()

            if (cause != null) {
                Snackbar.make(
                    main_container,
                    cause.localizedMessage ?: cause.message ?: cause.javaClass.simpleName,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.sync()
    }
}
