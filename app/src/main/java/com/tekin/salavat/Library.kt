package com.tekin.salavat

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Kutuphanedeki tek kayit: salavat, zikir, dua ya da kullanicinin
 * actigi bir bolumdeki metin.
 *
 * Hazir kayitlar assets/salavat.json'da, kullanicinin ekledikleri
 * LibraryStore'da. Tespih sayaci kaydin id'siyle tutulur (TasbihStore).
 */
data class Entry(
    val id: String,
    /** Section.id */
    val section: String,
    val name: String,
    /** Diger adlari ("Nâriye", "Tüncînâ" ...); bos olabilir. */
    val aka: String = "",
    /** Bos olabilir: "Arapça bekliyor". */
    val arabic: String = "",
    val reading: String = "",
    val meaning: String = "",
    /** Kime nispet edildigi. */
    val attribution: String = "",
    /** Hangi eserden / siteden alindigi. */
    val source: String = "",
    /** Okuma sayisi; 0 = sayacsiz (yalnizca okuma sayfasi). */
    val target: Int = 100,
    val note: String = "",
    /** Metin en az iki kaynakla harf harf karsilastirildi mi. */
    val verified: Boolean = false,
    /** Kullanici ekledi: duzenlenebilir ve silinebilir. */
    val user: Boolean = false,
    val createdAt: Long = 0
) {
    val hasCounter: Boolean get() = target > 0
    val missingArabic: Boolean get() = arabic.isBlank()
}

/** Ana ekrandaki kutu. Hazir uc bolum kodda, kullanicininkiler LibraryStore'da. */
data class Section(
    val id: String,
    val name: String,
    val icon: String,
    val color: Int,
    /** Bu bolume eklenen kaydin ilk okuma sayisi (0 = sayacsiz). */
    val defaultTarget: Int,
    val hint: String,
    val builtin: Boolean = false
)

object Library {

    const val ASSET = "salavat.json"
    const val SALAVAT = "salavat"
    const val ZIKIR = "zikir"
    const val DUA = "dua"

    val BUILTIN_SECTIONS = listOf(
        Section(SALAVAT, "Salavat", "📿", Color.parseColor("#E8B34A"), 100, "Sayaçlı · Arapça, okunuş, anlam", true),
        Section(ZIKIR, "Zikirler", "✦", Color.parseColor("#72C98A"), 100, "Sayaçlı · kısa metin", true),
        Section(DUA, "Dualar", "🤲", Color.parseColor("#7FB2F0"), 0, "Okuma sayfası · sayaç isteğe bağlı", true)
    )

    /** Yeni bolum icin secilebilecek simge ve renkler. */
    val SECTION_ICONS = listOf("✧", "☾", "❀", "✺", "📖", "🕌", "⭐", "💧", "🌿", "🤍")
    val SECTION_COLORS = listOf("#E39BD0", "#2DD4BF", "#F0A94B", "#A78BFA", "#E8746A", "#5B8DEF").map { Color.parseColor(it) }

    @Volatile
    private var builtinCache: List<Entry>? = null

    fun builtin(context: Context): List<Entry> = builtinCache ?: synchronized(this) {
        builtinCache ?: parse(context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() })
            .also { builtinCache = it }
    }

    fun sections(context: Context): List<Section> = BUILTIN_SECTIONS + LibraryStore(context).userSections()

    fun section(context: Context, id: String): Section? = sections(context).firstOrNull { it.id == id }

    /** Hazir kayitlar once, kullanicinin ekledikleri eklenme sirasiyla sonra. */
    fun entries(context: Context): List<Entry> = builtin(context) + LibraryStore(context).userEntries()

    fun get(context: Context, id: String): Entry? = entries(context).firstOrNull { it.id == id }

    /** Bozuk ya da eksik kayit (id ya da ad yok, hic metin yok) atlanir. */
    fun parse(json: String): List<Entry> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { fromJson(it, user = false) } }
    }

    fun toJson(e: Entry): JSONObject = JSONObject()
        .put("id", e.id)
        .put("section", e.section)
        .put("name", e.name)
        .put("aka", e.aka)
        .put("arabic", e.arabic)
        .put("reading", e.reading)
        .put("meaning", e.meaning)
        .put("attribution", e.attribution)
        .put("source", e.source)
        .put("target", e.target)
        .put("note", e.note)
        .put("verified", e.verified)
        .put("createdAt", e.createdAt)

    fun fromJson(o: JSONObject, user: Boolean): Entry? {
        val id = o.optString("id")
        val name = o.optString("name")
        val arabic = o.optString("arabic")
        val reading = o.optString("reading")
        val meaning = o.optString("meaning")
        if (id.isBlank() || name.isBlank()) return null
        if (arabic.isBlank() && reading.isBlank() && meaning.isBlank()) return null
        return Entry(
            id = id,
            section = o.optString("section").ifBlank { SALAVAT },
            name = name,
            aka = o.optString("aka"),
            arabic = arabic,
            reading = reading,
            meaning = meaning,
            attribution = o.optString("attribution"),
            source = o.optString("source"),
            target = o.optInt("target", 100).coerceIn(0, Tasbih.MAX_TARGET),
            note = o.optString("note"),
            verified = o.optBoolean("verified", false),
            user = user,
            createdAt = o.optLong("createdAt", 0)
        )
    }

    /**
     * Kaydin tesbihi (id ayni). Ilk acilista kaydin okuma sayisiyla
     * olusturulur (sayacsiz kayitta 33); sonra kullanicinin ayarlari korunur.
     */
    fun tasbihFor(store: TasbihStore, e: Entry, now: Long = System.currentTimeMillis()): Tasbih {
        store.get(e.id)?.let { return it }
        val t = Tasbih(
            id = e.id,
            name = e.name.take(Tasbih.MAX_NAME),
            target = if (e.hasCounter) e.target else 33,
            createdAt = now
        )
        store.save(t)
        return t
    }

    /** Bir bolumun bugunku toplami. */
    fun todayOf(context: Context, store: TasbihStore, sectionId: String): Int =
        entries(context).filter { it.section == sectionId }.sumOf { store.todayOf(it.id) }

    /**
     * Ust uste kac gun okundu. Bugun okunmadiysa dunden geriye sayilir
     * (gun bitmeden seri "kirilmis" gorunmesin). Saf; test ediliyor.
     */
    fun streak(days: List<Pair<String, Int>>, today: String): Int {
        val active = days.filter { it.second > 0 }.map { it.first }.toSet()
        var day = runCatching { LocalDate.parse(today) }.getOrNull() ?: return 0
        if (day.toString() !in active) day = day.minusDays(1)
        var count = 0
        while (day.toString() in active) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Aramada harf farklarini yok sayar: buyuk/kucuk, sapkali harfler, Arapca harekeler. */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text.lowercase(java.util.Locale.forLanguageTag("tr"))) {
            when (ch) {
                'â', 'á', 'à' -> sb.append('a')
                'î', 'í', 'ı' -> sb.append('i')
                'û', 'ú' -> sb.append('u')
                'ğ' -> sb.append('g')
                'ş' -> sb.append('s')
                'ç' -> sb.append('c')
                'ö' -> sb.append('o')
                'ü' -> sb.append('u')
                '\'', '’', '`', 'ʿ', 'ʾ', '-' -> {}
                else -> if (ch in 'ً'..'ٟ' || ch == 'ٰ' || ch == 'ـ') {} else sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun matches(e: Entry, query: String): Boolean {
        val q = normalize(query.trim())
        if (q.isEmpty()) return true
        return listOf(e.name, e.aka, e.reading, e.meaning, e.arabic, e.attribution)
            .any { normalize(it).contains(q) }
    }
}

/**
 * Kullanicinin verisi: ekledigi kayitlar ve bolumler, favoriler, gunluk
 * hedef. Kendi SharedPreferences dosyasinda ("kutuphane").
 */
class LibraryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ------------------------------------------------------------ kayitlar

    fun userEntries(): List<Entry> = readArray(KEY_ENTRIES).mapNotNull { Library.fromJson(it, user = true) }

    /** Yeniyse sona ekler, varsa yerinde gunceller. */
    fun saveEntry(e: Entry) {
        val list = userEntries()
        val next = if (list.any { it.id == e.id }) list.map { if (it.id == e.id) e else it } else list + e
        writeArray(KEY_ENTRIES, next.map { Library.toJson(it) })
    }

    fun deleteEntry(id: String) {
        writeArray(KEY_ENTRIES, userEntries().filter { it.id != id }.map { Library.toJson(it) })
        if (isFavorite(id)) toggleFavorite(id)
    }

    // ------------------------------------------------------------ bolumler

    fun userSections(): List<Section> = readArray(KEY_SECTIONS).mapNotNull { o ->
        val id = o.optString("id")
        val name = o.optString("name")
        if (id.isBlank() || name.isBlank()) return@mapNotNull null
        Section(
            id = id,
            name = name,
            icon = o.optString("icon").ifBlank { Library.SECTION_ICONS.first() },
            color = o.optInt("color", Library.SECTION_COLORS.first()),
            defaultTarget = o.optInt("target", 33).coerceIn(0, Tasbih.MAX_TARGET),
            hint = "Kendi bölümün"
        )
    }

    fun addSection(s: Section) {
        val list = readArray(KEY_SECTIONS) + JSONObject()
            .put("id", s.id).put("name", s.name).put("icon", s.icon)
            .put("color", s.color).put("target", s.defaultTarget)
        writeArray(KEY_SECTIONS, list)
    }

    // ------------------------------------------------------------ favoriler

    /** Eklenme sirasiyla. */
    fun favorites(): List<String> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_FAVORITES, "[]"))
        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    fun isFavorite(id: String): Boolean = id in favorites()

    /** Yeni durumu doner. */
    fun toggleFavorite(id: String): Boolean {
        val list = favorites()
        val now = id !in list
        val next = if (now) list + id else list - id
        prefs.edit().putString(KEY_FAVORITES, JSONArray(next).toString()).apply()
        return now
    }

    // ------------------------------------------------------------ hedef

    /** Tum bolumlerin toplami icin gunluk hedef. */
    var dailyGoal: Int
        get() = prefs.getInt(KEY_GOAL, DEFAULT_GOAL).coerceIn(1, Tasbih.MAX_TARGET)
        set(value) { prefs.edit().putInt(KEY_GOAL, value.coerceIn(1, Tasbih.MAX_TARGET)).apply() }

    // ---------------------------------------------------------------- json

    private fun readArray(key: String): List<JSONObject> = runCatching {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }.getOrDefault(emptyList())

    private fun writeArray(key: String, list: List<JSONObject>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "kutuphane"
        private const val KEY_ENTRIES = "entries"
        private const val KEY_SECTIONS = "sections"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_GOAL = "daily_goal"
        const val DEFAULT_GOAL = 300

        fun newId(now: Long): String = "u" + now.toString(36) + (0..999).random().toString(36)
    }
}
