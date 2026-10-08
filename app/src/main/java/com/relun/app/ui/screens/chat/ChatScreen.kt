package com.relun.app.ui.screens.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.data.model.ChatMessage
import com.relun.app.data.model.MessageRequest
import com.relun.app.data.model.MessageStatus
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.Avatar
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.CircleIconButton
import com.relun.app.ui.components.CircleStyle
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.RelunTextField
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.theme.Outfit
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatClock
import com.relun.app.util.formatDay
import java.time.ZoneId

private val OPENERS = listOf(
    "What’s the best thing you ate this week?",
    "Coffee or cocktails for a first date?",
    "Your second photo has a story. Spill.",
)

private sealed interface ChatItem {
    data class Day(val label: String) : ChatItem
    data class Bubble(val message: ChatMessage) : ChatItem
}

@Composable
fun ChatScreen(userId: String, initialName: String) {
    val vm = relunViewModel(key = "chat-$userId") { ChatViewModel(it, userId, initialName) }
    val s by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val seg = Relun.segment
    val list = rememberLazyListState()

    val items = buildList<ChatItem> {
        var lastDay: java.time.LocalDate? = null
        s.messages.forEach { m ->
            val day = m.sentAt.atZone(ZoneId.systemDefault()).toLocalDate()
            if (day != lastDay) {
                add(ChatItem.Day(formatDay(m.sentAt)))
                lastDay = day
            }
            add(ChatItem.Bubble(m))
        }
    }
    LaunchedEffect(items.size, s.otherTyping) {
        if (items.isNotEmpty()) list.animateScrollToItem(items.size + 1)
    }

    Column(Modifier.fillMaxSize().background(RelunColors.Background).imePadding()) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(Color.White).statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BackButton(actions.back, style = CircleStyle.Flat)
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { actions.openProfile(userId) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Avatar(s.person?.mainPhotoUrl, userId, s.name.take(1).uppercase(), 42.dp)
                Column {
                    Text(
                        listOfNotNull(s.name.ifBlank { "Chat" }, s.person?.age?.toString()).joinToString(", "),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val status = when {
                        s.otherTyping -> "typing…"
                        s.otherOnline -> "Online"
                        else -> "Offline"
                    }
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = if (s.otherTyping || s.otherOnline) RelunColors.Success else RelunColors.Muted,
                    )
                }
            }
            s.person?.let { p -> CircleIconButton(Icons.Rounded.MoreVert, "Report or block", { actions.openMore(p) }, style = CircleStyle.Flat) }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(RelunColors.BorderSoft))

        if (!s.connected) {
            Row(
                Modifier.fillMaxWidth().background(RelunColors.WarningFill).padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Sync, null, tint = RelunColors.WarningText, modifier = Modifier.size(15.dp))
                Text("  Reconnecting…", color = RelunColors.WarningText, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            }
        }

        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Row(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F1F1)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = RelunColors.Body, modifier = Modifier.size(14.dp))
                        Text("Keep your conversations safe. Report any suspicious behaviour.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Body, textAlign = TextAlign.Center)
                    }
                }
            }
            if (s.loading) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = seg.fill) } }
            } else if (s.messages.isEmpty()) {
                item { EmptyChat(s, onOpener = { vm.send(it) }) }
            }
            items(items) { item ->
                when (item) {
                    is ChatItem.Day -> Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = RelunColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                    )
                    is ChatItem.Bubble -> Bubble(item.message, onRetry = { vm.retry(item.message) })
                }
            }
            if (s.otherTyping) item { TypingBubble() }
        }

        // Composer, or why a message request's sender can't write right now.
        Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
            val request = s.request
            if (request != null && !s.loading) {
                RequestPanel(
                    request = request,
                    name = s.name.substringBefore(' ').ifBlank { s.name },
                    declining = s.declining,
                    onDecline = { vm.decline(onDone = actions.back) },
                    onSettings = actions.openSettings,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val person = s.person
                val block = s.requestBlock
                val first = s.name.substringBefore(' ').ifBlank { s.name }
                if (block != null) {
                    Row(
                        Modifier.weight(1f).heightIn(min = 50.dp).clip(RoundedCornerShape(16.dp)).background(RelunColors.ChipFill).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            if (block == RequestBlock.Declined) Icons.Rounded.Block else Icons.Rounded.AccessTime,
                            null,
                            tint = RelunColors.Body,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (block == RequestBlock.Declined) "$first declined your message request" else "Waiting for $first to reply",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = RelunColors.Body,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    RelunTextField(
                        value = s.draft,
                        onValueChange = vm::setDraft,
                        placeholder = "Type a message…",
                        singleLine = false,
                        minHeight = 46.dp,
                        radius = 23.dp,
                        fill = RelunColors.Background,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                        modifier = Modifier.weight(1f).heightIn(max = 120.dp),
                    )
                    val canSend = s.draft.isNotBlank()
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (canSend) seg.fill else RelunColors.Disabled)
                            .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Send") { vm.send() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, "Send", tint = if (canSend) seg.onFill else RelunColors.DisabledText, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

/**
 * The message request strip above the composer. The receiver gets the choice
 * (reply to match, or decline) and a pointer to the setting that turns these
 * off; the sender sees how many messages they have left.
 */
@Composable
private fun RequestPanel(
    request: MessageRequest,
    name: String,
    declining: Boolean,
    onDecline: () -> Unit,
    onSettings: () -> Unit,
) {
    val seg = Relun.segment
    if (request.outgoing) {
        if (request.declined || request.remaining <= 0) return
        val left = request.remaining
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.MarkChatUnread, null, tint = RelunColors.Muted, modifier = Modifier.size(16.dp))
            Text(
                "Message request · $left ${if (left == 1) "message" else "messages"} left until $name replies",
                style = MaterialTheme.typography.bodySmall,
                color = RelunColors.Muted,
            )
        }
        return
    }

    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp)
            .clip(RoundedCornerShape(20.dp)).background(seg.tint).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.MarkChatUnread, null, tint = seg.text, modifier = Modifier.padding(top = 1.dp).size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("$name sent you a message request", style = MaterialTheme.typography.titleSmall)
                Text(
                    "You haven’t matched. Reply to match and keep chatting, or decline and $name can’t message you again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RelunColors.Body,
                )
            }
        }
        OutlineButton(
            text = if (declining) "Declining…" else "Decline",
            onClick = { if (!declining) onDecline() },
            leadingIcon = Icons.Rounded.Block,
            height = 42.dp,
        )
        Text(
            buildAnnotatedString {
                append("Don’t want messages from people you haven’t matched with? ")
                withStyle(SpanStyle(color = RelunColors.Ink, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                    append("Turn off message requests")
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = RelunColors.Muted,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onSettings),
        )
    }
}

@Composable
private fun EmptyChat(s: ChatState, onOpener: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(s.person?.mainPhotoUrl, s.person?.id ?: s.name, s.name.take(1).uppercase(), 96.dp)
        Text("You matched with ${s.name.substringBefore(' ')}. Say hello!", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text("Need a nudge? Tap one to send it.", style = MaterialTheme.typography.bodySmall, color = RelunColors.Muted)
        OPENERS.forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, RelunColors.Border, RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable { onOpener(line) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, onRetry: () -> Unit) {
    val seg = Relun.segment
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 300.dp).padding(bottom = 4.dp),
            horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            val shape = if (m.mine) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
            Text(
                m.text,
                color = if (m.mine) seg.onFill else RelunColors.Ink,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                modifier = Modifier
                    .clip(shape)
                    .background(if (m.mine) seg.fill else Color.White)
                    .then(if (m.mine) Modifier else Modifier.border(1.dp, Color(0xFFECECEC), shape))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            when (m.status) {
                MessageStatus.Failed -> Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRetry).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.Error, null, tint = RelunColors.Error, modifier = Modifier.size(15.dp))
                    Text("Not delivered · Tap to retry", color = RelunColors.Error, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                MessageStatus.Sending -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.AccessTime, null, tint = RelunColors.Muted, modifier = Modifier.size(13.dp))
                    Text("Sending…", color = RelunColors.Muted, fontSize = 11.sp, fontFamily = Outfit)
                }
                else -> Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(formatClock(m.sentAt), color = RelunColors.Muted, fontSize = 11.sp, fontFamily = Outfit)
                    if (m.mine) {
                        val read = m.status == MessageStatus.Read
                        Icon(
                            if (read) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                            if (read) "Read" else "Sent",
                            tint = if (read) seg.text else RelunColors.Faint,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val t = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECECEC), RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { i ->
            val y by t.animateFloat(0f, -4f, infiniteRepeatable(tween(400, delayMillis = i * 150), RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.offset(y = y.dp).size(7.dp).clip(CircleShape).background(RelunColors.Muted))
        }
    }
}
