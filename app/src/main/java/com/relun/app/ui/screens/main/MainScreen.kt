package com.relun.app.ui.screens.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.data.model.Wallet
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.navigation.MainUiState
import com.relun.app.ui.navigation.MainViewModel
import com.relun.app.ui.navigation.Tab
import com.relun.app.ui.screens.dates.DatesTab
import com.relun.app.ui.screens.discover.DiscoverTab
import com.relun.app.ui.screens.me.MeTab
import com.relun.app.ui.screens.messages.MessagesTab
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors

/** Four tabs under a floating tab bar. Tab content stays put while pushed screens come and go. */
@Composable
fun MainScreen(shell: MainViewModel, state: MainUiState, wallet: Wallet) {
    val container = appContainer()
    val unread by container.chat.unreadTotal.collectAsStateWithLifecycle()

    // Ask for notification permission once the user is in (Android 13+).
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) container.push.register()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Box(Modifier.fillMaxSize().background(RelunColors.Background)) {
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "tab",
        ) { tab ->
            when (tab) {
                Tab.Discover -> DiscoverTab(wallet = wallet)
                Tab.Dates -> DatesTab(showMine = state.showMyDates)
                Tab.Messages -> MessagesTab(showLikes = state.showLikes)
                Tab.Me -> MeTab(wallet = wallet)
            }
        }
        TabBar(
            selected = state.tab,
            unread = unread,
            onSelect = shell::selectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private data class TabSpec(val tab: Tab, val label: String, val on: ImageVector, val off: ImageVector)

private val TABS = listOf(
    TabSpec(Tab.Discover, "Discover", Icons.Rounded.Explore, Icons.Outlined.Explore),
    TabSpec(Tab.Dates, "Dates", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    TabSpec(Tab.Messages, "Messages", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    TabSpec(Tab.Me, "Profile", Icons.Rounded.Person, Icons.Outlined.Person),
)

@Composable
private fun TabBar(selected: Tab, unread: Int, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val s = Relun.segment
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .height(68.dp)
            .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x38461E14), spotColor = Color(0x38461E14))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, Color(0x0D461E14), RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TABS.forEach { spec ->
            val on = spec.tab == selected
            Column(
                Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                    ) { onSelect(spec.tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 54.dp, height = 30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (on) s.tint else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (on) spec.on else spec.off,
                        contentDescription = null,
                        tint = if (on) s.text else RelunColors.Muted,
                        modifier = Modifier.size(22.dp),
                    )
                    if (spec.tab == Tab.Messages && unread > 0) {
                        Text(
                            unread.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = (-4).dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(RelunColors.Error)
                                .border(2.dp, Color.White, RoundedCornerShape(9.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    spec.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (on) s.text else RelunColors.Muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** Space reserved under scrolling tab content so the floating bar never hides the last item. */
val TabBarClearance = 120.dp
