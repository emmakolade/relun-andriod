package com.relun.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.relun.app.data.repository.AuthState
import com.relun.app.push.EXTRA_PUSH_TYPE
import com.relun.app.push.EXTRA_PUSH_USER
import com.relun.app.ui.navigation.PushTarget
import com.relun.app.ui.navigation.RelunRoot
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** A notification tap waiting for the signed-in UI to open it. */
    private val pendingPush = MutableStateFlow<PushTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as RelunApp).container
        // Hold the splash until we know whether to show Welcome or the app.
        splash.setKeepOnScreenCondition { container.auth.state.value == AuthState.Loading }
        // Remove the splash ourselves. Left to the system, its reveal animation can
        // stall and leave the splash layer covering the app.
        splash.setOnExitAnimationListener { it.remove() }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        readPush(intent)

        setContent { RelunRoot(pendingPush = pendingPush) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readPush(intent)
    }

    private fun readPush(intent: Intent?) {
        intent ?: return
        // FCM puts data fields straight into the extras when the system drew the
        // notification; our own foreground notifications use the EXTRA_ keys.
        val type = intent.getStringExtra(EXTRA_PUSH_TYPE) ?: intent.getStringExtra("type")
        val userId = intent.getStringExtra(EXTRA_PUSH_USER) ?: intent.getStringExtra("userId")
        if (type != null) pendingPush.value = PushTarget(type, userId)
    }
}
