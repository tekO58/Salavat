package com.tekin.salavat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Yuzen tespih balonunun gorunumu (workplanner-app'ten): koyu daire, vurgu
 * renginde ilerleme halkasi, kenarda 12 boncuk ve ortada sayi / hedef.
 * Renkler web uygulamasinin temasindan ve tespih materyalinden gelir.
 */
class TasbihBubbleView(context: Context) : View(context) {

    var colors: BubbleColors = BubbleColors(0xFF111F1E.toInt(), 0xFF1F3633.toInt(), 0xFFEEF5F1.toInt(),
        0xFFA9C2BA.toInt(), 0xFF2FC495.toInt(), 0xFF6E4326.toInt(), 0xFFB88458.toInt())
        set(value) { field = value; invalidate() }
    var count: Int = 0
        set(value) { field = value; invalidate() }
    var target: Int = 0
        set(value) { field = value; invalidate() }
    /** Kapatma hedefinin ustundeyken balon kirmiziya doner. */
    var closing: Boolean = false
        set(value) { field = value; invalidate() }

    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val oval = RectF()
    private val fmt = NumberFormat.getIntegerInstance(Locale("tr", "TR"))

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = minOf(width, height).toFloat()
        if (s <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val r = s / 2f - density * 2

        fill.color = if (closing) 0xFF5A1E22.toInt() else colors.bg
        canvas.drawCircle(cx, cy, r, fill)
        ring.strokeWidth = density * 1.5f
        ring.color = colors.stroke
        canvas.drawCircle(cx, cy, r, ring)

        // Ilerleme halkasi
        val pr = r - density * 4
        ring.strokeWidth = density * 3.5f
        oval.set(cx - pr, cy - pr, cx + pr, cy + pr)
        canvas.drawOval(oval, ring)
        val progress = if (target > 0) (count.toFloat() / target).coerceIn(0f, 1f) else 0f
        if (progress > 0f) {
            ring.color = colors.accent
            canvas.drawArc(oval, -90f, 360f * progress, false, ring)
        }

        // 12 boncuk ust ve yanlarda (240 derece); alt kisim hedef yazisina kalir.
        val br = r - density * 12
        val beadR = br * 0.14f
        val beads = 12
        val step = 240f / (beads - 1)
        for (i in 0 until beads) {
            val a = Math.toRadians((150f + step * i).toDouble())
            val x = cx + br * cos(a).toFloat()
            val y = cy + br * sin(a).toFloat()
            beadPaint.shader = RadialGradient(x - beadR * 0.35f, y - beadR * 0.35f, beadR * 1.3f,
                colors.beadLight, colors.bead, Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, beadR, beadPaint)
        }

        val main = fmt.format(count)
        text.typeface = Typeface.DEFAULT_BOLD
        text.color = colors.text
        fit(main, s * 0.30f, br * 1.35f)
        canvas.drawText(main, cx, cy + text.textSize * 0.25f, text)
        if (target > 0) {
            val sub = "/ " + fmt.format(target)
            text.typeface = Typeface.DEFAULT
            text.color = colors.text2
            fit(sub, s * 0.13f, br * 0.95f)
            canvas.drawText(sub, cx, cy + s * 0.27f, text)
        }
    }

    private fun fit(value: String, size: Float, maxWidth: Float) {
        text.textSize = size
        val w = text.measureText(value)
        if (w > maxWidth) text.textSize = size * maxWidth / w
    }
}
