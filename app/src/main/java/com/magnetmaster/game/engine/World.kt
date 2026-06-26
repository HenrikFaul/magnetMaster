package com.magnetmaster.game.engine

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/** Static obstacle (ramp/pipe/wall) as an axis-aligned rectangle. */
class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float)

/** Expanding shockwave ring spawned by the Pulse skill. */
class Pulse(val x: Float, val y: Float) {
    var radius = 0f
    var life = 1f
}

/**
 * Deterministic 2D physics for Magnet Master. Fixed-dt integration so replays
 * stay reproducible (AI_PROMPT.md §5.5 / §7.10). The magnet applies an inverse
 * square force F = k·q / r² with the lower r clamped to 50px so it never blows
 * up to NaN; acceleration is divided by body mass, so heavier metals are harder
 * to drag.
 */
class World(
    val width: Float = 1600f,
    val height: Float = 900f
) {
    val bodies = ArrayList<Body>()
    val obstacles = ArrayList<Obstacle>()
    val pulses = ArrayList<Pulse>()

    // Goal chute (deposit zone)
    var chuteX = 1380f
    var chuteY = 640f
    var chuteW = 200f
    var chuteH = 240f

    // Magnet (finger) state
    var magnetActive = false
    var magnetX = 0f
    var magnetY = 0f

    // Tunables fed from the skill tree each frame
    var magnetRadius = 430f      // range
    var pullK = 1f               // pull-strength multiplier
    var polarity = 1f            // +1 attract, -1 repel

    var haulKg = 0f
        private set
    var lastCaptureKg = 0f       // signals the view to buzz/flash; consumed by caller

    private val gravity = 280f
    private val charge = 2_600_000f   // base magnet charge constant
    private val rClampSq = 50f * 50f

    fun reset() {
        bodies.clear(); obstacles.clear(); pulses.clear()
        haulKg = 0f; lastCaptureKg = 0f; magnetActive = false
    }

    fun firePulse(strength: Float) {
        pulses.add(Pulse(magnetX, magnetY))
        // mega-pull impulse toward the magnet
        for (b in bodies) {
            if (b.dead || !b.kind.magnetic) continue
            val dx = magnetX - b.x; val dy = magnetY - b.y
            val r = max(hypot(dx, dy), 60f)
            if (r > magnetRadius * 1.4f) continue
            val imp = strength * 9000f / r
            b.vx += imp * dx / r
            b.vy += imp * dy / r
        }
    }

    /** Advance the simulation by one fixed step. Returns hazard damage dealt this step. */
    fun step(dt: Float): Float {
        var damage = 0f
        lastCaptureKg = 0f

        // advance pulses
        val itP = pulses.iterator()
        while (itP.hasNext()) {
            val p = itP.next()
            p.radius += 1700f * dt
            p.life -= dt * 1.4f
            if (p.life <= 0f) itP.remove()
        }

        for (b in bodies) {
            if (b.dead) continue

            // gravity
            b.vy += gravity * dt

            // magnet force
            if (magnetActive && b.kind.magnetic) {
                val dx = magnetX - b.x
                val dy = magnetY - b.y
                val distSq = dx * dx + dy * dy
                val dist = sqrt(distSq)
                if (dist < magnetRadius) {
                    val rSq = max(distSq, rClampSq)
                    val forceMag = pullK * charge / rSq          // F = k·q / r²
                    val accel = polarity * forceMag / b.mass     // a = F / m
                    val r = max(dist, 1f)
                    b.vx += accel * (dx / r) * dt
                    b.vy += accel * (dy / r) * dt
                }
            }

            // integrate
            b.vx *= 0.992f
            b.vy *= 0.992f
            // clamp insane speeds for stability
            val sp = hypot(b.vx, b.vy)
            if (sp > 6000f) { b.vx *= 6000f / sp; b.vy *= 6000f / sp }
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.spin += b.spinV * dt + (b.vx * dt) / max(b.radius, 1f)

            collideBounds(b)
            for (o in obstacles) collideRect(b, o)

            // hazard contact with the magnet (finger)
            if (magnetActive && b.kind.hazard > 0f) {
                if (hypot(magnetX - b.x, magnetY - b.y) < b.radius + 40f) {
                    damage += b.kind.hazard
                    b.dead = true
                    continue
                }
            }

            // chute capture
            if (b.kind.kg > 0f && inChute(b.x, b.y)) {
                b.captured = true
                b.dead = true
                haulKg += b.kind.kg
                lastCaptureKg += b.kind.kg
            }
        }

        // purge dead
        var i = bodies.size - 1
        while (i >= 0) {
            if (bodies[i].dead) bodies.removeAt(i)
            i--
        }
        return damage
    }

    fun inChute(x: Float, y: Float): Boolean =
        x in chuteX..(chuteX + chuteW) && y in chuteY..(chuteY + chuteH)

    private fun collideBounds(b: Body) {
        val r = b.radius
        if (b.x < r) { b.x = r; b.vx = -b.vx * 0.4f }
        if (b.x > width - r) { b.x = width - r; b.vx = -b.vx * 0.4f }
        if (b.y < r) { b.y = r; b.vy = -b.vy * 0.4f }
        if (b.y > height - r) { b.y = height - r; b.vy = -b.vy * 0.4f; b.vx *= 0.9f }
    }

    private fun collideRect(b: Body, o: Obstacle) {
        val nx = b.x.coerceIn(o.x, o.x + o.w)
        val ny = b.y.coerceIn(o.y, o.y + o.h)
        val dx = b.x - nx
        val dy = b.y - ny
        val d2 = dx * dx + dy * dy
        val r = b.radius
        if (d2 < r * r) {
            val d = sqrt(d2)
            if (d > 0.001f) {
                val push = (r - d)
                b.x += (dx / d) * push
                b.y += (dy / d) * push
                // reflect velocity along normal
                val nxn = dx / d; val nyn = dy / d
                val vn = b.vx * nxn + b.vy * nyn
                if (vn < 0) {
                    b.vx -= (1.3f) * vn * nxn
                    b.vy -= (1.3f) * vn * nyn
                    b.vx *= 0.7f; b.vy *= 0.7f
                }
            } else {
                // center inside: push up
                b.y = o.y - r
                b.vy = -abs(b.vy) * 0.3f
            }
        }
    }
}
