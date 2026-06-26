package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.Paint
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Screen
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui
import com.magnetmaster.game.game.Branch
import com.magnetmaster.game.game.Skills

/** Magnet Lab — 16-node skill tree, 4 branches × 4 nodes (AI_PROMPT §2.5). */
class SkillTreeScreen(game: Game) : Screen(game) {
    private val roman = arrayOf("I", "II", "III", "IV")

    override fun update(dt: Float) {}

    override fun render(c: Canvas) {
        buttons.clear()
        c.drawColor(Theme.STEEL_DARK)
        Ui.label(c, "MAGNET LAB", 80f, 96f, 60f, Theme.BONE, Paint.Align.LEFT, Theme.display)
        Ui.pill(c, 1180f, 48f, 200f, 60f, Theme.STEEL_PANEL)
        Ui.label(c, "SP  ${game.store.skillPoints}", 1280f, 88f, 36f, Theme.BLUE, Paint.Align.CENTER)
        Ui.button(c, buttons, 1400f, 48f, 160f, 60f, "◄ MENU", Theme.STEEL_PANEL, Theme.BONE, textSize = 28f) {
            game.go(TitleScreen(game))
        }

        val branches = Branch.values()
        val colW = 350f
        val startX = 60f
        for (i in branches.indices) {
            drawBranch(c, branches[i], startX + i * (colW + 12f), 150f, colW)
        }

        Ui.button(c, buttons, 60f, 800f, 300f, 70f, "RESET SKILLS", Theme.STEEL_LINE, Theme.BONE, textSize = 28f) {
            game.store.resetSkills()
        }
        Ui.button(c, buttons, 1140f, 800f, 420f, 70f, "▶ PLAY", Theme.ORANGE, Theme.STEEL_DARK, textSize = 36f) {
            game.go(LevelSelectScreen(game))
        }
    }

    private fun drawBranch(c: Canvas, branch: Branch, x: Float, y: Float, w: Float) {
        Ui.panel(c, x, y, w, 600f, Theme.STEEL_PANEL, 22f)
        Ui.label(c, branch.title, x + w / 2f, y + 46f, 34f, Theme.BLUE, Paint.Align.CENTER, Theme.display)
        Ui.label(c, branch.blurb, x + w / 2f, y + 78f, 20f, Theme.BONE_DIM, Paint.Align.CENTER)

        val lvl = game.store.skillLevels[branch.ordinal]
        val nodeR = 44f
        for (n in 0 until 4) {
            val ny = y + 150f + n * 92f
            val ncx = x + w / 2f
            val on = n < lvl
            // connector
            if (n > 0) {
                c.drawLine(ncx, ny - 92f + nodeR, ncx, ny - nodeR,
                    Theme.stroke(if (n <= lvl) Theme.BLUE else Theme.STEEL_LINE, 6f))
            }
            c.drawCircle(ncx, ny, nodeR, Theme.fill(if (on) Theme.BLUE else Theme.STEEL_LINE))
            c.drawCircle(ncx, ny, nodeR, Theme.stroke(if (on) Theme.BONE else Theme.STEEL_LINE, 3f))
            Ui.label(c, roman[n], ncx, ny + 14f, 36f, if (on) Theme.STEEL_DARK else Theme.BONE_DIM, Paint.Align.CENTER, Theme.display)
        }

        // upgrade button
        val maxed = lvl >= Skills.MAX_NODE
        val can = game.store.canUpgrade(branch)
        val label = if (maxed) "MAXED" else "UPGRADE  (1 SP)"
        val col = if (maxed) Theme.STEEL_LINE else if (can) Theme.ORANGE else Theme.STEEL_LINE
        Ui.button(c, buttons, x + 30f, y + 528f, w - 60f, 56f, label, col,
            if (can && !maxed) Theme.STEEL_DARK else Theme.BONE_DIM, textSize = 26f) {
            if (can) { game.store.upgrade(branch); game.haptics.light() }
        }
    }

    override fun onBack(): Boolean { game.go(TitleScreen(game)); return true }
}
