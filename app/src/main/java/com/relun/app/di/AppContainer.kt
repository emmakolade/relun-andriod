package com.relun.app.di

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.relun.app.billing.BillingManager
import com.relun.app.data.network.Network
import com.relun.app.data.repository.AuthRepository
import com.relun.app.data.repository.AuthState
import com.relun.app.data.repository.ChatRepository
import com.relun.app.data.repository.CoinsRepository
import com.relun.app.data.repository.DatesRepository
import com.relun.app.data.repository.PeopleRepository
import com.relun.app.data.repository.ProfileRepository
import com.relun.app.data.repository.SafetyRepository
import com.relun.app.data.repository.SettingsRepository
import com.relun.app.data.session.SessionStore
import com.relun.app.data.socket.ChatSocket
import com.relun.app.location.LocationProvider
import com.relun.app.push.PushRegistrar
import com.relun.app.ui.common.Messenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Hand-rolled dependency graph. Everything here lives as long as the process;
 * view models get what they need from it through [viewModelFactory].
 */
class AppContainer(app: Application) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val session = SessionStore(app, scope)
    val messenger = Messenger()

    lateinit var auth: AuthRepository
        private set

    private val network = Network(session) { auth.onSessionExpired() }
    val api = network.api
    val imageHttpClient = network.okHttp

    val socket = ChatSocket(network.baseUrl, session)
    val profile = ProfileRepository(api)
    val people = PeopleRepository(api)
    val chat = ChatRepository(api, socket, session, scope)
    val coins = CoinsRepository(api)
    val dates = DatesRepository(api)
    val safety = SafetyRepository(api, people)
    val settings = SettingsRepository(api)
    val location = LocationProvider(app)
    val billing = BillingManager(app, coins, scope)
    val push = PushRegistrar(app, api, session, scope)

    private var foreground = false

    init {
        auth = AuthRepository(api, session, scope) {
            push.unregister()
            socket.disconnect()
            profile.clear()
            coins.clear()
            chat.clear()
            settings.clear()
        }

        // Everything a signed-in session needs, started once per sign-in.
        scope.launch {
            auth.state.map { it == AuthState.SignedIn }.distinctUntilChanged().collect { signedIn ->
                if (!signedIn) return@collect
                if (foreground) socket.connect()
                push.register()
                launch { coins.refresh().onSuccess { w -> billing.loadProducts(w.packages.map { it.productId }) } }
                launch { profile.refresh() }
                launch { settings.refresh() }
                launch { billing.reconcile() }
                chat.refreshUnread()
            }
        }

        // A refreshed access token means the socket's handshake token is stale.
        scope.launch {
            session.session.map { it.accessToken }.distinctUntilChanged().drop(1).collect { token ->
                if (token != null && auth.state.value == AuthState.SignedIn && foreground) {
                    socket.pause()
                    socket.connect()
                }
            }
        }

        // The server pushes only to users with no live socket, so drop it in the background.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                foreground = true
                if (auth.state.value == AuthState.SignedIn) {
                    socket.connect()
                    chat.refreshUnread()
                    scope.launch { coins.refresh() }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                foreground = false
                socket.pause()
            }
        })

        auth.bootstrap()
    }
}
