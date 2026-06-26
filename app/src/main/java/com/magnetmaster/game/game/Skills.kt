package com.magnetmaster.game.game

/** The four skill-tree branches (AI_PROMPT.md §2.5). Each has 4 nodes. */
enum class Branch(val title: String, val blurb: String) {
    RANGE("RANGE", "Magnet radius +25% / node"),
    PULL("PULL STRENGTH", "Pull force +30% / node"),
    PULSE("PULSE", "Every 3s mega-pulse shockwave"),
    POLARITY("POLARITY FLIP", "Double-tap to repel; faster cooldown")
}

/** Derived gameplay tunables from the player's skill levels (0..4 per branch). */
class SkillStats(levels: IntArray) {
    val range = levels.getOrElse(Branch.RANGE.ordinal) { 0 }
    val pull = levels.getOrElse(Branch.PULL.ordinal) { 0 }
    val pulse = levels.getOrElse(Branch.PULSE.ordinal) { 0 }
    val polarity = levels.getOrElse(Branch.POLARITY.ordinal) { 0 }

    val magnetRadius = 430f * (1f + 0.25f * range)
    val pullK = 1f * (1f + 0.30f * pull)
    val pulseEnabled = pulse > 0
    val pulseStrength = pulse.toFloat()
    val pulseInterval = 3f                       // seconds (AI_PROMPT.md §2.5)
    val polarityEnabled = polarity > 0
    val polarityDuration = 1.2f + 0.2f * polarity
    val polarityCooldown = (4.0f - 0.6f * polarity).coerceAtLeast(1.2f)
}

object Skills {
    const val MAX_NODE = 4
    /** A node always costs 1 skill point to advance one level. */
    fun costFor(@Suppress("UNUSED_PARAMETER") branch: Branch, currentLevel: Int): Int = if (currentLevel >= MAX_NODE) 0 else 1
}
