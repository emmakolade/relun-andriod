package com.relun.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.BuildConfig
import com.relun.app.data.model.BlockedUserDto
import com.relun.app.data.model.SettingsDto
import com.relun.app.data.model.SettingsPatch
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.components.Avatar
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.ConfirmDialog
import com.relun.app.ui.components.DialogSpec
import com.relun.app.ui.components.EmptyState
import com.relun.app.ui.components.LinkButton
import com.relun.app.ui.components.ListRow
import com.relun.app.ui.components.RowGroup
import com.relun.app.ui.components.SectionHeader
import com.relun.app.ui.components.SegmentPill
import com.relun.app.ui.components.ToggleRow
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.screens.onboarding.openAppSettings
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatCoins
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val c = appContainer()
    val actions = LocalAppActions.current
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val wallet by c.coins.wallet.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<DialogSpec?>(null) }
    var blockedCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        c.settings.refresh()
        c.safety.blocked().onSuccess { blockedCount = it.size }
    }

    fun patch(patch: SettingsPatch, optimistic: SettingsDto) = scope.launch {
        c.settings.update(patch, optimistic).onFailure { c.messenger.error(it.message ?: "Couldn’t save that setting.") }
    }

    Column(Modifier.fillMaxSize().background(RelunColors.Background).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(actions.back)
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 40.dp, top = 10.dp),
        ) {
            Section("Discovery") {
                val top = if (settings.ageMax >= 99) "70+" else settings.ageMax.toString()
                ListRow("Age range", { c.messenger.info("Change this from the filters button on Discover") }, value = "${settings.ageMin}–$top")
                ListRow("Maximum distance", { c.messenger.info("Change this from the filters button on Discover") }, value = "${settings.maxDistanceKm} km")
                ListRow("Looking for", null, divider = false, showChevron = false) {
                    Icon(Icons.Rounded.Lock, null, tint = RelunColors.Muted, modifier = Modifier.size(14.dp))
                    SegmentPill(text = Relun.segment.label, small = true)
                }
            }
            Section("Privacy") {
                ToggleRow("Visible in Discover", settings.isVisible, subtitle = "Turn off to pause your profile", onToggle = {
                    patch(SettingsPatch(isVisible = !settings.isVisible), settings.copy(isVisible = !settings.isVisible))
                })
                ToggleRow("Show my age", settings.showAge, onToggle = {
                    patch(SettingsPatch(showAge = !settings.showAge), settings.copy(showAge = !settings.showAge))
                })
                ToggleRow("Show my distance", settings.showDistance, onToggle = {
                    patch(SettingsPatch(showDistance = !settings.showDistance), settings.copy(showDistance = !settings.showDistance))
                })
                ListRow("Blocked users", actions.openBlocked, value = blockedCount?.let { "$it blocked" }, divider = false)
            }
            Section("Preferences") {
                ToggleRow("Push notifications", settings.notificationsEnabled, onToggle = {
                    patch(SettingsPatch(notificationsEnabled = !settings.notificationsEnabled), settings.copy(notificationsEnabled = !settings.notificationsEnabled))
                })
                ListRow("Location services", { openAppSettings(context) }, value = if (c.location.hasPermission()) "On" else "Off", divider = false)
            }
            Section("Account") {
                ListRow("Coins & purchases", actions.openCoins, value = formatCoins(wallet.balance))
                ListRow("Help", { c.messenger.info("Coming soon") })
                ListRow("Terms of Service", { c.messenger.info("Coming soon") })
                ListRow("Log out", {
                    dialog = DialogSpec("Log out?", "You can sign back in any time with a one-time code.", "Log out") {
                        scope.launch { c.auth.signOut() }
                    }
                }, showChevron = false)
                ListRow(
                    "Delete account",
                    {
                        dialog = DialogSpec(
                            "Delete your account?",
                            "Your profile, matches and messages will be permanently removed. Unused coins can’t be refunded.",
                            "Delete account",
                            destructive = true,
                        ) {
                            scope.launch {
                                c.auth.deleteAccount()
                                    .onSuccess { c.messenger.info("Your account has been deleted.") }
                                    .onFailure { c.messenger.error(it.message ?: "Couldn’t delete your account.") }
                            }
                        }
                    },
                    leading = { Icon(Icons.Outlined.DeleteOutline, null, tint = RelunColors.Error, modifier = Modifier.size(20.dp)) },
                    titleColor = RelunColors.Error,
                    showChevron = false,
                    divider = false,
                )
            }
            Text(
                "Relun ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = RelunColors.Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
    dialog?.let { ConfirmDialog(it) { dialog = null } }
}

@Composable
private fun Section(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    SectionHeader(title, Modifier.padding(start = 20.dp, bottom = 8.dp))
    RowGroup(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp), content = content)
}

@Composable
fun BlockedUsersScreen() {
    val c = appContainer()
    val actions = LocalAppActions.current
    val scope = rememberCoroutineScope()
    var blocked by remember { mutableStateOf<List<BlockedUserDto>?>(null) }

    LaunchedEffect(Unit) {
        c.safety.blocked()
            .onSuccess { blocked = it }
            .onFailure { blocked = emptyList(); c.messenger.error(it.message ?: "Couldn’t load blocked users.") }
    }

    Column(Modifier.fillMaxSize().background(RelunColors.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(actions.back)
            Text("Blocked users", style = MaterialTheme.typography.headlineSmall)
        }
        val list = blocked
        if (list != null && list.isEmpty()) {
            EmptyState(Icons.Outlined.Block, "No one is blocked", "People you block can’t see your profile or message you.")
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(list.orEmpty(), key = { it.userId }) { user ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Avatar(user.photoUrl, user.userId, user.fullName.take(1).uppercase(), 48.dp)
                    Text(user.fullName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    LinkButton("Unblock", {
                        scope.launch {
                            c.safety.unblock(user.userId)
                                .onSuccess {
                                    blocked = blocked?.filterNot { it.userId == user.userId }
                                    c.messenger.info("${user.fullName.substringBefore(' ')} is unblocked")
                                }
                                .onFailure { c.messenger.error(it.message ?: "Couldn’t unblock.") }
                        }
                    }, color = Relun.segment.text)
                }
            }
        }
    }
}
