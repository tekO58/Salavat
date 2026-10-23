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

/**
 * Tesbih modulu: sayma kurallari, kalici kayit ve gecmis.
 *
 * En onemli kural: her dokunus hemen kaydediliyor ve yeniden acilista
 * (yeni TasbihStore) kalinan sayi aynen geliyor.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TasbihStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun store() = TasbihStore(context)

    private fun tasbih(target: Int, onTarget: TasbihOnTarget = TasbihOnTarget.STOP, every: Int = 33) =
        Tasbih(id = "t1", name = "Sabah Tesbihatı", target = target, onTarget = onTarget, vibrateEvery = every)

    // ------------------------------------------------------------ kurallar

    @Test
    fun `dur modunda hedefte kalir ve daha fazla saymaz`() {
        var t = tasbih(3)
        repeat(2) { t = TasbihLogic.tap(t, 1L).tasbih }
        val done = TasbihLogic.tap(t, 1L)
        assertEquals(TasbihEvent.COMPLETED, done.event)
        assertEquals(3, done.tasbih.count)
        assertTrue(done.tasbih.isDone)
        val blocked = TasbihLogic.tap(done.tasbih, 1L)
        assertEquals(TasbihEvent.BLOCKED, blocked.event)
        assertEquals(3L, blocked.tasbih.total)
    }

    @Test
    fun `yeni tur modunda sayac sifirlanir tur artar`() {
        var t = tasbih(33, TasbihOnTarget.NEW_ROUND)
        repeat(33) { t = TasbihLogic.tap(t, 1L).tasbih }
        assertEquals(0, t.count)
        assertEquals(1, t.rounds)
        assertEquals(33L, t.total)
        assertFalse(t.isDone)
    }

    @Test
    fun `titresim araliginin katlari`() {
        val t = tasbih(1000, every = 33)
        assertEquals(TasbihEvent.MILESTONE, TasbihLogic.tap(t.copy(count = 32), 1L).event)
        assertEquals(TasbihEvent.MILESTONE, TasbihLogic.tap(t.copy(count = 65), 1L).event)
        assertEquals(TasbihEvent.COUNTED, TasbihLogic.tap(t.copy(count = 33), 1L).event)
        assertEquals(TasbihEvent.COUNTED, TasbihLogic.tap(t.copy(count = 32, vibrateEvery = 0), 1L).event)
        assertEquals(TasbihEvent.MILESTONE, TasbihLogic.tap(t.copy(count = 7, vibrateEvery = 1), 1L).event)
    }

    @Test
    fun `elli bine kadar hedef`() {
        assertEquals(50_000, TasbihLogic.parseInt("50000", 1, Tasbih.MAX_TARGET))
        assertNull(TasbihLogic.parseInt("50001", 1, Tasbih.MAX_TARGET))
        assertNull(TasbihLogic.parseInt("0", 1, Tasbih.MAX_TARGET))
        assertNull(TasbihLogic.parseInt("abc", 1, Tasbih.MAX_TARGET))

        var t = tasbih(50_000).copy(count = 49_999)
        val r = TasbihLogic.tap(t, 1L)
        assertEquals(TasbihEvent.COMPLETED, r.event)
        assertEquals(50_000, r.tasbih.count)
    }

    @Test
    fun `geri alma`() {
        val stopDone = tasbih(3).copy(count = 3, rounds = 1, total = 3)
        assertEquals(tasbih(3).copy(count = 2, rounds = 0, total = 2), TasbihLogic.undo(stopDone))

        val roundStart = tasbih(33, TasbihOnTarget.NEW_ROUND).copy(count = 0, rounds = 2, total = 66)
        val undone = TasbihLogic.undo(roundStart)!!
        assertEquals(32, undone.count)
        assertEquals(1, undone.rounds)
        assertEquals(65L, undone.total)

        assertNull(TasbihLogic.undo(tasbih(33)))
    }

    // --------------------------------------------------------------- kayit

    @Test
    fun `her dokunus kaydedilir ve yeniden acilista kaldigi yerden devam eder`() {
        store().save(tasbih(1000))
        repeat(327) { store().tap("t1", 1_000L) }

        val reopened = store()
        val t = reopened.get("t1")!!
        assertEquals(327, t.count)
        assertEquals(327L, t.total)
        assertEquals("t1", reopened.getLastId())
    }

    @Test
    fun `gecmis gun gun tutulur ve silinen tesbihin gunluk toplami kalir`() {
        val store = store()
        store.save(tasbih(100))
        val now = System.currentTimeMillis()
        repeat(5) { store.tap("t1", now) }
        store.undo("t1", now)
        assertEquals(4, store.todayOf("t1", now))
        assertEquals(4, store.todayTotal(now))

        store.delete("t1")
        assertNull(store.get("t1"))
        assertTrue(store.historyOf("t1").isEmpty())
        assertEquals(4, store.todayTotal(now))
    }

    @Test
    fun `sifirlama toplami korur, tam sifirlama toplami da siler`() {
        val store = store()
        store.save(tasbih(33).copy(count = 20, rounds = 2, total = 86))
        store.resetCounter("t1")
        assertEquals(tasbih(33).copy(count = 0, rounds = 0, total = 86), store.get("t1"))
        store.resetAll("t1")
        assertEquals(0L, store.get("t1")!!.total)
    }

    @Test
    fun `kopya sifirdan baslar ve asilin altina eklenir`() {
        val store = store()
        store.save(tasbih(33).copy(id = "a", count = 10, total = 10))
        store.save(tasbih(99).copy(id = "b"))
        val copy = store.duplicate("a", 5L)!!
        assertEquals(listOf("a", copy.id, "b"), store.getAll().map { it.id })
        assertEquals(0, copy.count)
        assertEquals(33, copy.target)
    }

    @Test
    fun `bozuk kayit uygulamayi dusurmez`() {
        context.getSharedPreferences(TasbihStore.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("list", "{bozuk").putString("days", "[").commit()
        assertTrue(store().getAll().isEmpty())
        assertEquals(0, store().todayTotal())
    }

    @Test
    fun `sayac hedefin disinda okunmaz`() {
        store().save(tasbih(10).copy(count = 10))
        context.getSharedPreferences(TasbihStore.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("list", "[{\"id\":\"x\",\"name\":\"X\",\"target\":5,\"count\":99}]").commit()
        assertEquals(5, store().get("x")!!.count)
    }

    // Yedekleme testleri workplanner-app'te kaldi (DataBackup burada yok).

    @Test
    fun `tespih gorunumu tesbih bazinda saklanir`() {
        val store = store()
        store.save(tasbih(100).copy(id = "salavat", name = "Salavatı Fatih", material = TasbihMaterial.YESIL))
        store.save(tasbih(33).copy(id = "subhan", name = "SubhanAllah", material = TasbihMaterial.KUKA))
        store.get("subhan")!!.let { store.save(it.copy(material = TasbihMaterial.SIYAH)) }

        val reopened = store()
        assertEquals(TasbihMaterial.YESIL, reopened.get("salavat")!!.material)
        assertEquals(TasbihMaterial.SIYAH, reopened.get("subhan")!!.material)
    }

    @Test
    fun `eski renk kaydi en yakin materyale donusur`() {
        context.getSharedPreferences(TasbihStore.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("list", "[{\"id\":\"a\",\"name\":\"A\",\"target\":33,\"color\":1}," +
                "{\"id\":\"b\",\"name\":\"B\",\"target\":33}]").commit()
        assertEquals(TasbihMaterial.YESIL, store().get("a")!!.material)
        assertEquals(TasbihMaterial.KEHRIBAR, store().get("b")!!.material)
    }

    @Test
    fun `tespihte cekilen boncuk toplamla ilerler`() {
        val store = store()
        store.save(tasbih(100))
        repeat(35) { store.tap("t1") }
        assertEquals(2, store.get("t1")!!.beadsPulled)
        store.undo("t1")
        assertEquals(1, store.get("t1")!!.beadsPulled)
    }

    @Test
    fun `cekme mesafesi varsayilan normal, aralik disi kirpilir`() {
        assertEquals(56, store().getPullDp())
        assertEquals("Normal", TasbihPull.label(store().getPullDp()))
        store().setPullDp(40)
        assertEquals(40, store().getPullDp())
        assertEquals("Çok kısa", TasbihPull.label(40))
        store().setPullDp(500)
        assertEquals(TasbihPull.MAX_DP, store().getPullDp())
        assertEquals("Çok uzun", TasbihPull.label(TasbihPull.MAX_DP))
    }

    @Test
    fun `titresim sureleri milisaniye, aralik disi kirpilir`() {
        assertEquals(30, store().getTickMs())
        assertEquals(45, store().getPulseMs())
        store().setTickMs(14)
        store().setPulseMs(120)
        assertEquals(14, store().getTickMs())
        assertEquals(120, store().getPulseMs())
        store().setTickMs(1)
        store().setPulseMs(10_000)
        assertEquals(TasbihVibe.MIN_TICK_MS, store().getTickMs())
        assertEquals(TasbihVibe.MAX_PULSE_MS, store().getPulseMs())
    }

    @Test
    fun `baska uygulama verisine dokunmaz`() {
        context.getSharedPreferences("work_planner_accounts", Context.MODE_PRIVATE).edit()
            .putString("accounts", "[1,2,3]").commit()
        store().save(tasbih(33))
        store().tap("t1")
        store().deleteEverything()
        assertEquals("[1,2,3]",
            context.getSharedPreferences("work_planner_accounts", Context.MODE_PRIVATE).getString("accounts", null))
    }
}
