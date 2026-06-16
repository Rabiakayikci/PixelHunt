package com.example.pixelhunt.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pixelhunt.R

/** SAM-r maskotunun ruh hâli — çocukla iletişim için tepkisel durumlar. */
enum class SamrMood { Idle, Talking, Happy, Thinking, Sad }

/**
 * Animasyonlu SAM-r maskotu. Her ruh hâline göre farklı hareket eder:
 * - Idle    : yumuşak süzülme + nefes alma
 * - Talking : hızlı zıplama (konuşuyormuş gibi)
 * - Happy   : büyük zıplama + sağa-sola sallanma
 * - Thinking: yavaş eğilip kalkma
 * - Sad     : aşağı sarkma + hafif eğiklik
 */
@Composable
fun SamrMascot(
    mood: SamrMood,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val transition = rememberInfiniteTransition(label = "samr")

    // Dikey süzülme / zıplama
    val bobPeriod = when (mood) {
        SamrMood.Talking -> 320
        SamrMood.Happy   -> 420
        SamrMood.Thinking -> 1600
        SamrMood.Sad     -> 2200
        SamrMood.Idle    -> 2000
    }
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(bobPeriod), RepeatMode.Reverse),
        label = "bob"
    )

    // Sallanma (rotasyon)
    val wigglePeriod = when (mood) {
        SamrMood.Happy -> 300
        SamrMood.Talking -> 600
        else -> 1400
    }
    val wiggle by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(wigglePeriod), RepeatMode.Reverse),
        label = "wiggle"
    )

    // Ruh hâline göre dönüşümler
    val translateY = when (mood) {
        SamrMood.Talking -> -bob * 14f
        SamrMood.Happy   -> -bob * 26f
        SamrMood.Thinking -> bob * 6f
        SamrMood.Sad     -> 10f + bob * 4f
        SamrMood.Idle    -> -bob * 8f
    }
    val rotation = when (mood) {
        SamrMood.Happy    -> wiggle * 10f
        SamrMood.Talking  -> wiggle * 4f
        SamrMood.Thinking -> wiggle * 6f
        SamrMood.Sad      -> -8f
        SamrMood.Idle     -> wiggle * 2f
    }
    val scaleXtra = when (mood) {
        SamrMood.Happy   -> 1f + bob * 0.08f
        SamrMood.Talking -> 1f + bob * 0.04f
        SamrMood.Sad     -> 0.92f
        else             -> 1f
    }

    Image(
        painter = painterResource(id = R.drawable.img_samr_floating),
        contentDescription = "SAM-r",
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = translateY
                rotationZ = rotation
                scaleX = scaleXtra
                scaleY = scaleXtra
            }
    )
}
