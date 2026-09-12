package com.albumcatalog.app.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.albumcatalog.app.VinylCatalogApp

class FeedbackManager(private val context: Context = VinylCatalogApp.instance) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    /**
     * 1. Lock Detection Cue:
     * Distinct tactile mechanical click vibration when the camera locks onto a barcode or vinyl label.
     */
    fun onScanLocked() {
        if (!VinylCatalogApp.instance.isHapticsEnabled()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    /**
     * 2. Match Success Cue:
     * Subtle pleasant audio chime + crisp affirmative haptic tick when release metadata is found.
     */
    fun onMatchSuccess() {
        // Haptic feedback
        if (VinylCatalogApp.instance.isHapticsEnabled()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45)
                }
            } catch (_: Exception) {}
        }

        // Audio chime
        if (VinylCatalogApp.instance.isAudioFeedbackEnabled()) {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 160)
            } catch (_: Exception) {}
        }
    }

    /**
     * 3. No-Match / Miss Cue:
     * Distinct double-tap vibration + subtle miss tone alerting user without looking at screen.
     */
    fun onMatchMiss() {
        // Double-tap vibration pattern
        if (VinylCatalogApp.instance.isHapticsEnabled()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                } else {
                    val timings = longArrayOf(0, 60, 80, 60)
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, -1)
                }
            } catch (_: Exception) {
                try {
                    val timings = longArrayOf(0, 60, 80, 60)
                    val amplitudes = intArrayOf(0, 180, 0, 180)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                } catch (_: Exception) {}
            }
        }

        // Audio miss cue
        if (VinylCatalogApp.instance.isAudioFeedbackEnabled()) {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
