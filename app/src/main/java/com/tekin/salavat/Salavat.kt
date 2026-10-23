package com.tekin.salavat

import android.content.Context
import org.json.JSONArray

/**
 * Bir salavat: metin, okunus, anlam ve kaynak.
 *
 * Metinler assets/salavat.json'da. verified: metin en az iki kaynakla
 * harf harf karsilastirildi mi; false olanlar listede "kontrol bekliyor"
 * notuyla gorunur.
 */
data class Salavat(
    val id: String,
    val name: String,
    /** Diger adlari ("Nâriye", "Tüncînâ" ...); bos olabilir. */
    val aka: String,
    val arabic: String,
    val reading: String,
    val meaning: String,
    /** Kime nispet edildigi. */
    val attribution: String,
    /** Hangi eserden / hadis kaynagindan alindigi. */
    val source: String,
    /** Gelenekte okunan sayi; "Oku" ekranindaki ilk hedef. */
    val target: Int,
    val note: String,
    val verified: Boolean
)

object SalavatCatalog {

    const val ASSET = "salavat.json"

    @Volatile
    private var cache: List<Salavat>? = null

    fun all(context: Context): List<Salavat> = cache ?: synchronized(this) {
        cache ?: parse(context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() })
            .also { cache = it }
    }

    fun get(context: Context, id: String): Salavat? = all(context).firstOrNull { it.id == id }

    /** Bozuk ya da eksik kayit (id, ad, Arapca yok) atlanir. */
    fun parse(json: String): List<Salavat> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id")
            val name = o.optString("name")
            val arabic = o.optString("arabic")
            if (id.isBlank() || name.isBlank() || arabic.isBlank()) return@mapNotNull null
            Salavat(
                id = id,
                name = name,
                aka = o.optString("aka"),
                arabic = arabic,
                reading = o.optString("reading"),
                meaning = o.optString("meaning"),
                attribution = o.optString("attribution"),
                source = o.optString("source"),
                target = o.optInt("target", 100).coerceIn(1, Tasbih.MAX_TARGET),
                note = o.optString("note"),
                verified = o.optBoolean("verified", false)
            )
        }
    }

    /**
     * Salavatin tesbihi (id ayni). Ilk acilista salavatin geleneksel
     * hedefiyle olusturulur; sonra kullanicinin ayarlari korunur.
     */
    fun tasbihFor(store: TasbihStore, s: Salavat, now: Long = System.currentTimeMillis()): Tasbih {
        store.get(s.id)?.let { return it }
        val t = Tasbih(
            id = s.id,
            name = s.name.take(Tasbih.MAX_NAME),
            target = s.target,
            createdAt = now
        )
        store.save(t)
        return t
    }
}
