package com.relun.app.ui.screens.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import com.relun.app.data.repository.LockedPeople
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.EmptyState
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PersonPhoto
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.components.shimmer
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatAgo

/** Who viewed my profile, newest first. Opened from the Views card on the Me tab. */
@Composable
fun ProfileViewsScreen() {
    val c = appContainer()
    val actions = LocalAppActions.current
    val wallet by c.coins.wallet.collectAsState()
    var views by remember { mutableStateOf<LockedPeople?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        failed = false
        c.people.profileViews()
            .onSuccess { views = it }
            .onFailure { failed = views == null }
    }
    // Buying insights from the locked state reloads the list.
    LaunchedEffect(Unit) { c.people.events.collect { if (it == PeopleEvent.InsightsUnlocked) attempt++ } }

    Column(Modifier.fillMaxSize().background(RelunColors.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(actions.back)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Profile views", style = MaterialTheme.typography.headlineSmall)
                views?.takeIf { !it.locked && it.count > 0 }?.let {
                    Text(
                        if (it.count == 1) "1 person viewed your profile" else "${it.count} people viewed your profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = RelunColors.Muted,
                    )
                }
            }
        }

        val v = views
        when {
            failed -> EmptyState(Icons.Outlined.ErrorOutline, "Couldn’t load your views", error = true) {
                OutlineButton("Try again", { attempt++ }, Modifier.widthIn(max = 180.dp), leadingIcon = Icons.Rounded.Refresh, height = 48.dp)
            }
            v == null -> LazyVerticalGrid(
                GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(6) { Box(Modifier.fillMaxWidth().aspectRatio(0.75f).shimmer(RoundedCornerShape(20.dp))) }
            }
            v.locked -> EmptyState(
                Icons.Rounded.Lock,
                "See who viewed you",
                if (v.count == 1) "1 person viewed your profile. Unlock to see who." else "${v.count} people viewed your profile. Unlock to see who.",
            ) {
                PrimaryButton("Unlock for ${wallet.insightsCost} coins", actions.openInsights, Modifier.widthIn(max = 240.dp), height = 48.dp)
            }
            v.people.isEmpty() -> EmptyState(
                Icons.Outlined.Visibility,
                "No views yet",
                "When someone views your profile, they’ll show up here.",
            )
            else -> LazyVerticalGrid(
                GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(v.people, key = { it.id }) { person ->
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(20.dp))
                            .clickable { actions.openProfile(person.id) },
                    ) {
                        PersonPhoto(person.mainPhotoUrl, person.id, person.initial, Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp), initialSize = 56.sp)
                        Column(
                            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                                .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp),
                        ) {
                            Text(
                                listOfNotNull(person.firstName, person.age?.toString()).joinToString(", "),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                            )
                            v.viewedAt[person.id]?.let { at ->
                                Text(
                                    "Viewed ${formatAgo(at)}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
