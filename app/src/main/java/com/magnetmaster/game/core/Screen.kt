package com.magnetmaster.game.core

import android.graphics.Canvas

/** Base class for every full-screen state. Draws in virtual 1600x900 space. */
abstract class Screen(val game: Game) {
    /** Buttons registered during the last render(); used for tap hit-testing. */
    val buttons = ArrayList<Btn>()
    protected var pressX = -1f
    protected var pressY = -1f

    abstract fun update(dt: Float)
    abstract fun render(c: Canvas)

    open fun onDown(x: Float, y: Float) { pressX = x; pressY = y }
    open fun onMove(x: Float, y: Float) {}
    open fun onUp(x: Float, y: Float) {
        // tap = down & up close together inside a button
        for (b in buttons) {
            if (b.r.contains(x, y) && b.r.contains(pressX, pressY)) { b.onClick(); break }
        }
        pressX = -1f; pressY = -1f
    }
    /** Hardware back. Return true if consumed. */
    open fun onBack(): Boolean = false
}
