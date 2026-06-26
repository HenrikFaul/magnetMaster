package com.magnetmaster.game.screens

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Screen
import com.magnetmaster.game.core.Theme
import com.magnetmaster.game.core.Ui
import com.magnetmaster.game.engine.Kind
import com.magnetmaster.game.engine.World
import com.magnetmaster.game.game.LevelConfig
import com.magnetmaster.game.game.Levels
import com.magnetmaster.game.game.Skins
import kotlin.math.hypot
import kotlin.math.max

class GameplayScreen(
    game: Game,
    private val levelIndex: Int,
    private val daily: Boolean,
    private val dailySeed: Long = 0L
) : Screen(game) {

    private enum class Phase { PLAYING, WON, LOST, PAUSED }

    private val cfg: LevelConfig =
        if (daily) Levels.generate(maxOf(8, levelIndex), dailySeed) else Levels.generate(levelIndex)
    private val world = World()
    private val stats = game.store.stats()

    private var phase = Phase.PLAYING
    private var timeLeft = cfg.timeSec.toFloat()
    private var hp = 3
    private var t = 0f
    private var acc = 0f
    private val fixed = 1f / 60f

    private var pulseTimer = stats.pulseInterval
    private var polarityActive = false
    private var polarityTimer = 0f
    private var polarityCd = 0f
    private var lastTapDown = -1f
    private var lastTapX = 0f
    private var lastTapY = 0f

    private var idleTimer = 0f
    private var showHint = false
    private var hintX = 0f
    private var hintY = 0f

    private var captureFlash = 0f
    private var damageFlash = 0f
    private var extraTimeUsed = false
    private var doubledScore = false

    // result payload
    private var earnedStars = 0
    private var earnedGems = 0
    private var earnedCoins = 0
    private var spAwarded = 0

    private val pauseRect = RectF(1470f, 30f, 1560f, 110f)

    companion object { private val failMap = HashMap<Int, Int>() }

    init {
        world.bodies.addAll(Levels.spawnBodies(cfg))
        world.obstacles.addAll(cfg.obstacles)
        // DDA: 3+ fails grants +10% time silently (README)
        if ((failMap[levelIndex] ?: 0) >= 3) timeLeft *= 1.1f
    }

    // ---------------- update ----------------
    override fun update(dt: Float) {
        t += dt
        if (captureFlash > 0) captureFlash -= dt
        if (damageFlash > 0) damageFlash -= dt
        if (phase != Phase.PLAYING) return

        timeLeft -= dt
        idleTimer += dt
        if (!showHint && idleTimer > 5f && world.haulKg == 0f) computeHint()

        // feed skill tunables
        world.magnetRadius = stats.magnetRadius
        world.pullK = stats.pullK
        world.polarity = if (polarityActive) -1f else 1f

        // pulse skill
        if (stats.pulseEnabled && world.magnetActive) {
            pulseTimer -= dt
            if (pulseTimer <= 0f) { world.firePulse(stats.pulseStrength); pulseTimer = stats.pulseInterval; game.haptics.medium() }
        }
        // polarity timers
        if (polarityActive) { polarityTimer -= dt; if (polarityTimer <= 0f) polarityActive = false }
        if (polarityCd > 0f) polarityCd -= dt

        // fixed-step physics for determinism
        acc += dt
        var guard = 0
        while (acc >= fixed && guard < 6) {
            val dmg = world.step(fixed)
            if (dmg > 0f) {
                hp = max(0, hp - Math.round(dmg).coerceAtLeast(1))
                if (hp <= 0) hp = 0
                damageFlash = 0.4f
                game.haptics.heavy()
            }
            acc -= fixed; guard++
        }
        if (world.lastCaptureKg > 0f) { captureFlash = 0.35f; game.haptics.light() }

        if (world.haulKg >= cfg.targetKg) win()
        else if (timeLeft <= 0f || hp <= 0) lose()
    }

    private fun computeHint() {
        var best = -1f
        for (b in world.bodies) {
            if (!b.kind.isMetal) continue
            val score = b.kind.kg / max(50f, hypot(world.chuteX - b.x, world.chuteY - b.y))
            if (score > best) { best = score; hintX = b.x; hintY = b.y }
        }
        if (best > 0f) showHint = true
    }

    private fun win() {
        phase = Phase.WON
        game.haptics.heavy()
        val tFrac = timeLeft / cfg.timeSec
        earnedStars = 1
        if (tFrac > 0.33f || world.haulKg >= cfg.targetKg * 1.3f) earnedStars = 2
        if (tFrac > 0.55f && world.haulKg >= cfg.targetKg * 1.15f) earnedStars = 3
        if (cfg.isBoss) earnedStars = max(earnedStars, 2)
        earnedGems = Math.round(world.haulKg * 4f) + earnedStars * 5
        earnedCoins = Math.round(world.haulKg * 6f)
        if (daily) {
            applyDaily()
        } else {
            spAwarded = game.store.recordResult(levelIndex, earnedStars, earnedGems, earnedCoins)
        }
    }

    private fun applyDaily() {
        val s = game.store
        if (world.haulKg > s.dailyBestKg) s.dailyBestKg = world.haulKg
        s.gems += earnedGems
        s.save()
    }

    private fun lose() {
        phase = Phase.LOST
        failMap[levelIndex] = (failMap[levelIndex] ?: 0) + 1
        game.haptics.heavy()
    }

    // ---------------- render ----------------
    override fun render(c: Canvas) {
        buttons.clear()
        c.drawColor(cfg.environment.bg)
        drawGrid(c)
        drawObstacles(c)
        drawChute(c)
        for (b in world.bodies) Render.drawBody(c, b)
        drawPulses(c)

        if (world.magnetActive) {
            val skin = Skins.byId(game.store.activeSkin)
            Render.drawFieldLines(c, world.magnetX, world.magnetY, stats.magnetRadius,
                (t * 0.8f) % 1f, game.store.reduceMotion, if (polarityActive) Theme.RED else skin.glow)
            Render.drawMagnet(c, world.magnetX, world.magnetY, 54f, skin, polarityActive)
        }
        if (showHint && phase == Phase.PLAYING) drawHint(c)

        // flashes
        if (captureFlash > 0) c.drawColor(Ui.withAlpha(Theme.GREEN, (captureFlash * 120).toInt().coerceIn(0, 110)))
        if (damageFlash > 0) c.drawColor(Ui.withAlpha(Theme.RED, (damageFlash * 200).toInt().coerceIn(0, 150)))

        drawHud(c)
        when (phase) {
            Phase.WON -> drawResult(c, true)
            Phase.LOST -> drawResult(c, false)
            Phase.PAUSED -> drawPaused(c)
            else -> {}
        }
        if (levelIndex == 1 && !game.store.tutorialSeen && phase == Phase.PLAYING) drawTutorial(c)
    }

    private fun drawGrid(c: Canvas) {
        val p = Theme.stroke(Ui.withAlpha(cfg.environment.accent, 22), 2f)
        var x = 0f; while (x < Theme.WORLD_W) { c.drawLine(x, 0f, x, Theme.WORLD_H, p); x += 100f }
        var y = 0f; while (y < Theme.WORLD_H) { c.drawLine(0f, y, Theme.WORLD_W, y, p); y += 100f }
    }

    private fun drawObstacles(c: Canvas) {
        val p = Theme.fill(0xFF3A434F.toInt())
        val s = Theme.stroke(Ui.withAlpha(cfg.environment.accent, 120), 3f)
        for (o in world.obstacles) {
            c.drawRoundRect(o.x, o.y, o.x + o.w, o.y + o.h, 8f, 8f, p)
            c.drawRoundRect(o.x, o.y, o.x + o.w, o.y + o.h, 8f, 8f, s)
        }
    }

    private fun drawChute(c: Canvas) {
        val x = world.chuteX; val y = world.chuteY; val w = world.chuteW; val h = world.chuteH
        c.drawRoundRect(x, y, x + w, y + h, 14f, 14f, Theme.fill(Theme.STEEL_DARK))
        c.drawRoundRect(x, y, x + w, y + h, 14f, 14f, Theme.stroke(Theme.ORANGE, 5f))
        // hazard stripes lip
        val sp = Theme.fill(Theme.ORANGE)
        var sx = x
        while (sx < x + w) { c.drawRect(sx, y - 14f, sx + 18f, y, sp); sx += 36f }
        // down arrow
        Ui.label(c, "▼", x + w / 2f, y + h / 2f, 80f, Ui.withAlpha(Theme.ORANGE, 120), Paint.Align.CENTER)
        // TARGET chip
        Ui.pill(c, x + 6f, y + h - 70f, w - 12f, 56f, Theme.STEEL_PANEL)
        Ui.label(c, "TARGET", x + w / 2f, y + h - 44f, 22f, Theme.BONE_DIM, Paint.Align.CENTER)
        Ui.label(c, "%.2f KG".format(cfg.targetKg), x + w / 2f, y + h - 18f, 34f, Theme.ORANGE, Paint.Align.CENTER)
    }

    private fun drawPulses(c: Canvas) {
        for (p in world.pulses) {
            val pp = Theme.stroke(Ui.withAlpha(Theme.BLUE, (p.life * 160).toInt().coerceIn(0, 160)), 8f)
            c.drawCircle(p.x, p.y, p.radius, pp)
        }
    }

    private fun drawHint(c: Canvas) {
        val pulse = 1f + 0.15f * kotlin.math.sin(t * 6f)
        c.drawCircle(hintX, hintY, 60f * pulse, Theme.stroke(Theme.BONE, 4f))
        Ui.label(c, "☞", hintX + 36f, hintY + 50f, 80f, Theme.BONE, Paint.Align.CENTER)
    }

    private fun drawHud(c: Canvas) {
        // HP
        for (i in 0 until 3) {
            val col = if (i < hp) Theme.RED else Ui.withAlpha(Theme.BONE_DIM, 80)
            Ui.label(c, "♥", 44f + i * 46f, 76f, 48f, col)
        }
        // level
        Ui.label(c, if (cfg.isBoss) "BOSS  ${levelIndex}" else (if (daily) "DAILY" else "LEVEL $levelIndex"),
            44f, 128f, 38f, Theme.BONE, Paint.Align.LEFT, Theme.display)

        // haul big
        Ui.pill(c, 44f, 760f, 280f, 96f, Ui.withAlpha(Theme.STEEL_DARK, 200))
        Ui.label(c, "HAUL", 64f, 792f, 24f, Theme.BONE_DIM)
        Ui.label(c, "%.2f KG".format(world.haulKg), 64f, 838f, 52f, Theme.BLUE, Paint.Align.LEFT, Theme.display)

        // progress bar
        val frac = (world.haulKg / cfg.targetKg).coerceIn(0f, 1f)
        Ui.pill(c, 340f, 800f, 360f, 24f, Theme.STEEL_PANEL)
        Ui.pill(c, 340f, 800f, 360f * frac, 24f, Theme.GREEN)

        // timer
        val mm = (timeLeft.toInt() / 60); val ss = (timeLeft.toInt() % 60)
        val tcol = if (timeLeft < 10f) Theme.RED else Theme.BONE
        Ui.label(c, "%d:%02d".format(mm, ss), 1440f, 130f, 56f, tcol, Paint.Align.RIGHT, Theme.mono)

        // pulse / polarity status
        var sx = 720f
        if (stats.pulseEnabled) {
            val ready = pulseTimer < 0.1f
            Ui.pill(c, sx, 36f, 150f, 50f, if (ready) Theme.BLUE else Theme.STEEL_PANEL)
            Ui.label(c, if (ready) "PULSE!" else "PULSE %.0f".format(pulseTimer), sx + 75f, 70f, 26f,
                if (ready) Theme.STEEL_DARK else Theme.BONE_DIM, Paint.Align.CENTER); sx += 162f
        }
        if (stats.polarityEnabled) {
            val ready = polarityCd <= 0f
            Ui.pill(c, sx, 36f, 180f, 50f, if (polarityActive) Theme.RED else if (ready) Theme.ORANGE else Theme.STEEL_PANEL)
            Ui.label(c, if (polarityActive) "REPEL" else "FLIP (2×tap)", sx + 90f, 70f, 24f,
                Theme.STEEL_DARK, Paint.Align.CENTER)
        }

        // pause
        Ui.button(c, buttons, pauseRect.left, pauseRect.top, pauseRect.width(), pauseRect.height(),
            "❚❚", Theme.STEEL_PANEL, Theme.BONE, textSize = 40f) { phase = Phase.PAUSED }
    }

    private fun drawTutorial(c: Canvas) {
        c.drawColor(Ui.withAlpha(Theme.STEEL_DARK, 150))
        Ui.label(c, "Drag anywhere to move the magnet.", 800f, 420f, 44f, Theme.BONE, Paint.Align.CENTER)
        Ui.label(c, "Pull metal into the orange chute before time runs out.", 800f, 480f, 34f, Theme.BLUE, Paint.Align.CENTER)
        Ui.label(c, "Double-tap to repel. Avoid bombs (✕).", 800f, 532f, 30f, Theme.BONE_DIM, Paint.Align.CENTER)
        Ui.label(c, "— tap to start —", 800f, 600f, 30f, Theme.ORANGE, Paint.Align.CENTER)
    }

    private fun drawResult(c: Canvas, won: Boolean) {
        c.drawColor(Ui.withAlpha(Theme.STEEL_DARK, 210))
        val pw = 760f; val ph = 520f; val px = (Theme.WORLD_W - pw) / 2f; val py = 190f
        Ui.panel(c, px, py, pw, ph, Theme.STEEL_PANEL, 28f)
        Ui.label(c, if (won) (if (cfg.isBoss) "BOSS DOWN!" else "LEVEL CLEAR") else "OUT OF " + (if (hp <= 0) "HP" else "TIME"),
            Theme.WORLD_W / 2f, py + 90f, 64f, if (won) Theme.GREEN else Theme.RED, Paint.Align.CENTER, Theme.display)

        if (won) {
            for (s in 0 until 3) {
                val col = if (s < earnedStars) Theme.GOLD else Ui.withAlpha(Theme.BONE_DIM, 80)
                Ui.label(c, "★", Theme.WORLD_W / 2f - 90f + s * 90f, py + 190f, 90f, col, Paint.Align.CENTER)
            }
            Ui.label(c, "Hauled %.2f KG / %.2f KG".format(world.haulKg, cfg.targetKg),
                Theme.WORLD_W / 2f, py + 250f, 34f, Theme.BONE, Paint.Align.CENTER)
            Ui.label(c, "◆ +$earnedGems   ◉ +$earnedCoins" + if (spAwarded > 0) "   SP +$spAwarded" else "",
                Theme.WORLD_W / 2f, py + 300f, 32f, Theme.BLUE, Paint.Align.CENTER)

            if (!doubledScore && !daily) {
                Ui.button(c, buttons, px + 60f, py + 340f, pw - 120f, 64f, "▶ WATCH AD: 2× GEMS", Theme.STEEL_LINE, Theme.BONE, textSize = 30f) {
                    doubledScore = true
                    game.store.gems += earnedGems; game.store.save()
                }
            }
            val nextLevel = (levelIndex + 1).coerceAtMost(Levels.MAX_LEVEL)
            Ui.button(c, buttons, px + 60f, py + 420f, (pw - 140f) / 2f, 72f, "MENU", Theme.STEEL_LINE, Theme.BONE, textSize = 32f) {
                game.go(LevelSelectScreen(game))
            }
            Ui.button(c, buttons, px + 80f + (pw - 140f) / 2f, py + 420f, (pw - 140f) / 2f, 72f,
                if (daily) "DONE" else "NEXT ▶", Theme.ORANGE, Theme.STEEL_DARK, textSize = 34f) {
                if (daily) game.go(DailyScreen(game)) else game.go(GameplayScreen(game, nextLevel, false))
            }
        } else {
            Ui.label(c, "Hauled %.2f KG of %.2f KG".format(world.haulKg, cfg.targetKg),
                Theme.WORLD_W / 2f, py + 180f, 34f, Theme.BONE, Paint.Align.CENTER)
            if (!extraTimeUsed && hp > 0 && !daily) {
                Ui.button(c, buttons, px + 60f, py + 230f, pw - 120f, 70f, "▶ WATCH AD: +15s", Theme.BLUE, Theme.STEEL_DARK, textSize = 32f) {
                    extraTimeUsed = true; timeLeft += 15f; phase = Phase.PLAYING
                }
            }
            Ui.button(c, buttons, px + 60f, py + 420f, (pw - 140f) / 2f, 72f, "MENU", Theme.STEEL_LINE, Theme.BONE, textSize = 32f) {
                game.go(if (daily) DailyScreen(game) else LevelSelectScreen(game))
            }
            Ui.button(c, buttons, px + 80f + (pw - 140f) / 2f, py + 420f, (pw - 140f) / 2f, 72f, "RETRY ↻", Theme.ORANGE, Theme.STEEL_DARK, textSize = 34f) {
                game.go(GameplayScreen(game, levelIndex, daily, dailySeed))
            }
        }
    }

    private fun drawPaused(c: Canvas) {
        c.drawColor(Ui.withAlpha(Theme.STEEL_DARK, 210))
        Ui.label(c, "PAUSED", Theme.WORLD_W / 2f, 320f, 80f, Theme.BONE, Paint.Align.CENTER, Theme.display)
        Ui.button(c, buttons, 600f, 400f, 400f, 76f, "RESUME", Theme.ORANGE, Theme.STEEL_DARK, textSize = 36f) { phase = Phase.PLAYING }
        Ui.button(c, buttons, 600f, 496f, 400f, 70f, "RETRY", Theme.STEEL_LINE, Theme.BONE, textSize = 32f) {
            game.go(GameplayScreen(game, levelIndex, daily, dailySeed))
        }
        Ui.button(c, buttons, 600f, 580f, 400f, 70f, "QUIT TO MENU", Theme.STEEL_LINE, Theme.BONE, textSize = 32f) {
            game.go(if (daily) DailyScreen(game) else LevelSelectScreen(game))
        }
    }

    // ---------------- input ----------------
    override fun onDown(x: Float, y: Float) {
        pressX = x; pressY = y
        if (phase != Phase.PLAYING) return
        if (levelIndex == 1 && !game.store.tutorialSeen) { game.store.tutorialSeen = true; game.store.save() }
        if (pauseRect.contains(x, y)) { world.magnetActive = false; return }
        idleTimer = 0f; showHint = false
        // double-tap → polarity flip
        if (stats.polarityEnabled && lastTapDown > 0f && (t - lastTapDown) < 0.32f &&
            hypot(x - lastTapX, y - lastTapY) < 120f && polarityCd <= 0f) {
            polarityActive = true; polarityTimer = stats.polarityDuration; polarityCd = stats.polarityCooldown
            game.haptics.medium()
        }
        lastTapDown = t; lastTapX = x; lastTapY = y
        world.magnetActive = true; world.magnetX = x; world.magnetY = y
    }

    override fun onMove(x: Float, y: Float) {
        if (phase == Phase.PLAYING && world.magnetActive) { world.magnetX = x; world.magnetY = y; idleTimer = 0f }
    }

    override fun onUp(x: Float, y: Float) {
        if (phase == Phase.PLAYING) {
            world.magnetActive = false
            if (pauseRect.contains(x, y) && pauseRect.contains(pressX, pressY)) phase = Phase.PAUSED
            return
        }
        // modal buttons
        for (b in buttons) if (b.r.contains(x, y) && b.r.contains(pressX, pressY)) { b.onClick(); break }
        pressX = -1f; pressY = -1f
    }

    override fun onBack(): Boolean {
        when (phase) {
            Phase.PLAYING -> phase = Phase.PAUSED
            Phase.PAUSED -> phase = Phase.PLAYING
            else -> game.go(if (daily) DailyScreen(game) else LevelSelectScreen(game))
        }
        return true
    }
}
