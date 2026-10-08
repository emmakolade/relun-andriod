package com.relun.app.ui.screens.dates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.data.model.DatePost
import com.relun.app.data.model.DateRequestStatus
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.Avatar
import com.relun.app.ui.components.CircleIconButton
import com.relun.app.ui.components.ConfirmDialog
import com.relun.app.ui.components.EmptyState
import com.relun.app.ui.components.LabeledField
import com.relun.app.ui.components.OfflineBanner
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.components.RelunSheet
import com.relun.app.ui.components.RelunTextField
import com.relun.app.ui.components.SegmentPill
import com.relun.app.ui.components.SegmentedControl
import com.relun.app.ui.components.shimmer
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.screens.main.LoadState
import com.relun.app.ui.screens.main.TabBarClearance
import com.relun.app.ui.screens.main.TabSurface
import com.relun.app.ui.screens.main.TabTitle
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatAgo
import com.relun.app.util.formatWhen
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ACTIVITIES = listOf("Coffee", "Dinner", "Drinks", "Movie", "Walk")

private val cardModifier = Modifier
    .fillMaxWidth()
    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x29461E14), spotColor = Color(0x29461E14))
    .clip(RoundedCornerShape(24.dp))
    .background(Color.White)
    .padding(16.dp)

@Composable
fun DatesTab(showMine: Int) {
    val vm = relunViewModel { DatesViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val seg = Relun.segment

    LaunchedEffect(showMine) { if (showMine > 0) vm.setView(DatesView.Mine) }

    TabSurface {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = TabBarClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TabTitle("Date Requests", modifier = Modifier.padding(bottom = 0.dp)) {
                    Row(
                        Modifier.height(44.dp).clip(RoundedCornerShape(999.dp)).background(seg.fill).clickable { vm.openCreate(true) }.padding(start = 12.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = seg.onFill, modifier = Modifier.size(20.dp))
                        Text("Post", color = seg.onFill, style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp))
                    }
                }
            }
            item {
                SegmentedControl(
                    options = listOf(DatesView.Browse to "Browse", DatesView.Mine to "My Dates"),
                    selected = s.view,
                    onSelect = vm::setView,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (s.offline) item { OfflineBanner("You’re offline. Showing saved dates.", Modifier.padding(horizontal = 16.dp)) }

            if (s.view == DatesView.Browse) {
                when (s.browseLoad) {
                    LoadState.Loading -> items(2) { Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(220.dp).shimmer(RoundedCornerShape(24.dp))) }
                    LoadState.Empty -> item {
                        EmptyState(Icons.Outlined.CalendarMonth, "No dates found.", "Be the first: post a plan and let people ask to join.") {
                            PrimaryButton("Post a Date", { vm.openCreate(true) }, Modifier.width(200.dp), height = 48.dp)
                        }
                    }
                    LoadState.Error -> item {
                        EmptyState(Icons.Outlined.ErrorOutline, "Couldn’t load dates", error = true) {
                            OutlineButton("Try again", { vm.loadBrowse() }, Modifier.width(180.dp), leadingIcon = Icons.Rounded.Refresh, height = 48.dp)
                        }
                    }
                    LoadState.Ready -> items(s.browse, key = { it.id }) { post ->
                        BrowseCard(post, onOwner = { post.owner?.let { actions.openProfile(it.id) } }, onJoin = { vm.interested(post) }, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            } else {
                if (s.mine.isEmpty() && s.mineLoaded) {
                    item {
                        EmptyState(Icons.Outlined.CalendarMonth, "You haven’t posted any dates yet.") {
                            PrimaryButton("Post a Date", { vm.openCreate(true) }, Modifier.width(200.dp), height = 48.dp)
                        }
                    }
                }
                items(s.mine, key = { it.id }) { post ->
                    MineCard(post, vm, Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (s.creating) CreateDateSheet(posting = s.posting, onPost = vm::post, onDismiss = { vm.openCreate(false) })
    s.dialog?.let { ConfirmDialog(it, vm::dismissDialog) }
}

@Composable
private fun Meta(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = RelunColors.Body, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
    }
}

@Composable
private fun BrowseCard(post: DatePost, onOwner: () -> Unit, onJoin: () -> Unit, modifier: Modifier) {
    val owner = post.owner ?: return
    Column(modifier.then(cardModifier), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.clickable(onClick = onOwner), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(owner.mainPhotoUrl, owner.id, owner.initial, 44.dp)
            Column(Modifier.weight(1f)) {
                Text(listOfNotNull(owner.name, owner.age?.toString()).joinToString(", "), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatAgo(post.createdAt), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted)
            }
            SegmentPill(text = Relun.segment.label, small = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(post.activity, style = MaterialTheme.typography.titleMedium)
            Meta(Icons.Outlined.LocationOn, post.place)
            Meta(Icons.Outlined.CalendarMonth, formatWhen(post.scheduledFor))
            post.description?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Muted, modifier = Modifier.padding(top = 2.dp)) }
        }
        when (post.myRequestStatus) {
            null -> PrimaryButton("I’m Interested", onJoin, height = 48.dp)
            DateRequestStatus.Pending -> StatusBar("Interest sent · waiting for reply")
            DateRequestStatus.Accepted -> StatusBar("Accepted · check Messages")
            DateRequestStatus.Declined -> Text(
                "Not this time",
                style = MaterialTheme.typography.labelMedium,
                color = RelunColors.Muted,
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F1F1)).padding(top = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusBar(text: String) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(RelunColors.SuccessFill),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = RelunColors.SuccessText, modifier = Modifier.size(18.dp))
        Text("  $text", color = RelunColors.SuccessText, style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp))
    }
}

@Composable
private fun MineCard(post: DatePost, vm: DatesViewModel, modifier: Modifier) {
    val actions = LocalAppActions.current
    val seg = Relun.segment
    Column(modifier.then(cardModifier), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(post.activity, style = MaterialTheme.typography.titleMedium)
                Meta(Icons.Outlined.LocationOn, post.place)
                Meta(Icons.Outlined.CalendarMonth, formatWhen(post.scheduledFor))
            }
            CircleIconButton(Icons.Outlined.DeleteOutline, "Delete date", { vm.confirmDelete(post) }, style = com.relun.app.ui.components.CircleStyle.Flat, modifier = Modifier.background(Color(0xFFF4F4F4), CircleShape), iconSize = 18.dp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(RelunColors.BorderSoft))
        Text("Requests (${post.requests.size})", style = MaterialTheme.typography.labelMedium)
        if (post.requests.isEmpty()) {
            Text("No requests yet. We’ll notify you when someone asks to join.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Muted)
        }
        post.requests.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.weight(1f).clickable { actions.openProfile(r.person.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(r.person.mainPhotoUrl, r.person.id, r.person.initial, 40.dp)
                    Column {
                        Text(listOfNotNull(r.person.firstName, r.person.age?.toString()).joinToString(", "), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp))
                        when (r.status) {
                            DateRequestStatus.Pending -> Text("Wants to join", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted)
                            DateRequestStatus.Accepted -> Text("Accepted", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = RelunColors.SuccessText)
                            DateRequestStatus.Declined -> Text("Declined", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted)
                        }
                    }
                }
                when (r.status) {
                    DateRequestStatus.Pending -> {
                        CircleIconButton(Icons.Rounded.Close, "Decline", { vm.respond(post, r.id, accept = false) }, modifier = Modifier.border(1.5.dp, RelunColors.Border, CircleShape), style = com.relun.app.ui.components.CircleStyle.Flat)
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(seg.fill).clickable { vm.respond(post, r.id, accept = true) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Check, "Accept", tint = seg.onFill, modifier = Modifier.size(20.dp)) }
                    }
                    DateRequestStatus.Accepted -> Row(
                        Modifier.height(40.dp).clip(RoundedCornerShape(999.dp)).background(seg.tint).border(1.5.dp, seg.fill, RoundedCornerShape(999.dp))
                            .clickable { actions.openChat(r.person.copy(isMatch = true, chatUnlocked = true)) }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.ChatBubble, null, tint = seg.text, modifier = Modifier.size(15.dp))
                        Text("Chat", color = seg.text, style = MaterialTheme.typography.labelMedium)
                    }
                    DateRequestStatus.Declined -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateDateSheet(posting: Boolean, onPost: (String, String, Instant, String?) -> Unit, onDismiss: () -> Unit) {
    val seg = Relun.segment
    var activity by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var desc by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    val at = if (date != null && time != null) LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant() else null
    val inPast = at != null && at.isBefore(Instant.now())
    val valid = activity.isNotBlank() && place.isNotBlank() && at != null && !inPast

    RelunSheet(onDismiss) {
        Text("Post a Date", style = MaterialTheme.typography.headlineSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ACTIVITIES.forEach { a ->
                val on = activity == a
                Text(
                    a,
                    color = if (on) seg.text else RelunColors.Ink,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) seg.tint else Color.White)
                        .border(1.5.dp, if (on) seg.fill else RelunColors.Border, RoundedCornerShape(999.dp))
                        .clickable { activity = a }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        LabeledField("Activity") {
            RelunTextField(activity, { activity = it.take(80) }, Modifier.fillMaxWidth(), placeholder = "e.g. Sunset drinks",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        }
        LabeledField("Place") {
            RelunTextField(place, { place = it.take(120) }, Modifier.fillMaxWidth(), placeholder = "A public spot",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("Date", Modifier.weight(1.3f)) {
                PickerField(date?.format(DateTimeFormatter.ofPattern("EEE d MMM")) ?: "Pick a day") { pickDate = true }
            }
            LabeledField("Time", Modifier.weight(1f), error = if (inPast) "In the past" else null) {
                PickerField(time?.format(DateTimeFormatter.ofPattern("H:mm")) ?: "Pick a time") { pickTime = true }
            }
        }
        LabeledField("Description", trailingLabel = "Optional") {
            RelunTextField(desc, { desc = it.take(500) }, Modifier.fillMaxWidth(), placeholder = "Anything they should know?", singleLine = false, minLines = 2,
                textStyle = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton("Post Request", { at?.let { onPost(activity, place, it, desc) } }, enabled = valid, loading = posting)
    }

    if (pickDate) {
        val today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("OK", color = seg.text) }
            },
            dismissButton = { TextButton({ pickDate = false }) { Text("Cancel", color = RelunColors.Ink) } },
            colors = DatePickerDefaults.colors(containerColor = Color.White),
        ) {
            DatePicker(state, colors = DatePickerDefaults.colors(containerColor = Color.White, selectedDayContainerColor = seg.fill, selectedDayContentColor = seg.onFill, todayDateBorderColor = seg.fill, todayContentColor = seg.text))
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = time?.hour ?: 19, initialMinute = time?.minute ?: 0, is24Hour = true)
        Dialog(onDismissRequest = { pickTime = false }) {
            Column(
                Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Pick a time", style = MaterialTheme.typography.titleMedium)
                TimeInput(state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ pickTime = false }) { Text("Cancel", color = RelunColors.Ink) }
                    TextButton({ time = LocalTime.of(state.hour, state.minute); pickTime = false }) { Text("OK", color = seg.text) }
                }
            }
        }
    }
}

@Composable
private fun PickerField(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, RelunColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1)
    }
}
