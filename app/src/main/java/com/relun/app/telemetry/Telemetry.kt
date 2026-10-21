package com.relun.app.telemetry

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.relun.app.BuildConfig
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

/**
 * Sentry (crashes) and PostHog (product analytics). Each starts only when its
 * key is in BuildConfig, so builds without keys simply don't report anything.
 * Only the opaque user id is ever attached: no names, contacts or messages.
 */
object Telemetry {

    private var posthog = false

    fun init(app: Application) {
        if (BuildConfig.SENTRY_DSN.isNotBlank()) {
            SentryAndroid.init(app) { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.environment = if (BuildConfig.DEBUG) "debug" else "production"
                options.release = BuildConfig.VERSION_NAME
                options.isSendDefaultPii = false
                options.tracesSampleRate = 0.2
                // Session replay stays off: its sample rates are left unset.
            }
        }
        if (BuildConfig.POSTHOG_KEY.isNotBlank()) {
            val config = PostHogAndroidConfig(BuildConfig.POSTHOG_KEY, BuildConfig.POSTHOG_HOST).apply {
                // Compose has no automatic screen names; see TrackScreens.
                captureScreenViews = false
                sessionReplay = false
            }
            PostHogAndroid.setup(app, config)
            posthog = true
        }
    }

    /** On sign-in and on a cold start with a saved session. */
    fun identify(userId: String) {
        Sentry.setUser(User().apply { id = userId })
        if (posthog) PostHog.identify(userId)
    }

    /** On sign-out, account deletion or an expired session. */
    fun reset() {
        Sentry.setUser(null)
        if (posthog) PostHog.reset()
    }

    internal fun screen(name: String) {
        if (posthog) PostHog.screen(name)
    }

    internal fun capture(event: String, props: Map<String, Any>) {
        if (posthog) PostHog.capture(event, properties = props.ifEmpty { null })
    }
}

/** Product events. Never put names, contact details or message text in [props]. */
object Analytics {
    fun track(event: String, props: Map<String, Any> = emptyMap()) = Telemetry.capture(event, props)
}

/**
 * Reports each destination of [nav] as a PostHog screen, named after its route
 * class ("PersonRoute"). Route templates hold placeholders, never argument values.
 */
@Composable
fun TrackScreens(nav: NavController) {
    DisposableEffect(nav) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val route = destination.route ?: return@OnDestinationChangedListener
            Telemetry.screen(route.substringBefore('/').substringBefore('?').substringAfterLast('.'))
        }
        nav.addOnDestinationChangedListener(listener)
        onDispose { nav.removeOnDestinationChangedListener(listener) }
    }
}
