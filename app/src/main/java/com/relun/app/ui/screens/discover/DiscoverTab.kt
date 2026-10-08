package com.relun.app.ui.screens.discover

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relun.app.data.model.Person
import com.relun.app.data.model.Wallet
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.CircleIconButton
import com.relun.app.ui.components.CoinPill
import com.relun.app.ui.components.EmptyState
import com.relun.app.ui.components.OfflineBanner
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PersonPhoto
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.components.RelunSheet
import com.relun.app.ui.components.SegmentPill
import com.relun.app.ui.components.shimmer
import com.relun.app.ui.navigation.LocalAppActions
import com.relun.app.ui.screens.main.LoadState
import com.relun.app.ui.screens.main.TabBarClearance
import com.relun.app.ui.screens.main.TabSurface
import com.relun.app.ui.screens.main.TabTitle
import com.relun.app.ui.theme.Outfit
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.util.formatDistance
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverTab(wallet: Wallet) {
    val vm = relunViewModel { DiscoverViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current

    TabSurface {
        PullToRefreshBox(
            isRefreshing = s.refreshing,
            onRefresh = { vm.load(refresh = true) },
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            val header: @Composable () -> Unit = {
                Column {
                    TabTitle(
                        "Discover",
                        below = { SegmentPill(text = "${Relun.segment.label} · nearby", small = true) },
                    ) {
                        CoinPill(wallet.balance, low = wallet.balance < wallet.chatUnlockCost, onClick = actions.openCoins)
                        CircleIconButton(Icons.Outlined.Tune, "Filters", { vm.openFilters(true) })
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Within ${s.maxDistanceKm} km · ages ${s.ageMin}–${s.ageMax}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RelunColors.Muted,
                            modifier = Modifier.weight(1f),
                        )
                        ViewToggle(s.view, vm::setView)
                    }
                    if (s.offline) OfflineBanner("You’re offline. Showing saved profiles.", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
                }
            }

            when {
                s.load == LoadState.Loading -> SkeletonGrid(header)
                s.load == LoadState.Empty -> LazyColumn(Modifier.fillMaxSize()) {
                    item { header() }
                    item {
                        EmptyState(
                            Icons.Outlined.Explore,
                            "No profiles found yet",
                            "Try widening your distance or age range to see more people.",
                        ) { PrimaryButton("Adjust filters", { vm.openFilters(true) }, Modifier.width(200.dp), height = 48.dp) }
                    }
                }
                s.load == LoadState.Error -> LazyColumn(Modifier.fillMaxSize()) {
                    item { header() }
                    item {
                        EmptyState(
                            Icons.Outlined.CloudOff,
                            "Couldn’t load people",
                            "Check your connection and try again.",
                            error = true,
                        ) { OutlineButton("Try again", { vm.load() }, Modifier.width(180.dp), leadingIcon = Icons.Rounded.Refresh, height = 48.dp) }
                    }
                }
                s.view == DiscoverView.Grid -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = TabBarClearance),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.padding(horizontal = 0.dp)) { header() } }
                    items(s.people, key = { it.id }) { person ->
                        GridCard(person, onOpen = { actions.openProfile(person.id) }, onLike = { vm.like(person) })
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = TabBarClearance),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { header() }
                    items(s.people, key = { it.id }) { person ->
                        ListCard(
                            person,
                            onOpen = { actions.openProfile(person.id) },
                            onLike = { vm.like(person) },
                            onPass = { vm.pass(person) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }

    if (s.filtersOpen) {
        FiltersSheet(s.maxDistanceKm, s.ageMin, s.ageMax, onApply = vm::applyFilters, onDismiss = { vm.openFilters(false) })
    }
}

@Composable
private fun ViewToggle(view: DiscoverView, onChange: (DiscoverView) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(RelunColors.Track).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(DiscoverView.Grid to Icons.Rounded.GridView, DiscoverView.List to Icons.Rounded.ViewAgenda).forEach { (v, icon) ->
            val on = v == view
            Box(
                Modifier
                    .size(width = 44.dp, height = 34.dp)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(9.dp)) else Modifier)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable(role = Role.Tab) { onChange(v) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, if (v == DiscoverView.Grid) "Grid view" else "List view", tint = if (on) RelunColors.Ink else RelunColors.Muted, modifier = Modifier.size(17.dp))
            }
        }
    }
}

private val cardShadow = Modifier.shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x29461E14), spotColor = Color(0x29461E14))

@Composable
private fun GridCard(person: Person, onOpen: () -> Unit, onLike: () -> Unit) {
    val s = Relun.segment
    Box(
        Modifier
            .aspectRatio(0.75f)
            .then(cardShadow)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
    ) {
        PersonPhoto(person.mainPhotoUrl, person.id, person.initial, Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp))
        if (person.isOnline) {
            Row(
                Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(999.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(start = 5.dp, end = 7.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF3DDC84)))
                Text("Online", color = Color.White, fontSize = 10.sp, fontFamily = Outfit, fontWeight = FontWeight.Medium)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) { Icon(s.icon, s.label, tint = s.text, modifier = Modifier.size(12.dp)) }
        Text(
            listOfNotNull(person.name, person.age?.toString()).joinToString(", "),
            color = Color.White,
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))))
                .padding(start = 9.dp, end = 44.dp, top = 28.dp, bottom = 9.dp),
        )
        LikeButton(person.liked, onLike, Modifier.align(Alignment.BottomEnd), small = true)
    }
}

/** Heart that pops when liked. 44dp touch area around a smaller visible disc. */
@Composable
fun LikeButton(liked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
    val s = Relun.segment
    val pop by animateFloatAsState(if (liked) 1f else 0.9f, spring(dampingRatio = Spring.DampingRatioHighBouncy), label = "heart")
    val disc = if (small) 32.dp else 52.dp
    Box(
        modifier
            .size(if (small) 44.dp else 52.dp)
            .clickable(role = Role.Button, onClickLabel = if (liked) "Unlike" else "Like", onClick = onClick),
        contentAlignment = if (small) Alignment.BottomEnd else Alignment.Center,
    ) {
        Box(
            Modifier
                .padding(if (small) 6.dp else 0.dp)
                .size(disc)
                .shadow(if (small) 3.dp else 0.dp, CircleShape)
                .clip(CircleShape)
                .background(if (liked) s.fill else Color.White.copy(alpha = 0.92f))
                .then(if (!small) Modifier.border(1.5.dp, s.fill, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (liked) "Liked" else "Like",
                tint = if (liked) s.onFill else RelunColors.Ink,
                modifier = Modifier.size(if (small) 17.dp else 24.dp).scale(if (liked) pop else 1f),
            )
        }
    }
}

@Composable
private fun ListCard(person: Person, onOpen: () -> Unit, onLike: () -> Unit, onPass: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x38461E14), spotColor = Color(0x38461E14))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onOpen),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.8f)) {
            PersonPhoto(person.mainPhotoUrl, person.id, person.initial, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), initialSize = 96.sp, failureLabel = "Photo couldn’t load")
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                    .padding(start = 18.dp, end = 18.dp, top = 60.dp, bottom = 16.dp),
            ) {
                Text(
                    listOfNotNull(person.name, person.age?.toString()).joinToString(", "),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    formatDistance(person.distanceKm)?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.LocationOn, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text(it, color = Color.White, fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                    if (person.isOnline) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3DDC84)))
                            Text("Online", color = Color.White, fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentPill(small = true)
                Text(
                    person.bio.ifBlank { "No bio yet" },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = if (person.bio.isBlank()) RelunColors.Faint else RelunColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CircleIconButton(Icons.Rounded.Close, "Pass", onPass, size = 52.dp, iconSize = 24.dp)
            LikeButton(person.liked, onLike)
        }
    }
}

@Composable
private fun SkeletonGrid(header: @Composable () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = TabBarClearance),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(9) {
            Box(Modifier.aspectRatio(0.75f).shimmer()) {
                Box(Modifier.align(Alignment.BottomStart).padding(9.dp).fillMaxWidth(0.55f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFD9D9D9)))
            }
        }
    }
}

@Composable
private fun FiltersSheet(distance: Int, ageMin: Int, ageMax: Int, onApply: (Int, Int, Int) -> Unit, onDismiss: () -> Unit) {
    val s = Relun.segment
    var km by remember { mutableFloatStateOf(distance.toFloat()) }
    var ages by remember { mutableStateOf(ageMin.toFloat()..ageMax.coerceAtMost(70).toFloat()) }
    val colors = SliderDefaults.colors(thumbColor = s.fill, activeTrackColor = s.fill, inactiveTrackColor = RelunColors.Track)

    RelunSheet(onDismiss) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)
        Column {
            Row {
                Text("Maximum distance", style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                Text("${km.roundToInt()} km", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted)
            }
            Slider(value = km, onValueChange = { km = it }, valueRange = 1f..100f, colors = colors)
        }
        Column {
            Row {
                Text("Age range", style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                val top = ages.endInclusive.roundToInt()
                Text("${ages.start.roundToInt()}–${if (top >= 70) "70+" else top}", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted)
            }
            RangeSlider(value = ages, onValueChange = { ages = it }, valueRange = 18f..70f, colors = colors)
        }
        PrimaryButton("Show people", {
            val top = ages.endInclusive.roundToInt()
            onApply(km.roundToInt(), ages.start.roundToInt(), if (top >= 70) 99 else top)
        })
    }
}
