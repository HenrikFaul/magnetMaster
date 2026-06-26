package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Screen
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui
import com.magnetmaster.game.game.Skins
import kotlin.math.sin

class TitleScreen(game: Game) : Screen(game) {
    private var t = 0f
    private val bg = Paint()

    init {
        bg.shader = LinearGradient(0f, 0f, 0f, Theme.WORLD_H,
            Theme.STEEL, Theme.STEEL_DARK, Shader.TileMode.CLAMP)
    }

    override fun update(dt: Float) { t += dt }

    override fun render(c: Canvas) {
        buttons.clear()
        c.drawRect(0f, 0f, Theme.WORLD_W, Theme.WORLD_H, bg)

        val skin = Skins.byId(game.store.activeSkin)
        // behind-logo field lines
        val cx = 800f; val cy = 320f + sin(t * 1.4f) * 14f
        Render.drawFieldLines(c, cx, cy, 360f, (t * 0.4f) % 1f, game.store.reduceMotion, skin.glow)
        Render.drawMagnet(c, cx, cy, 120f, skin, false)

        Ui.label(c, "MAGNET MASTER", 800f, 560f, 110f, Theme.BONE, Paint.Align.CENTER, Theme.display)
        Ui.label(c, "Pull it in. Slam it home. Feel the force.", 800f, 612f, 32f, Theme.BLUE,
            Paint.Align.CENTER, Theme.sansBold)

        // currency chips
        Ui.pill(c, 1360f, 30f, 210f, 54f, Theme.STEEL_PANEL)
        Ui.label(c, "◆ ${game.store.gems}", 1380f, 65f, 32f, Theme.BLUE)
        Ui.label(c, "★ ${game.store.totalStars()}", 1490f, 65f, 32f, Theme.GOLD)

        // primary actions
        Ui.button(c, buttons, 600f, 650f, 400f, 86f, "PLAY", Theme.ORANGE, Theme.STEEL_DARK, textSize = 50f) {
            game.go(LevelSelectScreen(game))
        }
        Ui.button(c, buttons, 600f, 748f, 192f, 64f, "DAILY", Theme.STEEL_PANEL, Theme.BONE, textSize = 32f) {
            game.go(DailyScreen(game))
        }
        Ui.button(c, buttons, 808f, 748f, 192f, 64f, "UPGRADES", Theme.STEEL_PANEL, Theme.BONE, textSize = 30f) {
            game.go(SkillTreeScreen(game))
        }

        // bottom utility icons
        Ui.iconButton(c, buttons, 600f, 826f, 60f, "⚙", Theme.STEEL_PANEL) { game.go(SettingsScreen(game)) }
        Ui.iconButton(c, buttons, 676f, 826f, 60f, "▤", Theme.STEEL_PANEL) { game.go(DailyScreen(game)) }
        Ui.iconButton(c, buttons, 880f, 826f, 60f, "✦", Theme.STEEL_PANEL) { game.go(ShopScreen(game)) }
        Ui.iconButton(c, buttons, 940f, 826f, 60f, "⌂", Theme.STEEL_PANEL) { game.go(LevelSelectScreen(game)) }
    }

    override fun onBack(): Boolean = false   // allow app exit from title
}
