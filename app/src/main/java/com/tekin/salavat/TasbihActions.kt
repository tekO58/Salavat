package com.tekin.salavat

import android.app.Activity
import android.content.Intent
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * Tesbih uzerindeki islemler: hedef, gorunum, sifirla, gecmis.
 * Her salavatin tek tesbihi var (id = salavat id); kopyalama ve silme yok.
 * Sayma ekranindaki ⋮ menusunden aciliyor.
 */
object TasbihActions {

    const val EXTRA_ID = "tasbih_id"

    fun openCounter(activity: Activity, id: String) {
        // CLEAR_TOP: salavat -> tespih -> ℹ -> tespih zinciri yigilmasin.
        activity.startActivity(Intent(activity, TasbihCounterActivity::class.java).putExtra(EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    /**
     * onChanged: sayac/ayar degisti, ekran yeniden cizilsin.
     */
    fun showMenu(
        activity: Activity,
        store: TasbihStore,
        t: Tasbih,
        onChanged: () -> Unit
    ) {
        val items = mutableListOf<Pair<String, () -> Unit>>()
        items += "🎯  Hedef: ${TasbihUi.num(t.target)}" to {
            askNumber(activity, "Hedef", "Kaç kere okunacak?", t.target, 1, Tasbih.MAX_TARGET) { target ->
                store.get(t.id)?.let { store.save(it.copy(target = target, count = TasbihLogic.clampCount(it.count, target))) }
                onChanged()
            }
        }
        items += if (t.onTarget == TasbihOnTarget.STOP) "🔁  Hedefte: dur  →  yeni tur başlat" to {
            store.get(t.id)?.let { store.save(it.copy(onTarget = TasbihOnTarget.NEW_ROUND)) }
            onChanged()
        } else "⏹  Hedefte: yeni tur  →  dur" to {
            store.get(t.id)?.let { store.save(it.copy(onTarget = TasbihOnTarget.STOP)) }
            onChanged()
        }
        items += "🏁  Bitiş titreşimi: ${t.finishVibe.label}" to {
            pickFinish(activity, TasbihFeedback(activity), t.finishVibe) { v ->
                store.get(t.id)?.let { store.save(it.copy(finishVibe = v)) }
                onChanged()
            }
        }
        items += "🎨  Tespih görünümü" to {
            // Yalnizca bu tesbihin materyali degisir; guncel hali okunur ki
            // arada sayilan boncuklar ezilmesin.
            TasbihAppearancePicker.show(activity, t.material) { m ->
                store.get(t.id)?.let { store.save(it.copy(material = m)) }
                onChanged()
            }
        }
        items += "🔄  Sıfırla" to { confirmReset(activity, store, t, onChanged) }
        items += "📈  Geçmiş" to { showHistory(activity, store, t) }

        AlertDialog.Builder(activity)
            .setTitle(t.name)
            .setItems(items.map { it.first }.toTypedArray()) { _, which -> items[which].second() }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    fun confirmReset(activity: Activity, store: TasbihStore, t: Tasbih, onChanged: () -> Unit) {
        val options = arrayOf(
            "Sayacı ve turu sıfırla  (toplam kalır)",
            "Toplamı da sıfırla"
        )
        AlertDialog.Builder(activity)
            .setTitle("Sıfırla · ${t.name}")
            .setItems(options) { _, which ->
                if (which == 0) {
                    store.resetCounter(t.id)
                    Toast.makeText(activity, "Sayaç sıfırlandı", Toast.LENGTH_SHORT).show()
                    onChanged()
                } else {
                    AlertDialog.Builder(activity)
                        .setTitle("Toplam da sıfırlansın mı?")
                        .setMessage("\"${t.name}\" için sayaç, tur ve toplam (${TasbihUi.num(t.total)} adet) sıfırlanacak. Günlük geçmiş kalır.")
                        .setPositiveButton("Sıfırla") { _, _ ->
                            store.resetAll(t.id)
                            Toast.makeText(activity, "Sıfırlandı", Toast.LENGTH_SHORT).show()
                            onChanged()
                        }
                        .setNegativeButton("Vazgeç", null)
                        .show()
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    fun showHistory(activity: Activity, store: TasbihStore, t: Tasbih) {
        val days = store.historyOf(t.id)
        val text = buildString {
            if (days.isEmpty()) append("Henüz kayıt yok.\n")
            days.take(30).forEach { (day, count) ->
                append(TasbihStore.dayLabel(day)).append(": ").append(TasbihUi.num(count)).append('\n')
            }
            if (days.size > 30) append("…\n")
            append("\nToplam: ").append(TasbihUi.num(t.total))
            append("\nTamamlanan tur: ").append(TasbihUi.num(t.rounds))
        }
        AlertDialog.Builder(activity)
            .setTitle("📈 ${t.name}")
            .setMessage(text)
            .setPositiveButton("Tamam", null)
            .show()
    }

    /**
     * Sayi soran kucuk dialog. Deger min..max disindaysa uyarip acik kalir.
     */
    fun askNumber(
        activity: Activity,
        title: String,
        message: String?,
        current: Int?,
        min: Int,
        max: Int,
        onValue: (Int) -> Unit
    ) {
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current?.toString().orEmpty())
            setSelection(text.length)
            hint = "$min – ${TasbihUi.num(max)}"
        }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = TasbihUi.dp(activity, 22)
            setPadding(pad, TasbihUi.dp(activity, 6), pad, 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .apply { if (message != null) setMessage(message) }
            .setView(box)
            .setPositiveButton("Tamam", null)
            .setNegativeButton("Vazgeç", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = TasbihLogic.parseInt(input.text.toString(), min, max)
            if (value == null) {
                Toast.makeText(activity, "$min ile ${TasbihUi.num(max)} arasında bir sayı gir", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onValue(value)
            dialog.dismiss()
        }
    }

    /** Titresim araligi secici: hazir secenekler + Özel. */
    fun pickEvery(activity: Activity, current: Int, onValue: (Int) -> Unit) {
        val presets = Tasbih.EVERY_PRESETS
        val labels = presets.map { Tasbih.everyLabel(it) } + "Özel…"
        val checked = presets.indexOf(current).let { if (it < 0) labels.size - 1 else it }
        AlertDialog.Builder(activity)
            .setTitle("Titreşim")
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                dialog.dismiss()
                if (which < presets.size) {
                    onValue(presets[which])
                } else {
                    askNumber(
                        activity, "Özel titreşim aralığı", "Kaç adette bir titresin?",
                        current.takeIf { it > 1 }, 1, Tasbih.MAX_EVERY, onValue
                    )
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    fun pickFinish(activity: Activity, feedback: TasbihFeedback, current: TasbihFinishVibe, onValue: (TasbihFinishVibe) -> Unit) {
        val values = TasbihFinishVibe.values()
        AlertDialog.Builder(activity)
            .setTitle("Bitiş titreşimi")
            .setSingleChoiceItems(values.map { it.label }.toTypedArray(), values.indexOf(current)) { dialog, which ->
                feedback.finish(values[which])   // secerken hissettir
                onValue(values[which])
                dialog.dismiss()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}
