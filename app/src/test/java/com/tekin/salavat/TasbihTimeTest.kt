package com.tekin.salavat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tesbih suresi: dokunuslar arasi, duraklama haric, tur bitince kaydedilir. */
class TasbihTimeTest {

    private val idle = 30_000L

    private fun tasbih(target: Int, onTarget: TasbihOnTarget = TasbihOnTarget.STOP) =
        Tasbih(id = "t", name = "Test", target = target, onTarget = onTarget)

    @Test
    fun `sure dokunuslar arasindan birikir`() {
        var t = tasbih(10)
        t = TasbihLogic.tap(t, 1_000L, idle).tasbih        // ilk cekis: sure baslar
        assertEquals(0L, t.roundMs)
        t = TasbihLogic.tap(t, 3_000L, idle).tasbih
        t = TasbihLogic.tap(t, 6_000L, idle).tasbih
        assertEquals(5_000L, t.roundMs)
    }

    @Test
    fun `uzun ara duraklama sayilir, devam edince kaldigi yerden`() {
        var t = tasbih(10)
        t = TasbihLogic.tap(t, 0L + 1, idle).tasbih
        t = TasbihLogic.tap(t, 4_001L, idle).tasbih        // +4 sn
        t = TasbihLogic.tap(t, 600_000L, idle).tasbih      // 10 dk ara: eklenmez
        t = TasbihLogic.tap(t, 602_000L, idle).tasbih      // +2 sn
        assertEquals(6_000L, t.roundMs)
    }

    @Test
    fun `tur bitince son, en hizli ve ortalama kaydedilir`() {
        var t = tasbih(3, TasbihOnTarget.NEW_ROUND)
        var now = 1L
        // 1. tur: 2 aralik x 5 sn = 10 sn (ilk cekis sureyi baslatir)
        repeat(3) { t = TasbihLogic.tap(t, now, idle).tasbih; now += 5_000L }
        assertEquals(10_000L, t.lastRoundMs)
        assertEquals(0L, t.roundMs)
        // 2. tur: 3 aralik x 2 sn = 6 sn (turlar arasi gecis de sayilir)
        now = t.lastTapAt + 2_000L
        repeat(3) { t = TasbihLogic.tap(t, now, idle).tasbih; now += 2_000L }
        assertEquals(6_000L, t.lastRoundMs)
        assertEquals(6_000L, t.bestRoundMs)
        assertEquals(2, t.timedRounds)
        assertEquals(8_000L, t.averageRoundMs)
    }

    @Test
    fun `canli sure ve bicim`() {
        val t = tasbih(10).copy(roundMs = 60_000L, lastTapAt = 100_000L)
        assertEquals(65_000L, TasbihTime.liveRoundMs(t, 105_000L, idle))
        assertEquals(60_000L, TasbihTime.liveRoundMs(t, 200_000L, idle))
        assertEquals("1:05", TasbihTime.format(65_000L))
        assertEquals("1:02:33", TasbihTime.format(3_753_000L))
        assertEquals("", TasbihTime.summary(tasbih(10)))
    }
}
