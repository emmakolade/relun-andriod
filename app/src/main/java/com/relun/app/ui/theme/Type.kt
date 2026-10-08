package com.relun.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.relun.app.R

@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: Int) = Font(
    R.font.outfit,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Outfit is a variable font; each weight is an instance of the same file. */
val Outfit = FontFamily(outfit(400), outfit(500), outfit(600), outfit(700))

/** The "relun" wordmark only. */
val Pacifico = FontFamily(Font(R.font.pacifico))

private fun style(size: Int, weight: Int, lineHeight: Double? = null, tracking: Double = 0.0) = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight?.em ?: TextStyle.Default.lineHeight,
    letterSpacing = tracking.em,
)

val RelunTypography = Typography(
    displayLarge = style(40, 700, 1.05, -0.02),
    displayMedium = style(36, 700, 1.05, -0.02),
    displaySmall = style(34, 700, 1.1),
    headlineLarge = style(30, 700, 1.15, -0.01),
    headlineMedium = style(28, 700, 1.15, -0.01),
    headlineSmall = style(24, 700, 1.15),
    titleLarge = style(22, 700, 1.2),
    titleMedium = style(20, 700, 1.2),
    titleSmall = style(16, 600, 1.25),
    bodyLarge = style(17, 400, 1.45),
    bodyMedium = style(15, 400, 1.45),
    bodySmall = style(13, 400, 1.4),
    labelLarge = style(17, 600),
    labelMedium = style(14, 600),
    labelSmall = style(12, 600),
)

/** Small caps section headers: "ABOUT", "NEW MATCHES". */
val SectionLabel = style(13, 600, tracking = 0.06)
