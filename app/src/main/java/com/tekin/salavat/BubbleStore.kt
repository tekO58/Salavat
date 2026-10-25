package com.tekin.salavat

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/**
 * Balon sayacinin kaydi (SharedPreferences): servis kapanip acilsa da, uygulama
 * kapaliyken sayilsa da bekleyen dokunuslar kaybolmaz.
 */
class BubbleStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): BubbleCounter? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun save(c: BubbleCounter) {
        prefs.edit().putString(KEY, toJson(c).toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        /** Balon servisi ve web koprusu ayni kaydi degistirir. */
        val LOCK = Any()
        private const val PREFS = "tespih_balonu"
        private const val KEY = "sayac"

        private fun color(o: JSONObject, key: String, fallback: Int): Int =
            runCatching { Color.parseColor(o.getString(key)) }.getOrDefault(fallback)

        /** Web'den gelen (startBubble) ya da kayitli JSON. */
        fun fromJson(o: JSONObject): BubbleCounter {
            val taps = mutableListOf<Long>()
            o.optJSONArray("taps")?.let { a -> for (i in 0 until a.length()) taps.add(a.getLong(i)) }
            val colors = o.optJSONObject("colors") ?: o
            return BubbleCounter(
                id = o.getString("id"),
                name = o.optString("name", ""),
                count = o.optInt("count", 0),
                target = o.optInt("target", 33).coerceAtLeast(1),
                onTarget = o.optString("onTarget", "round"),
                every = o.optInt("every", 33),
                finish = o.optBoolean("finish", true),
                sound = o.optBoolean("sound", false),
                colors = BubbleColors(
                    bg = color(colors, "bg", 0xFF111F1E.toInt()),
                    stroke = color(colors, "stroke", 0xFF1F3633.toInt()),
                    text = color(colors, "text", 0xFFEEF5F1.toInt()),
                    text2 = color(colors, "text2", 0xFFA9C2BA.toInt()),
                    accent = color(colors, "accent", 0xFF2FC495.toInt()),
                    bead = color(colors, "bead", 0xFF6E4326.toInt()),
                    beadLight = color(colors, "beadLight", 0xFFB88458.toInt())
                ),
                taps = taps
            )
        }

        private fun hex(c: Int) = String.format("#%06X", 0xFFFFFF and c)

        fun toJson(c: BubbleCounter): JSONObject = JSONObject()
            .put("id", c.id).put("name", c.name).put("count", c.count).put("target", c.target)
            .put("onTarget", c.onTarget).put("every", c.every).put("finish", c.finish).put("sound", c.sound)
            .put("taps", JSONArray(c.taps))
            .put("colors", JSONObject()
                .put("bg", hex(c.colors.bg)).put("stroke", hex(c.colors.stroke))
                .put("text", hex(c.colors.text)).put("text2", hex(c.colors.text2))
                .put("accent", hex(c.colors.accent)).put("bead", hex(c.colors.bead))
                .put("beadLight", hex(c.colors.beadLight)))
    }
}
