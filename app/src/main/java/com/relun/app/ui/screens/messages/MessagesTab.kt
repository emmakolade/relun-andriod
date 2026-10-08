package com.relun.app.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.relun.app.data.model.Conversation
import com.relun.app.data.model.Person
import com.relun.app.data.network.ApiException
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.data.socket.SocketEvent
import com.relun.app.di.AppContainer
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.Avatar
import com.relun.app.ui.components.EmptyState
import com.relun.app.ui.components.OfflineBanner
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PersonPhoto
import com.relun.app.ui.components.SectionHeader
import com.relun.app.ui.components.SegmentedControl
import com.relun.app.ui.components.placeholderBrush
import com.relun.app.ui.components.shimmer
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.screens.main.LoadState
import com.relun.app.ui.screens.main.TabBarClearance
import com.relun.app.ui.screens.main.TabSurface
import com.relun.app.ui.screens.main.TabTitle
import com.relun.app.ui.theme.Outfit
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatShortAgo
import kotlinx.coroutines.async
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MessagesView { Matches, Likes }

data class ConversationRow(val conversation: Conversation, val person: Person?)

data class MessagesState(
    val view: MessagesView = MessagesView.Matches,
    val load: LoadState = LoadState.Loading,
    val offline: Boolean = false,
    val newMatches: List<Person> = emptyList(),
    val conversations: List<ConversationRow> = emptyList(),
    /** Incoming message requests: people who paid to message the user without a match. */
    val requests: List<ConversationRow> = emptyList(),
    val likesLocked: Boolean = true,
    val likesCount: Int = 0,
    val likers: List<Person> = emptyList(),
)

class MessagesViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(MessagesState())
    val state: StateFlow<MessagesState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            c.socket.events.collect { if (it is SocketEvent.MessageNotification || it is SocketEvent.NewMessage) load(quiet = true) }
        }
        viewModelScope.launch {
            c.people.events.collect { if (it !is PeopleEvent.Passed) load(quiet = true) }
        }
    }

    fun setView(view: MessagesView) = _state.update { it.copy(view = view) }

    fun load(quiet: Boolean = false) {
        if (!quiet) _state.update { if (it.load == LoadState.Ready) it else it.copy(load = LoadState.Loading) }
        viewModelScope.launch {
            val matchesCall = async { c.people.matches() }
            val convCall = async { c.chat.conversations() }
            val likesCall = async { c.people.receivedLikes() }
            val matches = matchesCall.await()
            val conversations = convCall.await()
            val likes = likesCall.await()

            val error = matches.exceptionOrNull() ?: conversations.exceptionOrNull()
            if (error != null) {
                val network = (error as? ApiException)?.isNetwork == true
                _state.update {
                    if (it.load == LoadState.Ready && network) it.copy(offline = true)
                    else it.copy(load = LoadState.Error, offline = network)
                }
                return@launch
            }
            val people = matches.getOrThrow()
            val byId = people.associateBy { it.id }
            val all = conversations.getOrThrow()
            val convs = all.filter { it.userId in byId && it.request == null }
            val talking = convs.map { it.userId }.toSet()

            // Request conversations are with people who aren't matches, so their cards
            // come from the profile endpoint. Ones already on screen are reused.
            val requestConvs = all.filter { it.request != null && it.userId !in byId }
            val known = (_state.value.conversations + _state.value.requests).mapNotNull { it.person }.associateBy { it.id }
            val requestPeople = requestConvs.map { conv ->
                async { known[conv.userId] ?: c.people.person(conv.userId).getOrNull() }
            }.awaitAll()
            val requestRows = requestConvs.mapIndexed { i, conv ->
                ConversationRow(conv, requestPeople[i]?.copy(messageRequest = conv.request))
            }
            val rows = (convs.map { conv -> ConversationRow(conv, byId[conv.userId]) } + requestRows.filter { it.conversation.request?.outgoing == true })
                .sortedByDescending { it.conversation.lastAt }

            _state.update {
                it.copy(
                    load = if (people.isEmpty() && requestRows.isEmpty()) LoadState.Empty else LoadState.Ready,
                    offline = false,
                    newMatches = people.filterNot { p -> p.id in talking },
                    conversations = rows,
                    requests = requestRows.filter { r -> r.conversation.request?.outgoing == false },
                    likesLocked = likes.getOrNull()?.locked ?: it.likesLocked,
                    likesCount = likes.getOrNull()?.count ?: it.likesCount,
                    likers = likes.getOrNull()?.people ?: it.likers,
                )
            }
        }
    }
}

@Composable
fun MessagesTab(showLikes: Int) {
    val vm = relunViewModel { MessagesViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current

    LaunchedEffect(showLikes) { if (showLikes > 0) vm.setView(MessagesView.Likes) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load(quiet = true) }

    TabSurface {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = TabBarClearance),
        ) {
            item { TabTitle("Messages") }
            item {
                SegmentedControl(
                    options = listOf(MessagesView.Matches to "Matches", MessagesView.Likes to "Likes You · ${s.likesCount}"),
                    selected = s.view,
                    onSelect = vm::setView,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }
            if (s.offline) item {
                OfflineBanner("You’re offline. New messages will appear when you reconnect.", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
            }

            if (s.view == MessagesView.Likes) {
                item { LikesYou(s) }
            } else when (s.load) {
                LoadState.Loading -> items(5) { SkeletonRow() }
                LoadState.Empty -> item {
                    EmptyState(Icons.Outlined.ChatBubbleOutline, "No Matches Yet", "Like people in Discover. When they like you back, they’ll show up here.")
                }
                LoadState.Error -> item {
                    EmptyState(Icons.Outlined.ErrorOutline, "Could not load matches", error = true) {
                        OutlineButton("Try again", { vm.load() }, Modifier.width(180.dp), leadingIcon = Icons.Rounded.Refresh, height = 48.dp)
                    }
                }
                LoadState.Ready -> {
                    if (s.requests.isNotEmpty()) {
                        item { SectionHeader("Message requests · ${s.requests.size}", Modifier.padding(start = 20.dp, bottom = 8.dp)) }
                        item { RequestsNote(onSettings = actions.openSettings) }
                        items(s.requests, key = { "request-${it.conversation.userId}" }) { row ->
                            ConversationItem(row) { row.person?.let(actions.openChat) }
                        }
                        item { Spacer(Modifier.height(14.dp)) }
                    }
                    if (s.newMatches.isNotEmpty()) {
                        item { SectionHeader("New matches", Modifier.padding(start = 20.dp, bottom = 8.dp)) }
                        item {
                            LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(s.newMatches, key = { it.id }) { p -> NewMatch(p) { actions.openChat(p) } }
                            }
                        }
                    }
                    if (s.conversations.isNotEmpty()) {
                        item { SectionHeader("Conversations", Modifier.padding(start = 20.dp, bottom = 4.dp)) }
                        items(s.conversations, key = { it.conversation.userId }) { row ->
                            ConversationItem(row) { row.person?.let(actions.openChat) }
                        }
                    } else if (s.newMatches.isNotEmpty()) {
                        item {
                            Text(
                                "Tap a new match to start the conversation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RelunColors.Muted,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewMatch(person: Person, onClick: () -> Unit) {
    Column(
        Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Relun.segment.fill).padding(3.dp)) {
            Avatar(person.mainPhotoUrl, person.id, person.initial, 66.dp, Modifier.border(3.dp, RelunColors.Background, CircleShape))
        }
        Text(person.firstName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Explains incoming requests and points to the setting that turns them off. */
@Composable
private fun RequestsNote(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(Color(0xFFF1F1F1)).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.MarkChatUnread, null, tint = RelunColors.Body, modifier = Modifier.padding(top = 1.dp).size(17.dp))
        Text(
            buildAnnotatedString {
                append("These people haven’t matched with you. Reply to match, or decline. ")
                withStyle(SpanStyle(color = RelunColors.Ink, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                    append("Turn off message requests")
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = RelunColors.Body,
            modifier = Modifier.clickable(onClick = onSettings),
        )
    }
}

@Composable
private fun ConversationItem(row: ConversationRow, onClick: () -> Unit) {
    val c = row.conversation
    val unread = c.unread > 0
    val request = c.request
    val tag = when {
        request == null -> null
        !request.outgoing -> "Request"
        request.declined -> "Not accepted"
        else -> "Request sent"
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box {
            Avatar(row.person?.mainPhotoUrl, c.userId, c.name.take(1).uppercase(), 58.dp)
            if (row.person?.isOnline == true) {
                Box(Modifier.align(Alignment.BottomEnd).size(15.dp).clip(CircleShape).background(RelunColors.Background).padding(3.dp).clip(CircleShape).background(RelunColors.OnlineDot))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    listOfNotNull(c.name, row.person?.age?.toString()).joinToString(", "),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatShortAgo(c.lastAt), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tag != null) {
                    val muted = request?.declined == true
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (muted) RelunColors.Muted else Relun.segment.text,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (muted) RelunColors.ChipFill else Relun.segment.tint)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(
                    (if (c.lastFromMe) "You: " else "") + c.lastMessage,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (unread) RelunColors.Ink else RelunColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (unread) {
                    Text(
                        c.unread.toString(),
                        color = Relun.segment.onFill,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Relun.segment.fill).padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(58.dp).shimmer(CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(0.45f).height(12.dp).shimmer(RoundedCornerShape(6.dp)))
            Box(Modifier.fillMaxWidth(0.8f).height(10.dp).shimmer(RoundedCornerShape(5.dp)))
        }
    }
}

@Composable
private fun LikesYou(s: MessagesState) {
    val actions = LocalAppActions.current
    val seg = Relun.segment
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (s.likesLocked) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).border(1.dp, RelunColors.BorderSoft, RoundedCornerShape(24.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (s.likesCount == 1) "1 person already likes you" else "${s.likesCount} people already like you",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("See who they are and match instantly. Included with Relun Plus, or unlock with coins.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Muted)
                Row(
                    Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(seg.fill).clickable(onClick = actions.openInsights),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("See who likes you", color = seg.onFill, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                }
            }
        }
        if (s.likesCount == 0) {
            EmptyState(Icons.Outlined.FavoriteBorder, "No likes yet", "When someone likes you, they’ll show up here.")
        } else {
            val tiles = if (s.likesLocked) List(s.likesCount.coerceAtMost(8)) { null } else s.likers
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEachIndexed { i, person ->
                        Box(Modifier.weight(1f).aspectRatio(0.75f).clip(RoundedCornerShape(20.dp)).clickable {
                            if (person == null) actions.openInsights() else actions.openProfile(person.id)
                        }) {
                            if (person == null) {
                                Box(Modifier.fillMaxSize().background(placeholderBrush("locked-$i-${pair.size}", i)), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Lock, "Locked", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            } else {
                                PersonPhoto(person.mainPhotoUrl, person.id, person.initial, Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp), initialSize = 56.sp)
                                Text(
                                    listOfNotNull(person.firstName, person.age?.toString()).joinToString(", "),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                                        .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp),
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}
