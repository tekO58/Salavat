package com.tekin.salavat

/**
 * Tespih balonunun sayaci. Asil sayac web uygulamasinda (web/tasbih.js);
 * balon onun kopyasini gosterir ve kendi dokunuslarini zamanlariyla biriktirir.
 * Uygulama one gelince web bu dokunuslari alip ayni kuralla (TasbihCore.tap)
 * isler: [take]. Sayma kurali tasbih.js'teki tap / undo ile ayni.
 */
class BubbleCounter(
    val id: String,
    val name: String,
    var count: Int,
    var target: Int,
    /** "round": hedefte yeni tur (0'dan); "stop": hedefte durur. */
    val onTarget: String,
    /** Kac adette bir titresim (0: yalnizca hedefte). */
    val every: Int,
    val finish: Boolean,
    val sound: Boolean,
    val colors: BubbleColors,
    /** Web'e henuz islenmemis dokunuslarin zamani (ms). */
    val taps: MutableList<Long> = mutableListOf()
) {
    enum class Event { BLOCKED, COUNTED, MILESTONE, COMPLETED }

    val isDone: Boolean get() = onTarget == "stop" && count >= target

    fun tap(now: Long): Event {
        if (isDone) return Event.BLOCKED
        val next = count + 1
        taps.add(now)
        if (next >= target) {
            count = if (onTarget == "stop") target else 0
            return Event.COMPLETED
        }
        count = next
        return if (every > 0 && next % every == 0) Event.MILESTONE else Event.COUNTED
    }

    /** Yalnizca balonun kendi (web'e gitmemis) dokunuslari geri alinir. */
    fun undo(): Boolean {
        if (taps.isEmpty()) return false
        when {
            onTarget == "stop" && count >= target -> count = target - 1
            count > 0 -> count--
            onTarget != "stop" -> count = target - 1
            else -> return false
        }
        taps.removeAt(taps.size - 1)
        return true
    }

    /** Web'deki sayac degisti (uygulamada sayildi, sifirlandi): bekleyen dokunuslar ustune eklenir. */
    fun update(baseCount: Int, baseTarget: Int) {
        target = baseTarget.coerceAtLeast(1)
        count = baseCount
        val pending = taps.toList()
        taps.clear()
        // Bekleyenleri yeni tabanin ustune yeniden say (zamanlari ayni kalir).
        for (at in pending) tap(at)
    }

    /** Web'e islenecek dokunuslar; alindiktan sonra bosaltilir. */
    fun take(): List<Long> {
        val out = taps.toList()
        taps.clear()
        return out
    }
}

/** Balonun renkleri: web'deki temadan ve tespih materyalinden. */
data class BubbleColors(
    val bg: Int, val stroke: Int, val text: Int, val text2: Int,
    val accent: Int, val bead: Int, val beadLight: Int
)
