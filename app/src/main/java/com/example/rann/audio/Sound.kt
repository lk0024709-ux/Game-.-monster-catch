package com.example.rann.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.rann.sim.Sfx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tiny offline audio + haptics layer.
 *
 * The game ships without audio assets, so every cue is synthesised with the
 * platform [ToneGenerator]. It is deliberately cheap: cues are fire-and-forget
 * on a background dispatcher so the battle loop never blocks.
 */
object Sound {

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile var enabled: Boolean = true
    @Volatile var hapticsEnabled: Boolean = true
    @Volatile var ready: Boolean = false

    fun init(context: Context) {
        if (ready) return
        val app = context.applicationContext
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (t: Throwable) {
            Log.w("Rann", "Tone generator unavailable", t)
        }
        try {
            vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } catch (t: Throwable) {
            vibrator = null
        }
        ready = true
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Throwable) {
        }
        toneGenerator = null
        ready = false
    }

    private fun beep(tone: Int, ms: Int) {
        if (!enabled || !ready) return
        val gen = toneGenerator ?: return
        try {
            gen.startTone(tone, ms)
        } catch (_: Throwable) {
        }
    }

    private fun sequence(vararg steps: Pair<Int, Int>) {
        if (!enabled || !ready) return
        scope.launch {
            try {
                for ((tone, ms) in steps) {
                    toneGenerator?.startTone(tone, ms)
                    delay(ms.toLong() + 20L)
                }
            } catch (_: Throwable) {
            }
        }
    }

    /** Play a simulation event id (see [Sfx]). */
    fun play(event: Int) {
        when (event) {
            Sfx.SLASH -> beep(ToneGenerator.TONE_PROP_BEEP, 45)
            Sfx.HIT -> beep(ToneGenerator.TONE_PROP_BEEP2, 70)
            Sfx.HURT -> beep(ToneGenerator.TONE_PROP_NACK, 120)
            Sfx.DEATH -> beep(ToneGenerator.TONE_CDMA_ABBR_ALERT, 160)
            Sfx.PICKUP -> beep(ToneGenerator.TONE_PROP_ACK, 80)
            Sfx.CHEST -> sequence(ToneGenerator.TONE_DTMF_3 to 90, ToneGenerator.TONE_DTMF_7 to 140)
            Sfx.LIGHTNING -> beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            Sfx.GEYSER -> beep(ToneGenerator.TONE_PROP_BEEP2, 180)
            Sfx.SHIELD -> beep(ToneGenerator.TONE_SUP_PIP, 110)
            Sfx.REVIVE -> sequence(ToneGenerator.TONE_DTMF_1 to 90, ToneGenerator.TONE_DTMF_6 to 90, ToneGenerator.TONE_DTMF_9 to 160)
            Sfx.WIN -> sequence(
                ToneGenerator.TONE_DTMF_4 to 140,
                ToneGenerator.TONE_DTMF_7 to 140,
                ToneGenerator.TONE_DTMF_9 to 260,
            )
            Sfx.LOSE -> sequence(ToneGenerator.TONE_DTMF_9 to 180, ToneGenerator.TONE_DTMF_4 to 320)
            Sfx.CLONE -> beep(ToneGenerator.TONE_SUP_RADIO_ACK, 160)
            Sfx.POWER -> beep(ToneGenerator.TONE_SUP_PIP, 140)
            else -> beep(ToneGenerator.TONE_PROP_ACK, 60)
        }
    }

    fun ui() = beep(ToneGenerator.TONE_PROP_ACK, 45)

    fun tap() = beep(ToneGenerator.TONE_PROP_BEEP, 40)

    fun merge() = sequence(
        ToneGenerator.TONE_DTMF_5 to 70,
        ToneGenerator.TONE_DTMF_8 to 110,
        ToneGenerator.TONE_DTMF_2 to 150,
    )

    fun coin() = beep(ToneGenerator.TONE_SUP_CONGESTION, 90)

    fun levelUp() = sequence(
        ToneGenerator.TONE_DTMF_1 to 110,
        ToneGenerator.TONE_DTMF_5 to 110,
        ToneGenerator.TONE_DTMF_9 to 220,
    )

    fun win() = play(Sfx.WIN)

    fun lose() = play(Sfx.LOSE)

    // ---- haptics ----
    private fun buzz(ms: Long, amplitude: Int) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Throwable) {
        }
    }

    fun hapticTap() = buzz(12L, 60)

    fun hapticMerge() = buzz(28L, 140)

    fun hapticWin() = buzz(60L, 200)

    fun hapticLose() = buzz(90L, 180)
}
