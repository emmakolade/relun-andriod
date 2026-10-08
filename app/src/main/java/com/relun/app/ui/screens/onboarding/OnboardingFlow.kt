package com.relun.app.ui.screens.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.relun.app.data.model.Segment
import com.relun.app.data.repository.OnboardingStart
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.BackButton
import com.relun.app.ui.components.ErrorLine
import com.relun.app.ui.components.IconTile
import com.relun.app.ui.components.InfoNote
import com.relun.app.ui.components.LabeledField
import com.relun.app.ui.components.LinkButton
import com.relun.app.ui.components.OutlineButton
import com.relun.app.ui.components.PersonPhoto
import com.relun.app.ui.components.PrimaryButton
import com.relun.app.ui.components.RelunTextField
import com.relun.app.ui.navigation.DetailsRoute
import com.relun.app.ui.navigation.LocationRoute
import com.relun.app.ui.navigation.PhotosRoute
import com.relun.app.ui.navigation.SegmentRoute
import com.relun.app.ui.navigation.YoureInRoute
import com.relun.app.ui.navigation.popIn
import com.relun.app.ui.navigation.popOut
import com.relun.app.ui.navigation.pushIn
import com.relun.app.ui.navigation.pushOut
import com.relun.app.ui.theme.Relun
import com.relun.app.ui.theme.RelunColors
import com.relun.app.ui.theme.SegmentColors

@Composable
fun OnboardingFlow(start: OnboardingStart) {
    val nav = rememberNavController()
    val vm = relunViewModel(key = "onboarding") { OnboardingViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val startRoute: Any = if (start == OnboardingStart.Photos) PhotosRoute else SegmentRoute

    NavHost(
        navController = nav,
        startDestination = startRoute,
        enterTransition = pushIn,
        exitTransition = pushOut,
        popEnterTransition = popIn,
        popExitTransition = popOut,
    ) {
        composable<SegmentRoute> {
            StepScaffold(nav, step = 1) { SegmentStep(state, vm) { nav.navigate(DetailsRoute) } }
        }
        composable<DetailsRoute> {
            StepScaffold(nav, step = 2, scrolls = true) { DetailsStep(state, vm) { nav.navigate(LocationRoute) } }
        }
        composable<LocationRoute> {
            StepScaffold(nav, step = 3) { LocationStep(state, vm) { nav.navigate(PhotosRoute) } }
        }
        composable<PhotosRoute> {
            StepScaffold(nav, step = 4) { PhotosStep(state, vm) { nav.navigate(YoureInRoute) } }
        }
        composable<YoureInRoute> { YoureInStep(state, vm) }
    }
}

/** Back button, 4-segment progress bar and "Step n of 4" above each setup step. */
@Composable
private fun StepScaffold(
    nav: NavHostController,
    step: Int,
    scrolls: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val canGoBack = nav.previousBackStackEntry != null
    val s = Relun.segment
    Column(
        Modifier
            .fillMaxSize()
            .background(RelunColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (canGoBack) BackButton({ nav.popBackStack() }) else Spacer(Modifier.size(0.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i < step) s.fill else RelunColors.Border)
                        )
                    }
                }
                Text("Step $step of 4", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = RelunColors.Muted)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scrolls) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

// ---------- Step 1: segment ----------

@Composable
private fun ColumnScope.SegmentStep(state: OnboardingState, vm: OnboardingViewModel, onNext: () -> Unit) {
    val s = Relun.segment
    Text("What are you looking for?", style = MaterialTheme.typography.headlineLarge)
    Text(
        "Choose your relationship intention. You’ll only see people with the same goal.",
        style = MaterialTheme.typography.bodyMedium,
        color = RelunColors.Muted,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    listOf(Segment.Relationship, Segment.Fun).forEach { seg ->
        val g = SegmentColors.of(seg)
        val on = state.segment == seg
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (on) g.tint else Color.White)
                .border(2.dp, if (on) g.fill else g.wash, RoundedCornerShape(24.dp))
                .clickable(role = Role.RadioButton) { vm.chooseSegment(seg) }
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(if (on) g.fill else g.tint),
                contentAlignment = Alignment.Center,
            ) { Icon(g.icon, null, tint = if (on) g.onFill else g.text, modifier = Modifier.size(28.dp)) }
            Column(Modifier.weight(1f)) {
                Text(g.label, style = MaterialTheme.typography.titleMedium)
                Text(g.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Muted, modifier = Modifier.padding(top = 3.dp))
            }
            if (on) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(g.fill), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = g.onFill, modifier = Modifier.size(17.dp))
                }
            } else {
                Box(Modifier.size(26.dp).border(2.dp, g.fill.copy(alpha = 0.35f), CircleShape))
            }
        }
    }
    InfoNote(
        "This choice is permanent. It keeps both spaces honest, so pick the one that fits you now.",
        Icons.Rounded.Lock,
    )
    Spacer(Modifier.weight(1f))
    PrimaryButton("Continue as ${s.label}", onNext)
}

// ---------- Step 2: details ----------

@Composable
private fun ColumnScope.DetailsStep(state: OnboardingState, vm: OnboardingViewModel, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("About you", style = MaterialTheme.typography.headlineLarge)
        Text("Use your real name and birthday. Authentic profiles get more matches.", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted)
    }
    LabeledField("Full name", error = state.nameError) {
        RelunTextField(
            value = state.name,
            onValueChange = vm::setName,
            placeholder = "e.g. Ada Okafor",
            isError = state.nameError != null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusLost(vm::touchName),
            keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
            trailing = if (state.name.trim().length > 1) {
                { Icon(Icons.Rounded.CheckCircle, null, tint = RelunColors.Success, modifier = Modifier.size(22.dp)) }
            } else null,
        )
    }
    LabeledField("Date of birth", error = state.dobError) {
        val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RelunTextField(state.day, vm::setDay, Modifier.weight(1f), placeholder = "DD", isError = state.dobError != null, keyboardOptions = numeric, textAlign = TextAlign.Center)
            RelunTextField(state.month, vm::setMonth, Modifier.weight(1f), placeholder = "MM", isError = state.dobError != null, keyboardOptions = numeric, textAlign = TextAlign.Center)
            RelunTextField(state.year, vm::setYear, Modifier.weight(1.6f), placeholder = "YYYY", isError = state.dobError != null, keyboardOptions = numeric, textAlign = TextAlign.Center)
        }
        InfoNote(
            "Your date of birth can only be set once and cannot be changed later.",
            Icons.Rounded.Info,
            background = RelunColors.WarningFill,
            color = RelunColors.WarningText,
        )
    }
    LabeledField("Gender") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach { g ->
                val on = state.gender == g
                val s = Relun.segment
                Box(
                    Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) s.tint else Color.White)
                        .border(1.5.dp, if (on) s.fill else RelunColors.Border, RoundedCornerShape(16.dp))
                        .clickable(role = Role.RadioButton) { vm.setGender(g) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(g.label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp), color = if (on) s.text else RelunColors.Ink)
                }
            }
        }
    }
    LabeledField(if (state.signedInByPhone) "Email · for account recovery" else "Phone number · for account recovery") {
        RelunTextField(
            value = state.recovery,
            onValueChange = vm::setRecovery,
            placeholder = if (state.signedInByPhone) "you@example.com" else "+234 801 234 5678",
            keyboardOptions = KeyboardOptions(keyboardType = if (state.signedInByPhone) KeyboardType.Email else KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    LabeledField("Bio", trailingLabel = "Optional") {
        RelunTextField(
            value = state.bio,
            onValueChange = vm::setBio,
            placeholder = "Something true and a little specific…",
            singleLine = false,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        )
        Text("${state.bio.length}/150", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RelunColors.Muted, modifier = Modifier.align(Alignment.End))
    }
    Spacer(Modifier.height(4.dp))
    PrimaryButton("Continue", { vm.submitDetails(onNext) }, enabled = state.detailsValid, loading = state.submitting)
}

/** Runs [block] when the field loses focus after having had it (blur validation). */
private fun Modifier.onFocusLost(block: () -> Unit): Modifier = composed {
    var hadFocus by remember { androidx.compose.runtime.mutableStateOf(false) }
    onFocusChanged { focus ->
        if (hadFocus && !focus.isFocused) block()
        hadFocus = focus.isFocused
    }
}

// ---------- Step 3: location ----------

@Composable
private fun ColumnScope.LocationStep(state: OnboardingState, vm: OnboardingViewModel, onNext: () -> Unit) {
    val s = Relun.segment
    val container = appContainer()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.onLocationPermission(result.values.any { it })
    }
    val context = LocalContext.current

    if (state.location != LocationStatus.Denied) {
        Box(Modifier.padding(top = 8.dp).size(88.dp).clip(RoundedCornerShape(28.dp)).background(s.tint), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LocationOn, null, tint = s.text, modifier = Modifier.size(42.dp))
        }
        Text("Enable Location", style = MaterialTheme.typography.headlineLarge)
        Text("We use your location to show you people nearby. You can change this any time in Settings.", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted)
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Benefit(Icons.Outlined.People, "Find people nearby", "Matches within your chosen distance")
            Benefit(Icons.Outlined.Navigation, "Distance info", "See roughly how far someone is")
            Benefit(Icons.Outlined.VerifiedUser, "Exact location is never shared", "Others only see a rounded distance")
        }
        Spacer(Modifier.weight(1f))
        when (state.location) {
            LocationStatus.Granted -> {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(RelunColors.SuccessFill).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = RelunColors.SuccessText, modifier = Modifier.size(22.dp))
                    Text(
                        "Location enabled" + (state.cityLabel?.let { " · $it" } ?: ""),
                        color = RelunColors.SuccessText,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                    )
                }
                PrimaryButton("Continue", onNext)
            }
            LocationStatus.Loading -> PrimaryButton("Enable Location", {}, loading = true, loadingText = "Finding you…")
            else -> {
                PrimaryButton("Enable Location", {
                    if (container.location.hasPermission()) vm.onLocationPermission(true)
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                })
                LinkButton("Not now", { vm.onLocationPermission(false) }, Modifier.fillMaxWidth())
            }
        }
    } else {
        Box(Modifier.padding(top = 8.dp).size(88.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFFF1F1F1)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.LocationOn, null, tint = RelunColors.Body, modifier = Modifier.size(42.dp))
        }
        Text("Location is off", style = MaterialTheme.typography.headlineLarge)
        Text("Relun needs an area to show you people nearby. Turn on location, or tell us your city instead.", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted)
        OutlineButton("Open Settings", { openAppSettings(context) }, leadingIcon = Icons.Outlined.Settings)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(1.dp).background(RelunColors.Border))
            Text("or enter a city", style = MaterialTheme.typography.bodySmall, color = RelunColors.Muted)
            Box(Modifier.weight(1f).height(1.dp).background(RelunColors.Border))
        }
        RelunTextField(
            value = state.cityInput,
            onValueChange = vm::setCityInput,
            placeholder = "City, e.g. Lagos",
            modifier = Modifier.fillMaxWidth(),
            leading = { Icon(Icons.Rounded.Search, null, tint = RelunColors.Muted, modifier = Modifier.size(20.dp)) },
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Use this city", { vm.useCity(onNext) }, enabled = state.cityInput.trim().length > 1, loading = state.submitting)
    }
}

@Composable
private fun Benefit(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(icon)
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = RelunColors.Muted)
        }
    }
}

// ---------- Step 4: photos ----------

@Composable
private fun ColumnScope.PhotosStep(state: OnboardingState, vm: OnboardingViewModel, onNext: () -> Unit) {
    val context = LocalContext.current
    var target by remember { mutableIntStateOf(-1) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && target >= 0) vm.addPhoto(context, target, uri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Add your photos", style = MaterialTheme.typography.headlineLarge)
        Text("Add at least 2. Your first photo is your main one.", style = MaterialTheme.typography.bodyMedium, color = RelunColors.Muted)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        state.slots.forEachIndexed { index, slot ->
            PhotoSlotView(
                slot = slot,
                main = index == 0,
                onAdd = {
                    target = index
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemove = { vm.removePhoto(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Text(
        "${state.uploadedPhotos.size}/3 photos",
        style = MaterialTheme.typography.bodySmall,
        color = RelunColors.Muted,
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, RelunColors.BorderSoft, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("What works", style = MaterialTheme.typography.labelMedium)
        Tip(Icons.Outlined.SentimentSatisfied, "A clear face shot, smiling")
        Tip(Icons.Outlined.AccessTime, "Taken in the last year")
        Tip(Icons.Outlined.WbSunny, "Good light, no sunglasses")
    }
    Spacer(Modifier.weight(1f))
    PrimaryButton("Finish", onNext, enabled = state.photosValid)
}

@Composable
private fun Tip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = RelunColors.Body, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = RelunColors.Body)
    }
}

@Composable
fun PhotoSlotView(
    slot: PhotoSlot,
    main: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    seed: String = "me",
) {
    val s = Relun.segment
    Box(modifier.aspectRatio(0.8f).clip(RoundedCornerShape(16.dp))) {
        when (slot) {
            PhotoSlot.Empty -> Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .dashedBorder()
                    .clickable(onClick = onAdd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(s.fill), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "Add photo", tint = s.onFill, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text("Add", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = RelunColors.Muted)
            }
            is PhotoSlot.Uploading -> Column(
                Modifier.fillMaxSize().background(Color(0xFFEDEDED)).padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Uploading", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = RelunColors.Body)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = s.fill,
                    trackColor = Color(0xFFD6D6D6),
                )
            }
            is PhotoSlot.Done -> {
                PersonPhoto(slot.photo.url, seed, "", Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp))
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(44.dp)
                        .clickable(onClick = onRemove, role = Role.Button)
                        .padding(6.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (main && slot is PhotoSlot.Done) {
            Text(
                "Main",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(999.dp)).background(RelunColors.Ink).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

private fun Modifier.dashedBorder(): Modifier = drawWithContent {
    drawContent()
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = Color(0xFFCFCFCF),
        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        ),
    )
}

// ---------- You're in ----------

@Composable
private fun YoureInStep(state: OnboardingState, vm: OnboardingViewModel) {
    val s = Relun.segment
    LaunchedEffect(Unit) { vm.loadNearbyCount() }
    val scale by animateFloatAsState(1f, tween(400), label = "in")
    val photo = state.uploadedPhotos.firstOrNull()
    Column(
        Modifier
            .fillMaxSize()
            .background(RelunColors.HeroGradient)
            .background(Brush.radialGradient(listOf(RelunColors.HeroGlow, Color.Transparent), radius = 900f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.weight(1f))
        PersonPhoto(
            url = photo?.url,
            seed = "me",
            initial = state.name.firstOrNull()?.uppercase() ?: "",
            modifier = Modifier
                .width((148 * scale).dp)
                .aspectRatio(0.8f)
                .border(4.dp, Color.White, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            initialSize = 54.sp,
        )
        Text(
            "You’re in, ${state.name.trim().substringBefore(' ').ifBlank { "friend" }}",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        val count = state.nearbyCount
        Text(
            when {
                count == null -> "Finding people near you…"
                count == 0 -> "You’re early. We’ll show you new people as they join nearby."
                count == 1 -> "1 person nearby is also looking for ${s.label.lowercase()}."
                count >= 50 -> "50+ people nearby are also looking for ${s.label.lowercase()}."
                else -> "$count people nearby are also looking for ${s.label.lowercase()}."
            },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Start discovering", vm::finish, container = Color.White, content = RelunColors.Ink)
    }
}
