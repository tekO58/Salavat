package com.tekin.salavat

import java.util.Locale

/**
 * Akilli yapistir: internetten kopyalanan bir metin blogunu ad, Arapca,
 * okunus, anlam ve kaynak alanlarina ayirir. Android'e bagli degil.
 *
 * Kurallar sirasiyla:
 *  1. "Okunuşu:", "Anlamı:", "Meali:", "Kaynak:" gibi etiketler kendinden
 *     sonraki satirlari o alana yollar.
 *  2. Harflerinin cogu Arapca olan satir her zaman Arapca alanina gider.
 *  3. Baglanti ya da site adi iceren satir kaynaktir.
 *  4. Etiketsiz Latin satir: sapkali harfler (â î û) ve tireler okunusa,
 *     Turkce bag kelimeleri (bir, için, eyle, Allah'ım ...) anlama isaret eder.
 *  5. Ilk satir kisaysa ve "salât", "duası", "zikir" gibi bir kelime
 *     iceriyorsa kaydin adidir.
 *
 * Sonuc her zaman kullaniciya duzeltilmek uzere gosterilir.
 */
object PasteParser {

    data class Result(
        val name: String = "",
        val arabic: String = "",
        val reading: String = "",
        val meaning: String = "",
        val source: String = ""
    ) {
        val isEmpty: Boolean get() = name.isBlank() && arabic.isBlank() && reading.isBlank() && meaning.isBlank() && source.isBlank()
    }

    private enum class Field { NAME, ARABIC, READING, MEANING, SOURCE }

    private val TR = Locale.forLanguageTag("tr")

    /** Uzun etiket once: "türkçe okunuşu" "okunuşu"ndan once denenmeli. */
    private val LABELS: List<Pair<String, Field>> = listOf(
        "türkçe okunuşu" to Field.READING, "türkçe okunuş" to Field.READING, "latin harfleriyle" to Field.READING,
        "okunuşu" to Field.READING, "okunuş" to Field.READING, "okunusu" to Field.READING, "okunus" to Field.READING,
        "türkçe anlamı" to Field.MEANING, "türkçe meali" to Field.MEANING, "türkçe tercümesi" to Field.MEANING,
        "anlamı" to Field.MEANING, "anlami" to Field.MEANING, "anlam" to Field.MEANING, "meali" to Field.MEANING,
        "meal" to Field.MEANING, "manası" to Field.MEANING, "mânâsı" to Field.MEANING, "tercümesi" to Field.MEANING,
        "arapça yazılışı" to Field.ARABIC, "arapça metni" to Field.ARABIC, "arapçası" to Field.ARABIC, "arapça" to Field.ARABIC,
        "kaynakça" to Field.SOURCE, "kaynak" to Field.SOURCE, "nispet" to Field.SOURCE,
        "adı" to Field.NAME, "başlık" to Field.NAME
    )

    private val TITLE_WORDS = listOf(
        "salât", "salat", "salavat", "salevat", "salâvat", "duası", "dua", "zikri", "zikir", "tesbih",
        "virdi", "esmâ", "esma", "tevhid", "tevhîd", "istiğfar", "istiğfâr", "suresi", "sûresi", "âyeti", "ayeti"
    )

    private val MEANING_WORDS = setOf(
        "bir", "için", "olan", "eyle", "ki", "bizi", "beni", "bize", "bana", "ey", "allah'ım", "allahım", "rabbim",
        "rabbimiz", "efendimiz", "senin", "sana", "senden", "bütün", "olarak", "gibi", "şüphesiz", "öyle", "onun",
        "ona", "onu", "kendisi", "her", "ve", "çok", "yüce", "olsun", "et", "ver", "kıl", "bu", "şey"
    )

    private val DOMAIN = Regex("""\b[\w-]+\.(com|org|net|tr|info|edu)\b""", RegexOption.IGNORE_CASE)

    fun parse(text: String): Result {
        val name = StringBuilder()
        val arabic = mutableListOf<String>()
        val reading = mutableListOf<String>()
        val meaning = mutableListOf<String>()
        val source = mutableListOf<String>()
        var mode: Field? = null
        var first = true
        val contentLines = text.lines().map { cleanLine(it) }.filter { it.isNotEmpty() }

        for ((index, raw) in contentLines.withIndex()) {
            var line = raw
            val label = splitLabel(line)
            if (label != null) {
                mode = label.first
                line = label.second
                if (line.isEmpty()) { first = false; continue }
            }
            when {
                arabicRatio(line) > 0.5 -> arabic += line
                // Baglanti her zaman kaynaktir; cumle icindeki "site.com" yalnizca etiketsiz satirda.
                isLink(line) || (looksLikeSource(line) && mode != Field.MEANING && mode != Field.READING) -> source += line
                mode == Field.NAME -> { if (name.isEmpty()) name.append(line) else name.append(' ').append(line); mode = null }
                mode == Field.READING -> reading += line
                mode == Field.MEANING -> meaning += line
                mode == Field.SOURCE -> source += line
                first && name.isEmpty() && index < contentLines.size - 1 && looksLikeTitle(line) -> name.append(line)
                isReading(line) -> reading += line
                else -> meaning += line
            }
            first = false
        }
        return Result(
            name = name.toString().trim().trimEnd(':'),
            arabic = arabic.joinToString(" "),
            reading = reading.joinToString(" "),
            meaning = meaning.joinToString(" "),
            source = source.joinToString(" · ")
        )
    }

    /** Ad bos kalirsa okunusun (yoksa anlamin) ilk dort kelimesi. */
    fun autoName(r: Result): String {
        val base = r.reading.ifBlank { r.meaning }
        val words = base.split(Regex("\\s+")).filter { it.isNotBlank() }.take(4)
        return words.joinToString(" ").trimEnd(',', '.', ';', ':', '!').take(Tasbih.MAX_NAME)
    }

    fun isArabicChar(c: Char): Boolean =
        c in '؀'..'ۿ' || c in 'ݐ'..'ݿ' || c in 'ࢠ'..'ࣿ' ||
            c in 'ﭐ'..'﷿' || c in 'ﹰ'..'﻿'

    /** Harflerin (bosluk ve noktalama haric) ne kadari Arapca. */
    fun arabicRatio(line: String): Double {
        val letters = line.filter { it.isLetter() || isArabicChar(it) }
        if (letters.isEmpty()) return 0.0
        return letters.count { isArabicChar(it) }.toDouble() / letters.length
    }

    /** Bas taraftaki madde isareti, numara ve tirnaklar atilir. */
    private fun cleanLine(line: String): String =
        line.trim().trimStart('•', '-', '–', '—', '*', '>', '"', '“', '”', '«', '»', ' ')
            .replace(Regex("^\\d+[.)]\\s+"), "").trim()

    /** "Okunuşu: Allâhümme..." -> (READING, "Allâhümme..."); etiket yoksa null. */
    private fun splitLabel(line: String): Pair<Field, String>? {
        val lower = line.lowercase(TR)
        for ((label, field) in LABELS) {
            if (!lower.startsWith(label)) continue
            val rest = line.substring(label.length)
            val trimmed = rest.trimStart()
            return when {
                trimmed.isEmpty() -> field to ""
                trimmed.startsWith(":") || trimmed.startsWith("-") || trimmed.startsWith("–") ->
                    field to trimmed.drop(1).trim()
                else -> continue
            }
        }
        return null
    }

    private fun isLink(line: String): Boolean {
        val lower = line.lowercase(TR)
        return "http://" in lower || "https://" in lower || lower.startsWith("www.")
    }

    private fun looksLikeSource(line: String): Boolean {
        val lower = line.lowercase(TR)
        return "http" in lower || "www." in lower || DOMAIN.containsMatchIn(lower)
    }

    private fun looksLikeTitle(line: String): Boolean {
        if (line.length > 45) return false
        if (line.trimEnd().lastOrNull() in listOf('.', ',', ';', '!')) return false
        val words = words(line)
        if (words.size > 6) return false
        return words.any { w -> TITLE_WORDS.any { w.startsWith(it) } }
    }

    private fun isReading(line: String): Boolean {
        val words = words(line)
        if (words.isEmpty()) return false
        val readingMarks = line.count { it in "âîûÂÎÛ" } + line.count { it == '-' }
        val meaningMarks = words.count { it in MEANING_WORDS }
        return readingMarks > meaningMarks
    }

    private fun words(line: String): List<String> =
        line.lowercase(TR).split(Regex("\\s+"))
            .map { it.trim(',', '.', ';', ':', '!', '?', '(', ')', '"', '“', '”') }
            .filter { it.isNotEmpty() }
}
