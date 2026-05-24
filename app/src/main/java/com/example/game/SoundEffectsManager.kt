package com.example.game

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundEffectsManager {
    private var toneGen: ToneGenerator? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun getToneGenerator(): ToneGenerator? {
        if (toneGen == null) {
            synchronized(this) {
                if (toneGen == null) {
                    try {
                        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
                    } catch (e: Throwable) {
                        Log.e("SoundEffects", "Could not initialize ToneGenerator", e)
                    }
                }
            }
        }
        return toneGen
    }

    fun playHit() {
        scope.launch {
            try {
                // Short deep strike tone
                getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            } catch (e: Throwable) {
                // Ignore audio fails
            }
        }
    }

    fun playCured() {
        scope.launch {
            try {
                // Higher positive tinkle bells
                val tg = getToneGenerator()
                tg?.startTone(ToneGenerator.TONE_SUP_PIP, 100)
                delay(120)
                tg?.startTone(ToneGenerator.TONE_SUP_PIP, 100)
            } catch (e: Throwable) {
            }
        }
    }

    fun playSelected() {
        scope.launch {
            try {
                getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
            } catch (e: Throwable) {
            }
        }
    }

    fun playLevelUp() {
        scope.launch {
            try {
                val tg = getToneGenerator()
                // Tri-tone rising fanfare
                tg?.startTone(ToneGenerator.TONE_DTMF_1, 150)
                delay(150)
                tg?.startTone(ToneGenerator.TONE_DTMF_5, 150)
                delay(150)
                tg?.startTone(ToneGenerator.TONE_DTMF_9, 300)
            } catch (e: Throwable) {
            }
        }
    }

    fun playCaptureSuccess() {
        scope.launch {
            try {
                val tg = getToneGenerator()
                // Epic high-pitched capture celebration tinkle
                tg?.startTone(ToneGenerator.TONE_SUP_CONGESTION, 150)
                delay(200)
                tg?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            } catch (e: Throwable) {
            }
        }
    }

    fun playCaptureFail() {
        scope.launch {
            try {
                // Low rejection buzz
                getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_NACK, 300)
            } catch (e: Throwable) {
            }
        }
    }

    fun initialize(context: android.content.Context) {
        // Automatically initialized lazily, but exposed for user system flow compatibility
    }

    fun release() {
        try {
            toneGen?.release()
        } catch (e: Throwable) {
        }
        toneGen = null
    }
}
