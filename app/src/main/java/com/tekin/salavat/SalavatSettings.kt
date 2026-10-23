package com.tekin.salavat

import android.content.Context

/** Okuma ayarlari: yazi boyutu ve sayma ekranindaki metin. */
class SalavatSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Arapca metin boyutu (sp). */
    var arabicSp: Float
        get() = prefs.getFloat(KEY_ARABIC, DEFAULT_ARABIC).coerceIn(MIN_SP, MAX_ARABIC)
        set(value) { prefs.edit().putFloat(KEY_ARABIC, value.coerceIn(MIN_SP, MAX_ARABIC)).apply() }

    /** Okunus ve anlam boyutu (sp). */
    var textSp: Float
        get() = prefs.getFloat(KEY_TEXT, DEFAULT_TEXT).coerceIn(MIN_SP, MAX_TEXT)
        set(value) { prefs.edit().putFloat(KEY_TEXT, value.coerceIn(MIN_SP, MAX_TEXT)).apply() }

    /** Sayma ekraninin ustundeki kartta hangi metin: TEXT_ARABIC / READING / MEANING. */
    var counterText: Int
        get() = prefs.getInt(KEY_COUNTER_TEXT, TEXT_READING).coerceIn(0, TEXT_MODES - 1)
        set(value) { prefs.edit().putInt(KEY_COUNTER_TEXT, value.coerceIn(0, TEXT_MODES - 1)).apply() }

    /** A- / A+ : iki boyut birlikte buyur, kuculur. */
    fun step(delta: Int) {
        arabicSp += delta * 2f
        textSp += delta * 1.5f
    }

    companion object {
        private const val PREFS = "salavat_ayarlar"
        private const val KEY_ARABIC = "arabic_sp"
        private const val KEY_TEXT = "text_sp"
        private const val KEY_COUNTER_TEXT = "counter_text"

        const val MIN_SP = 12f
        const val MAX_ARABIC = 44f
        const val MAX_TEXT = 30f
        const val DEFAULT_ARABIC = 26f
        const val DEFAULT_TEXT = 16f

        const val TEXT_ARABIC = 0
        const val TEXT_READING = 1
        const val TEXT_MEANING = 2
        const val TEXT_MODES = 3
        val TEXT_LABELS = arrayOf("ARAPÇA", "OKUNUŞ", "ANLAM")
    }
}
