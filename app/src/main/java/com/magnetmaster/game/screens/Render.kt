package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui
import com.magnetmaster.game.engine.Body
import com.magnetmaster.game.engine.Kind
import com.magnetmaster.game.game.Skin
import kotlin.math.cos
import kotlin.math.sin

/** Draws the magnet, field lines and every metal/distractor sprite. */
object Render {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun drawBody(c: Canvas, b: Body) {
        c.save()
        c.translate(b.x, b.y)
        c.rotate(Math.toDegrees(b.spin.toDouble()).toFloat())
        when (b.kind) {
            Kind.BOLT -> nut(c, b.radius)
            Kind.COIN -> coin(c, b.radius)
            Kind.GEAR -> gear(c, b.radius)
            Kind.PLATE -> plate(c, b.radius)
            Kind.WRECKAGE -> wreckage(c, b.radius)
            Kind.BOMB -> bomb(c, b.radius)
            Kind.SHARP -> sharp(c, b.radius)
            Kind.RUBBER -> rubber(c, b.radius)
            Kind.PANEL -> panel(c, b.radius)
            Kind.CORE -> core(c, b.radius)
        }
        c.restore()
    }

    private fun nut(c: Canvas, r: Float) {
        fill.color = 0xFFB9C2CC.toInt()
        hexagon(r)
        c.drawPath(path, fill)
        stroke.color = 0xFF6A7480.toInt(); stroke.strokeWidth = 2f
        c.drawPath(path, stroke)
        fill.color = Theme.STEEL_DARK
        c.drawCircle(0f, 0f, r * 0.42f, fill)
    }

    private fun hexagon(r: Float) {
        path.reset()
        for (i in 0 until 6) {
            val a = Math.PI / 3 * i
            val x = (cos(a) * r).toFloat(); val y = (sin(a) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun coin(c: Canvas, r: Float) {
        fill.color = Theme.GOLD; c.drawCircle(0f, 0f, r, fill)
        stroke.color = 0xFFB8860B.toInt(); stroke.strokeWidth = 3f
        c.drawCircle(0f, 0f, r * 0.78f, stroke)
        fill.color = 0xFFB8860B.toInt()
        Ui.label(c, "$", -r * 0.28f, r * 0.34f, r * 1.05f, 0xFFB8860B.toInt())
    }

    private fun gear(c: Canvas, r: Float) {
        fill.color = 0xFF8A95A1.toInt()
        path.reset()
        val teeth = 9
        for (i in 0 until teeth * 2) {
            val rr = if (i % 2 == 0) r else r * 0.78f
            val a = Math.PI / teeth * i
            val x = (cos(a) * rr).toFloat(); val y = (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, fill)
        fill.color = Theme.STEEL; c.drawCircle(0f, 0f, r * 0.45f, fill)
        fill.color = 0xFF8A95A1.toInt(); c.drawCircle(0f, 0f, r * 0.18f, fill)
    }

    private fun plate(c: Canvas, r: Float) {
        fill.color = 0xFF9AA6B2.toInt()
        c.drawRoundRect(-r, -r * 0.62f, r, r * 0.62f, 8f, 8f, fill)
        stroke.color = 0xFF60697A.toInt(); stroke.strokeWidth = 3f
        c.drawRoundRect(-r, -r * 0.62f, r, r * 0.62f, 8f, 8f, stroke)
        fill.color = 0xFF60697A.toInt()
        c.drawCircle(-r * 0.7f, -r * 0.4f, r * 0.1f, fill)
        c.drawCircle(r * 0.7f, -r * 0.4f, r * 0.1f, fill)
        c.drawCircle(-r * 0.7f, r * 0.4f, r * 0.1f, fill)
        c.drawCircle(r * 0.7f, r * 0.4f, r * 0.1f, fill)
    }

    private fun wreckage(c: Canvas, r: Float) {
        fill.color = 0xFF6E7682.toInt()
        path.reset()
        val pts = floatArrayOf(-1f, -0.6f, 0.3f, -1f, 1f, -0.3f, 0.7f, 0.8f, -0.5f, 1f, -1f, 0.2f)
        path.moveTo(pts[0] * r, pts[1] * r)
        var i = 2
        while (i < pts.size) { path.lineTo(pts[i] * r, pts[i + 1] * r); i += 2 }
        path.close(); c.drawPath(path, fill)
        stroke.color = 0xFF454C57.toInt(); stroke.strokeWidth = 3f; c.drawPath(path, stroke)
    }

    private fun bomb(c: Canvas, r: Float) {
        fill.color = 0xFF14181E.toInt(); c.drawCircle(0f, 0f, r, fill)
        stroke.color = Theme.RED; stroke.strokeWidth = 4f; c.drawCircle(0f, 0f, r, stroke)
        // colour-blind safe X marker (AI_PROMPT §3 accessibility)
        stroke.color = Theme.RED; stroke.strokeWidth = 5f
        val d = r * 0.45f
        c.drawLine(-d, -d, d, d, stroke); c.drawLine(-d, d, d, -d, stroke)
        // fuse spark
        fill.color = Theme.ORANGE; c.drawCircle(0f, -r, r * 0.18f, fill)
    }

    private fun sharp(c: Canvas, r: Float) {
        fill.color = 0xFF3A3027.toInt()
        path.reset()
        for (i in 0 until 8) {
            val rr = if (i % 2 == 0) r else r * 0.45f
            val a = Math.PI / 4 * i
            val x = (cos(a) * rr).toFloat(); val y = (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, fill)
        stroke.color = Theme.ORANGE; stroke.strokeWidth = 3f; c.drawPath(path, stroke)
    }

    private fun rubber(c: Canvas, r: Float) {
        fill.color = 0xFF2A2E34.toInt(); c.drawCircle(0f, 0f, r, fill)
        stroke.color = 0xFF4A5058.toInt(); stroke.strokeWidth = 6f
        c.drawCircle(0f, 0f, r * 0.7f, stroke)
    }

    private fun panel(c: Canvas, r: Float) {
        fill.color = 0xFF7E8794.toInt()
        c.drawRoundRect(-r, -r * 0.7f, r, r * 0.7f, 6f, 6f, fill)
        stroke.color = Theme.ORANGE; stroke.strokeWidth = 3f
        c.drawRoundRect(-r, -r * 0.7f, r, r * 0.7f, 6f, 6f, stroke)
        stroke.color = 0xFF565E69.toInt(); stroke.strokeWidth = 2f
        c.drawLine(-r * 0.5f, -r * 0.7f, -r * 0.5f, r * 0.7f, stroke)
        c.drawLine(r * 0.5f, -r * 0.7f, r * 0.5f, r * 0.7f, stroke)
    }

    private fun core(c: Canvas, r: Float) {
        glowPaint.shader = RadialGradient(0f, 0f, r * 1.4f,
            intArrayOf(0xFFFFE08A.toInt(), 0xFFFF7A2A.toInt(), 0x00FF7A2A), null, Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, r * 1.4f, glowPaint)
        glowPaint.shader = null
        fill.color = 0xFFFFC14D.toInt(); c.drawCircle(0f, 0f, r * 0.7f, fill)
        fill.color = Theme.BONE; c.drawCircle(0f, 0f, r * 0.3f, fill)
    }

    // ---- magnet + field lines ----
    fun drawFieldLines(c: Canvas, x: Float, y: Float, radius: Float, phase: Float,
                       reduceMotion: Boolean, color: Int) {
        stroke.color = Ui.withAlpha(color, 120); stroke.strokeWidth = 4f
        val pulses = if (reduceMotion) 1 else 3
        for (p in 0 until pulses) {
            val frac = if (reduceMotion) 0.7f else ((phase + p.toFloat() / pulses) % 1f)
            val rr = radius * (0.25f + 0.75f * frac)
            stroke.color = Ui.withAlpha(color, (140 * (1f - frac)).toInt().coerceIn(20, 160))
            for (i in 0 until 8) {
                val a = Math.PI / 4 * i
                val sx = x + (cos(a) * radius * 0.12f).toFloat()
                val sy = y + (sin(a) * radius * 0.12f).toFloat()
                val ex = x + (cos(a) * rr).toFloat()
                val ey = y + (sin(a) * rr).toFloat()
                c.drawLine(sx, sy, ex, ey, stroke)
            }
        }
    }

    /** Horseshoe magnet glyph centred at (x,y). poleUp rotates the opening up. */
    fun drawMagnet(c: Canvas, x: Float, y: Float, size: Float, skin: Skin, repel: Boolean) {
        c.save()
        c.translate(x, y)
        // soft glow
        glowPaint.shader = RadialGradient(0f, 0f, size * 1.6f,
            intArrayOf(Ui.withAlpha(skin.glow, if (repel) 150 else 90), 0x00000000), null, Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, size * 1.6f, glowPaint)
        glowPaint.shader = null

        val w = size            // half width of the U
        val arm = size * 0.95f  // arm height
        val thick = size * 0.5f
        // left pole
        fill.color = skin.poleA
        c.drawRoundRect(-w, -arm, -w + thick, arm, 8f, 8f, fill)
        // right pole
        fill.color = skin.poleB
        c.drawRoundRect(w - thick, -arm, w, arm, 8f, 8f, fill)
        // bottom bridge
        fill.color = Ui.darken(skin.poleA, 0.8f)
        c.drawRoundRect(-w, arm - thick, w, arm, 10f, 10f, fill)
        // pole caps (north/south)
        fill.color = Theme.BONE
        c.drawRect(-w, -arm, -w + thick, -arm + thick * 0.45f, fill)
        fill.color = Ui.withAlpha(Theme.BONE, 220)
        c.drawRect(w - thick, -arm, w, -arm + thick * 0.45f, fill)
        c.restore()
    }
}
