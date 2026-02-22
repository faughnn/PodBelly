package com.podbelly.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val podAlpha = remember { Animatable(0f) }
    val bellyAlpha = remember { Animatable(0f) }
    val bellyOffset = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // "pod" fades in
        podAlpha.animateTo(1f, animationSpec = tween(400))
        // "belly" slides in from right + fades in with spring
        launch {
            bellyAlpha.animateTo(1f, animationSpec = tween(400))
        }
        bellyOffset.animateTo(
            0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        )
        // Hold briefly, then finish
        delay(200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Row {
            Text(
                text = "pod",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(podAlpha.value),
            )
            Text(
                text = "belly",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .alpha(bellyAlpha.value)
                    .offset(x = bellyOffset.value.dp),
            )
        }
    }
}
