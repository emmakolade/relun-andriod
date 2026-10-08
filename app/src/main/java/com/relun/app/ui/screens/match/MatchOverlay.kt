package com.relun.app.ui.screens.match

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.data.model.Person
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.components.LinkButton
import com.relun.app.ui.components.PersonPhoto
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MatchOverlay(person: Person, onMessage: () -> Unit, onKeepBrowsing: () -> Unit) {
    val s = Relun.segment
    val me by appContainer().profile.me.collectAsStateWithLifecycle()
    BackHandler(onBack = onKeepBrowsing)

    val fly = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(person.id) {
        launch { fall.animateTo(1f, tween(3000, easing = LinearEasing)) }
        fly.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
        launch { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy)) }
        delay(120)
        rise.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(RelunColors.HeroGradient)
            .background(Brush.radialGradient(listOf(RelunColors.HeroGlow, Color.Transparent), radius = 900f))
            // Swallow taps so the screen underneath stays inert.
            .clickable(remember { MutableInteractionSource() }, null) {},
    ) {
        Confetti(progress = fall.value, accent = s.brand)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Row {
                Card(me?.photos?.firstOrNull()?.url, "me", me?.initial ?: "", rotation = -7f, from = -240f, progress = fly.value, shift = 16.dp)
                Card(person.mainPhotoUrl, person.id, person.initial, rotation = 7f, from = 240f, progress = fly.value, shift = (-16).dp)
            }
            Box(
                Modifier
                    .offset(y = (-28).dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .size(56.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Icon(s.icon, null, tint = s.fill, modifier = Modifier.size(28.dp)) }
            Column(
                Modifier.graphicsLayer { alpha = rise.value; translationY = (1 - rise.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("It’s a match!", style = MaterialTheme.typography.displayLarge, color = Color.White, textAlign = TextAlign.Center)
                Text(
                    "You and ${person.firstName} like each other.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton("Send a message", onMessage, container = Color.White, content = RelunColors.Ink, leadingIcon = Icons.Outlined.ChatBubbleOutline)
            LinkButton("Keep browsing", onKeepBrowsing, Modifier.padding(top = 8.dp), color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
        }
    }
}

@Composable
private fun Card(url: String?, seed: String, initial: String, rotation: Float, from: Float, progress: Float, shift: androidx.compose.ui.unit.Dp) {
    PersonPhoto(
        url = url,
        seed = seed,
        initial = initial,
        modifier = Modifier
            .offset(x = shift)
            .graphicsLayer {
                translationX = from * (1 - progress)
                rotationZ = rotation * progress + (rotation * 4) * (1 - progress)
                alpha = progress.coerceIn(0f, 1f)
            }
            .width(140.dp)
            .aspectRatio(0.8f)
            .shadow(16.dp, RoundedCornerShape(28.dp))
            .border(4.dp, Color.White, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        initialSize = 48.sp,
    )
}

/** Thirty falling paper pieces in white, gold, brand and pink. */
@Composable
private fun Confetti(progress: Float, accent: Color) {
    val colors = listOf(Color.White, Color(0xFFFFD700), accent, Color(0xFFFFC2D4))
    Canvas(Modifier.fillMaxSize()) {
        repeat(30) { i ->
            val delay = (i % 8) * 0.04f
            val p = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (p <= 0f) return@repeat
            val x = ((i * 37) % 100) / 100f * size.width
            val y = -30f + p * (size.height + 60f)
            val w = (6 + (i % 3) * 3).dp.toPx()
            val h = (10 + (i % 4) * 3).dp.toPx()
            rotate(degrees = p * 620f * if (i % 2 == 0) 1 else -1, pivot = Offset(x, y)) {
                drawRect(colors[i % 4], topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), alpha = 1f - p * 0.3f)
            }
        }
    }
}
