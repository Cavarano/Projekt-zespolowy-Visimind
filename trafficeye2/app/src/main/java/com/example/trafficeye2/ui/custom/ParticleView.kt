package com.example.trafficeye2.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.util.TypedValue
import kotlin.math.*
import androidx.core.graphics.withTranslation

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (particles.isEmpty()) {
            generateParticles()
            startAnimation()
        }
    }

    private fun generateParticles(count: Int = 100) {
        val shapes = ShapeType.entries.toTypedArray()
        repeat(count) {
            particles.add(
                Particle(
                    x = (Math.random() * width).toFloat(),
                    y = (Math.random() * height / 1.5).toFloat(),
                    size = (15 + Math.random() * Math.random() * 50).toFloat(),
                    shape = shapes.random(),
                    rotation = (0..360).random().toFloat(),
                    rotationSpeed = (-2..2).random().toFloat(),
                    speedY = (0.2 + Math.random()).toFloat(),
                    alpha = 255
                )
            )
        }
    }


    private fun getThemeColorPrimary(): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
        return typedValue.data
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (particle in particles) {
            val colorPrimary = getThemeColorPrimary()
            val r = Color.red(colorPrimary) + 20
            val g = Color.green(colorPrimary) + 20
            val b = Color.blue(colorPrimary) + 20
            Float

            paint.color = Color.argb(particle.alpha, r, g, b)
            paint.alpha = particle.alpha

            canvas.withTranslation(particle.x, particle.y) {
                rotate(particle.rotation)
                drawShape(this, particle)
            }
        }
    }

    private fun drawShape(canvas: Canvas, p: Particle) {
        val path = Path()
        when (p.shape) {
            ShapeType.CIRCLE -> {
                canvas.drawCircle(0f, 0f, p.size / 2, paint)
            }
            ShapeType.TRIANGLE -> {
                val r = p.size / 2
                val angleOffset = -90f // Starts pointing up

                for (i in 0 until 3) {
                    val angleDeg = angleOffset + i * 120
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val x = cos(angleRad).toFloat() * r
                    val y = sin(angleRad).toFloat() * r
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.RHOMBUS -> {
                path.moveTo(0f, -p.size / 2)
                path.lineTo(p.size / 2, 0f)
                path.lineTo(0f, p.size / 2)
                path.lineTo(-p.size / 2, 0f)
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeType.OCTAGON -> {
                val r = p.size / 2
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val x = cos(angle).toFloat() * r
                    val y = sin(angle).toFloat() * r
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 16L
        animator.repeatCount = ValueAnimator.INFINITE

        animator.addUpdateListener {
            for (p in particles) {
                p.y += p.speedY
                p.rotation += p.rotationSpeed

                // Fade out smoothly
                val startFadeY = height / 4f
                val endFadeY = height * 2f / 3f

                if (p.y in startFadeY..endFadeY) {
                    val progress = (p.y - startFadeY) / (endFadeY - startFadeY) // 0 → 1
                    p.alpha = (255 * (1 - progress)).toInt().coerceIn(0, 255)
                } else if (p.y > endFadeY) {
                    p.alpha = 0
                } else {
                    p.alpha = 255
                }

                // Respawn at top when invisible
                if (p.y > height || p.alpha <= 0) {
                    p.y = 0f
                    p.x = (Math.random() * width).toFloat()
                    p.alpha = 255
                }
            }
            invalidate()
        }
        animator.start()
    }

    data class Particle(
        var x: Float,
        var y: Float,
        val size: Float,
        val shape: ShapeType,
        var rotation: Float,
        val rotationSpeed: Float,
        val speedY: Float,
        var alpha: Int
    )

    enum class ShapeType {
        CIRCLE, TRIANGLE, RHOMBUS, OCTAGON
    }
}
