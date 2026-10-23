package com.tekin.salavat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Akilli yapistir: internetten kopyalanan tipik metinler. */
class PasteParserTest {

    @Test
    fun `etiketsiz blok - arapca, okunus, anlam ve kaynak ayrilir`() {
        val r = PasteParser.parse("""
            اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ صَلَاةً تُنْجِينَا
            Allâhümme salli alâ seyyidinâ Muhammedin salâten tüncînâ bihâ
            Allah'ım! Efendimiz Muhammed'e öyle bir salât eyle ki bizi kurtarasın.
            Kaynak: islamveihsan.com
        """.trimIndent())
        assertTrue(r.arabic.startsWith("اللَّهُمَّ"))
        assertTrue(r.reading.startsWith("Allâhümme salli"))
        assertTrue(r.meaning.startsWith("Allah'ım! Efendimiz"))
        assertEquals("islamveihsan.com", r.source)
        assertEquals("", r.name)
    }

    @Test
    fun `etiketli metin ve baslik`() {
        val r = PasteParser.parse("""
            Salat-ı Münciye Duası

            Arapça:
            اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ
            Okunuşu: Allahümme salli ala seyyidina Muhammed
            Anlamı:
            Allah'ım, Efendimiz Muhammed'e salat eyle.
            https://example.com/munciye
        """.trimIndent())
        assertEquals("Salat-ı Münciye Duası", r.name)
        assertEquals("اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ", r.arabic)
        // Etiket sayesinde sapkasiz okunus da okunusa gider.
        assertEquals("Allahümme salli ala seyyidina Muhammed", r.reading)
        assertEquals("Allah'ım, Efendimiz Muhammed'e salat eyle.", r.meaning)
        assertEquals("https://example.com/munciye", r.source)
    }

    @Test
    fun `cok satirli arapca birlestirilir, madde isaretleri atilir`() {
        val r = PasteParser.parse("• سُبْحَانَ اللّٰهِ\n• وَبِحَمْدِهِ\n- Sübhânallâhi ve bi-hamdihî")
        assertEquals("سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", r.arabic)
        assertEquals("Sübhânallâhi ve bi-hamdihî", r.reading)
    }

    @Test
    fun `yalnizca turkce okunus - arapca bos kalir`() {
        val r = PasteParser.parse("Lâ ilâhe illallâhü vahdehû lâ şerîke leh")
        assertEquals("", r.arabic)
        assertTrue(r.reading.startsWith("Lâ ilâhe"))
        assertEquals("Lâ ilâhe illallâhü vahdehû", PasteParser.autoName(r))
    }

    @Test
    fun `bos metin`() {
        assertTrue(PasteParser.parse("  \n \n").isEmpty)
    }
}
