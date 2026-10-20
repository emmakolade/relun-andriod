package com.relun.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.components.CircleIconButton
import com.relun.app.ui.navigation.LocalAppActions
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
        NotificationBell()
        actions()
    }
}

/** The bell in every tab's header, with the unread count. */
@Composable
private fun NotificationBell() {
    val c = appContainer()
    val actions = LocalAppActions.current
    val seg = Relun.segment
    val unread by c.inbox.unread.collectAsStateWithLifecycle()
    Box {
        CircleIconButton(
            Icons.Outlined.Notifications,
            if (unread > 0) "Notifications, $unread unread" else "Notifications",
            actions.openNotifications,
        )
        if (unread > 0) {
            Text(
                if (unread > 99) "99+" else unread.toString(),
                color = seg.onFill,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .border(2.dp, RelunColors.Background, RoundedCornerShape(9.dp))
                    .clip(RoundedCornerShape(9.dp))
                    .background(seg.fill)
                    .defaultMinSize(minWidth = 18.dp)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}
