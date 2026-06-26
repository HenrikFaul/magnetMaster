package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.Paint
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Screen
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui

class SettingsScreen(game: Game) : Screen(game) {
    private var confirmReset = false

    override fun update(dt: Float) {}

    override fun render(c: Canvas) {
        buttons.clear()
        c.drawColor(Theme.STEEL_DARK)
        Ui.label(c, "SETTINGS", 80f, 96f, 60f, Theme.BONE, Paint.Align.LEFT, Theme.display)
        Ui.button(c, buttons, 1400f, 48f, 160f, 60f, "◄ MENU", Theme.STEEL_PANEL, Theme.BONE, textSize = 28f) {
            game.go(TitleScreen(game))
        }

        toggle(c, 80f, 200f, "Haptics", game.store.hapticsOn) {
            game.store.hapticsOn = !game.store.hapticsOn; game.store.save(); game.refreshHaptics()
            if (game.store.hapticsOn) game.haptics.medium()
        }
        toggle(c, 80f, 300f, "Sound", game.store.soundOn) {
            game.store.soundOn = !game.store.soundOn; game.store.save()
        }
        toggle(c, 80f, 400f, "Reduce Motion (static field lines)", game.store.reduceMotion) {
            game.store.reduceMotion = !game.store.reduceMotion; game.store.save()
        }

        // info
        Ui.panel(c, 880f, 200f, 640f, 300f, Theme.STEEL_PANEL, 22f)
        Ui.label(c, "MAGNET MASTER", 910f, 256f, 36f, Theme.BLUE, Paint.Align.LEFT, Theme.display)
        Ui.label(c, "Physics Arcade · v1.0.0", 910f, 300f, 26f, Theme.BONE_DIM)
        Ui.label(c, "Pull it in. Slam it home.", 910f, 344f, 26f, Theme.BONE_DIM)
        Ui.label(c, "Age 9+ · mild fantasy violence (bombs)", 910f, 388f, 22f, Theme.BONE_DIM)
        Ui.label(c, "Best: ${game.store.totalStars()} ★   ◆ ${game.store.gems}", 910f, 440f, 26f, Theme.BONE)

        // reset progress
        if (!confirmReset) {
            Ui.button(c, buttons, 80f, 760f, 420f, 80f, "RESET PROGRESS", Theme.STEEL_LINE, Theme.RED, textSize = 30f) {
                confirmReset = true
            }
        } else {
            Ui.label(c, "Erase ALL progress?", 80f, 740f, 30f, Theme.RED)
            Ui.button(c, buttons, 80f, 760f, 200f, 80f, "YES, WIPE", Theme.RED, Theme.STEEL_DARK, textSize = 28f) {
                wipe(); confirmReset = false
            }
            Ui.button(c, buttons, 300f, 760f, 200f, 80f, "CANCEL", Theme.STEEL_LINE, Theme.BONE, textSize = 28f) {
                confirmReset = false
            }
        }
    }

    private fun toggle(c: Canvas, x: Float, y: Float, label: String, on: Boolean, onClick: () -> Unit) {
        Ui.label(c, label, x, y + 44f, 34f, Theme.BONE)
        val tx = x + 660f
        Ui.pill(c, tx, y, 120f, 60f, if (on) Theme.GREEN else Theme.STEEL_LINE)
        c.drawCircle(if (on) tx + 90f else tx + 30f, y + 30f, 24f, Theme.fill(Theme.BONE))
        buttons.add(com.magnetmaster.game.core.Btn(android.graphics.RectF(tx, y, tx + 120f, y + 60f), onClick))
    }

    private fun wipe() {
        val s = game.store
        s.gems = 50; s.coins = 0; s.skillPoints = 1; s.highestUnlocked = 1
        for (i in s.skillLevels.indices) s.skillLevels[i] = 0
        s.unlockedSkins.clear(); s.unlockedSkins.add("default"); s.activeSkin = "default"
        s.dailyBestKg = 0f; s.tutorialSeen = false
        // clear stars by re-saving an empty map: easiest is to rebuild via reflection-free approach
        s.clearStars()
        s.save()
    }

    override fun onBack(): Boolean { game.go(TitleScreen(game)); return true }
}
