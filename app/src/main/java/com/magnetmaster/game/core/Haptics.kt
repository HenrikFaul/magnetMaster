package com.magnetmaster.game.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** Capacitor-Haptics style feedback (AI_PROMPT.md §3). Buzzes on every slam. */
class Haptics(ctx: Context) {
    private val vib = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var enabled = true

    private fun buzz(ms: Long, amp: Int) {
        if (!enabled) return
        val v = vib ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, amp))
            } else {
                @Suppress("DEPRECATION") v.vibrate(ms)
            }
        } catch (_: Throwable) {}
    }

    fun light() = buzz(12, 60)
    fun medium() = buzz(22, 130)
    fun heavy() = buzz(40, 255)
}
