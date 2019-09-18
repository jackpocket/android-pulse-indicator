package com.jackpocket.pulse.test

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.jackpocket.pulse.layouts.PulsingLinearLayout
import com.jackpocket.pulse.test.R

class MainActivity: AppCompatActivity() {

    private val pulseLayout: PulsingLinearLayout
        get() = findViewById(R.id.main__pulse_layout)

    private val pulseTarget: AppCompatImageView
        get() = findViewById(R.id.main__pulse_target_image)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    fun onTestPulseClicked(view: View?) {
        Log.d(TAG, "Pulse starting...")

        pulseLayout.pulseController
                .setCirclePathOverride(true)
                .setPulsingColor(0x22FF22)
                .setPulsingStrokeWidth(10)
                .setDurationMs(1500)
                .setPulseLifeSpanMs(900)
                .setRespawnRateMs(300)
                .setAlphaInterpolator(AccelerateInterpolator())
                .setScaleInterpolator(LinearInterpolator())
                .setFinishedListener({  _ ->
                    Log.d(TAG, "Pulse completed.")
                })
                .attachTo(this, pulseTarget)
    }

    fun onStopPulseClicked(view: View?) {
        Log.d(TAG, "Stopping pulse...")

        pulseLayout.pulseController.stopPulsing()
    }

    fun onSuspendPulseClicked(view: View?) {
        Log.d(TAG, "Stopping pulse...")

        pulseLayout.pulseController.suspendPulseCreation()
    }

    companion object {

        const val TAG = "PulseIndicatorTest"
    }
}