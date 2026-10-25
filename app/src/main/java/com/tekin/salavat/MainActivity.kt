package com.tekin.salavat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.net.URLConnection

/**
 * Uygulamanin tek ekrani: web uygulamasi (../web, APK'nin icinde assets/web/)
 * WebView'da. Ekranlarin hepsi orada; burada yalnizca Android'in yapabildikleri
 * var: geri tusu, dosya kaydetme / secme, titresim, ekran acik, durum cubugu
 * rengi ve tespih balonu ([WebBridge], [TasbihFloatService]).
 *
 * Sayfa https://appassets.androidplatform.net/web/ adresinden yuklenir (istekler
 * assets'ten karsilanir): localStorage ve fetch duzgun calissin diye gercek bir
 * https kaynagi.
 */
class MainActivity : Activity() {

    lateinit var web: WebView
        private set
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    /** Kaydedilmeyi bekleyen yedek (sistem "Farkli kaydet" penceresi acikken). */
    var pendingSave: String? = null
    /** Izin ekranindan donunce balon baslasin. */
    var bubbleAfterPermission = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#0A1414"))
        setContentView(web)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            textZoom = 100
        }
        web.addJavascriptInterface(WebBridge(this), "SalavatAndroid")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                asset(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host == HOST) return false
                // Kaynak baglantilari (http) tarayicida acilir.
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, url)) }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "application/octet-stream"))
                return runCatching { startActivityForResult(pick, REQ_OPEN); true }.getOrElse {
                    fileCallback = null
                    false
                }
            }
        }
        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl(START_URL)
        handleOpen(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpen(intent)
    }

    /** Balona uzun basinca ya da bildirime dokununca o kaydin tespihi acilir. */
    private fun handleOpen(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_OPEN_ID) ?: return
        intent.removeExtra(EXTRA_OPEN_ID)
        js("window.salavatOpen && salavatOpen(${org.json.JSONObject.quote(id)})")
    }

    override fun onResume() {
        super.onResume()
        BubbleBus.listener = { runOnUiThread { js("window.salavatBubbleSync && salavatBubbleSync()") } }
        js("window.salavatBubbleSync && salavatBubbleSync()")
        if (bubbleAfterPermission) {
            bubbleAfterPermission = false
            if (Settings.canDrawOverlays(this)) TasbihFloatService.start(this)
            else toast("İzin verilmedi: balon yerine bildirimdeki +1 ile sayabilirsin")
        }
    }

    override fun onPause() {
        BubbleBus.listener = null
        super.onPause()
    }

    @Deprecated("Activity.onBackPressed")
    override fun onBackPressed() {
        web.evaluateJavascript("window.salavatBack ? salavatBack() : false") { handled ->
            if (handled != "true") {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    fun js(code: String) {
        runOnUiThread { if (::web.isInitialized) web.evaluateJavascript(code, null) }
    }

    fun toast(text: String) {
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    }

    // ------------------------------------------------------------ dosya

    fun startSave(name: String, json: String) {
        pendingSave = json
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json").putExtra(Intent.EXTRA_TITLE, name)
        runCatching { startActivityForResult(intent, REQ_SAVE) }.onFailure {
            pendingSave = null
            js("window.salavatSaved && salavatSaved(false)")
        }
    }

    @Deprecated("Activity.onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val uri = if (resultCode == RESULT_OK) data?.data else null
        when (requestCode) {
            REQ_OPEN -> {
                fileCallback?.onReceiveValue(uri?.let { arrayOf(it) })
                fileCallback = null
            }
            REQ_SAVE -> {
                val json = pendingSave
                pendingSave = null
                val ok = uri != null && json != null && runCatching {
                    contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
                js("window.salavatSaved && salavatSaved($ok)")
            }
        }
    }

    // --------------------------------------------------------- tema, ekran

    fun applyTheme(bg: String, light: Boolean) {
        runOnUiThread {
            val color = runCatching { Color.parseColor(bg) }.getOrDefault(Color.parseColor("#0A1414"))
            window.statusBarColor = color
            window.navigationBarColor = color
            web.setBackgroundColor(color)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (light)
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            else 0
        }
    }

    fun keepScreenOn(on: Boolean) {
        runOnUiThread {
            if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ------------------------------------------------------------ balon

    /** Bildirim (Android 13+) ve "diger uygulamalarin ustunde" izni; sonra servis. */
    fun startBubble() {
        runOnUiThread {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFY)
            }
            if (Settings.canDrawOverlays(this)) {
                TasbihFloatService.start(this)
                moveTaskToBack(true)
            } else {
                toast("Salavat için \"Diğer uygulamaların üzerinde göster\" iznini aç, sonra geri dön")
                bubbleAfterPermission = true
                runCatching {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }.onFailure {
                    bubbleAfterPermission = false
                    TasbihFloatService.start(this)
                }
            }
        }
    }

    // ------------------------------------------------------------ assets

    private fun asset(url: Uri): WebResourceResponse? {
        if (url.host != HOST) return null
        val path = (url.path ?: "/").trimStart('/').ifEmpty { "web/index.html" }.let {
            if (it.endsWith("/")) it + "index.html" else it
        }
        return runCatching {
            val stream = assets.open(path)
            val mime = when {
                path.endsWith(".js") -> "text/javascript"
                path.endsWith(".css") -> "text/css"
                path.endsWith(".json") || path.endsWith(".webmanifest") -> "application/json"
                path.endsWith(".svg") -> "image/svg+xml"
                else -> URLConnection.guessContentTypeFromName(path) ?: "application/octet-stream"
            }
            WebResourceResponse(mime, "utf-8", stream)
        }.getOrElse { WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null) }
    }

    companion object {
        const val EXTRA_OPEN_ID = "salavat.open_id"
        private const val HOST = "appassets.androidplatform.net"
        private const val START_URL = "https://$HOST/web/index.html"
        private const val REQ_OPEN = 41
        private const val REQ_SAVE = 42
        private const val REQ_NOTIFY = 43
    }
}
