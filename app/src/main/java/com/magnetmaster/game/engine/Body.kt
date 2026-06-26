package com.magnetmaster.game.engine

/**
 * Metal types and distractors (AI_PROMPT.md §2.3 / §2.4).
 *
 * mass    – inertia used by the magnet force (heavier = harder to pull).
 * kg      – salvage value added to the haul when chuted.
 * radius  – collision/draw radius in world units.
 * magnetic– whether the magnet force affects it.
 * hazard  – HP damage dealt when it touches the magnet (0 = harmless).
 */
enum class Kind(
    val mass: Float,
    val kg: Float,
    val radius: Float,
    val magnetic: Boolean,
    val hazard: Float
) {
    BOLT(1f, 0.05f, 15f, true, 0f),
    COIN(0.5f, 0.10f, 17f, true, 0f),
    GEAR(4f, 0.40f, 27f, true, 0f),
    PLATE(8f, 1.00f, 34f, true, 0f),
    WRECKAGE(20f, 2.50f, 46f, true, 0f),

    // Distractors
    BOMB(2.5f, 0f, 26f, true, 1f),
    SHARP(1.5f, 0f, 20f, true, 0.5f),
    RUBBER(7f, 0f, 30f, false, 0f),

    // Boss debris (rip-off panels / reactor core)
    PANEL(10f, 0.8f, 40f, true, 0f),
    CORE(30f, 4.0f, 54f, true, 0f);

    val isMetal: Boolean get() = kg > 0f && hazard == 0f
}

class Body(
    var x: Float,
    var y: Float,
    val kind: Kind
) {
    var vx = 0f
    var vy = 0f
    var captured = false      // entered the chute
    var dead = false          // removed (hazard consumed / chuted)
    var spin = 0f             // visual rotation
    var spinV = 0f
    val mass get() = kind.mass
    val radius get() = kind.radius
}
