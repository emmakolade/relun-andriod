package com.relun.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.relun.app.data.repository.AuthState
import com.relun.app.data.repository.OnboardingStart
import com.relun.app.ui.common.Toast
import com.relun.app.ui.common.appContainer
import com.relun.app.ui.components.RingsMark
import com.relun.app.ui.components.ToastHost
import com.relun.app.ui.screens.auth.CodeSignInScreen
import com.relun.app.ui.screens.auth.WelcomeScreen
import com.relun.app.ui.screens.onboarding.OnboardingFlow
import com.relun.app.ui.theme.RelunColors
import com.relun.app.ui.theme.RelunTheme
import kotlinx.coroutines.flow.MutableStateFlow

private const val ANIM_MS = 320

internal val pushIn: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_MS))
}
internal val pushOut: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_MS), targetOffset = { it / 3 }) +
        fadeOut(tween(ANIM_MS))
}
internal val popIn: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_MS), initialOffset = { it / 3 }) +
        fadeIn(tween(ANIM_MS))
}
internal val popOut: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_MS))
}

/**
 * The whole app. Auth state picks one of three trees (signed out, onboarding,
 * signed in); the user's segment picks the accent colours for all of them.
 */
@Composable
fun RelunRoot(pendingPush: MutableStateFlow<PushTarget?>) {
    val container = appContainer()
    val state by container.auth.state.collectAsStateWithLifecycle()
    val segment by container.auth.segment.collectAsStateWithLifecycle()

    var toast by remember { mutableStateOf<Toast?>(null) }
    LaunchedEffect(Unit) { container.messenger.toasts.collect { toast = it } }

    RelunTheme(segment = segment) {
        Box(Modifier.fillMaxSize().background(RelunColors.Background)) {
            when (val s = state) {
                AuthState.Loading -> Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                    // Same size and place as the splash icon, so launch looks seamless.
                    RingsMark(164.dp)
                }
                AuthState.SignedOut -> SignedOutFlow()
                is AuthState.Onboarding -> OnboardingFlow(start = s.start)
                AuthState.SignedIn -> MainFlow(pendingPush)
            }
            ToastHost(toast, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun SignedOutFlow() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = WelcomeRoute,
        enterTransition = pushIn,
        exitTransition = pushOut,
        popEnterTransition = popIn,
        popExitTransition = popOut,
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onPhone = { nav.navigate(CodeRoute("phone")) },
                onEmail = { nav.navigate(CodeRoute("email")) },
            )
        }
        composable<CodeRoute> { entry ->
            val route = entry.toRoute<CodeRoute>()
            CodeSignInScreen(
                initialMethod = route.method,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
