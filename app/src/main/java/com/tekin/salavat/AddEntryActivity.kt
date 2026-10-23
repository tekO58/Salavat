package com.tekin.salavat

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Ekleme ve duzenleme (C): 1) internetten kopyalanan metni yapistir,
 * 2) uygulama alanlara ayirir, kullanici duzeltir, 3) bolum ve okuma
 * sayisi. Arapca bos birakilabilir ("Arapça bekliyor").
 */
class AddEntryActivity : AppCompatActivity() {

    private lateinit var library: LibraryStore
    private lateinit var store: TasbihStore
    private var editing: Entry? = null
    private var sectionId: String = Library.SALAVAT
    /** null: bolumun varsayilani; 0: sayacsiz. */
    private var target: Int? = null

    private lateinit var pasteBox: EditText
    private lateinit var parsedTag: TextView
    private lateinit var nameField: EditText
    private lateinit var arabicField: EditText
    private lateinit var readingField: EditText
    private lateinit var meaningField: EditText
    private lateinit var sourceField: EditText
    private lateinit var sectionChips: FrameLayout
    private lateinit var targetChips: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        library = LibraryStore(this)
        store = TasbihStore(this)
        editing = intent.getStringExtra(EXTRA_EDIT)?.let { id -> library.userEntries().firstOrNull { it.id == id } }
        sectionId = editing?.section ?: intent.getStringExtra(EXTRA_SECTION)?.takeIf { Library.section(this, it) != null } ?: Library.SALAVAT
        target = editing?.target
        buildUi()
        editing?.let { fill(it) }
        if (editing == null && intent.getBooleanExtra(EXTRA_PASTE, false)) pasteFromClipboard()
    }

    private fun buildUi() {
        val root = TasbihUi.column(this).apply { setBackgroundColor(AppColors.BG_APP) }

        val head = TasbihUi.row(this).apply { setPadding(dp(8), dp(10), dp(16), dp(4)) }
        head.addView(LibraryUi.headButton(this, "‹", 30f) { finish() })
        head.addView(TasbihUi.text(this, if (editing == null) "Yapıştır ve ekle" else "Düzenle", 19f, bold = true),
            TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        root.addView(head)

        val scroll = ScrollView(this)
        val body = TasbihUi.column(this).apply { setPadding(dp(16), dp(4), dp(16), dp(16)) }
        scroll.addView(body)
        root.addView(scroll, TasbihUi.lp(TasbihUi.MATCH, 0, 1f))

        // 1 · yapistir (duzenlemede yok)
        pasteBox = LibraryUi.field(this, "İnternetten kopyaladığın metni buraya yapıştır: Arapça, okunuş, anlam, kaynak… Hepsi bir arada olabilir.", minLines = 5)
        parsedTag = TasbihUi.text(this, "", 12.5f, AppColors.SUCCESS)
        if (editing == null) {
            body.addView(LibraryUi.label(this, "1 · İNTERNETTEN KOPYALADIĞIN METİN"))
            body.addView(pasteBox, lpField())
            val buttons = TasbihUi.row(this)
            buttons.addView(smallButton("📋  Panodan yapıştır") { pasteFromClipboard() }, TasbihUi.lp(0, TasbihUi.WRAP, 1f))
            buttons.addView(smallButton("Ayır  ↓") { split(pasteBox.text.toString()) },
                TasbihUi.lp(0, TasbihUi.WRAP, 1f).apply { marginStart = dp(8) })
            body.addView(buttons, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(8) })
        }

        // 2 · alanlar
        val step2 = TasbihUi.row(this)
        step2.addView(LibraryUi.label(this, if (editing == null) "2 · KONTROL ET, GEREKİRSE DÜZELT" else "METİN"), TasbihUi.lp(0, TasbihUi.WRAP, 1f))
        step2.addView(parsedTag)
        body.addView(step2, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(if (editing == null) 18 else 4) })

        nameField = LibraryUi.field(this, "Ad (boş bırakırsan okunuştan alınır)")
        arabicField = LibraryUi.field(this, "Arapça (yoksa boş bırak)", minLines = 2, rtl = true)
        readingField = LibraryUi.field(this, "Türkçe okunuş", minLines = 2)
        meaningField = LibraryUi.field(this, "Türkçe anlam", minLines = 2)
        sourceField = LibraryUi.field(this, "Kaynak / nispet (site, kitap, kime nispet)")
        listOf("AD" to nameField, "ARAPÇA" to arabicField, "OKUNUŞ" to readingField, "ANLAM" to meaningField, "KAYNAK" to sourceField)
            .forEach { (label, field) ->
                body.addView(TasbihUi.text(this, label, 11f, AppColors.TEXT_MUTED, bold = true),
                    TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(10) })
                body.addView(field, lpField())
            }
        body.addView(TasbihUi.text(this, "Arapçası yoksa boş bırak: kayıt \"Arapça bekliyor\" etiketiyle durur, bulunca Düzenle'den eklersin.",
            12.5f, AppColors.TEXT_MUTED), TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(8) })

        // 3 · bolum ve sayi
        body.addView(LibraryUi.label(this, if (editing == null) "3 · BÖLÜM VE OKUMA SAYISI" else "BÖLÜM VE OKUMA SAYISI"),
            TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(18); bottomMargin = dp(8) })
        sectionChips = FrameLayout(this)
        body.addView(sectionChips)
        targetChips = FrameLayout(this)
        body.addView(targetChips, TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(8) })
        paintChips()

        val bottom = FrameLayout(this).apply { setPadding(dp(16), dp(10), dp(16), dp(16)) }
        bottom.addView(TasbihUi.text(this, "Kaydet", 17f, AppColors.ON_ACCENT, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = TasbihUi.card(this@AddEntryActivity, stroke = AppColors.ACCENT, fill = AppColors.ACCENT, radiusDp = 16)
            setOnClickListener { save() }
        }, FrameLayout.LayoutParams(TasbihUi.MATCH, TasbihUi.WRAP))
        root.addView(bottom)

        setContentView(root)
    }

    private fun paintChips() {
        val sections = Library.sections(this)
        val chips = sections.map { s ->
            LibraryUi.chip(this, "${s.icon} ${s.name}", s.id == sectionId, s.color) { sectionId = s.id; paintChips() }
        } + LibraryUi.chip(this, "＋ Yeni bölüm", false) {
            LibraryUi.askNewSection(this) { s -> sectionId = s.id; paintChips() }
        }
        sectionChips.removeAllViews()
        sectionChips.addView(LibraryUi.chipRow(this, chips))

        val current = target ?: Library.section(this, sectionId)?.defaultTarget ?: 100
        val presets = listOf(33, 100)
        val custom = current > 0 && current !in presets
        val color = Library.section(this, sectionId)?.color ?: AppColors.ACCENT
        val targets = listOf(LibraryUi.chip(this, "Sayaçsız", current == 0, color) { target = 0; paintChips() }) +
            presets.map { v -> LibraryUi.chip(this, TasbihUi.num(v), current == v, color) { target = v; paintChips() } } +
            LibraryUi.chip(this, if (custom) TasbihUi.num(current) else "Özel…", custom, color) {
                TasbihActions.askNumber(this, "Okuma sayısı", "Kaç kere okunacak?", current.takeIf { it > 0 }, 1, Tasbih.MAX_TARGET) {
                    target = it; paintChips()
                }
            }
        targetChips.removeAllViews()
        targetChips.addView(LibraryUi.chipRow(this, targets))
    }

    private fun pasteFromClipboard() {
        val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, "Panoda metin yok. Önce internetten metni kopyala.", Toast.LENGTH_LONG).show()
            return
        }
        pasteBox.setText(text)
        split(text)
    }

    /** Ayirir ve alanlara yazar; ayirmanin bos biraktigi alan elle yazilmissa korunur. */
    private fun split(text: String) {
        val r = PasteParser.parse(text)
        if (r.isEmpty) {
            Toast.makeText(this, "Ayrılacak metin yok", Toast.LENGTH_SHORT).show()
            return
        }
        fun put(field: EditText, value: String) { if (value.isNotBlank()) field.setText(value) }
        put(nameField, r.name)
        put(arabicField, r.arabic)
        put(readingField, r.reading)
        put(meaningField, r.meaning)
        put(sourceField, r.source)
        val found = listOf(r.arabic to "Arapça", r.reading to "okunuş", r.meaning to "anlam", r.source to "kaynak")
            .filter { it.first.isNotBlank() }.joinToString(", ") { it.second }
        parsedTag.text = "✓ ayrıldı: $found"
    }

    private fun fill(e: Entry) {
        nameField.setText(e.name)
        arabicField.setText(e.arabic)
        readingField.setText(e.reading)
        meaningField.setText(e.meaning)
        sourceField.setText(listOf(e.attribution, e.source).filter { it.isNotBlank() }.joinToString(" · "))
    }

    private fun save() {
        val arabic = arabicField.text.toString().trim()
        val reading = readingField.text.toString().trim()
        val meaning = meaningField.text.toString().trim()
        if (arabic.isBlank() && reading.isBlank() && meaning.isBlank()) {
            Toast.makeText(this, "Arapça, okunuş ya da anlamdan en az biri gerekli", Toast.LENGTH_LONG).show()
            return
        }
        val name = nameField.text.toString().trim().ifBlank {
            PasteParser.autoName(PasteParser.Result(reading = reading, meaning = meaning)).ifBlank { "Adsız" }
        }
        val now = System.currentTimeMillis()
        val old = editing
        val finalTarget = target ?: Library.section(this, sectionId)?.defaultTarget ?: 100
        val entry = Entry(
            id = old?.id ?: LibraryStore.newId(now),
            section = sectionId,
            name = name.take(60),
            arabic = arabic,
            reading = reading,
            meaning = meaning,
            // Duzenlemede nispet ve kaynak tek alanda birlesik gosterildi; kaynak olarak saklanir.
            source = sourceField.text.toString().trim(),
            target = finalTarget,
            user = true,
            createdAt = old?.createdAt ?: now
        )
        library.saveEntry(entry)
        // Sayac varsa ad ve hedef yeni hale gelsin.
        store.get(entry.id)?.let { t ->
            val newTarget = if (finalTarget > 0) finalTarget else t.target
            store.save(t.copy(name = entry.name.take(Tasbih.MAX_NAME), target = newTarget, count = TasbihLogic.clampCount(t.count, newTarget)))
        }
        Toast.makeText(this, if (old == null) "Eklendi" else "Kaydedildi", Toast.LENGTH_SHORT).show()
        if (old == null) EntryActivity.open(this, entry.id)
        finish()
    }

    private fun smallButton(label: String, onClick: () -> Unit) = TasbihUi.text(this, label, 14f, AppColors.TEXT_PRIMARY, bold = true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = TasbihUi.card(this@AddEntryActivity, radiusDp = 12)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun lpField() = TasbihUi.lp(TasbihUi.MATCH, TasbihUi.WRAP).apply { topMargin = dp(4) }

    private fun dp(value: Int) = TasbihUi.dp(this, value)

    companion object {
        private const val EXTRA_SECTION = "section"
        private const val EXTRA_EDIT = "edit_id"
        private const val EXTRA_PASTE = "paste"

        fun open(activity: Activity, section: String?, editId: String? = null, paste: Boolean = false) {
            val intent = Intent(activity, AddEntryActivity::class.java)
            if (section != null) intent.putExtra(EXTRA_SECTION, section)
            if (editId != null) intent.putExtra(EXTRA_EDIT, editId)
            intent.putExtra(EXTRA_PASTE, paste)
            activity.startActivity(intent)
        }
    }
}
