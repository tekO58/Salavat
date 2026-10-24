package com.tekin.salavat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Hazir katalog, kullanici kayitlari, favoriler, seri ve arama. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LibraryTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `hazir katalogdaki her kayit eksiksiz ve bolumu var`() {
        val raw = context.assets.open(Library.ASSET).bufferedReader().use { it.readText() }
        val list = Library.parse(raw)
        // Atlanan (bozuk) kayit yok.
        assertEquals(org.json.JSONArray(raw).length(), list.size)
        val sections = Library.BUILTIN_SECTIONS.map { it.id }.toSet()
        list.forEach { e ->
            assertTrue(e.id, e.section in sections)
            assertTrue(e.id, e.reading.isNotBlank())
            // Anlam her kayitta yok (ornegin Iksir-i A'zam); okunus ya da anlamdan biri gerekli.
            assertTrue(e.id, e.reading.isNotBlank() || e.meaning.isNotBlank())
            assertTrue(e.id, e.source.isNotBlank())
            assertTrue(e.id, e.arabic.any { PasteParser.isArabicChar(it) })
        }
        assertEquals(list.size, list.map { it.id }.toSet().size)
        assertTrue(list.any { it.section == Library.ZIKIR })
        assertTrue(list.any { it.section == Library.DUA && !it.hasCounter })
    }

    @Test
    fun `eksik kayit atlanir, bolumsuz kayit salavata gider`() {
        val list = Library.parse("""[{"id":"a","name":"A","arabic":"ص"},{"id":"","name":"B","arabic":"ص"},{"id":"c","name":"C"}]""")
        assertEquals(listOf("a"), list.map { it.id })
        assertEquals(Library.SALAVAT, list[0].section)
        assertEquals(100, list[0].target)
    }

    @Test
    fun `kullanici kaydi eklenir, guncellenir, silinir`() {
        val lib = LibraryStore(context)
        val e = Entry(id = "u1", section = Library.DUA, name = "Deneme", reading = "Rabbenâ", target = 0, user = true)
        lib.saveEntry(e)
        assertEquals("Deneme", Library.get(context, "u1")!!.name)
        assertTrue(Library.get(context, "u1")!!.user)
        assertTrue(Library.get(context, "u1")!!.missingArabic)

        lib.saveEntry(e.copy(name = "Yeni ad", arabic = "رَبَّنَا"))
        assertEquals(1, lib.userEntries().size)
        assertFalse(Library.get(context, "u1")!!.missingArabic)

        lib.toggleFavorite("u1")
        lib.deleteEntry("u1")
        assertNull(Library.get(context, "u1"))
        assertFalse(lib.isFavorite("u1"))
    }

    @Test
    fun `yeni bolum kalici ve listede hazirlardan sonra`() {
        LibraryStore(context).addSection(Section("s1", "Esmâ", "✧", 0xFFE39BD0.toInt(), 33, ""))
        val sections = Library.sections(context)
        assertEquals(Library.BUILTIN_SECTIONS.size + 1, sections.size)
        assertEquals("Esmâ", sections.last().name)
    }

    @Test
    fun `favoriler eklenme sirasiyla`() {
        val lib = LibraryStore(context)
        assertTrue(lib.toggleFavorite("b"))
        assertTrue(lib.toggleFavorite("a"))
        assertEquals(listOf("b", "a"), lib.favorites())
        assertFalse(lib.toggleFavorite("b"))
        assertEquals(listOf("a"), lib.favorites())
    }

    @Test
    fun `sayacsiz kaydin tespihi 33 ile acilir`() {
        val store = TasbihStore(context)
        val dua = Library.builtin(context).first { !it.hasCounter }
        assertEquals(33, Library.tasbihFor(store, dua, 1L).target)
        val tef = Library.builtin(context).first { it.id == "tefriciye" }
        assertEquals(4444, Library.tasbihFor(store, tef, 1L).target)
    }

    @Test
    fun `seri bugun ya da dunden geriye sayilir`() {
        val days = listOf("2026-10-23" to 5, "2026-10-22" to 3, "2026-10-21" to 1, "2026-10-19" to 9)
        assertEquals(3, Library.streak(days, "2026-10-23"))
        // Bugun henuz okunmadi: seri dunden sayilir, kirilmis sayilmaz.
        assertEquals(3, Library.streak(days, "2026-10-24"))
        assertEquals(0, Library.streak(days, "2026-10-26"))
        assertEquals(0, Library.streak(listOf("2026-10-23" to 0), "2026-10-23"))
    }

    @Test
    fun `arama sapka, buyuk harf ve harekeyi yok sayar`() {
        val e = Entry(id = "x", section = Library.ZIKIR, name = "Sübhânallah", arabic = "سُبْحَانَ اللّٰهِ", reading = "Sübhânallah")
        assertTrue(Library.matches(e, "subhanallah"))
        assertTrue(Library.matches(e, "SÜBHÂN"))
        assertTrue(Library.matches(e, "سبحان"))
        assertFalse(Library.matches(e, "tevhid"))
        assertTrue(Library.matches(e, "  "))
    }
}
