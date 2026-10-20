package com.relun.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relun.app.data.model.NotificationDto
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.components.Avatar
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.EmptyState
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.shimmer
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatShortAgo
import java.time.Instant

/** The icon for notifications without a person's photo. */
private fun iconFor(type: String) = when (type) {
    "view" -> Icons.Outlined.Visibility
    "message_request" -> Icons.Outlined.MarkChatUnread
    "date_request" -> Icons.Rounded.Event
    "date_accepted" -> Icons.Rounded.EventAvailable
    "plus" -> Icons.Rounded.WorkspacePremium
    "coins" -> Icons.Rounded.Paid
    else -> Icons.Rounded.Favorite
}

/**
 * The list behind the bell: matches, likes, views, message and date requests,
 * and Plus or coin updates. Opening it marks everything read.
 */
@Composable
fun NotificationsScreen() {
    val c = appContainer()
    val actions = LocalAppActions.current
    var items by remember { mutableStateOf<List<NotificationDto>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        failed = false
        c.inbox.list()
            .onSuccess { list ->
                items = list
                if (list.any { !it.read }) c.inbox.markAllRead()
            }
            .onFailure { failed = items == null }
    }
    // Buying Likes & Views or Plus reveals who liked or viewed.
    LaunchedEffect(Unit) {
        c.people.events.collect { if (it == PeopleEvent.InsightsUnlocked || it == PeopleEvent.PlusChanged) attempt++ }
    }

    fun open(n: NotificationDto) {
        val userId = n.data.userId
        when (n.type) {
            "match", "message_request", "date_accepted" -> userId?.let { actions.openConversation(it, n.actor?.name.orEmpty()) }
            "like", "view" -> if (userId != null) actions.openProfile(userId) else actions.openInsights()
            // Their own dates, where requests are answered.
            "date_request" -> actions.openMyDates()
            "plus" -> actions.openPlus()
            "coins" -> actions.openCoins()
        }
    }

    Column(Modifier.fillMaxSize().background(RelunColors.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(actions.back)
            Text("Notifications", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 12.dp))
        }
        val list = items
        when {
            failed && list == null -> EmptyState(Icons.Outlined.ErrorOutline, "Couldn’t load notifications", error = true) {
                OutlineButton("Try again", { attempt++ }, Modifier.width(180.dp), leadingIcon = Icons.Rounded.Refresh, height = 48.dp)
            }
            list == null -> Column {
                repeat(5) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(48.dp).shimmer(CircleShape))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth(0.55f).height(12.dp).shimmer(RoundedCornerShape(6.dp)))
                            Box(Modifier.fillMaxWidth(0.85f).height(10.dp).shimmer(RoundedCornerShape(5.dp)))
                        }
                    }
                }
            }
            list.isEmpty() -> EmptyState(
                Icons.Outlined.Notifications,
                "No notifications yet",
                "Matches, likes, message requests and date requests will show up here.",
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.id }) { n -> NotificationRow(n) { open(n) } }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationDto, onClick: () -> Unit) {
    val seg = Relun.segment
    val unread = !n.read
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (unread) seg.tint else RelunColors.Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val actor = n.actor
        if (actor != null) {
            Avatar(actor.photoUrl, actor.id, actor.name?.take(1)?.uppercase() ?: "?", 48.dp)
        } else {
            Box(Modifier.size(48.dp).clip(CircleShape).background(seg.tint), contentAlignment = Alignment.Center) {
                Icon(iconFor(n.type), null, tint = seg.text, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(n.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold))
            Text(n.body, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val at = runCatching { Instant.parse(n.createdAt) }.getOrNull()
            if (at != null) Text(formatShortAgo(at), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted)
            if (unread) Box(Modifier.size(8.dp).clip(CircleShape).background(seg.fill))
        }
    }
}
