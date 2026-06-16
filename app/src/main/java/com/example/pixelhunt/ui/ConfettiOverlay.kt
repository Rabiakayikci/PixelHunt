package com.example.pixelhunt.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tam ekran konfeti animasyonu — harici kütüphane gerektirmez, saf Compose Canvas.
 * [visible] true olduğunda renkli parçacıklar yukarıdan aşağıya düşer ve döner.
 */
@Composable
fun ConfettiOverlay(visible: Boolean, particleCount: Int = 90) {
    if (!visible) return

    val colors = remember {
        listOf(
            Color(0xFFFF9800), Color(0xFFB388FF), Color(0xFF4CAF50),
            Color(0xFFFFD54F), Color(0xFFE91E63), Color(0xFF03A9F4)
        )
    }

    // Her parçacık için sabit rastgele özellikler
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                startXFraction = Random.nextFloat(),
                color          = colors[Random.nextInt(colors.size)],
                sizePx         = Random.nextInt(14, 30).toFloat(),
                phase          = Random.nextFloat(),
                driftAmplitude = Random.nextInt(20, 70).toFloat(),
                rotationSpeed  = Random.nextInt(2, 6).toFloat(),
                fallSpeed      = Random.nextFloat() * 0.5f + 0.75f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val progress = ((time * p.fallSpeed) + p.phase) % 1f
            val y = progress * (h + 60f) - 30f
            val x = p.startXFraction * w + sin((progress + p.phase) * 6.28f * 2) * p.driftAmplitude
            val rotation = (time * 360f * p.rotationSpeed + p.phase * 360f) % 360f

            rotate(degrees = rotation, pivot = Offset(x, y)) {
                drawRect(
                    color   = p.color,
                    topLeft = Offset(x - p.sizePx / 2, y - p.sizePx / 2),
                    size    = Size(p.sizePx, p.sizePx * 0.6f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val startXFraction: Float,
    val color: Color,
    val sizePx: Float,
    val phase: Float,
    val driftAmplitude: Float,
    val rotationSpeed: Float,
    val fallSpeed: Float
)
