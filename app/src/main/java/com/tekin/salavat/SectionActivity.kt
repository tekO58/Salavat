package com.tekin.salavat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Bolum listesi (C): ustte arama ve filtre cipleri (Tümü, ★, bolumler),
 * altta satirlar; satirin saginda Arapcanin basi. Alttaki "+ Ekle" secili
 * bolume ekler.
 */
class SectionActivity : AppCompatActivity() {

    private lateinit var store: TasbihStore
    private lateinit var library: LibraryStore
    private lateinit var filter: String
    private var query = ""

    private lateinit var titleView: TextView
    private lateinit var chipsBox: FrameLayout
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TasbihStore(this)
        library = LibraryStore(this)
        filter = intent.getStringExtra(EXTRA_SECTION) ?: ALL
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(AppColors.BG_APP) }
        val col = TasbihUi.column(this)
        root.addView(col, FrameLayout.LayoutParams(TasbihUi.MATCH, TasbihUi.MATCH))

        val head = TasbihUi.row(this).apply { setPadding(dp(8), dp(10), dp(16), dp(4)) }
        head.addView(LibraryUi.headButton(this, "‹", 30f) { finish() })
        titleView = TasbihUi.text(this, "", 22f, bold = true, maxLines = 1)
        head.addView(titleView, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        col.addView(head)

        val top = TasbihUi.column(this).apply { setPadding(dp(16), dp(4), dp(16), dp(6)) }
        val search = LibraryUi.field(this, "🔍  Ara: ad, Arapça ya da Türkçe…").apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { query = s?.toString().orEmpty(); renderList() }
            })
        }
        top.addView(search)
        chipsBox = FrameLayout(this)
        top.addView(chipsBox, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(10) })
        col.addView(top)

        val scroll = ScrollView(this)
        list = TasbihUi.column(this).apply { setPadding(dp(12), 0, dp(12), dp(96)) }
        scroll.addView(list)
        col.addView(scroll, TasbihUi.lp(TasbihUi.MATCH, 0, 1f))

        root.addView(TasbihUi.text(this, "＋ Ekle", 16f, AppColors.ON_ACCENT, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(26), dp(14), dp(26), dp(14))
            background = TasbihUi.card(this@SectionActivity, stroke = AppColors.ACCENT, fill = AppColors.ACCENT, radiusDp = 28)
            elevation = dp(6).toFloat()
            setOnClickListener {
                val section = filter.takeIf { it != ALL && it != FAVORITES }
                if (section == null) LibraryUi.showAddSheet(this@SectionActivity)
                else AddEntryActivity.open(this@SectionActivity, section = section)
            }
        }, FrameLayout.LayoutParams(TasbihUi.WRAP, TasbihUi.WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(22) })

        setContentView(root)
    }

    private fun render() {
        val sections = Library.sections(this)
        titleView.text = when (filter) {
            ALL -> "Tümü"
            FAVORITES -> "Favoriler"
            else -> sections.firstOrNull { it.id == filter }?.let { "${it.icon}  ${it.name}" } ?: "Tümü"
        }
        val chips = mutableListOf(
            LibraryUi.chip(this, "Tümü", filter == ALL) { filter = ALL; render() },
            LibraryUi.chip(this, "★", filter == FAVORITES) { filter = FAVORITES; render() }
        )
        sections.forEach { s ->
            chips += LibraryUi.chip(this, s.name, filter == s.id, s.color) { filter = s.id; render() }
        }
        chipsBox.removeAllViews()
        chipsBox.addView(LibraryUi.chipRow(this, chips))
        renderList()
    }

    private fun renderList() {
        list.removeAllViews()
        val sections = Library.sections(this).associateBy { it.id }
        val favorites = library.favorites()
        val items = Library.entries(this).filter { e ->
            when (filter) {
                ALL -> true
                FAVORITES -> e.id in favorites
                else -> e.section == filter
            } && Library.matches(e, query)
        }
        if (items.isEmpty()) {
            val msg = when {
                query.isNotBlank() -> "“$query” için sonuç yok."
                filter == FAVORITES -> "Henüz favori yok. Bir kaydın sayfasında ☆ işaretine dokun."
                else -> "Bu bölümde henüz kayıt yok.\nAşağıdaki + Ekle ile internetten bulduğun metni yapıştır."
            }
            list.addView(TasbihUi.text(this, msg, 14f, AppColors.TEXT_MUTED).apply { gravity = Gravity.CENTER },
                TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(40) })
            return
        }
        items.forEach { e ->
            val section = sections[e.section]
            list.addView(LibraryUi.entryRow(this, e, section, LibraryUi.entrySub(e, section, store.get(e.id))) {
                EntryActivity.open(this, e.id)
            })
            list.addView(LibraryUi.divider(this), TasbihUi.lp(TasbihUi.MATCH, 1))
        }
    }

    private fun dp(value: Int) = TasbihUi.dp(this, value)

    companion object {
        const val EXTRA_SECTION = "section"
        const val ALL = "_all"
        const val FAVORITES = "_fav"

        fun open(activity: Activity, section: String) {
            activity.startActivity(Intent(activity, SectionActivity::class.java).putExtra(EXTRA_SECTION, section))
        }
    }
}
