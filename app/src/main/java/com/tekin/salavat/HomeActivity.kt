package com.tekin.salavat

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ana ekran (pano): bugunku toplam ve gunluk hedef halkasi, seri, kaldigin
 * yer, renkli bolum kutulari ve favoriler. Ustteki + "Ne ekliyorsun?"
 * panelini, 🔍 tum kayitlarda aramayi acar.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var store: TasbihStore
    private lateinit var library: LibraryStore
    private lateinit var body: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TasbihStore(this)
        library = LibraryStore(this)
        val scroll = ScrollView(this).apply { setBackgroundColor(AppColors.BG_APP) }
        body = TasbihUi.column(this).apply { setPadding(dp(16), dp(12), dp(16), dp(24)) }
        scroll.addView(body)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        body.removeAllViews()
        header()
        todayCard()
        resumeCard()
        sectionTiles()
        favorites()
        footer()
    }

    private fun header() {
        val head = TasbihUi.row(this)
        val texts = TasbihUi.column(this)
        val date = SimpleDateFormat("EEEE · d MMMM", Locale.forLanguageTag("tr")).format(Date())
        texts.addView(TasbihUi.text(this, date.replaceFirstChar { it.titlecase(Locale.forLanguageTag("tr")) }, 13f, AppColors.TEXT_MUTED))
        texts.addView(TasbihUi.text(this, "Esselâmü aleyküm", 24f, bold = true, maxLines = 1))
        head.addView(texts, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        head.addView(LibraryUi.headButton(this, "🔍", 20f) { SectionActivity.open(this, SectionActivity.ALL) }
            .apply { contentDescription = "Ara" })
        head.addView(TasbihUi.text(this, "+", 26f, AppColors.ON_ACCENT).apply {
            gravity = Gravity.CENTER
            background = TasbihUi.oval(AppColors.ACCENT, AppColors.ACCENT, 0)
            contentDescription = "Ekle"
            setOnClickListener { LibraryUi.showAddSheet(this@HomeActivity) }
        }, TasbihUi.lp(dp(42), dp(42)).apply { marginStart = dp(6) })
        body.addView(head, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(14) })
    }

    /** Bugun: halka + "335 / 500 günlük hedef" + seri. Dokununca hedef degisir. */
    private fun todayCard() {
        val today = store.todayTotal()
        val goal = library.dailyGoal
        val streak = Library.streak(store.dailyTotals(), TasbihStore.dayKey(System.currentTimeMillis()))
        val card = TasbihUi.row(this).apply {
            background = TasbihUi.card(this@HomeActivity)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            isClickable = true
            setOnClickListener {
                TasbihActions.askNumber(this@HomeActivity, "Günlük hedef", "Bütün bölümlerde günde kaç okuma?",
                    goal, 1, Tasbih.MAX_TARGET) { library.dailyGoal = it; render() }
            }
        }
        card.addView(GoalRingView(this).apply {
            progress = today.toFloat() / goal
            color = if (today >= goal) AppColors.SUCCESS else AppColors.ACCENT
        }, TasbihUi.lp(dp(64), dp(64)))
        val texts = TasbihUi.column(this)
        texts.addView(LibraryUi.label(this, "BUGÜN"))
        val line = TasbihUi.row(this, Gravity.BOTTOM)
        line.addView(TasbihUi.text(this, TasbihUi.num(today), 22f, bold = true))
        line.addView(TasbihUi.text(this, "  / ${TasbihUi.num(goal)} günlük hedef", 14f, AppColors.TEXT_SECONDARY, maxLines = 1))
        texts.addView(line)
        texts.addView(TasbihUi.text(this,
            when {
                today >= goal -> "✅ Bugünün hedefi tamam" + if (streak > 1) "  ·  🔥 $streak gün" else ""
                streak > 0 -> "🔥 $streak gün üst üste"
                else -> "Hedefi değiştirmek için dokun"
            }, 12.5f, AppColors.TEXT_SECONDARY, maxLines = 1))
        card.addView(texts, TasbihUi.lp(0, TasbihUi.WRAP, 1f).apply { marginStart = dp(14) })
        body.addView(card, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(12) })
    }

    /** Son sayilan kayit, bitmediyse. */
    private fun resumeCard() {
        val t = store.getLast() ?: return
        val e = Library.get(this, t.id) ?: return
        if (t.count <= 0 || t.isDone) return
        val card = TasbihUi.row(this).apply {
            background = TasbihUi.card(this@HomeActivity, stroke = TasbihUi.mix(AppColors.ACCENT, AppColors.BG_CARD, 0.6f))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            setOnClickListener { TasbihActions.openCounter(this@HomeActivity, e.id) }
        }
        val texts = TasbihUi.column(this)
        texts.addView(LibraryUi.label(this, "KALDIĞIN YER"))
        texts.addView(TasbihUi.text(this, "${e.name} · ${TasbihUi.num(t.count)}/${TasbihUi.num(t.target)}", 16f, bold = true, maxLines = 1))
        card.addView(texts, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        card.addView(TasbihUi.text(this, "Devam ▶", 15f, AppColors.ACCENT, bold = true))
        body.addView(card, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(14) })
    }

    /** Iki sutunlu bolum kutulari; sonda "+ Yeni bölüm". */
    private fun sectionTiles() {
        val entries = Library.entries(this)
        val tiles = Library.sections(this).map { s ->
            val count = entries.count { it.section == s.id }
            val today = entries.filter { it.section == s.id }.sumOf { store.todayOf(it.id) }
            tile(s, count, today)
        } + newSectionTile()
        tiles.chunked(2).forEach { pair ->
            val row = TasbihUi.row(this, Gravity.TOP)
            pair.forEachIndexed { i, v ->
                row.addView(v, TasbihUi.lp(0, dp(128), 1f).apply { if (i > 0) marginStart = dp(10) })
            }
            if (pair.size == 1) row.addView(FrameLayout(this), TasbihUi.lp(0, 1, 1f).apply { marginStart = dp(10) })
            body.addView(row, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { bottomMargin = dp(10) })
        }
    }

    private fun tile(s: Section, count: Int, today: Int): View {
        val box = TasbihUi.column(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(TasbihUi.mix(s.color, AppColors.BG_CARD, 0.16f), AppColors.BG_CARD)).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), TasbihUi.mix(s.color, AppColors.BG_CARD, 0.35f))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            setOnClickListener { SectionActivity.open(this@HomeActivity, s.id) }
        }
        box.addView(TasbihUi.text(this, s.icon, 24f))
        box.addView(TasbihUi.text(this, s.name, 16.5f, bold = true, maxLines = 1),
            TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(6) })
        box.addView(TasbihUi.text(this, "$count kayıt", 12.5f, AppColors.TEXT_SECONDARY))
        box.addView(TasbihUi.text(this, "Bugün ${TasbihUi.num(today)}", 12.5f, s.color, bold = true),
            TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(4) })
        return box
    }

    private fun newSectionTile(): View {
        val box = TasbihUi.column(this).apply {
            gravity = Gravity.CENTER
            background = TasbihUi.card(this@HomeActivity).apply { setStroke(dp(1), AppColors.STROKE_STRONG, dp(6).toFloat(), dp(4).toFloat()) }
            isClickable = true
            setOnClickListener {
                LibraryUi.askNewSection(this@HomeActivity) { s -> render(); SectionActivity.open(this@HomeActivity, s.id) }
            }
        }
        box.addView(TasbihUi.text(this, "＋", 24f, AppColors.TEXT_MUTED).apply { gravity = Gravity.CENTER })
        box.addView(TasbihUi.text(this, "Yeni bölüm", 14f, AppColors.TEXT_SECONDARY).apply { gravity = Gravity.CENTER })
        return box
    }

    private fun favorites() {
        body.addView(LibraryUi.label(this, "FAVORİLER"), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(6); bottomMargin = dp(8) })
        val entries = Library.entries(this).associateBy { it.id }
        val favs = library.favorites().mapNotNull { entries[it] }
        if (favs.isEmpty()) {
            body.addView(TasbihUi.text(this, "Bir kaydın sayfasında ☆ işaretine dokun, buraya gelsin.", 13f, AppColors.TEXT_MUTED))
            return
        }
        val chips = favs.map { e ->
            val color = Library.section(this, e.section)?.color ?: AppColors.ACCENT
            LibraryUi.chip(this, "★ " + e.name, false, color) { EntryActivity.open(this, e.id) }
        }
        body.addView(LibraryUi.chipRow(this, chips))
    }

    /** Hangi APK'nin kurulu oldugu okunabilsin. */
    private fun footer() {
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: return
        body.addView(TasbihUi.text(this, "Sürüm $version", 11.5f, AppColors.TEXT_MUTED).apply { gravity = Gravity.CENTER },
            TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(24) })
    }

    private fun dp(value: Int) = TasbihUi.dp(this, value)
}
