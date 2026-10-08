package com.relun.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors

/** Loading state shared by the data-driven tabs. */
enum class LoadState { Loading, Ready, Empty, Error }

/** Tab background: the segment wash fading into the page grey. */
@Composable
fun TabSurface(content: @Composable BoxScope.() -> Unit) {
    val s = Relun.segment
    Box(
        Modifier
            .fillMaxSize()
            .background(RelunColors.Background)
            .background(Brush.verticalGradient(0f to s.wash, 0.35f to RelunColors.Background, endY = 900f)),
        content = content,
    )
}

@Composable
fun TabTitle(
    title: String,
    modifier: Modifier = Modifier,
    below: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            below?.invoke()
        }
        actions()
    }
}
