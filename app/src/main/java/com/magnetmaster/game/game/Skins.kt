package com.magnetmaster.game.game

import com.magnetmaster.game.core.Theme

/** Cosmetic magnet skins (README "neon, gold, plazma"). */
class Skin(
    val id: String,
    val name: String,
    val costGems: Int,
    val poleA: Int,   // north pole colour
    val poleB: Int,   // south pole colour
    val glow: Int
)

object Skins {
    val ALL = listOf(
        Skin("default", "Steel", 0, Theme.BLUE, Theme.ORANGE, Theme.BLUE),
        Skin("neon", "Neon", 120, 0xFF20E0C0.toInt(), 0xFFFF3FA0.toInt(), 0xFF20E0C0.toInt()),
        Skin("gold", "Gold Magnet", 200, 0xFFFFD24D.toInt(), 0xFFB8860B.toInt(), 0xFFFFD24D.toInt()),
        Skin("plasma", "Plasma", 160, 0xFF9B6CFF.toInt(), 0xFF2FB3C9.toInt(), 0xFF9B6CFF.toInt())
    )
    fun byId(id: String): Skin = ALL.firstOrNull { it.id == id } ?: ALL[0]
}
