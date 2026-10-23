package com.tekin.salavat

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Kutuphane ekranlarinin ortak parcalari (pano, liste, ekleme). */
object LibraryUi {

    private fun dp(c: Context, v: Int) = TasbihUi.dp(c, v)

    fun headButton(c: Context, label: String, size: Float, onClick: () -> Unit) = TextView(c).apply {
        text = label
        textSize = size
        gravity = Gravity.CENTER
        setTextColor(AppColors.TEXT_PRIMARY)
        layoutParams = LinearLayout.LayoutParams(dp(c, 44), dp(c, 44))
        setOnClickListener { onClick() }
    }

    fun label(c: Context, text: String) = TasbihUi.text(c, text, 11.5f, AppColors.TEXT_MUTED, bold = true)

    /** Koyu zeminli metin kutusu. rtl: Arapca. */
    fun field(c: Context, hint: String, minLines: Int = 1, rtl: Boolean = false) = EditText(c).apply {
        this.hint = hint
        textSize = if (rtl) 20f else 15f
        setTextColor(AppColors.TEXT_PRIMARY)
        setHintTextColor(AppColors.TEXT_MUTED)
        background = TasbihUi.card(c, fill = AppColors.BG_FIELD, radiusDp = 12)
        setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10))
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
            (if (minLines > 1) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0)
        if (minLines > 1) {
            this.minLines = minLines
            gravity = Gravity.TOP or (if (rtl) Gravity.END else Gravity.START)
            isSingleLine = false
        }
        if (rtl) textDirection = View.TEXT_DIRECTION_RTL
    }

    fun chip(c: Context, text: String, selected: Boolean, color: Int = AppColors.ACCENT, onClick: () -> Unit) =
        TasbihUi.text(c, text, 13.5f, if (selected) AppColors.ON_ACCENT else AppColors.TEXT_PRIMARY, bold = selected, maxLines = 1).apply {
            gravity = Gravity.CENTER
            setPadding(dp(c, 13), dp(c, 7), dp(c, 13), dp(c, 7))
            background = TasbihUi.card(c,
                stroke = if (selected) color else TasbihUi.mix(color, AppColors.BG_CARD, 0.55f),
                fill = if (selected) color else AppColors.BG_CARD, radiusDp = 18)
            isClickable = true
            setOnClickListener { onClick() }
        }

    /** Yatay kaydirilan cip satiri; ciplerin arasina bosluk koyar. */
    fun chipRow(c: Context, chips: List<View>): HorizontalScrollView {
        val row = TasbihUi.row(c)
        chips.forEach { row.addView(it, TasbihUi.lp(TasbihUi.WRAP, TasbihUi.WRAP).apply { marginEnd = dp(c, 6) }) }
        return HorizontalScrollView(c).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    /**
     * Liste satiri (C): solda bolum rengi serit, ad ve alt satir, sagda
     * Arapcanin basi.
     */
    fun entryRow(c: Context, e: Entry, section: Section?, sub: String, onClick: () -> Unit): View {
        val row = TasbihUi.row(c).apply {
            setPadding(dp(c, 4), dp(c, 12), dp(c, 4), dp(c, 12))
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(View(c).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(c, 2).toFloat()
                setColor(section?.color ?: AppColors.ACCENT)
            }
        }, TasbihUi.lp(dp(c, 4), dp(c, 40)).apply { marginEnd = dp(c, 12) })
        val texts = TasbihUi.column(c)
        texts.addView(TasbihUi.text(c, e.name, 15.5f, bold = true, maxLines = 2))
        texts.addView(TasbihUi.text(c, sub, 12f, if (e.missingArabic) AppColors.WARNING else AppColors.TEXT_SECONDARY, maxLines = 1))
        row.addView(texts, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        if (e.arabic.isNotBlank()) {
            row.addView(TasbihUi.text(c, e.arabic, 16f, AppColors.TEXT_SECONDARY, maxLines = 1).apply {
                gravity = Gravity.END
                textDirection = View.TEXT_DIRECTION_RTL
            }, TasbihUi.lp(dp(c, 120), TasbihUi.WRAP).apply { marginStart = dp(c, 10) })
        }
        return row
    }

    /** "Salavat · 23 / 100", "Dua · Okuma", "Arapça bekliyor" eklenir. */
    fun entrySub(e: Entry, section: Section?, t: Tasbih?): String {
        val parts = mutableListOf(section?.name ?: "")
        parts += when {
            t != null && t.total > 0 -> "${TasbihUi.num(t.count)} / ${TasbihUi.num(t.target)}"
            e.hasCounter -> "Hedef ${TasbihUi.num(e.target)}"
            else -> "Okuma"
        }
        if (e.missingArabic) parts += "Arapça bekliyor"
        return parts.filter { it.isNotBlank() }.joinToString(" · ")
    }

    fun divider(c: Context) = View(c).apply { setBackgroundColor(AppColors.STROKE) }

    // ----------------------------------------------------------- + paneli

    /**
     * "Ne ekliyorsun?" (B): bolumler, yeni bolum, panodan yapistir.
     */
    fun showAddSheet(activity: Activity) {
        val dialog = BottomSheetDialog(activity)
        fun d(v: Int) = dp(activity, v)
        val root = TasbihUi.column(activity).apply {
            setPadding(d(16), d(10), d(16), d(20))
            background = GradientDrawable().apply {
                setColor(AppColors.BG_ELEVATED)
                val r = d(24).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        root.addView(View(activity).apply {
            background = GradientDrawable().apply { cornerRadius = d(2).toFloat(); setColor(AppColors.STROKE_STRONG) }
        }, TasbihUi.lp(d(40), d(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = d(12) })
        root.addView(TasbihUi.text(activity, "Ne ekliyorsun?", 18f, bold = true),
            TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = d(12) })

        fun option(icon: String, title: String, hint: String, dashed: Boolean = false, onClick: () -> Unit) {
            val row = TasbihUi.row(activity).apply {
                background = TasbihUi.card(activity).apply { if (dashed) setStroke(d(1), AppColors.STROKE_STRONG, d(6).toFloat(), d(4).toFloat()) }
                setPadding(d(14), d(12), d(14), d(12))
                isClickable = true
                setOnClickListener { dialog.dismiss(); onClick() }
            }
            row.addView(TasbihUi.text(activity, icon, 20f).apply { gravity = Gravity.CENTER }, TasbihUi.lp(d(36), TasbihUi.WRAP))
            val texts = TasbihUi.column(activity)
            texts.addView(TasbihUi.text(activity, title, 15f, bold = true))
            texts.addView(TasbihUi.text(activity, hint, 12.5f, AppColors.TEXT_SECONDARY, maxLines = 1))
            row.addView(texts, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
            row.addView(TasbihUi.text(activity, "›", 20f, AppColors.TEXT_MUTED))
            root.addView(row, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = d(8) })
        }

        Library.sections(activity).forEach { s ->
            option(s.icon, s.name, s.hint) { AddEntryActivity.open(activity, section = s.id) }
        }
        option("＋", "Yeni bölüm oluştur", "Ad, simge ve renk seç") {
            askNewSection(activity) { s -> AddEntryActivity.open(activity, section = s.id) }
        }
        root.addView(label(activity, "YA DA"), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = d(4); bottomMargin = d(6) })
        option("📋", "Panodan yapıştır", "Kopyaladığın metni otomatik ayırır", dashed = true) {
            AddEntryActivity.open(activity, section = null, paste = true)
        }

        dialog.setContentView(root)
        dialog.setOnShowListener {
            // Kutuphanenin beyaz zemini yerine kendi koyu, yuvarlak zeminimiz gorunsun.
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    /** Yeni bolum: ad, simge, renk. Olusturulan bolum onCreated'a verilir. */
    fun askNewSection(activity: Activity, onCreated: (Section) -> Unit) {
        fun d(v: Int) = dp(activity, v)
        var icon = Library.SECTION_ICONS.first()
        var color = Library.SECTION_COLORS.first()
        val box = TasbihUi.column(activity).apply { setPadding(d(20), d(8), d(20), 0) }
        val name = field(activity, "Bölüm adı (ör. Esmâ-ül Hüsnâ)")
        box.addView(name)
        val icons = TasbihUi.row(activity)
        val colors = TasbihUi.row(activity)
        lateinit var paint: () -> Unit
        paint = {
            icons.removeAllViews()
            Library.SECTION_ICONS.forEach { ic ->
                icons.addView(TasbihUi.text(activity, ic, 20f).apply {
                    gravity = Gravity.CENTER
                    background = if (ic == icon) TasbihUi.card(activity, stroke = color, fill = TasbihUi.mix(color, AppColors.BG_CARD, 0.2f), radiusDp = 10) else null
                    setOnClickListener { icon = ic; paint() }
                }, TasbihUi.lp(d(40), d(40)))
            }
            colors.removeAllViews()
            Library.SECTION_COLORS.forEach { col ->
                colors.addView(View(activity).apply {
                    background = TasbihUi.oval(col, if (col == color) AppColors.TEXT_PRIMARY else col, d(if (col == color) 3 else 1))
                    setOnClickListener { color = col; paint() }
                }, TasbihUi.lp(d(32), d(32)).apply { marginEnd = d(10) })
            }
        }
        paint()
        box.addView(label(activity, "SİMGE"), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = d(14); bottomMargin = d(4) })
        box.addView(HorizontalScrollView(activity).apply { isHorizontalScrollBarEnabled = false; addView(icons) })
        box.addView(label(activity, "RENK"), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = d(14); bottomMargin = d(6) })
        box.addView(colors)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Yeni bölüm")
            .setView(box)
            .setPositiveButton("Oluştur", null)
            .setNegativeButton("Vazgeç", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = name.text.toString().trim().take(30)
            if (title.isEmpty()) {
                Toast.makeText(activity, "Bölüme bir ad ver", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val s = Section(LibraryStore.newId(System.currentTimeMillis()), title, icon, color, 33, "Kendi bölümün")
            LibraryStore(activity).addSection(s)
            dialog.dismiss()
            onCreated(s)
        }
    }
}

/** Gunluk hedef halkasi (pano). */
class GoalRingView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()

    var color: Int = AppColors.ACCENT
        set(value) { field = value; invalidate() }

    var progress: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = width * 0.11f
        paint.strokeWidth = stroke
        val inset = stroke / 2f + 1f
        rect.set(inset, inset, width - inset, height - inset)
        paint.color = AppColors.STROKE
        canvas.drawArc(rect, 0f, 360f, false, paint)
        if (progress > 0f) {
            paint.color = color
            canvas.drawArc(rect, -90f, 360f * progress, false, paint)
        }
    }
}
