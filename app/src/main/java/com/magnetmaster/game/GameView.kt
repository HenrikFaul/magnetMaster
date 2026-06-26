package com.magnetmaster.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.magnetmaster.game.core.Game
import com.magnetmaster.game.core.Theme

/**
 * SurfaceView host: owns the render/update thread and maps the device surface
 * onto a fixed 1600x900 (16:9) virtual canvas with letterboxing. All input is
 * converted into virtual coordinates before reaching a Screen.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    val game = Game(context)
    private var thread: Thread? = null
    @Volatile private var running = false

    private var scale = 1f
    private var offX = 0f
    private var offY = 0f
    private val bgPaint = Paint().apply { color = Color.BLACK }

    init { holder.addCallback(this); isFocusable = true }

    override fun surfaceCreated(h: SurfaceHolder) {
        running = true
        thread = Thread(this, "magnet-loop").also { it.start() }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        scale = minOf(width / Theme.WORLD_W, height / Theme.WORLD_H)
        offX = (width - Theme.WORLD_W * scale) / 2f
        offY = (height - Theme.WORLD_H * scale) / 2f
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        try { thread?.join(800) } catch (_: InterruptedException) {}
    }

    fun pause() { game.store.save() }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.05f) dt = 0.05f      // clamp after stalls

            game.applyTransition()
            val screen = game.screen
            try { screen.update(dt) } catch (_: Throwable) {}

            val canvas: Canvas = (if (android.os.Build.VERSION.SDK_INT >= 26)
                holder.lockHardwareCanvas() else holder.lockCanvas()) ?: continue
            try {
                canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), bgPaint)
                canvas.save()
                canvas.translate(offX, offY)
                canvas.scale(scale, scale)
                canvas.clipRect(0f, 0f, Theme.WORLD_W, Theme.WORLD_H)
                try { game.screen.render(canvas) } catch (_: Throwable) {}
                canvas.restore()
            } finally {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Throwable) {}
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vx = (event.x - offX) / scale
        val vy = (event.y - offY) / scale
        val s = game.screen
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> s.onDown(vx, vy)
            MotionEvent.ACTION_MOVE -> s.onMove(vx, vy)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> s.onUp(vx, vy)
        }
        return true
    }

    fun handleBack(): Boolean = game.screen.onBack()
}
