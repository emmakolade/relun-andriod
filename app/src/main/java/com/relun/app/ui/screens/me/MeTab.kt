package com.relun.app.ui.screens.me

import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.WorkspacePremium
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.relun.app.BuildConfig
import com.relun.app.data.model.Photo
import com.relun.app.data.model.Wallet
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.di.AppContainer
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.CircleIconButton
import com.relun.app.ui.components.CoinIcon
import com.relun.app.ui.components.ConfirmDialog
import com.relun.app.ui.components.DialogSpec
import com.relun.app.ui.components.LinkButton
import com.relun.app.ui.components.ListRow
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PersonPhoto
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.components.RelunSheet
import com.relun.app.ui.components.RowGroup
import com.relun.app.ui.components.SectionHeader
import com.relun.app.ui.components.SegmentPill
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.screens.main.TabBarClearance
import com.relun.app.ui.screens.main.TabSurface
import com.relun.app.ui.screens.main.TabTitle
import com.relun.app.ui.screens.onboarding.PhotoSlot
import com.relun.app.ui.screens.onboarding.PhotoSlotView
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.compressForUpload
import com.relun.app.util.formatCoins
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeState(
    val likesCount: Int = 0,
    val viewsCount: Int = 0,
    val uploading: Set<Int> = emptySet(),
    val dialog: DialogSpec? = null,
)

class MeViewModel(private val c: AppContainer) : ViewModel() {
    val me = c.profile.me

    private val _state = MutableStateFlow(MeState())
    val state: StateFlow<MeState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { c.people.events.collect { if (it == PeopleEvent.InsightsUnlocked) refresh() } }
    }

    fun refresh() {
        viewModelScope.launch {
            launch { c.profile.refresh() }
            val likes = async { c.people.receivedLikes() }
            val views = async { c.people.profileViews() }
            _state.update {
                it.copy(
                    likesCount = likes.await().getOrNull()?.count ?: it.likesCount,
                    viewsCount = views.await().getOrNull()?.count ?: it.viewsCount,
                )
            }
        }
    }

    fun addPhoto(context: Context, slot: Int, uri: Uri) = upload(context, slot, uri, replace = null)
    fun replacePhoto(context: Context, slot: Int, photo: Photo, uri: Uri) = upload(context, slot, uri, replace = photo)

    private fun upload(context: Context, slot: Int, uri: Uri, replace: Photo?) {
        _state.update { it.copy(uploading = it.uploading + slot) }
        viewModelScope.launch {
            val bytes = runCatching { compressForUpload(context, uri) }.getOrNull()
            if (bytes == null) {
                c.messenger.error("We couldn’t read that photo. Try another one.")
            } else {
                val result = if (replace != null) c.profile.replacePhoto(replace.id, bytes) else c.profile.uploadPhoto(bytes)
                result.onSuccess { c.messenger.success("Photo saved") }.onFailure { c.messenger.error(it.message ?: "Upload failed.") }
            }
            _state.update { it.copy(uploading = it.uploading - slot) }
        }
    }

    fun deletePhoto(photo: Photo) {
        if ((me.value?.photos?.size ?: 0) <= 2) {
            c.messenger.warning("Keep at least 2 photos on your profile.")
            return
        }
        viewModelScope.launch {
            c.profile.deletePhoto(photo.id).onFailure { c.messenger.error(it.message ?: "Couldn’t remove that photo.") }
        }
    }

    fun soon(text: String = "Coming soon") = c.messenger.info(text)

    fun confirmLogout() = _state.update {
        it.copy(dialog = DialogSpec("Log out?", "You can sign back in any time with a one-time code.", "Log out") {
            viewModelScope.launch { c.auth.signOut() }
        })
    }

    fun confirmDelete() = _state.update {
        it.copy(dialog = DialogSpec(
            "Delete your account?",
            "Your profile, matches and messages will be permanently removed. Unused coins can’t be refunded.",
            "Delete account",
            destructive = true,
        ) {
            viewModelScope.launch {
                c.auth.deleteAccount()
                    .onSuccess { c.messenger.info("Your account has been deleted.") }
                    .onFailure { e -> c.messenger.error(e.message ?: "Couldn’t delete your account.") }
            }
        })
    }

    fun dismissDialog() = _state.update { it.copy(dialog = null) }
}

@Composable
fun MeTab(wallet: Wallet) {
    val vm = relunViewModel { MeViewModel(it) }
    val me by vm.me.collectAsStateWithLifecycle()
    val s by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val seg = Relun.segment
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    var pickSlot by remember { mutableIntStateOf(-1) }
    var replacing by remember { mutableStateOf<Photo?>(null) }
    var photoMenu by remember { mutableStateOf<Pair<Int, Photo>?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && pickSlot >= 0) {
            val r = replacing
            if (r != null) vm.replacePhoto(context, pickSlot, r, uri) else vm.addPhoto(context, pickSlot, uri)
        }
        replacing = null
    }
    val pick = { slot: Int, replace: Photo? ->
        pickSlot = slot
        replacing = replace
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    TabSurface {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = TabBarClearance),
        ) {
            TabTitle("Profile") { CircleIconButton(Icons.Outlined.Settings, "Settings", actions.openSettings) }

            // Avatar with completeness ring.
            val pct = me?.completeness ?: 0
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 5.dp.toPx()
                        drawCircle(Color(0xFFECECEC), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
                        drawArc(seg.fill, -90f, pct * 3.6f, false, topLeft = Offset(stroke / 2, stroke / 2),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    PersonPhoto(me?.photos?.firstOrNull()?.url, "me", me?.initial ?: "", Modifier.size(114.dp), shape = CircleShape, initialSize = 44.sp, failureLabel = null)
                    Text(
                        "$pct% complete",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 8.dp).clip(RoundedCornerShape(999.dp)).background(RelunColors.Ink).padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Text(
                    listOfNotNull(me?.name?.ifBlank { null }, me?.age?.toString()).joinToString(", "),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
                SegmentPill(text = seg.label)
                Text(
                    me?.bio?.ifBlank { null } ?: "Add a short bio so people know what you’re about.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (me?.bio.isNullOrBlank()) RelunColors.Faint else RelunColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp),
                )
                OutlineButton("Edit profile", actions.openEditProfile, Modifier.widthIn(max = 180.dp), leadingIcon = Icons.Outlined.Create, height = 44.dp)
            }

            // Relun Plus
            val plus = wallet.plus
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (plus != null) seg.tint else seg.fill)
                    .clickable(onClick = actions.openPlus)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val ink = if (plus != null) seg.text else seg.onFill
                Icon(Icons.Rounded.WorkspacePremium, null, tint = ink, modifier = Modifier.size(30.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (plus != null) "Relun Plus · ${if (plus.plan == "weekly") "Weekly" else "Monthly"}" else "Get Relun Plus",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (plus != null) RelunColors.Ink else seg.onFill,
                    )
                    Text(
                        if (plus != null) {
                            val date = plus.until.atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
                            if (plus.autoRenew) "Renews $date" else "Ends $date"
                        } else {
                            "Unlimited likes, see who likes you, free message requests"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (plus != null) RelunColors.Body else seg.onFill,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = ink, modifier = Modifier.size(22.dp))
            }

            // Insights
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x29461E14), spotColor = Color(0x29461E14))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(Icons.Outlined.FavoriteBorder, "Likes", s.likesCount, wallet.insightsActive, Modifier.weight(1f)) {
                        if (wallet.insightsActive) actions.openLikes() else actions.openInsights()
                    }
                    Stat(Icons.Outlined.Visibility, "Views", s.viewsCount, wallet.insightsActive, Modifier.weight(1f)) {
                        if (wallet.insightsActive) actions.openViews() else actions.openInsights()
                    }
                }
                if (!wallet.insightsActive) {
                    PrimaryButton(
                        "Unlock Likes & Views",
                        actions.openInsights,
                        leadingIcon = Icons.Rounded.LockOpen,
                        height = 48.dp,
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = RelunColors.SuccessText, modifier = Modifier.size(16.dp))
                        Text("  Insights active", color = RelunColors.SuccessText, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp))
                    }
                }
            }

            // Photos
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("My photos", Modifier.padding(horizontal = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val photos = me?.photos.orEmpty()
                    repeat(3) { i ->
                        val photo = photos.getOrNull(i)
                        val slot = when {
                            i in s.uploading -> PhotoSlot.Uploading(Uri.EMPTY)
                            photo != null -> PhotoSlot.Done(photo)
                            else -> PhotoSlot.Empty
                        }
                        Box(Modifier.weight(1f).clickable(enabled = photo != null) { photo?.let { photoMenu = i to it } }) {
                            PhotoSlotView(
                                slot = slot,
                                main = i == 0,
                                onAdd = { pick(i, null) },
                                onRemove = { photo?.let { photoMenu = i to it } },
                            )
                        }
                    }
                }
            }

            // Menu
            RowGroup(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                ListRow("Get Coins", actions.openCoins, leading = { CoinIcon(24.dp) }, value = formatCoins(wallet.balance))
                ListRow("Edit Profile", actions.openEditProfile, leading = { RowIcon(Icons.Outlined.Create) })
                ListRow("Verification", { vm.soon("Photo verification is coming soon") }, leading = { RowIcon(Icons.Outlined.VerifiedUser) }, showChevron = false) {
                    Text("Soon", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Body,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFFF1F1F1)).padding(horizontal = 8.dp, vertical = 2.dp))
                }
                ListRow("Settings", actions.openSettings, leading = { RowIcon(Icons.Outlined.Settings) })
                ListRow("Help & Support", { vm.soon() }, leading = { RowIcon(Icons.AutoMirrored.Outlined.HelpOutline) })
                ListRow("Privacy Policy", { vm.soon() }, leading = { RowIcon(Icons.Outlined.Description) })
                ListRow("About", { vm.soon("Relun ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})") }, leading = { RowIcon(Icons.Outlined.Info) }, divider = false)
            }

            Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LinkButton("Log out", vm::confirmLogout, Modifier.fillMaxWidth().height(50.dp), style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                LinkButton("Delete account", vm::confirmDelete, Modifier.fillMaxWidth().height(50.dp), color = RelunColors.Error, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                Text("Relun ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    photoMenu?.let { (slot, photo) ->
        RelunSheet({ photoMenu = null }) {
            Text("Photo", style = MaterialTheme.typography.titleLarge)
            OutlineButton("Replace photo", { photoMenu = null; pick(slot, photo) })
            OutlineButton("Remove photo", { photoMenu = null; vm.deletePhoto(photo) }, textColor = RelunColors.Error)
        }
    }
    s.dialog?.let { ConfirmDialog(it, vm::dismissDialog) }
}

@Composable
private fun RowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) =
    Icon(icon, null, tint = RelunColors.Ink, modifier = Modifier.size(22.dp))

@Composable
private fun Stat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
    open: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF7F7F7)).clickable(onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = RelunColors.Muted, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = RelunColors.Muted)
        }
        // Without insights the number is shown, but who is behind it is not.
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp))
        if (!open) Text("Tap to see who", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = RelunColors.Faint)
    }
}
