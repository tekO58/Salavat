package com.tekin.salavat

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Titresim ve kisa ses (web'deki vib / beep karsiligi). */
class Feedback(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    private var tone: ToneGenerator? = null

    /** Desen web'deki gibi: [bekle, titret, bekle, titret, ...] ms. */
    fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator() || pattern.isEmpty()) return
        runCatching {
            if (pattern.size == 2 && pattern[0] == 0L) v.vibrate(VibrationEffect.createOneShot(pattern[1].coerceAtLeast(1), VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    fun beep(finish: Boolean) {
        runCatching {
            val t = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 40).also { tone = it }
            t.startTone(if (finish) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, if (finish) 220 else 35)
        }
    }

    fun release() {
        tone?.release()
        tone = null
    }
}
