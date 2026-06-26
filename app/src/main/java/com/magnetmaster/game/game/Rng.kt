package com.magnetmaster.game.game

/** Tiny deterministic LCG so levels/daily seeds reproduce exactly. */
class Rng(seed: Long) {
    private var s: Long = if (seed == 0L) 0x9E3779B9L else seed

    fun nextInt(): Int {
        s = (s * 6364136223846793005L + 1442695040888963407L) and Long.MAX_VALUE
        return (s ushr 17).toInt()
    }

    fun nextFloat(): Float = (nextInt() and 0xFFFFFF) / 16_777_216f
    fun range(min: Float, max: Float): Float = min + nextFloat() * (max - min)
    fun rangeInt(minInclusive: Int, maxExclusive: Int): Int {
        if (maxExclusive <= minInclusive) return minInclusive
        return minInclusive + (nextInt() % (maxExclusive - minInclusive))
    }
    fun <T> pick(list: List<T>): T = list[rangeInt(0, list.size)]
}
