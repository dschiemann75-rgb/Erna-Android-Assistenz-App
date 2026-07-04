package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Represents a node in the neural particle fallback network
data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun FuturisticBackground(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FuturisticBackground")

    // Pulse value for glowing cyber-rings
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Scanning line animation
    val scanY by infiniteTransition.animateFloat(
        initialValue = -0.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanY"
    )

    // Base cosmic color gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF030712), // Very dark space slate
            Color(0xFF0F172A), // Dark slate blue
            Color(0xFF020617)  // Deep cosmic void
        )
    )

    // Particle state initialized once
    val particles = remember {
        val list = mutableListOf<Particle>()
        val rand = Random(42)
        for (i in 0..25) {
            list.add(
                Particle(
                    x = rand.nextFloat(),
                    y = rand.nextFloat(),
                    vx = (rand.nextFloat() - 0.5f) * 0.001f,
                    vy = (rand.nextFloat() - 0.5f) * 0.001f,
                    radius = rand.nextFloat() * 4f + 2f,
                    color = when (rand.nextInt(3)) {
                        0 -> Color(0xFF06B6D4).copy(alpha = 0.4f) // Glowing Cyan
                        1 -> Color(0xFF3B82F6).copy(alpha = 0.3f) // Tech Blue
                        else -> Color(0xFF8B5CF6).copy(alpha = 0.3f) // Purple accent
                    }
                )
            )
        }
        list
    }

    // Dynamic wave animation for active voice/sound waves
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Update & Draw Neural Particles
            particles.forEach { p ->
                // Move
                p.x = (p.x + p.vx + 1f) % 1f
                p.y = (p.y + p.vy + 1f) % 1f

                val px = p.x * w
                val py = p.y * h

                // Draw node
                drawCircle(
                    color = p.color,
                    radius = p.radius,
                    center = Offset(px, py)
                )
            }

            // Draw connecting lines between close particles
            for (i in particles.indices) {
                for (j in i + 1 until particles.size) {
                    val p1 = particles[i]
                    val p2 = particles[j]
                    val dx = (p1.x - p2.x) * w
                    val dy = (p1.y - p2.y) * h
                    val dist = dx * dx + dy * dy
                    val maxDist = (w * 0.15f) * (w * 0.15f)

                    if (dist < maxDist) {
                        val alpha = (1f - (dist / maxDist)).coerceIn(0f, 1f) * 0.25f
                        drawLine(
                            color = Color(0xFF06B6D4).copy(alpha = alpha),
                            start = Offset(p1.x * w, p1.y * h),
                            end = Offset(p2.x * w, p2.y * h),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // Draw pulsing concentric target circles (hologram lens)
            val centerX = w * 0.5f
            val centerY = h * 0.4f
            val baseRadius = w * 0.3f

            // Inner Ring
            drawCircle(
                color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                radius = baseRadius * pulseScale,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )

            // Outer ring
            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                radius = baseRadius * (pulseScale * 1.3f),
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // Dynamic voice response visualizer (sine waves at the bottom)
            if (isListening || isSpeaking) {
                val waveCount = 3
                val points = 80
                val waveHeight = if (isListening) h * 0.04f else h * 0.025f

                for (wIdx in 0 until waveCount) {
                    val waveColor = when (wIdx) {
                        0 -> Color(0xFF22D3EE).copy(alpha = 0.6f) // Cyan
                        1 -> Color(0xFF6366F1).copy(alpha = 0.4f) // Indigo
                        else -> Color(0xFFD946EF).copy(alpha = 0.3f) // Magenta
                    }

                    val frequency = 2.0f * Math.PI.toFloat() / w
                    val phaseShift = waveOffset + (wIdx * 1.5f)
                    var prevOffset = Offset(0f, h * 0.85f)

                    for (xStep in 0..points) {
                        val currX = (xStep.toFloat() / points) * w
                        // Build standard sine wave
                        val sine = sin(currX * frequency * (wIdx + 1.2f) + phaseShift)
                        // Fade at edges
                        val edgeFade = sin((xStep.toFloat() / points) * Math.PI.toFloat()).toFloat()
                        val currY = h * 0.85f + (sine * waveHeight * edgeFade)

                        val currOffset = Offset(currX, currY)
                        if (xStep > 0) {
                            drawLine(
                                color = waveColor,
                                start = prevOffset,
                                end = currOffset,
                                strokeWidth = (3 - wIdx).toFloat() * 1.5f
                            )
                        }
                        prevOffset = currOffset
                    }
                }
            }

            // Scanning laser line
            val scanLineY = scanY * h
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF06B6D4).copy(alpha = 0.4f),
                        Color(0xFF22D3EE).copy(alpha = 0.8f),
                        Color(0xFF06B6D4).copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, scanLineY),
                end = Offset(w, scanLineY),
                strokeWidth = 3f
            )
        }
    }
}
