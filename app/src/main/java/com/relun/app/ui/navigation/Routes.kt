package com.relun.app.ui.navigation

import kotlinx.serialization.Serializable

// Signed out
@Serializable data object WelcomeRoute
@Serializable data class CodeRoute(val method: String)

// Onboarding
@Serializable data object SegmentRoute
@Serializable data object DetailsRoute
@Serializable data object LocationRoute
@Serializable data object PhotosRoute
@Serializable data object YoureInRoute

// Signed in
@Serializable data object MainRoute
@Serializable data class PersonRoute(val userId: String)
@Serializable data class ChatRoute(val userId: String, val name: String)
@Serializable data object SettingsRoute
@Serializable data object BlockedRoute
@Serializable data object EditProfileRoute
@Serializable data object ProfileViewsRoute

/** Where a notification tap should land. */
data class PushTarget(val type: String, val userId: String?)
