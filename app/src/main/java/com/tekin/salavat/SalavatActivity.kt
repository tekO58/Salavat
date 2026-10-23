package com.tekin.salavat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Salavatin tamami: Arapca, okunus, anlam, nispet ve kaynak. Altta hedef
 * secip tespihle okuma.
 */
class SalavatActivity : AppCompatActivity() {

    private lateinit var store: TasbihStore
    private lateinit var settings: SalavatSettings
    private lateinit var s: Salavat

    private lateinit var arabicView: TextView
    private lateinit var readingView: TextView
    private lateinit var meaningView: TextView
    private lateinit var targetRow: LinearLayout
    private lateinit var statsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TasbihStore(this)
        settings = SalavatSettings(this)
        val found = SalavatCatalog.get(this, intent.getStringExtra(EXTRA_ID).orEmpty())
        if (found == null) {
            Toast.makeText(this, "Salavat bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        s = found
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::s.isInitialized) paint()
    }

    private fun buildUi() {
        val root = TasbihUi.column(this).apply { setBackgroundColor(AppColors.BG_APP) }

        val head = TasbihUi.row(this).apply { setPadding(dp(8), dp(10), dp(8), dp(4)) }
        head.addView(headButton("‹", 30f) { finish() })
        head.addView(TasbihUi.text(this, s.name, 19f, bold = true, maxLines = 1), TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        head.addView(headButton("A−", 16f) { settings.step(-1); paintSizes() }.apply { contentDescription = "Yazıyı küçült" })
        head.addView(headButton("A+", 18f) { settings.step(+1); paintSizes() }.apply { contentDescription = "Yazıyı büyüt" })
        root.addView(head)

        val scroll = ScrollView(this)
        val body = TasbihUi.column(this).apply { setPadding(dp(16), dp(4), dp(16), dp(16)) }
        scroll.addView(body)
        root.addView(scroll, TasbihUi.lp(TasbihUi.MATCH, 0, 1f))

        if (s.aka.isNotBlank()) {
            body.addView(TasbihUi.text(this, s.aka, 13.5f, AppColors.TEXT_MUTED),
                TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(8) })
        }

        arabicView = TasbihUi.text(this, s.arabic, settings.arabicSp).apply {
            gravity = Gravity.END
            textDirection = View.TEXT_DIRECTION_RTL
            setLineSpacing(0f, 1.45f)
            setTextIsSelectable(true)
        }
        body.addView(section("ARAPÇA", arabicView))

        readingView = TasbihUi.text(this, s.reading, settings.textSp).apply {
            setLineSpacing(0f, 1.25f)
            setTextIsSelectable(true)
        }
        body.addView(section("OKUNUŞU", readingView))

        meaningView = TasbihUi.text(this, s.meaning, settings.textSp, AppColors.TEXT_SECONDARY).apply {
            setLineSpacing(0f, 1.25f)
            setTextIsSelectable(true)
        }
        body.addView(section("ANLAMI", meaningView))

        val origin = buildString {
            if (s.attribution.isNotBlank()) append("Nispet: ").append(s.attribution)
            if (s.source.isNotBlank()) { if (isNotEmpty()) append("\n\n"); append("Kaynak: ").append(s.source) }
            if (s.note.isNotBlank()) { if (isNotEmpty()) append("\n\n"); append(s.note) }
        }
        if (origin.isNotBlank()) {
            body.addView(section("KAYNAK", TasbihUi.text(this, origin, 13.5f, AppColors.TEXT_SECONDARY).apply {
                setLineSpacing(0f, 1.2f)
            }))
        }
        if (!s.verified) {
            body.addView(TasbihUi.text(this, "⚠ Bu metin henüz ikinci bir kaynakla harf harf karşılaştırılmadı.",
                12.5f, AppColors.WARNING), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(4) })
        }

        // Alt bolum: hedef ve tespih
        val bottom = TasbihUi.column(this).apply {
            setPadding(dp(16), dp(10), dp(16), dp(16))
            setBackgroundColor(AppColors.BG_FIELD)
        }
        statsView = TasbihUi.text(this, "", 13f, AppColors.TEXT_SECONDARY, maxLines = 1)
        bottom.addView(statsView)
        targetRow = TasbihUi.row(this)
        bottom.addView(targetRow, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(8) })
        val read = TasbihUi.text(this, "📿  Tespihle oku", 17f, AppColors.ON_ACCENT, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = TasbihUi.card(this@SalavatActivity, stroke = AppColors.ACCENT, fill = AppColors.ACCENT, radiusDp = 16)
            isClickable = true
            setOnClickListener {
                SalavatCatalog.tasbihFor(store, s)
                TasbihActions.openCounter(this@SalavatActivity, s.id)
            }
        }
        bottom.addView(read, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(10) })
        root.addView(bottom)

        setContentView(root)
    }

    private fun paint() {
        val t = store.get(s.id)
        val target = t?.target ?: s.target
        statsView.text = if (t == null || t.total <= 0L) "Hedef seç ve okumaya başla"
            else "Bugün ${TasbihUi.num(store.todayOf(s.id))}  ·  Toplam ${TasbihUi.num(t.total)}  ·  Şu an ${TasbihUi.num(t.count)} / ${TasbihUi.num(t.target)}"

        targetRow.removeAllViews()
        val presets = (listOf(s.target) + PRESETS).distinct()
        val isCustom = target !in presets
        presets.forEach { value -> targetRow.addView(chip(TasbihUi.num(value), value == target) { setTarget(value) }, chipLp()) }
        targetRow.addView(chip(if (isCustom) TasbihUi.num(target) else "Özel…", isCustom) {
            TasbihActions.askNumber(this, "Hedef", "Kaç kere okunacak?", target, 1, Tasbih.MAX_TARGET) { setTarget(it) }
        }, chipLp())
    }

    private fun setTarget(value: Int) {
        val t = SalavatCatalog.tasbihFor(store, s)
        store.save(t.copy(target = value, count = TasbihLogic.clampCount(t.count, value)))
        paint()
    }

    private fun paintSizes() {
        arabicView.textSize = settings.arabicSp
        readingView.textSize = settings.textSp
        meaningView.textSize = settings.textSp
    }

    private fun section(label: String, content: View): View {
        val card = TasbihUi.column(this).apply {
            background = TasbihUi.card(this@SalavatActivity)
            setPadding(dp(14), dp(10), dp(14), dp(14))
        }
        card.addView(TasbihUi.text(this, label, 11.5f, AppColors.TEXT_MUTED, bold = true),
            TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(6) })
        card.addView(content, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP))
        card.layoutParams = TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(12) }
        return card
    }

    private fun chip(label: String, selected: Boolean, onClick: () -> Unit) = TasbihUi.text(
        this, label, 14f, if (selected) AppColors.ON_ACCENT else AppColors.TEXT_PRIMARY, bold = selected, maxLines = 1
    ).apply {
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(9), dp(4), dp(9))
        background = TasbihUi.card(this@SalavatActivity,
            stroke = if (selected) AppColors.ACCENT else AppColors.STROKE,
            fill = if (selected) AppColors.ACCENT else AppColors.BG_CARD, radiusDp = 12)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun chipLp() = TasbihUi.lp(0, TasbihUi.WRAP, 1f).apply { marginEnd = dp(6) }

    private fun headButton(label: String, size: Float, onClick: () -> Unit) = TextView(this).apply {
        text = label
        textSize = size
        gravity = Gravity.CENTER
        setTextColor(AppColors.TEXT_PRIMARY)
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = TasbihUi.dp(this, value)

    companion object {
        const val EXTRA_ID = "salavat_id"
        private val PRESETS = listOf(33, 100, 1000)

        fun open(activity: Activity, id: String) {
            activity.startActivity(Intent(activity, SalavatActivity::class.java).putExtra(EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
    }
}
