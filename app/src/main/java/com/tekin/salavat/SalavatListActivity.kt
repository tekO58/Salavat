package com.tekin.salavat

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

/**
 * Ana ekran: salavat kutuphanesi.
 *
 * Ustte bugunun ve tum zamanlarin toplami, varsa "kaldigin yer" karti;
 * altta salavatlar. Karta dokununca salavatin tamami (SalavatActivity),
 * sagdaki ▶ ile dogrudan tespih.
 */
class SalavatListActivity : AppCompatActivity() {

    private lateinit var store: TasbihStore
    private lateinit var body: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TasbihStore(this)
        val scroll = ScrollView(this).apply { setBackgroundColor(AppColors.BG_APP) }
        body = TasbihUi.column(this).apply { setPadding(dp(16), dp(14), dp(16), dp(24)) }
        scroll.addView(body)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        body.removeAllViews()
        val list = SalavatCatalog.all(this)

        body.addView(TasbihUi.text(this, "Salavat", 26f, bold = true))
        val todayAll = store.todayTotal()
        val totalAll = store.getAll().sumOf { it.total }
        body.addView(TasbihUi.text(this, "Bugün ${TasbihUi.num(todayAll)}  ·  Toplam ${TasbihUi.num(totalAll)}",
            14f, AppColors.TEXT_SECONDARY), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(14) })

        // Kaldigin yer: son sayilan salavat, tamamlanmadiysa.
        val last = store.getLast()
        val lastSalavat = last?.let { t -> list.firstOrNull { it.id == t.id } }
        if (last != null && lastSalavat != null && last.count > 0 && !last.isDone) {
            body.addView(resumeCard(lastSalavat, last), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(14) })
        }

        list.forEach { s ->
            body.addView(row(s), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(10) })
        }
    }

    private fun resumeCard(s: Salavat, t: Tasbih): View {
        val card = TasbihUi.row(this).apply {
            background = TasbihUi.card(this@SalavatListActivity, stroke = t.accent,
                fill = TasbihUi.mix(t.accent, AppColors.BG_CARD, 0.14f))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            setOnClickListener { TasbihActions.openCounter(this@SalavatListActivity, s.id) }
        }
        val texts = TasbihUi.column(this)
        texts.addView(TasbihUi.text(this, "KALDIĞIN YER", 11.5f, AppColors.TEXT_MUTED, bold = true))
        texts.addView(TasbihUi.text(this, s.name, 17f, bold = true, maxLines = 1))
        texts.addView(TasbihUi.text(this, "${TasbihUi.num(t.count)} / ${TasbihUi.num(t.target)}", 14f, AppColors.TEXT_SECONDARY))
        card.addView(texts, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        card.addView(TasbihUi.text(this, "Devam ▶", 15f, t.accent, bold = true))
        return card
    }

    private fun row(s: Salavat): View {
        val t = store.get(s.id)
        val card = TasbihUi.row(this).apply {
            background = TasbihUi.card(this@SalavatListActivity)
            setPadding(dp(14), dp(12), dp(6), dp(12))
            isClickable = true
            setOnClickListener { SalavatActivity.open(this@SalavatListActivity, s.id) }
        }
        val texts = TasbihUi.column(this)
        texts.addView(TasbihUi.text(this, s.name, 16.5f, bold = true, maxLines = 1))
        val sub = listOf(s.aka, s.attribution.substringBefore(';').substringBefore('(').trim())
            .filter { it.isNotBlank() }.joinToString("  ·  ")
        if (sub.isNotBlank()) texts.addView(TasbihUi.text(this, sub, 12.5f, AppColors.TEXT_MUTED, maxLines = 1))
        val stats = if (t == null || t.total <= 0L) "Hedef ${TasbihUi.num(s.target)}"
            else "Bugün ${TasbihUi.num(store.todayOf(s.id))}  ·  Toplam ${TasbihUi.num(t.total)}  ·  ${TasbihUi.num(t.count)}/${TasbihUi.num(t.target)}"
        texts.addView(TasbihUi.text(this, stats, 12.5f, AppColors.TEXT_SECONDARY, maxLines = 1),
            TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(2) })
        if (t != null && t.total > 0L) {
            texts.addView(TasbihBarView(this).apply { color = t.accent; progress = t.progress },
                TasbihUi.lp(TasbihUi.MATCH, dp(4)).apply { topMargin = dp(6) })
        }
        card.addView(texts, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        card.addView(TasbihUi.text(this, "▶", 20f, AppColors.ACCENT).apply {
            gravity = Gravity.CENTER
            contentDescription = "Tespihle oku"
            setOnClickListener {
                SalavatCatalog.tasbihFor(store, s)
                TasbihActions.openCounter(this@SalavatListActivity, s.id)
            }
        }, TasbihUi.lp(dp(48), dp(48)))
        return card
    }

    private fun dp(value: Int) = TasbihUi.dp(this, value)
}
