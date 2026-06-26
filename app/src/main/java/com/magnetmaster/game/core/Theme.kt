package com.magnetmaster.game.core

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Central brand theme. Magnet Master palette (AI_PROMPT.md §1):
 *   Steel Gray #2B313A, Electric Blue #2F8DFF, Safety Orange #FF7A2A, Bone #F2ECE3.
 * All screens render onto a virtual 1600x900 (16:9) canvas, so font sizes here
 * are in virtual units.
 */
object Theme {
    const val WORLD_W = 1600f
    const val WORLD_H = 900f

    val STEEL = Color.parseColor("#2B313A")
    val STEEL_DARK = Color.parseColor("#1B2027")
    val STEEL_PANEL = Color.parseColor("#232A33")
    val STEEL_LINE = Color.parseColor("#3A434F")
    val BLUE = Color.parseColor("#2F8DFF")
    val BLUE_DIM = Color.parseColor("#1E5DAA")
    val ORANGE = Color.parseColor("#FF7A2A")
    val ORANGE_DIM = Color.parseColor("#A8501C")
    val BONE = Color.parseColor("#F2ECE3")
    val BONE_DIM = Color.parseColor("#9AA1AB")
    val GREEN = Color.parseColor("#3FD17A")
    val RED = Color.parseColor("#FF4D4D")
    val GOLD = Color.parseColor("#FFC94D")

    // Typeface approximations of Fraunces / Inter Tight / JetBrains Mono.
    val display: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val sans: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val sansBold: Typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    val mono: Typeface = Typeface.MONOSPACE

    fun fill(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    fun stroke(color: Int, w: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = w
        this.color = color
        strokeCap = Paint.Cap.ROUND
    }

    fun text(color: Int, size: Float, tf: Typeface = sansBold, align: Paint.Align = Paint.Align.LEFT): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = tf
            textAlign = align
        }
}
