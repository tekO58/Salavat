package com.tekin.salavat

import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject

/**
 * Web uygulamasina verilen kopru: sayfada window.SalavatAndroid (web/app.js'te
 * NATIVE). Metotlar WebView'in kendi is parcaciginda calisir; ekrana dokunanlar
 * MainActivity'de ana is parcacigina gecer.
 */
class WebBridge(private val activity: MainActivity) {
    private val feedback = Feedback(activity)
    private val store = BubbleStore(activity)

    @JavascriptInterface
    fun version(): String = runCatching {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    /** [bekle, titret, ...] ms, JSON dizi. */
    @JavascriptInterface
    fun vibrate(pattern: String) {
        val p = runCatching { JSONArray(pattern).let { a -> LongArray(a.length()) { a.getLong(it) } } }.getOrNull() ?: return
        feedback.vibrate(p)
    }

    @JavascriptInterface
    fun keepAwake(on: Boolean) = activity.keepScreenOn(on)

    @JavascriptInterface
    fun setTheme(bg: String, light: Boolean) = activity.applyTheme(bg, light)

    @JavascriptInterface
    fun readClipboard(): String {
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return ""
        if (clip.itemCount == 0) return ""
        return clip.getItemAt(0).coerceToText(activity)?.toString() ?: ""
    }

    /** Yedek: sistemin "Farkli kaydet" penceresi; sonuc window.salavatSaved(ok). */
    @JavascriptInterface
    fun saveFile(name: String, json: String) = activity.startSave(name, json)

    // ------------------------------------------------------------ balon

    @JavascriptInterface
    fun startBubble(json: String) {
        val c = runCatching { BubbleStore.fromJson(JSONObject(json)) }.getOrNull() ?: return
        // Ayni kayitta bekleyen dokunuslar korunur; baska kayda geciliyorsa once onlar web'e islenmis olmali.
        synchronized(BubbleStore.LOCK) {
            val old = store.load()
            if (old != null && old.id == c.id) c.taps.addAll(old.taps)
            store.save(c)
        }
        TasbihFloatService.reload()
        activity.startBubble()
    }

    /** Web'deki sayac degisti: balon yeni tabanla gosterir. */
    @JavascriptInterface
    fun updateBubble(id: String, count: Int, target: Int) {
        synchronized(BubbleStore.LOCK) {
            val c = store.load() ?: return
            if (c.id != id || (c.count == count && c.target == target && c.taps.isEmpty())) return
            c.update(count, target)
            store.save(c)
        }
        TasbihFloatService.reload()
    }

    /** Balonda sayilip web'e islenmemis dokunuslar: {"id":..., "taps":[ms,...]}; alininca bosalir. */
    @JavascriptInterface
    fun takeBubbleTaps(): String {
        val (id, taps) = synchronized(BubbleStore.LOCK) {
            val c = store.load() ?: return "null"
            val taps = c.take()
            if (taps.isEmpty()) return "null"
            store.save(c)
            c.id to taps
        }
        TasbihFloatService.reload()
        return JSONObject().put("id", id).put("taps", JSONArray(taps)).toString()
    }
}
