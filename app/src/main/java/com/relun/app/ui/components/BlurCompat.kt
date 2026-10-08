package com.relun.app.ui.components

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp

/**
 * Blur on Android 12+, where RenderEffect exists. Older devices have no blur, so
 * the photo is hidden entirely and the gradient placeholder shows instead:
 * locked likes must never be readable.
 */
fun Modifier.blurCompat(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blur(radius) else alpha(0f)
