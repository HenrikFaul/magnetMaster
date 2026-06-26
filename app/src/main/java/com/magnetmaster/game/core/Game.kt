package com.magnetmaster.game.core

import android.content.Context
import com.magnetmaster.game.game.Store
import com.magnetmaster.game.screens.TitleScreen

/**
 * Top-level controller. Owns the Store, haptics and the active Screen, and
 * routes transitions. Created once by the GameView.
 */
class Game(val context: Context) {
    val store = Store(context)
    val haptics = Haptics(context).also { it.enabled = store.hapticsOn }

    @Volatile var screen: Screen = TitleScreen(this)
        private set

    private var pending: Screen? = null

    fun go(s: Screen) { pending = s }

    /** Called on the render thread between frames to apply a queued transition. */
    fun applyTransition() {
        pending?.let { screen = it; pending = null }
    }

    fun refreshHaptics() { haptics.enabled = store.hapticsOn }
}
