package com.relun.app.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.relun.app.data.model.Person
import com.relun.app.ui.common.relunViewModel
import com.relun.app.ui.components.ConfirmDialog
import com.relun.app.ui.screens.chat.ChatScreen
import com.relun.app.ui.screens.main.MainScreen
import com.relun.app.ui.screens.match.MatchOverlay
import com.relun.app.ui.screens.me.EditProfileScreen
import com.relun.app.ui.screens.me.ProfileViewsScreen
import com.relun.app.ui.screens.profile.PersonProfileScreen
import com.relun.app.ui.screens.settings.BlockedUsersScreen
import com.relun.app.ui.screens.settings.SettingsScreen
import com.relun.app.ui.screens.sheets.CoinsSheet
import com.relun.app.ui.screens.sheets.InsightsSheet
import com.relun.app.ui.screens.sheets.MessageRequestSheet
import com.relun.app.ui.screens.sheets.MoreSheet
import com.relun.app.ui.screens.sheets.LikeLimitSheet
import com.relun.app.ui.screens.sheets.PlusSheet
import com.relun.app.ui.screens.sheets.WelcomeCoinsSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/** What any signed-in screen can ask the shell to do. */
class AppActions(
    val openProfile: (userId: String) -> Unit,
    val openChat: (Person) -> Unit,
    val openCoins: () -> Unit,
    val openInsights: () -> Unit,
    /** Relun Plus: plans, or the user's own status. */
    val openPlus: () -> Unit,
    /** Messages tab, "Likes You". */
    val openLikes: () -> Unit,
    val openMore: (Person) -> Unit,
    val openSettings: () -> Unit,
    val openBlocked: () -> Unit,
    val openEditProfile: () -> Unit,
    /** Who viewed my profile (needs insights). */
    val openViews: () -> Unit,
    val back: () -> Unit,
)

val LocalAppActions = staticCompositionLocalOf<AppActions> { error("AppActions not provided") }

@Composable
fun MainFlow(pendingPush: MutableStateFlow<PushTarget?>) {
    val nav = rememberNavController()
    val vm = relunViewModel { MainViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val wallet by vm.wallet.collectAsStateWithLifecycle()
    val prices by vm.prices.collectAsStateWithLifecycle()
    val plusPrices by vm.plusPrices.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    val actions = remember(nav, vm) {
        AppActions(
            openProfile = { nav.navigate(PersonRoute(it)) { launchSingleTop = true } },
            openChat = vm::openChat,
            openCoins = { vm.openCoins() },
            openInsights = vm::openInsights,
            openPlus = vm::openPlus,
            openLikes = vm::showLikes,
            openMore = vm::openMore,
            openSettings = { nav.navigate(SettingsRoute) { launchSingleTop = true } },
            openBlocked = { nav.navigate(BlockedRoute) { launchSingleTop = true } },
            openEditProfile = { nav.navigate(EditProfileRoute) { launchSingleTop = true } },
            openViews = { nav.navigate(ProfileViewsRoute) { launchSingleTop = true } },
            back = { nav.popBackStack() },
        )
    }

    LaunchedEffect(vm) {
        vm.nav.collect { event ->
            when (event) {
                is NavEvent.OpenChat -> nav.navigate(ChatRoute(event.userId, event.name)) { launchSingleTop = true }
                NavEvent.BackToMain -> nav.popBackStack(MainRoute, inclusive = false)
            }
        }
    }

    // Notification taps.
    LaunchedEffect(pendingPush) {
        pendingPush.filterNotNull().collect { target ->
            pendingPush.value = null
            when (target.type) {
                "message", "message_request" -> target.userId?.let { nav.navigate(ChatRoute(it, "")) { launchSingleTop = true } }
                "match" -> target.userId?.let { nav.navigate(PersonRoute(it)) { launchSingleTop = true } }
                    ?: vm.selectTab(Tab.Messages)
                "date_request" -> {
                    nav.popBackStack(MainRoute, inclusive = false)
                    vm.showDates()
                }
                else -> nav.popBackStack(MainRoute, inclusive = false)
            }
        }
    }

    CompositionLocalProvider(LocalAppActions provides actions) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = MainRoute,
                enterTransition = pushIn,
                exitTransition = pushOut,
                popEnterTransition = popIn,
                popExitTransition = popOut,
            ) {
                composable<MainRoute> { MainScreen(shell = vm, state = state, wallet = wallet) }
                composable<PersonRoute> { entry -> PersonProfileScreen(userId = entry.toRoute<PersonRoute>().userId) }
                composable<ChatRoute> { entry ->
                    val route = entry.toRoute<ChatRoute>()
                    ChatScreen(userId = route.userId, initialName = route.name)
                }
                composable<SettingsRoute> { SettingsScreen() }
                composable<BlockedRoute> { BlockedUsersScreen() }
                composable<EditProfileRoute> { EditProfileScreen() }
                composable<ProfileViewsRoute> { ProfileViewsScreen() }
            }

            state.match?.let { person ->
                MatchOverlay(person = person, onMessage = { vm.openChat(person) }, onKeepBrowsing = vm::dismissMatch)
            }

            when (val sheet = state.sheet) {
                is MainSheet.Coins -> CoinsSheet(
                    wallet = wallet,
                    prices = prices,
                    selected = state.selectedPackage,
                    buying = state.buying,
                    onSelect = vm::selectPackage,
                    onBuy = { activity?.let(vm::buy) },
                    onDismiss = vm::closeSheet,
                )
                is MainSheet.Request -> MessageRequestSheet(
                    person = sheet.person,
                    wallet = wallet,
                    draft = state.requestDraft,
                    sending = state.sendingRequest,
                    onDraft = vm::setRequestDraft,
                    onSend = { vm.sendRequest(sheet.person) },
                    onPlus = vm::openPlus,
                    onDismiss = vm::closeSheet,
                )
                MainSheet.Insights -> InsightsSheet(
                    wallet = wallet,
                    plusPrices = plusPrices,
                    buying = state.buyingInsights,
                    onPlus = vm::openPlus,
                    onBuy = vm::buyInsights,
                    onDismiss = vm::closeSheet,
                )
                MainSheet.Plus -> PlusSheet(
                    wallet = wallet,
                    plusPrices = plusPrices,
                    buying = state.buying,
                    onBuy = { basePlan -> activity?.let { vm.buyPlus(it, basePlan) } },
                    onDismiss = vm::closeSheet,
                )
                MainSheet.LikeLimit -> LikeLimitSheet(wallet = wallet, onPlus = vm::openPlus, onDismiss = vm::closeSheet)
                is MainSheet.Bonus -> WelcomeCoinsSheet(bonus = sheet.bonus, wallet = wallet, onDismiss = vm::closeSheet)
                is MainSheet.More -> MoreSheet(
                    person = sheet.person,
                    onReport = { vm.confirmReport(sheet.person) },
                    onBlock = { vm.confirmBlock(sheet.person) },
                    onDismiss = vm::closeSheet,
                )
                null -> Unit
            }

            state.dialog?.let { ConfirmDialog(it, onDismiss = vm::dismissDialog) }
        }
    }
}
