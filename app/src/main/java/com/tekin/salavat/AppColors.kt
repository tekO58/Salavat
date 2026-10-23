package com.tekin.salavat

import android.graphics.Color

/**
 * Koddan kurulan view'lar icin renk paleti.
 *
 * Tesbih kodu workplanner-app'ten kopyalandi ve renkleri buradan aliyor;
 * isimler oradaki AppColors ile ayni (yalnizca kullanilanlar). Simdilik
 * yalnizca koyu tema; gece/gunduz secimi sonra eklenecek.
 */
object AppColors {

    private fun c(hex: String): Int = Color.parseColor(hex)

    // Zeminler
    val BG_APP = c("#12172B")
    val BG_CARD = c("#151D32")
    val BG_FIELD = c("#0E1526")
    val BG_ELEVATED = c("#1A2440")

    // Cizgiler
    val STROKE = c("#26314D")
    val STROKE_STRONG = c("#333A55")

    // Metin
    val TEXT_PRIMARY = c("#F2EFE6")
    val TEXT_SECONDARY = c("#AEB5CF")
    val TEXT_MUTED = c("#8790AD")

    // Vurgu
    val ACCENT = c("#E8B34A")
    val ON_ACCENT = c("#111522")

    // Durum
    val SUCCESS = c("#7FBF8C")
    val WARNING = c("#C79A46")
}
