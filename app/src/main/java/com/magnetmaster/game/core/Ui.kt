package com.magnetmaster.game.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** A registered, hit-testable button for the current frame. */
class Btn(val r: RectF, val onClick: () -> Unit)

/** Immediate-mode drawing helpers shared by every screen. */
object Ui {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun panel(c: Canvas, x: Float, y: Float, w: Float, h: Float, fill: Int = Theme.STEEL_PANEL, radius: Float = 22f) {
        p.reset(); p.isAntiAlias = true
        p.style = Paint.Style.FILL; p.color = fill
        c.drawRoundRect(x, y, x + w, y + h, radius, radius, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = Theme.STEEL_LINE
        c.drawRoundRect(x, y, x + w, y + h, radius, radius, p)
    }

    fun pill(c: Canvas, x: Float, y: Float, w: Float, h: Float, fill: Int, radius: Float = h / 2f) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL; p.color = fill
        c.drawRoundRect(x, y, x + w, y + h, radius, radius, p)
    }

    private val txt = Theme.text(Theme.BONE, 32f)
    fun label(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
              align: Paint.Align = Paint.Align.LEFT, tf: android.graphics.Typeface = Theme.sansBold) {
        txt.color = color; txt.textSize = size; txt.textAlign = align; txt.typeface = tf
        c.drawText(s, x, y, txt)
    }

    /** Draws a button and registers it for hit-testing this frame. */
    fun button(c: Canvas, list: MutableList<Btn>, x: Float, y: Float, w: Float, h: Float,
               text: String, fill: Int, textColor: Int = Theme.BONE,
               pressed: Boolean = false, textSize: Float = 38f, onClick: () -> Unit) {
        val rr = RectF(x, y, x + w, y + h)
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.color = if (pressed) darken(fill, 0.82f) else fill
        c.drawRoundRect(rr, 16f, 16f, p)
        // top sheen
        p.color = lighten(fill, 1.12f)
        c.drawRoundRect(x, y, x + w, y + h * 0.5f, 16f, 16f, p)
        p.color = if (pressed) darken(fill, 0.82f) else fill
        c.drawRect(x, y + h * 0.32f, x + w, y + h, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = darken(fill, 0.7f)
        c.drawRoundRect(rr, 16f, 16f, p)
        label(c, text, x + w / 2f, y + h / 2f + textSize * 0.35f, textSize, textColor, Paint.Align.CENTER)
        list.add(Btn(rr, onClick))
    }

    fun iconButton(c: Canvas, list: MutableList<Btn>, x: Float, y: Float, size: Float,
                   glyph: String, fill: Int, pressed: Boolean = false, onClick: () -> Unit) {
        button(c, list, x, y, size, size, glyph, fill, Theme.BONE, pressed, size * 0.5f, onClick)
    }

    fun darken(color: Int, f: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = (((color ushr 16) and 0xFF) * f).toInt().coerceIn(0, 255)
        val g = (((color ushr 8) and 0xFF) * f).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * f).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    fun lighten(color: Int, f: Float): Int = darken(color, f)

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
