package com.magnetmaster.game.game

import com.magnetmaster.game.engine.Body
import com.magnetmaster.game.engine.Kind
import com.magnetmaster.game.engine.Obstacle

enum class Environment(val title: String, val bg: Int, val accent: Int) {
    INDUSTRIAL("Industrial", 0xFF222A33.toInt(), 0xFF2F8DFF.toInt()),
    JUNKYARD("Junkyard", 0xFF2A2620.toInt(), 0xFFFF7A2A.toInt()),
    WRECK("Ocean Wreck", 0xFF18313A.toInt(), 0xFF2FB3C9.toInt()),
    SPACE("Space Scrap", 0xFF1A1730.toInt(), 0xFF9B6CFF.toInt())
}

class Spawn(val kind: Kind, val x: Float, val y: Float)

class LevelConfig(
    val index: Int,
    val environment: Environment,
    val targetKg: Float,
    val timeSec: Int,
    val isBoss: Boolean,
    val spawns: List<Spawn>,
    val obstacles: List<Obstacle>,
    val bossParts: Int,
    val availableKg: Float
)

object Levels {
    const val MAX_LEVEL = 120

    /** Target KG progression: T(n) = 1.5 + 0.15·n (README). */
    fun targetKg(n: Int): Float = 1.5f + 0.15f * n
    /** Timer: 90 − n/2 seconds, floored. */
    fun timeSec(n: Int): Int = (90 - n / 2).coerceAtLeast(30)
    /** Distractor (bomb) count: floor(n/10). */
    fun bombs(n: Int): Int = n / 10
    fun isBoss(n: Int): Boolean = n % 10 == 0
    fun envFor(n: Int): Environment = Environment.values()[((n - 1) / 10) % 4]

    fun generate(index: Int, seedOverride: Long? = null): LevelConfig {
        val seed = seedOverride ?: (index.toLong() * 0x100000001L + 7919L)
        val rng = Rng(seed)
        val env = envFor(index)
        return if (isBoss(index)) buildBoss(index, env, rng)
        else buildStandard(index, env, rng)
    }

    private fun buildStandard(n: Int, env: Environment, rng: Rng): LevelConfig {
        val target = targetKg(n)
        val spawns = ArrayList<Spawn>()

        // metal pool grows heavier with level
        val pool = when {
            n < 8 -> listOf(Kind.BOLT, Kind.COIN, Kind.BOLT, Kind.GEAR)
            n < 20 -> listOf(Kind.BOLT, Kind.COIN, Kind.GEAR, Kind.GEAR, Kind.PLATE)
            n < 45 -> listOf(Kind.COIN, Kind.GEAR, Kind.PLATE, Kind.PLATE, Kind.WRECKAGE)
            else -> listOf(Kind.GEAR, Kind.PLATE, Kind.WRECKAGE, Kind.WRECKAGE)
        }

        var available = 0f
        var guard = 0
        while (available < target * 1.8f && guard < 80) {
            val k = rng.pick(pool)
            val x = rng.range(120f, 1180f)
            val y = rng.range(120f, 760f)
            spawns.add(Spawn(k, x, y))
            available += k.kg
            guard++
        }

        // distractors
        repeat(bombs(n)) {
            spawns.add(Spawn(Kind.BOMB, rng.range(200f, 1180f), rng.range(160f, 720f)))
        }
        repeat((n / 14)) {
            spawns.add(Spawn(Kind.SHARP, rng.range(200f, 1180f), rng.range(160f, 720f)))
        }
        repeat((n / 9)) {
            spawns.add(Spawn(Kind.RUBBER, rng.range(220f, 1140f), rng.range(200f, 700f)))
        }

        val obstacles = buildObstacles(rng, n)
        return LevelConfig(n, env, target, timeSec(n), false, spawns, obstacles, 0, available)
    }

    private fun buildBoss(n: Int, env: Environment, rng: Rng): LevelConfig {
        val spawns = ArrayList<Spawn>()
        val cx = 620f; val cy = 440f
        val tier = n / 10
        val panels = (6 + tier).coerceAtMost(14)
        // Outer panels arranged around the hulk
        for (i in 0 until panels) {
            val a = (i.toFloat() / panels) * 6.2832f
            val rad = 200f + rng.range(-30f, 30f)
            spawns.add(Spawn(Kind.PANEL, cx + Math.cos(a.toDouble()).toFloat() * rad,
                cy + Math.sin(a.toDouble()).toFloat() * rad * 0.8f))
        }
        // Inner reactor cores
        val cores = 1 + tier / 3
        for (i in 0 until cores) {
            spawns.add(Spawn(Kind.CORE, cx + rng.range(-60f, 60f), cy + rng.range(-40f, 40f)))
        }
        // a few free bolts for warm-up
        repeat(6) { spawns.add(Spawn(Kind.BOLT, rng.range(120f, 360f), rng.range(120f, 760f))) }
        repeat(bombs(n)) { spawns.add(Spawn(Kind.BOMB, rng.range(200f, 900f), rng.range(160f, 720f))) }

        var available = 0f
        for (s in spawns) available += s.kind.kg
        val target = available * 0.82f
        val obstacles = listOf(
            Obstacle(0f, 860f, 1600f, 40f) // floor lip
        )
        return LevelConfig(n, env, target, 80, true, spawns, obstacles, panels + cores, available)
    }

    private fun buildObstacles(rng: Rng, n: Int): List<Obstacle> {
        val list = ArrayList<Obstacle>()
        // floor lip near the chute so metals funnel
        list.add(Obstacle(1340f, 600f, 24f, 300f))      // chute left wall
        // a ramp
        list.add(Obstacle(rng.range(400f, 700f), rng.range(500f, 680f), rng.range(180f, 300f), 28f))
        if (n > 5) list.add(Obstacle(rng.range(700f, 1000f), rng.range(260f, 420f), 28f, rng.range(140f, 240f)))
        if (n > 15) list.add(Obstacle(rng.range(250f, 500f), rng.range(300f, 460f), rng.range(120f, 220f), 26f))
        return list
    }

    /** Materialize a config into live physics bodies. */
    fun spawnBodies(cfg: LevelConfig): List<Body> = cfg.spawns.map { s ->
        Body(s.x, s.y, s.kind).also { it.spinV = (s.x % 7f - 3.5f) * 0.2f }
    }
}
