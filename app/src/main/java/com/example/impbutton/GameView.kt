package com.example.impbutton

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private data class Obstacle(
        var x: Float,
        var y: Float,
        val r: Float,
        val speed: Float,
        var phase: Float,
        val drift: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val obstacles = mutableListOf<Obstacle>()
    private val stars = MutableList(90) { Pair(Random.nextFloat(), Random.nextFloat()) }
    private val prefs = context.getSharedPreferences("neon_dodge", Context.MODE_PRIVATE)

    private var playerX = 0.5f
    private var targetX = 0.5f
    private var score = 0f
    private var best = prefs.getInt("best", 0)
    private var lives = 3
    private var paused = false
    private var gameOver = false
    private var spawnTimer = 0f
    private var lastFrame = 0L

    init {
        isFocusable = true
        keepScreenOn = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrame = System.nanoTime()
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = min(0.05f, (now - lastFrame) / 1_000_000_000f)
        lastFrame = now

        if (!paused && !gameOver) updateGame(dt)
        drawGame(canvas)
        postInvalidateOnAnimation()
    }

    private fun updateGame(dt: Float) {
        score += dt * 12f
        playerX += (targetX - playerX) * min(1f, dt * 12f)

        val difficulty = min(2.5f, 1f + score / 600f)
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnObstacle(difficulty)
            spawnTimer = max(0.28f, 0.85f / difficulty)
        }

        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val playerY = h * 0.84f
        val playerR = w * 0.047f

        val it = obstacles.iterator()
        while (it.hasNext()) {
            val o = it.next()
            o.y += o.speed * dt
            o.phase += dt * 2f
            o.x += sin(o.phase) * o.drift * dt

            val dx = o.x - playerX * w
            val dy = o.y - playerY
            val rr = o.r + playerR - 8f
            if (dx * dx + dy * dy < rr * rr) {
                it.remove()
                lives--
                if (lives <= 0) endGame()
                continue
            }

            if (o.y > h + 120f) it.remove()
        }
    }

    private fun spawnObstacle(difficulty: Float) {
        val w = width.toFloat().coerceAtLeast(1f)
        val r = Random.nextFloat() * 30f + 24f
        val x = r + Random.nextFloat() * max(1f, w - 2f * r)
        val speed = (340f * densityScale()) * (0.85f + Random.nextFloat() * 0.4f) * difficulty
        val drift = (-55f + Random.nextFloat() * 110f) * densityScale()
        obstacles += Obstacle(x, -80f, r * densityScale(), speed, Random.nextFloat() * 6.28318f, drift)
    }

    private fun densityScale(): Float = resources.displayMetrics.density.coerceAtLeast(1f)

    private fun endGame() {
        gameOver = true
        best = max(best, score.toInt())
        prefs.edit().putInt("best", best).apply()
    }

    private fun restart() {
        score = 0f
        lives = 3
        paused = false
        gameOver = false
        spawnTimer = 0.2f
        obstacles.clear()
        playerX = 0.5f
        targetX = 0.5f
    }

    private fun drawGame(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.drawColor(Color.rgb(8, 17, 31))

        paint.strokeWidth = 3f
        for ((sx, sy) in stars) {
            val alpha = (120 + 80 * sin((System.currentTimeMillis() / 600.0 + sx * 30))).toInt().coerceIn(40, 220)
            paint.color = Color.argb(alpha, 100, 190, 255)
            canvas.drawCircle(sx * w, sy * h, 2.2f, paint)
        }

        paint.color = Color.argb(34, 0, 230, 255)
        canvas.drawLine(w * 0.12f, 0f, w * 0.12f, h, paint)
        canvas.drawLine(w * 0.88f, 0f, w * 0.88f, h, paint)

        for (o in obstacles) {
            paint.color = Color.argb(40, 255, 50, 95)
            canvas.drawCircle(o.x, o.y, o.r + 12f, paint)
            paint.color = Color.rgb(255, 49, 95)
            canvas.drawCircle(o.x, o.y, o.r, paint)
            paint.color = Color.argb(90, 255, 255, 255)
            canvas.drawCircle(o.x - o.r * 0.25f, o.y - o.r * 0.25f, o.r * 0.25f, paint)
        }

        val px = playerX * w
        val py = h * 0.84f
        val pr = w * 0.047f
        paint.color = Color.argb(45, 40, 230, 255)
        canvas.drawCircle(px, py, pr + 16f, paint)
        paint.color = Color.rgb(38, 230, 255)
        canvas.drawCircle(px, py, pr, paint)
        paint.color = Color.argb(130, 255, 255, 255)
        canvas.drawCircle(px - pr * 0.25f, py - pr * 0.25f, pr * 0.23f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.WHITE
        paint.textSize = w * 0.047f
        canvas.drawText("SCORE  %05d".format(score.toInt()), w * 0.04f, h * 0.065f, paint)

        paint.textSize = w * 0.034f
        paint.color = Color.rgb(165, 205, 255)
        canvas.drawText("BEST   %05d".format(best), w * 0.04f, h * 0.105f, paint)

        paint.textSize = w * 0.043f
        paint.color = Color.rgb(255, 102, 136)
        canvas.drawText("♥ $lives", w * 0.76f, h * 0.065f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.textSize = w * 0.05f
        canvas.drawText(if (paused) "▶" else "Ⅱ", w * 0.90f, h * 0.11f, paint)

        if (paused || gameOver) {
            paint.color = Color.argb(if (gameOver) 185 else 145, 0, 0, 0)
            canvas.drawRect(0f, 0f, w, h, paint)
        }

        if (paused) {
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.WHITE
            paint.textSize = w * 0.075f
            canvas.drawText("PAUSE", w / 2f, h * 0.48f, paint)
            paint.textSize = w * 0.035f
            paint.color = Color.rgb(190, 220, 255)
            canvas.drawText("Tap top-right to continue", w / 2f, h * 0.54f, paint)
        }

        if (gameOver) {
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.rgb(255, 95, 130)
            paint.textSize = w * 0.08f
            canvas.drawText("GAME OVER", w / 2f, h * 0.43f, paint)
            paint.color = Color.WHITE
            paint.textSize = w * 0.045f
            canvas.drawText("Score: ${score.toInt()}", w / 2f, h * 0.49f, paint)
            paint.color = Color.rgb(190, 230, 255)
            paint.textSize = w * 0.035f
            canvas.drawText("Tap anywhere to restart", w / 2f, h * 0.55f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (gameOver) {
                    restart()
                    return true
                }
                if (event.y < h * 0.15f && event.x > w * 0.78f) {
                    paused = !paused
                    return true
                }
                targetX = (event.x / w).coerceIn(0.06f, 0.94f)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!gameOver) targetX = (event.x / w).coerceIn(0.06f, 0.94f)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
