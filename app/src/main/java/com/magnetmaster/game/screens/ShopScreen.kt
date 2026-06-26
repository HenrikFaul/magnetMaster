package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.Paint
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Screen
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui
import com.magnetmaster.game.game.Skins

/** Cosmetic shop — magnet skins bought with gems, plus a rewarded "daily wheel". */
class ShopScreen(game: Game) : Screen(game) {
    private var t = 0f
    private var freeClaimed = false

    override fun update(dt: Float) { t += dt }

    override fun render(c: Canvas) {
        buttons.clear()
        c.drawColor(Theme.STEEL_DARK)
        Ui.label(c, "MAGNET SHOP", 80f, 92f, 56f, Theme.BONE, Paint.Align.LEFT, Theme.display)
        Ui.pill(c, 1120f, 48f, 260f, 60f, Theme.STEEL_PANEL)
        Ui.label(c, "◆ ${game.store.gems}", 1250f, 88f, 36f, Theme.BLUE, Paint.Align.CENTER)
        Ui.button(c, buttons, 1400f, 48f, 160f, 60f, "◄ MENU", Theme.STEEL_PANEL, Theme.BONE, textSize = 28f) {
            game.go(TitleScreen(game))
        }

        val skins = Skins.ALL
        val cw = 350f; val ch = 360f; val sx = 60f; val sy = 150f
        for (i in skins.indices) {
            val x = sx + i * (cw + 12f); val y = sy
            val skin = skins[i]
            Ui.panel(c, x, y, cw, ch, Theme.STEEL_PANEL, 22f)
            Render.drawFieldLines(c, x + cw / 2f, y + 140f, 120f, (t * 0.4f) % 1f, game.store.reduceMotion, skin.glow)
            Render.drawMagnet(c, x + cw / 2f, y + 140f, 60f, skin, false)
            Ui.label(c, skin.name, x + cw / 2f, y + 250f, 34f, Theme.BONE, Paint.Align.CENTER, Theme.display)

            val owned = game.store.unlockedSkins.contains(skin.id)
            val equipped = game.store.activeSkin == skin.id
            val label: String; val col: Int; val tcol: Int
            when {
                equipped -> { label = "EQUIPPED"; col = Theme.GREEN; tcol = Theme.STEEL_DARK }
                owned -> { label = "EQUIP"; col = Theme.BLUE; tcol = Theme.STEEL_DARK }
                else -> { label = "◆ ${skin.costGems}"; col = if (game.store.gems >= skin.costGems) Theme.ORANGE else Theme.STEEL_LINE; tcol = Theme.STEEL_DARK }
            }
            Ui.button(c, buttons, x + 40f, y + 285f, cw - 80f, 56f, label, col, tcol, textSize = 28f) {
                if (game.store.buySkin(skin)) game.haptics.medium()
            }
        }

        // rewarded daily wheel
        Ui.panel(c, 60f, 540f, 720f, 300f, Theme.STEEL_PANEL, 24f)
        Ui.label(c, "DAILY WHEEL", 100f, 600f, 36f, Theme.BLUE, Paint.Align.LEFT, Theme.display)
        Ui.label(c, "Watch a short ad for free gems.", 100f, 644f, 28f, Theme.BONE_DIM)
        Ui.button(c, buttons, 100f, 690f, 640f, 90f, if (freeClaimed) "COME BACK LATER" else "▶ SPIN: +25 ◆",
            if (freeClaimed) Theme.STEEL_LINE else Theme.ORANGE, if (freeClaimed) Theme.BONE_DIM else Theme.STEEL_DARK, textSize = 36f) {
            if (!freeClaimed) { game.store.gems += 25; game.store.save(); freeClaimed = true; game.haptics.medium() }
        }

        // IAP note
        Ui.panel(c, 800f, 540f, 720f, 300f, Theme.STEEL_PANEL, 24f)
        Ui.label(c, "PREMIUM", 840f, 600f, 36f, Theme.ORANGE, Paint.Align.LEFT, Theme.display)
        Ui.label(c, "Gem packs · No-Ads · Battle Pass · VIP", 840f, 648f, 26f, Theme.BONE_DIM)
        Ui.label(c, "In-app purchases activate once the app is", 840f, 700f, 24f, Theme.BONE_DIM)
        Ui.label(c, "connected to Google Play Billing.", 840f, 732f, 24f, Theme.BONE_DIM)
        Ui.button(c, buttons, 840f, 758f, 320f, 60f, "RESTORE", Theme.STEEL_LINE, Theme.BONE_DIM, textSize = 26f) {}
    }

    override fun onBack(): Boolean { game.go(TitleScreen(game)); return true }
}
