package com.tekin.salavat

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Kucuk ekranda tespih: yuzen balon ve bildirim (workplanner-app'ten uyarlandi).
 *
 * Sayac BubbleCounter'da (BubbleStore'da kayitli); dokunuslar zamanlariyla
 * birikir, uygulama one gelince web uygulamasi bunlari kendi sayacina isler.
 * Uygulama acikken her dokunusta hemen haber verilir ([BubbleBus]).
 *
 * Balon: dokun = +1, surukle = tasi (birakinca kenara yapisir), uzun bas =
 * uygulamaya don (balon acik kalir), asagidaki X'e birak = kapat. "Diger
 * uygulamalarin uzerinde goster" izni yoksa yalnizca bildirim; sayma
 * bildirimdeki +1 ile surer. Kapaninca bildirim de kalkar, sayac kalir.
 */
class TasbihFloatService : Service() {

    private lateinit var store: BubbleStore
    private lateinit var feedback: Feedback
    private var counter: BubbleCounter? = null

    private var windowManager: WindowManager? = null
    private var bubble: TasbihBubbleView? = null
    private var closeTarget: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val fmt = NumberFormat.getIntegerInstance(Locale("tr", "TR"))

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = BubbleStore(this)
        feedback = Feedback(this)
        ensureChannel()
        running = true
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val c = store.load()
        if (c == null) {
            stopEverything()
            return START_NOT_STICKY
        }
        counter = c
        // Android, on plan servisinin bildirimini birkac saniye icinde ister.
        startInForeground(buildNotification(c))

        when (intent?.action) {
            ACTION_COUNT -> count()
            ACTION_UNDO -> undo()
            ACTION_OPEN -> { openApp(); return START_NOT_STICKY }
            ACTION_CLOSE -> { stopEverything(); return START_NOT_STICKY }
            else -> showBubbleIfAllowed()
        }
        refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        instance = null
        removeBubble()
        feedback.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ------------------------------------------------------------ sayma

    private fun count() {
        // Kayit tek kaynak: web de ayni kaydi okuyup yaziyor (WebBridge).
        val c = synchronized(BubbleStore.LOCK) {
            val c = store.load() ?: return
            val ev = c.tap(System.currentTimeMillis())
            store.save(c)
            counter = c
            ev to c
        }.let { (ev, c) -> onEvent(ev, c); c }
        refresh()
        bubble?.let { b ->
            b.animate().cancel()
            b.scaleX = 0.92f; b.scaleY = 0.92f
            b.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
        BubbleBus.changed()
    }

    private fun onEvent(ev: BubbleCounter.Event, c: BubbleCounter) {
        when (ev) {
            BubbleCounter.Event.BLOCKED -> feedback.vibrate(longArrayOf(0, 30, 60, 30))
            BubbleCounter.Event.COMPLETED -> {
                if (c.finish) feedback.vibrate(longArrayOf(0, 700, 250, 700))
                if (c.sound) feedback.beep(true)
            }
            BubbleCounter.Event.MILESTONE -> {
                feedback.vibrate(longArrayOf(0, if (c.every > 1) 60 else 25))
                if (c.sound) feedback.beep(false)
            }
            BubbleCounter.Event.COUNTED -> if (c.sound) feedback.beep(false)
        }
    }

    private fun undo() {
        val undone = synchronized(BubbleStore.LOCK) {
            val c = store.load() ?: return
            val ok = c.undo()
            if (ok) store.save(c)
            counter = c
            ok
        }
        if (undone) {
            feedback.vibrate(longArrayOf(0, 20))
            BubbleBus.changed()
        }
        refresh()
    }

    /** Web sayaci degisti (uygulamada sayildi ya da dokunuslar islendi). */
    fun reload() {
        counter = store.load() ?: return
        refresh()
    }

    private fun refresh() {
        val c = counter ?: return
        bubble?.apply {
            colors = c.colors
            count = c.count
            target = c.target
        }
        runCatching {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildNotification(c))
        }
    }

    private fun openApp() {
        val c = counter
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_ID, c?.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        runCatching { startActivity(intent) }
    }

    private fun stopEverything() {
        removeBubble()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    // ------------------------------------------------------------ balon

    private fun showBubbleIfAllowed() {
        if (bubble != null) return
        if (!Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val size = dp(84)
        val pos = getSharedPreferences(POS_PREFS, Context.MODE_PRIVATE)
        val params = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = pos.getInt("x", screenWidth() - size - dp(8))
            y = pos.getInt("y", dp(220))
        }
        val view = TasbihBubbleView(this).apply {
            elevation = dp(8).toFloat()
            contentDescription = "Tespih balonu. Dokun: bir say. Uzun bas: uygulamaya dön."
        }
        bindTouch(view, params)
        runCatching { wm.addView(view, params) }.onSuccess { bubble = view }
    }

    private fun removeBubble() {
        val wm = windowManager ?: return
        bubble?.let { runCatching { wm.removeView(it) } }
        closeTarget?.let { runCatching { wm.removeView(it) } }
        bubble = null
        closeTarget = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindTouch(view: TasbihBubbleView, params: WindowManager.LayoutParams) {
        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var dragging = false
        var longPressed = false
        val slop = dp(8)
        val longPress = Runnable {
            if (!dragging) {
                longPressed = true
                feedback.vibrate(longArrayOf(0, 60))
                openApp()
            }
        }

        view.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    dragging = false; longPressed = false
                    handler.postDelayed(longPress, LONG_PRESS_MS)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true
                        handler.removeCallbacks(longPress)
                        showCloseTarget()
                    }
                    if (dragging) {
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()
                        runCatching { windowManager?.updateViewLayout(view, params) }
                        view.closing = overCloseTarget(params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPress)
                    when {
                        longPressed -> Unit
                        dragging -> {
                            val close = overCloseTarget(params)
                            hideCloseTarget()
                            if (close) stopEverything() else snapToEdge(view, params)
                        }
                        else -> count()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    hideCloseTarget()
                    view.closing = false
                }
            }
            true
        }
    }

    /** Birakilinca en yakin yan kenara yapisir; konum hatirlanir. */
    private fun snapToEdge(view: View, params: WindowManager.LayoutParams) {
        val w = screenWidth()
        val size = params.width
        params.x = if (params.x + size / 2 < w / 2) dp(8) else w - size - dp(8)
        params.y = params.y.coerceIn(dp(24), screenHeight() - size - dp(24))
        runCatching { windowManager?.updateViewLayout(view, params) }
        getSharedPreferences(POS_PREFS, Context.MODE_PRIVATE).edit()
            .putInt("x", params.x).putInt("y", params.y).apply()
    }

    private fun showCloseTarget() {
        if (closeTarget != null) return
        val wm = windowManager ?: return
        val c = counter ?: return
        val size = dp(64)
        val view = TextView(this).apply {
            text = "✕"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(c.colors.text)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor((c.colors.bg and 0x00FFFFFF) or 0xCC000000.toInt())
                setStroke(dp(2), c.colors.text2)
            }
        }
        val params = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth() - size) / 2
            y = screenHeight() - size - dp(72)
        }
        runCatching { wm.addView(view, params) }.onSuccess { closeTarget = view }
    }

    private fun hideCloseTarget() {
        closeTarget?.let { runCatching { windowManager?.removeView(it) } }
        closeTarget = null
    }

    private fun overCloseTarget(params: WindowManager.LayoutParams): Boolean {
        if (closeTarget == null) return false
        val bx = params.x + params.width / 2f
        val by = params.y + params.height / 2f
        val tx = screenWidth() / 2f
        val ty = screenHeight() - dp(72) - dp(32).toFloat()
        return hypot(bx - tx, by - ty) < dp(70)
    }

    private fun screenWidth() = resources.displayMetrics.widthPixels
    private fun screenHeight() = resources.displayMetrics.heightPixels
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // --------------------------------------------------------- bildirim

    private fun ensureChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tespih", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Tespih balonu açıkken sayaç ve +1 düğmesi"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(false)
                setSound(null, null)
            }
        )
    }

    private fun buildNotification(c: BubbleCounter): Notification {
        val status = if (c.isDone) "Tamamlandı ✓" else "${fmt.format(c.count)} / ${fmt.format(c.target)}"
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(c.name)
            .setContentText(status)
            .setProgress(c.target, c.count.coerceAtMost(c.target), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(pending(ACTION_OPEN, 1))
            .addAction(Notification.Action.Builder(null, "+1", pending(ACTION_COUNT, 2)).build())
            .addAction(Notification.Action.Builder(null, "↶ Geri al", pending(ACTION_UNDO, 3)).build())
            .addAction(Notification.Action.Builder(null, "Kapat", pending(ACTION_CLOSE, 4)).build())
            .build()
    }

    private fun pending(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code,
            Intent(this, TasbihFloatService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun startInForeground(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "tespih_balonu"
        private const val NOTIFICATION_ID = 7301
        private const val POS_PREFS = "tespih_balonu_konum"
        private const val LONG_PRESS_MS = 550L
        // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE (API 34)
        private const val FOREGROUND_SERVICE_TYPE_SPECIAL_USE = 1 shl 30

        const val ACTION_START = "tespih.START"
        const val ACTION_COUNT = "tespih.COUNT"
        const val ACTION_UNDO = "tespih.UNDO"
        const val ACTION_OPEN = "tespih.OPEN"
        const val ACTION_CLOSE = "tespih.CLOSE"

        @Volatile var running = false
            private set
        @Volatile private var instance: TasbihFloatService? = null

        fun start(context: Context) {
            val intent = Intent(context, TasbihFloatService::class.java).setAction(ACTION_START)
            runCatching { context.startForegroundService(intent) }
        }

        /** Kayit degisti: acik balon ve bildirim tazelenir (ana is parcaciginda). */
        fun reload() {
            val s = instance ?: return
            s.handler.post { s.reload() }
        }
    }
}

/** Balonda sayilinca acik uygulamaya haber (MainActivity web'e islenmesini ister). */
object BubbleBus {
    @Volatile var listener: (() -> Unit)? = null
    fun changed() { listener?.invoke() }
}
