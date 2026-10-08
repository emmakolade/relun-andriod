package com.relun.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.relun.app.ui.theme.Outfit
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.ui.theme.SegmentColors
import com.relun.app.util.formatCoins
import kotlin.math.abs

/**
 * The Relun mark: a rose ring (Relationship) linked with an orange one (Fun).
 * Same drawing as the launcher icon; [width] is the width of both rings.
 */
@Composable
fun RingsMark(width: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = width, height = width * (51f / 73f))) {
        // Units of the 108-unit launcher grid; the rings span 73 x 51 of them.
        val u = size.width / 73f
        val radius = 21f * u
        val stroke = Stroke(width = 9f * u)
        val y = size.height / 2
        val rose = Offset(25.5f * u, y)
        val orange = Offset(47.5f * u, y)
        drawCircle(SegmentColors.Relationship.fill, radius, rose, style = stroke)
        drawCircle(SegmentColors.Fun.fill, radius, orange, style = stroke)
        // Linked: rose passes over orange at the top crossing, under it at the bottom.
        clipRect(left = rose.x, top = 0f, right = size.width, bottom = y) {
            drawCircle(SegmentColors.Relationship.fill, radius, rose, style = stroke)
        }
    }
}

/** The single coin mark: a gold disc with a star. */
@Composable
fun CoinIcon(size: Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    0f to RelunColors.CoinLight,
                    0.55f to RelunColors.Coin,
                    1f to RelunColors.CoinDark,
                    center = Offset.Unspecified,
                )
            )
            .border((size.value / 16f).coerceAtLeast(1f).dp, RelunColors.CoinRim, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Star, null, tint = RelunColors.CoinText, modifier = Modifier.size(size * 0.48f))
    }
}

/** Balance pill in the Discover header. Turns red when a chat unlock is no longer affordable. */
@Composable
fun CoinPill(balance: Int, low: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (low) RelunColors.ErrorFill else Color.White
    val border = if (low) RelunColors.ErrorBorder else Color(0xFFEDEDED)
    val fg = if (low) RelunColors.Error else RelunColors.Ink
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinIcon(26.dp)
        Text(formatCoins(balance), color = fg, style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp))
    }
}

@Composable
fun SegmentPill(modifier: Modifier = Modifier, text: String = Relun.segment.pill, small: Boolean = false) {
    val s = Relun.segment
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(s.tint)
            .padding(start = if (small) 7.dp else 10.dp, end = if (small) 9.dp else 12.dp, top = if (small) 3.dp else 5.dp, bottom = if (small) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(s.icon, null, tint = s.text, modifier = Modifier.size(if (small) 12.dp else 14.dp))
        Text(text, color = s.text, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = if (small) 11.sp else 13.sp)
    }
}

/**
 * Soft two-tone gradient used where a photo is missing or still loading. Stable
 * per person, so the same person always gets the same colour.
 */
fun placeholderBrush(seed: String, step: Int = 0): Brush {
    val hue = ((abs(seed.hashCode()) % 360) + step * 45) % 360
    val top = Color.hsv(hue.toFloat(), 0.22f, 0.9f)
    val bottom = Color.hsv(((hue + 25) % 360).toFloat(), 0.42f, 0.62f)
    return Brush.linearGradient(listOf(top, bottom), start = Offset(0f, 0f), end = Offset(400f, 900f))
}

/**
 * A person's photo with a stable gradient and initial behind it. If the image
 * fails, shows "Photo unavailable" instead of a blank box.
 */
@Composable
fun PersonPhoto(
    url: String?,
    seed: String,
    initial: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    initialSize: TextUnit = 44.sp,
    step: Int = 0,
    blur: Boolean = false,
    failureLabel: String? = "Photo unavailable",
) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(modifier.clip(shape).background(placeholderBrush(seed, step)), contentAlignment = Alignment.Center) {
        if (url == null || failed) {
            if (failed && failureLabel != null) {
                Box(Modifier.fillMaxSize().background(Color(0xFFE3E3E3)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.BrokenImage, null, tint = RelunColors.Muted, modifier = Modifier.size(22.dp))
                        Text(failureLabel, color = RelunColors.Muted, fontSize = 11.sp, fontFamily = Outfit)
                    }
                }
            } else if (!blur) {
                Text(initial, color = Color.White.copy(alpha = 0.6f), fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = initialSize)
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blur) Modifier.blurCompat(28.dp) else Modifier),
            )
        }
    }
}

@Composable
fun Avatar(url: String?, seed: String, initial: String, size: Dp, modifier: Modifier = Modifier) =
    PersonPhoto(
        url = url,
        seed = seed,
        initial = initial,
        modifier = modifier.size(size),
        shape = CircleShape,
        initialSize = (size.value * 0.38f).sp,
        failureLabel = null,
    )

/** Animated grey shimmer for loading placeholders. */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(16.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    clip(shape).background(
        Brush.linearGradient(
            listOf(RelunColors.Skeleton, RelunColors.SkeletonHighlight, RelunColors.Skeleton),
            start = Offset(x * 600f, 0f),
            end = Offset(x * 600f + 600f, 0f),
        )
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val s = Relun.segment
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (error) RelunColors.ErrorFill else s.tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (error) RelunColors.Error else s.text, modifier = Modifier.size(34.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = RelunColors.Ink,
            modifier = Modifier.padding(top = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = RelunColors.Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (action != null) Box(Modifier.padding(top = 8.dp)) { action() }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = com.relun.app.ui.theme.SectionLabel,
        color = RelunColors.Muted,
        modifier = modifier,
    )
}

@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp, background: Color = Color.White, tint: Color = RelunColors.Ink) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(0.5.dp, RelunColors.BorderSoft, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size / 2)) }
}
