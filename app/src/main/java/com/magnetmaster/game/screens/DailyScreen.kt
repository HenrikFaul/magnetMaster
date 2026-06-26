package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.Paint
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Screen
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui
import com.magnetmaster.game.game.Levels
import com.magnetmaster.game.game.Rng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily challenge + salvage-yard leaderboard (README, mockup screen 4).
 * The seed is derived from the calendar day so every device sees the same
 * board; rival rows are generated deterministically and the player's best is
 * spliced in. A real build would POST to /functions/v1/submit-daily.
 */
class DailyScreen(game: Game) : Screen(game) {
    private val dayMillis = 86_400_000L
    private val dayIndex = System.currentTimeMillis() / dayMillis
    private val seed = dayIndex * 2654435761L
    private val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private val board: List<Pair<String, Float>>

    private val rivalNames = listOf(
        "IronAddict", "ScrapKing", "Metal_Marauder", "BoltCollector", "Magnetron",
        "ShrapnelCEO", "FluxOverlord", "SteelWhisperer", "HeavyHands", "GaussGoblin",
        "RustBaron", "AlloyAce", "FerroFiend", "PullLord", "ChuteChamp"
    )

    init {
        // reset daily best if the day rolled over
        if (game.store.dailyDate != dateStr) {
            game.store.dailyDate = dateStr; game.store.dailyBestKg = 0f; game.store.save()
        }
        val rng = Rng(seed)
        val rivals = ArrayList<Pair<String, Float>>()
        for (name in rivalNames.shuffledBy(rng).take(11)) {
            rivals.add(name to (4f + rng.nextFloat() * 9f))
        }
        rivals.add("You" to game.store.dailyBestKg)
        board = rivals.sortedByDescending { it.second }
    }

    private fun <T> List<T>.shuffledBy(rng: Rng): List<T> {
        val a = toMutableList()
        for (i in a.indices.reversed()) {
            val j = rng.rangeInt(0, i + 1)
            val tmp = a[i]; a[i] = a[j]; a[j] = tmp
        }
        return a
    }

    override fun update(dt: Float) {}

    override fun render(c: Canvas) {
        buttons.clear()
        c.drawColor(Theme.STEEL_DARK)
        Ui.label(c, "DAILY CHALLENGE", 80f, 92f, 56f, Theme.BONE, Paint.Align.LEFT, Theme.display)
        Ui.label(c, "Industrial Salvage Yard   ·   $dateStr", 80f, 132f, 28f, Theme.BONE_DIM)
        Ui.button(c, buttons, 1400f, 50f, 160f, 60f, "◄ MENU", Theme.STEEL_PANEL, Theme.BONE, textSize = 28f) {
            game.go(TitleScreen(game))
        }

        // today's haul card
        Ui.panel(c, 80f, 170f, 520f, 240f, Theme.STEEL_PANEL, 24f)
        Ui.label(c, "TODAY'S HAUL", 110f, 222f, 26f, Theme.BONE_DIM)
        Ui.label(c, "%.1f".format(game.store.dailyBestKg), 110f, 330f, 130f, Theme.BLUE, Paint.Align.LEFT, Theme.display)
        Ui.label(c, "KG", 360f, 330f, 60f, Theme.BONE_DIM)
        Ui.button(c, buttons, 110f, 348f, 460f, 50f, "▶ PLAY TODAY'S RUN", Theme.ORANGE, Theme.STEEL_DARK, textSize = 28f) {
            game.go(GameplayScreen(game, 12, true, seed))
        }

        // leaderboard
        Ui.panel(c, 640f, 170f, 880f, 660f, Theme.STEEL_PANEL, 24f)
        Ui.label(c, "LEADERBOARD  ·  TOP 12", 672f, 218f, 28f, Theme.BONE_DIM)
        var ry = 256f
        var rank = 1
        for ((name, kg) in board.take(12)) {
            val me = name == "You"
            if (me) Ui.pill(c, 660f, ry - 30f, 840f, 44f, Ui.withAlpha(Theme.BLUE, 60))
            val col = when (rank) { 1 -> Theme.GOLD; 2 -> Theme.BONE; 3 -> Theme.ORANGE; else -> Theme.BONE_DIM }
            Ui.label(c, "$rank", 690f, ry, 30f, col, Paint.Align.CENTER, Theme.mono)
            Ui.label(c, name, 740f, ry, 30f, if (me) Theme.BLUE else Theme.BONE)
            Ui.label(c, "%.1f kg".format(kg), 1480f, ry, 30f, if (me) Theme.BLUE else Theme.BONE, Paint.Align.RIGHT, Theme.mono)
            ry += 46f; rank++
        }
    }

    override fun onBack(): Boolean { game.go(TitleScreen(game)); return true }
}
