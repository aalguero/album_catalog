package com.albumcatalog.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.albumcatalog.app.VinylCatalogApp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

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

    // Pre-synthesized PCM audio tracks routed to STREAM_MUSIC (unmuted even in vibrate mode)
    private var lockTrack: AudioTrack? = null
    private var successTrack: AudioTrack? = null
    private var missTrack: AudioTrack? = null

    // Fallback ToneGenerator on STREAM_MUSIC
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            // Lock cue: 25ms crisp mechanical tick (2400 Hz)
            lockTrack = createToneTrack(
                listOf(Pair(2400.0, 0.025))
            )
            // Match success cue: Upward bright two-tone chime D5 (587 Hz, 75ms) -> A5 (880 Hz, 140ms)
            successTrack = createToneTrack(
                listOf(
                    Pair(587.33, 0.075),
                    Pair(880.00, 0.140)
                )
            )
            // Miss cue: Downward two-tone alert F4 (349 Hz, 90ms) -> C4 (261 Hz, 140ms)
            missTrack = createToneTrack(
                listOf(
                    Pair(349.23, 0.090),
                    Pair(261.63, 0.140)
                )
            )
        } catch (_: Exception) {}

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {}
    }

    private fun createToneTrack(notes: List<Pair<Double, Double>>): AudioTrack? {
        try {
            val sampleRate = 44100
            val totalDurationSec = notes.sumOf { it.second }
            val totalSamples = (sampleRate * totalDurationSec).toInt()
            val buffer = ShortArray(totalSamples)

            var offset = 0
            for ((freq, durationSec) in notes) {
                val noteSamples = (sampleRate * durationSec).toInt()
                for (i in 0 until noteSamples) {
                    val t = i.toDouble() / sampleRate
                    val wave = sin(2.0 * PI * freq * t)
                    val attackTime = 0.004
                    val envelope = if (t < attackTime) {
                        t / attackTime
                    } else {
                        val decayProgress = (t - attackTime) / (durationSec - attackTime)
                        exp(-3.5 * decayProgress)
                    }
                    val sampleValue = (wave * envelope * Short.MAX_VALUE * 0.90).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    if (offset + i < totalSamples) {
                        buffer[offset + i] = sampleValue.toShort()
                    }
                }
                offset += noteSamples
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack(
                audioAttributes,
                format,
                buffer.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(buffer, 0, buffer.size)
            return track
        } catch (_: Exception) {
            return null
        }
    }

    private fun playTrack(track: AudioTrack?, fallbackTone: Int = -1, fallbackDuration: Int = 0) {
        var played = false
        if (track != null) {
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.reloadStaticData()
                track.play()
                played = true
            } catch (_: Exception) {
                played = false
            }
        }
        if (!played && fallbackTone >= 0) {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                }
                toneGenerator?.startTone(fallbackTone, fallbackDuration)
            } catch (_: Exception) {}
        }
    }

    /**
     * 1. Lock Detection Cue:
     * Distinct tactile mechanical click vibration + subtle acoustic tick when camera locks onto barcode/label.
     */
    fun onScanLocked() {
        // Haptic feedback
        if (VinylCatalogApp.instance.isHapticsEnabled()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
            } catch (_: Exception) {}
        }

        // Audio lock tick
        if (VinylCatalogApp.instance.isAudioFeedbackEnabled()) {
            playTrack(lockTrack, ToneGenerator.TONE_PROP_BEEP, 30)
        }
    }

    /**
     * 2. Match Success Cue:
     * Melodic upward chime + crisp affirmative heavy click when release metadata is found.
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
            playTrack(successTrack, ToneGenerator.TONE_PROP_ACK, 160)
        }
    }

    /**
     * 3. No-Match / Miss Cue:
     * Distinct double-tap vibration + downward miss cue alerting user without looking at screen.
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
            playTrack(missTrack, ToneGenerator.TONE_PROP_NACK, 180)
        }
    }

    fun release() {
        try {
            lockTrack?.release()
            lockTrack = null
        } catch (_: Exception) {}
        try {
            successTrack?.release()
            successTrack = null
        } catch (_: Exception) {}
        try {
            missTrack?.release()
            missTrack = null
        } catch (_: Exception) {}
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
