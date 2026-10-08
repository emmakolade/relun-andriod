package com.relun.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import com.relun.app.data.model.Segment

/** Neutrals and status colours shared by both segments. */
object RelunColors {
    val Ink = Color(0xFF1A1A1A)
    val Body = Color(0xFF4A4A4A)
    val Muted = Color(0xFF757575)
    val Faint = Color(0xFFA0A0A0)
    val Background = Color(0xFFFAFAFA)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE3E3E3)
    val BorderSoft = Color(0xFFEFEFEF)
    val Divider = Color(0xFFF2F2F2)
    val Track = Color(0xFFEFEFEF)
    val Disabled = Color(0xFFECECEC)
    val DisabledText = Color(0xFF6B6B6B)
    val ChipFill = Color(0xFFF5F3F2)
    val Skeleton = Color(0xFFECECEC)
    val SkeletonHighlight = Color(0xFFF8F8F8)

    // Separate from the rose accent so errors never read as branding.
    val Error = Color(0xFFB3261E)
    val ErrorFill = Color(0xFFFDECEA)
    val ErrorBorder = Color(0xFFF2B8B5)
    val Success = Color(0xFF1E8E5A)
    val SuccessFill = Color(0xFFE6F4EC)
    val SuccessText = Color(0xFF14653F)
    val OnlineDot = Color(0xFF22A861)
    val WarningFill = Color(0xFFFFF6E0)
    val WarningText = Color(0xFF6B4A00)

    val CoinLight = Color(0xFFFFF3A6)
    val Coin = Color(0xFFFFD700)
    val CoinDark = Color(0xFFE0B400)
    val CoinRim = Color(0xFFC99A00)
    val CoinText = Color(0xFF7A5A00)
    val CoinPanel = Color(0xFFFFF8D6)
    val CoinPanelBorder = Color(0xFFF3E2A0)

    val Scrim = Color(0x73140A0A)

    /** Welcome, "You're in" and the match screen: rose (Relationship) into orange (Fun). */
    val HeroGradient = Brush.linearGradient(listOf(Color(0xFFE0245E), Color(0xFFFF9F1C)))
    val HeroGlow = Color(0x73FF8C64)
}

/**
 * The accent set for the user's segment. Rose buttons use a deeper fill than the
 * brand pink so white text passes contrast; Fun buttons keep orange with dark text.
 */
@Immutable
data class SegmentColors(
    val segment: Segment,
    val brand: Color,
    val fill: Color,
    val onFill: Color,
    val text: Color,
    val tint: Color,
    val wash: Color,
    val label: String,
    val pill: String,
    val description: String,
    val icon: ImageVector,
) {
    companion object {
        val Relationship = SegmentColors(
            segment = Segment.Relationship,
            brand = Color(0xFFFF4B7D),
            fill = Color(0xFFE0245E),
            onFill = Color.White,
            text = Color(0xFFC81E52),
            tint = Color(0xFFFFE9F0),
            wash = Color(0xFFFFE1EA),
            label = "Relationship",
            pill = "Here for love",
            description = "Looking for something serious and long-term",
            icon = Icons.Rounded.Favorite,
        )
        val Fun = SegmentColors(
            segment = Segment.Fun,
            brand = Color(0xFFFF9F1C),
            fill = Color(0xFFFF9F1C),
            onFill = Color(0xFF1A1A1A),
            text = Color(0xFFA35400),
            tint = Color(0xFFFFF1DB),
            wash = Color(0xFFFFE7C2),
            label = "Fun",
            pill = "Here for fun",
            description = "Casual, flirty, and keeping it light",
            icon = Icons.Rounded.AutoAwesome,
        )

        fun of(segment: Segment) = if (segment == Segment.Fun) Fun else Relationship
    }
}

val LocalSegment = staticCompositionLocalOf { SegmentColors.Relationship }

object Relun {
    val segment: SegmentColors
        @Composable get() = LocalSegment.current
}

@Composable
fun RelunTheme(segment: Segment = Segment.Relationship, content: @Composable () -> Unit) {
    val accent = SegmentColors.of(segment)
    val scheme = lightColorScheme(
        primary = accent.fill,
        onPrimary = accent.onFill,
        primaryContainer = accent.tint,
        onPrimaryContainer = accent.text,
        secondary = RelunColors.Ink,
        onSecondary = Color.White,
        background = RelunColors.Background,
        onBackground = RelunColors.Ink,
        surface = RelunColors.Surface,
        onSurface = RelunColors.Ink,
        surfaceVariant = RelunColors.ChipFill,
        onSurfaceVariant = RelunColors.Muted,
        surfaceContainerLow = RelunColors.Surface,
        surfaceContainer = RelunColors.Surface,
        surfaceContainerHigh = RelunColors.Surface,
        outline = RelunColors.Border,
        outlineVariant = RelunColors.BorderSoft,
        error = RelunColors.Error,
        onError = Color.White,
        scrim = Color(0xFF140A0A),
    )
    CompositionLocalProvider(LocalSegment provides accent) {
        MaterialTheme(colorScheme = scheme, typography = RelunTypography, content = content)
    }
}
