package com.tekin.salavat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 33'luk tespihin dizilisi. Saf hesap; Android gerekmiyor.
 *
 * En onemli kural sureklilik: bir cekisin sonu bir sonraki durumun
 * basiyla ayni olmali, yoksa her sayimda boncuklar ziplar.
 */
class TasbihRosaryLayoutTest {

    private val L = TasbihRosaryLayout

    @Test
    fun `otuz uc boncuk, iki nisane, bir imame`() {
        assertEquals(33, L.STRING.count { it.isBead })
        assertEquals(1, L.STRING.count { it.isImame })
        assertEquals(2, L.STRING.count { !it.isBead && !it.isImame })
    }

    @Test
    fun `imame ile nisaneler arasinda hep 11 boncuk`() {
        // Ip halka: imameden baslayip her iki nisane ve geri donuste 11 boncuk
        val s = L.STRING
        val marks = s.indices.filter { !s[it].isBead }
        val gaps = (marks + (marks.first() + s.size)).zipWithNext { a, b -> b - a - 1 }
        assertEquals(listOf(11, 11, 11), gaps)
    }

    @Test
    fun `cekis sonu bir sonraki durumun basiyla ayni`() {
        for (k in 0 until 33) {
            val end = L.positions(k, 1f).associate { it.first.bead to it.second }
            val next = L.positions(k + 1, 0f).associate { it.first.bead to it.second }
            end.forEach { (bead, u) ->
                val d = abs(u - next.getValue(bead)).let { minOf(it, L.TOTAL - it) }
                assertTrue("k=$k boncuk=$bead ziplıyor", d < 1e-3f)
            }
        }
    }

    @Test
    fun `siradaki boncuk sirayla cekilir ve 33te basa doner`() {
        for (k in 0 until 33) assertEquals((k % 33) + 1, L.movingBead(k))
        assertEquals(L.order(0), L.order(33))
    }

    @Test
    fun `durgun halde bosluk bos ve boncuklar ust uste binmez`() {
        for (k in 0 until 33) {
            val ps = L.positions(k, 0f).map { it.second }.sorted()
            assertTrue(ps.all { it < L.WIDTH })
            for (i in 1 until ps.size) assertTrue(ps[i] - ps[i - 1] >= L.SEP_W - 1e-3f)
        }
    }

    @Test
    fun `nisane ve imame boncukla birlikte gecer`() {
        // 33 (tur sonu) cekildikten sonra imame cekme noktasinda: ilk boncukla gecer.
        assertEquals(2, L.chunkSize(L.order(0)))
        assertTrue(L.order(0).takeLast(2).any { it.isImame })
        assertEquals(2, L.chunkSize(L.order(11)))
        assertEquals(2, L.chunkSize(L.order(22)))
        assertEquals(1, L.chunkSize(L.order(5)))
    }

    @Test
    fun `cekilen boncuk boslugu atlar`() {
        val before = L.positions(4, 0f).first { it.first.bead == 5 }.second
        val after = L.positions(4, 1f).first { it.first.bead == 5 }.second
        assertTrue(before > L.WIDTH - 1f)
        assertTrue(after < 1f)
    }
}
