package com.tekin.salavat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** assets/salavat.json: her kayit eksiksiz, id'ler tekil, tesbih salavata bagli. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SalavatCatalogTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `katalogdaki her salavat eksiksiz`() {
        val raw = context.assets.open(SalavatCatalog.ASSET).bufferedReader().use { it.readText() }
        val list = SalavatCatalog.parse(raw)
        // Atlanan (bozuk) kayit yok.
        assertEquals(org.json.JSONArray(raw).length(), list.size)
        assertTrue(list.size >= 5)
        list.forEach { s ->
            assertTrue(s.id, s.reading.isNotBlank())
            assertTrue(s.id, s.meaning.isNotBlank())
            assertTrue(s.id, s.attribution.isNotBlank())
            assertTrue(s.id, s.source.isNotBlank())
            // Arapca alanda Arapca harf var.
            assertTrue(s.id, s.arabic.any { it in '؀'..'ۿ' })
        }
        assertEquals(list.size, list.map { it.id }.toSet().size)
    }

    @Test
    fun `eksik kayit atlanir`() {
        val list = SalavatCatalog.parse("""[{"id":"a","name":"A","arabic":"ص"},{"id":"","name":"B","arabic":"ص"},{"id":"c","name":"C"}]""")
        assertEquals(listOf("a"), list.map { it.id })
        assertEquals(100, list[0].target)
    }

    @Test
    fun `tesbih ilk acilista salavat hedefiyle olusur, sonra korunur`() {
        val store = TasbihStore(context)
        store.deleteEverything()
        val s = SalavatCatalog.all(context).first { it.id == "tefriciye" }
        val t = SalavatCatalog.tasbihFor(store, s, now = 1L)
        assertEquals("tefriciye", t.id)
        assertEquals(4444, t.target)
        store.save(t.copy(target = 100))
        store.tap(s.id, now = 2L)
        val again = SalavatCatalog.tasbihFor(store, s, now = 3L)
        assertEquals(100, again.target)
        assertEquals(1, again.count)
    }
}
