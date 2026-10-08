package com.relun.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.relun.app.data.model.Person
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.di.AppContainer
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.CircleIconButton
import com.relun.app.ui.components.CircleStyle
import com.relun.app.ui.components.CoinIcon
import com.relun.app.ui.components.EmptyState
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PersonPhoto
import com.relun.app.ui.components.SectionHeader
import com.relun.app.ui.components.SegmentPill
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.screens.discover.LikeButton
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatDistance
import com.relun.app.util.formatHeight
import com.relun.app.util.formatLastActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonState(val person: Person? = null, val loading: Boolean = true, val error: String? = null)

class PersonProfileViewModel(private val c: AppContainer, private val userId: String) : ViewModel() {
    private val _state = MutableStateFlow(PersonState())
    val state: StateFlow<PersonState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            c.people.events.collect { e ->
                when (e) {
                    is PeopleEvent.ChatUnlocked -> if (e.userId == userId) update { it.copy(chatUnlocked = true) }
                    is PeopleEvent.Liked -> if (e.userId == userId) update { it.copy(liked = true, isMatch = it.isMatch || e.isMatch) }
                    else -> Unit
                }
            }
        }
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            c.people.person(userId)
                .onSuccess { p -> _state.update { PersonState(person = p, loading = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun like() {
        val p = _state.value.person ?: return
        if (p.liked || p.isMatch) return
        update { it.copy(liked = true) }
        viewModelScope.launch {
            c.people.like(p).onFailure { e ->
                update { it.copy(liked = false) }
                c.messenger.error(e.message ?: "Couldn’t like ${p.firstName}.")
            }
        }
    }

    fun pass(onDone: () -> Unit) {
        viewModelScope.launch { c.people.pass(userId) }
        onDone()
    }

    private fun update(block: (Person) -> Person) = _state.update { s -> s.copy(person = s.person?.let(block)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonProfileScreen(userId: String) {
    val vm = relunViewModel(key = "person-$userId") { PersonProfileViewModel(it, userId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val person = s.person

    Box(Modifier.fillMaxSize().background(Color.White)) {
        when {
            person != null -> Content(person, vm)
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Relun.segment.fill) }
            else -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                BackButton(actions.back, Modifier.padding(16.dp))
                EmptyState(Icons.Outlined.Person, "Profile unavailable", s.error ?: "This profile can’t be shown right now.", error = true) {
                    OutlineButton("Try again", vm::load, leadingIcon = Icons.Rounded.Refresh, height = 48.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Content(person: Person, vm: PersonProfileViewModel) {
    val actions = LocalAppActions.current
    val seg = Relun.segment
    var photo by rememberSaveable { mutableIntStateOf(0) }
    val count = person.photos.size.coerceAtLeast(1)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
            Box(Modifier.fillMaxWidth().height(540.dp)) {
                PersonPhoto(
                    url = person.photos.getOrNull(photo)?.url,
                    seed = person.id,
                    initial = person.initial,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                    initialSize = 140.sp,
                    step = photo,
                    failureLabel = "Photo couldn’t load",
                )
                // Tap the left side for the previous photo, the right side for the next.
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(0.4f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { photo = (photo - 1).coerceAtLeast(0) })
                    Box(Modifier.weight(0.6f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { photo = (photo + 1).coerceAtMost(count - 1) })
                }
                Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))))
                Row(
                    Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BackButton(actions.back, style = CircleStyle.Glass)
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (count > 1) repeat(count) { i ->
                            Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (i == photo) Color.White else Color.White.copy(alpha = 0.45f)))
                        }
                    }
                    CircleIconButton(Icons.Rounded.MoreHoriz, "More options", { actions.openMore(person) }, style = CircleStyle.Glass, iconSize = 22.dp)
                }
            }
            Column(
                Modifier
                    .offset(y = (-32).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        listOfNotNull(person.name, person.age?.toString()).joinToString(", "),
                        style = MaterialTheme.typography.displayMedium,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        formatDistance(person.distanceKm)?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.LocationOn, null, tint = RelunColors.Body, modifier = Modifier.size(15.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = RelunColors.Body)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (person.isOnline) Box(Modifier.size(8.dp).clip(CircleShape).background(RelunColors.OnlineDot))
                            Text(formatLastActive(person.isOnline, person.lastActive), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = RelunColors.Body)
                        }
                    }
                    SegmentPill()
                }
                if (person.bio.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeader("About")
                        Text(person.bio, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 29.sp))
                    }
                }
                val details = listOfNotNull(
                    person.occupation?.let { Icons.Outlined.WorkOutline to it },
                    person.education?.let { Icons.Outlined.School to it },
                    formatHeight(person.heightCm)?.let { Icons.Outlined.Straighten to it },
                    person.drinking?.let { Icons.Outlined.WineBar to it },
                    person.smoking?.let { Icons.Outlined.SmokeFree to "Smoking: $it" },
                    person.religion?.let { Icons.Outlined.AutoAwesome to it },
                    person.city?.let { Icons.Outlined.LocationOn to it },
                )
                if (details.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader("Details")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            details.forEach { (icon, value) -> DetailChip(icon, value) }
                        }
                    }
                }
                if (person.interests.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader("Interests")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            person.interests.forEach {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.5.dp, RelunColors.Border, RoundedCornerShape(999.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
                Column {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(RelunColors.BorderSoft))
                    Row(
                        Modifier.padding(top = 8.dp).height(44.dp).clickable { actions.openMore(person) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.Flag, null, tint = RelunColors.Body, modifier = Modifier.size(16.dp))
                        Text("Report or block", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = RelunColors.Body)
                    }
                }
            }
        }

        // Action bar.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0f), 0.3f to Color.White))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircleIconButton(Icons.Rounded.Close, "Pass", { vm.pass(actions.back) }, size = 56.dp, iconSize = 26.dp, modifier = Modifier.shadow(4.dp, CircleShape))
            LikeButton(person.liked || person.isMatch, vm::like, Modifier.size(56.dp))
            val enabled = person.isMatch
            Row(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (enabled) seg.fill else RelunColors.Disabled)
                    .clickable(enabled = enabled, role = Role.Button) { actions.openChat(person) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val fg = if (enabled) seg.onFill else RelunColors.DisabledText
                when {
                    !person.isMatch -> {
                        Icon(Icons.Rounded.Lock, null, tint = fg, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Match to message", color = fg, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    }
                    !person.chatUnlocked -> {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, tint = fg, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Message · ", color = fg, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                        CoinIcon(20.dp)
                        Text(" 15", color = fg, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    }
                    else -> {
                        Icon(Icons.Rounded.ChatBubble, null, tint = fg, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Message", color = fg, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(icon: ImageVector, value: String) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(RelunColors.ChipFill).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = RelunColors.Body, modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
