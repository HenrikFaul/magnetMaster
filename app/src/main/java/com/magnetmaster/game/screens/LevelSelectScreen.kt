package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Screen
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui
import com.magnetmaster.game.game.Levels
import kotlin.math.abs

class LevelSelectScreen(game: Game) : Screen(game) {
    private val cols = 9
    private val cell = 150f
    private val gap = 14f
    private val startX = 80f
    private val startY = 170f
    private var scrollY = 0f
    private var dragging = false
    private var dragMoved = 0f
    private var lastY = 0f

    private val cellRects = ArrayList<Pair<RectF, Int>>()
    private val rows: Int get() = (Levels.MAX_LEVEL + cols - 1) / cols
    private val contentH: Float get() = rows * (cell + gap) + 60f
    private val maxScroll: Float get() = (contentH - (Theme.WORLD_H - startY - 30f)).coerceAtLeast(0f)

    override fun update(dt: Float) {}

    override fun render(c: Canvas) {
        buttons.clear(); cellRects.clear()
        c.drawColor(Theme.STEEL_DARK)

        // grid (clipped to content area)
        c.save()
        c.clipRect(0f, startY - 20f, Theme.WORLD_W, Theme.WORLD_H)
        val hi = game.store.highestUnlocked
        for (i in 1..Levels.MAX_LEVEL) {
            val col = (i - 1) % cols
            val row = (i - 1) / cols
            val x = startX + col * (cell + gap)
            val y = startY + row * (cell + gap) - scrollY
            if (y > Theme.WORLD_H || y + cell < startY - 20f) continue
            drawCell(c, x, y, i, i <= hi)
        }
        c.restore()

        // header bar (drawn over grid)
        c.drawRect(0f, 0f, Theme.WORLD_W, startY - 20f, Theme.fill(Theme.STEEL_DARK))
        Ui.label(c, "SELECT LEVEL", 80f, 90f, 56f, Theme.BONE, Paint.Align.LEFT, Theme.display)
        Ui.label(c, "${game.store.totalStars()} ★   ◆ ${game.store.gems}   SP ${game.store.skillPoints}",
            80f, 132f, 30f, Theme.BLUE)
        Ui.button(c, buttons, 1360f, 60f, 200f, 70f, "◄ MENU", Theme.STEEL_PANEL, Theme.BONE, textSize = 32f) {
            game.go(TitleScreen(game))
        }
    }

    private fun drawCell(c: Canvas, x: Float, y: Float, level: Int, unlocked: Boolean) {
        val boss = Levels.isBoss(level)
        val baseColor = when {
            !unlocked -> Theme.STEEL_PANEL
            boss -> Ui.darken(Theme.ORANGE, 0.95f)
            else -> Theme.STEEL_LINE
        }
        Ui.panel(c, x, y, cell, cell, baseColor, 18f)
        if (unlocked) cellRects.add(RectF(x, y, x + cell, y + cell) to level)

        if (!unlocked) {
            Ui.label(c, "🔒", x + cell / 2f, y + cell / 2f + 18f, 48f, Theme.BONE_DIM, Paint.Align.CENTER)
            return
        }
        Ui.label(c, if (boss) "BOSS" else "$level", x + cell / 2f,
            y + cell / 2f + (if (boss) 6f else 16f), if (boss) 34f else 56f,
            if (boss) Theme.STEEL_DARK else Theme.BONE, Paint.Align.CENTER, Theme.display)
        if (boss) Ui.label(c, "$level", x + cell / 2f, y + 44f, 26f, Theme.STEEL_DARK, Paint.Align.CENTER)
        // stars
        val st = game.store.starsFor(level)
        val sy = y + cell - 24f
        for (s in 0 until 3) {
            val col = if (s < st) Theme.GOLD else Ui.withAlpha(Theme.BONE_DIM, 90)
            Ui.label(c, "★", x + cell / 2f - 36f + s * 36f, sy, 30f, col, Paint.Align.CENTER)
        }
    }

    override fun onDown(x: Float, y: Float) {
        super.onDown(x, y); dragging = true; dragMoved = 0f; lastY = y
    }
    override fun onMove(x: Float, y: Float) {
        if (!dragging) return
        val dy = y - lastY; lastY = y
        dragMoved += abs(dy)
        scrollY = (scrollY - dy).coerceIn(0f, maxScroll)
    }
    override fun onUp(x: Float, y: Float) {
        dragging = false
        if (dragMoved < 18f) {
            // first try header buttons
            for (b in buttons) if (b.r.contains(x, y) && b.r.contains(pressX, pressY)) { b.onClick(); return }
            for ((r, lvl) in cellRects) if (r.contains(x, y)) {
                game.go(GameplayScreen(game, lvl, false)); return
            }
        }
        pressX = -1f; pressY = -1f
    }

    override fun onBack(): Boolean { game.go(TitleScreen(game)); return true }
}
