package com.relun.app.data.repository

import com.relun.app.data.model.ContactBody
import com.relun.app.data.model.Segment
import com.relun.app.data.model.VerifyOtpBody
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import com.relun.app.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

enum class ContactMethod(val wire: String) { Phone("phone"), Email("email") }

enum class OnboardingStart { Segment, Photos }

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class Onboarding(val start: OnboardingStart) : AuthState
    data object SignedIn : AuthState
}

data class VerifyOutcome(val needsProfile: Boolean, val needsPhotos: Boolean)

/**
 * Sign-in, sign-out and the app-wide [AuthState] that decides which part of the
 * app is shown. Signing out from anywhere, including an expired refresh token,
 * goes through [signOutLocally].
 */
class AuthRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val scope: CoroutineScope,
    private val onSignedOut: suspend () -> Unit,
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _segment = MutableStateFlow(Segment.Relationship)
    /** The signed-in user's segment; drives the accent colours everywhere. */
    val segment: StateFlow<Segment> = _segment.asStateFlow()

    fun setSegment(segment: Segment) {
        _segment.value = segment
    }

    /** Decides where a cold start lands. Offline with a saved session goes straight in. */
    fun bootstrap() {
        scope.launch {
            if (!session.current.isSignedIn) {
                _state.value = AuthState.SignedOut
                return@launch
            }
            apiCall { api.myProfile() }
                .onSuccess { me ->
                    _segment.value = Segment.from(me.profile?.segment)
                    _state.value = when {
                        me.user?.fullName.isNullOrBlank() || me.user?.dateOfBirth == null ->
                            AuthState.Onboarding(OnboardingStart.Segment)
                        me.photos.size < MIN_PHOTOS -> AuthState.Onboarding(OnboardingStart.Photos)
                        else -> AuthState.SignedIn
                    }
                }
                .onFailure { error ->
                    _state.value = if (error is com.relun.app.data.network.ApiException && error.isUnauthorized) {
                        signOutLocally(); AuthState.SignedOut
                    } else {
                        AuthState.SignedIn
                    }
                }
        }
    }

    suspend fun requestCode(method: ContactMethod, contact: String) = apiCall {
        api.requestOtp(contact.toBody(method))
    }

    suspend fun verifyCode(method: ContactMethod, contact: String, code: String): Result<VerifyOutcome> =
        apiCall {
            val body = contact.toBody(method)
            api.verifyOtp(VerifyOtpBody(email = body.email, phone = body.phone, otp = code))
        }.map { response ->
            session.saveSignIn(response.accessToken, response.refreshToken, response.user.id, method.wire)
            _segment.value = Segment.from(response.user.profile?.segment)
            VerifyOutcome(
                needsProfile = response.needsProfileCompletion,
                needsPhotos = response.photoCount < MIN_PHOTOS,
            )
        }

    fun enterOnboarding(start: OnboardingStart) {
        _state.value = AuthState.Onboarding(start)
    }

    fun finishOnboarding() {
        _state.value = AuthState.SignedIn
    }

    suspend fun signOut() {
        apiCall { api.logout() }
        signOutLocally()
    }

    suspend fun deleteAccount(): Result<Unit> = apiCall { api.deleteAccount() }.map { signOutLocally() }

    /** Clears everything on this device. Safe to call more than once. */
    suspend fun signOutLocally() {
        onSignedOut()
        session.clear()
        _segment.value = Segment.Relationship
        _state.value = AuthState.SignedOut
    }

    /** Called by the network layer when the refresh token is rejected. */
    fun onSessionExpired() {
        if (_state.value == AuthState.SignedOut) return
        scope.launch { signOutLocally() }
    }

    init {
        // Keep the state honest if tokens disappear underneath us.
        scope.launch {
            session.session.distinctUntilChangedBy { it.isSignedIn }.collect { s ->
                if (!s.isSignedIn && _state.value != AuthState.Loading && _state.value != AuthState.SignedOut) {
                    _state.value = AuthState.SignedOut
                }
            }
        }
    }

    private fun String.toBody(method: ContactMethod) = when (method) {
        ContactMethod.Phone -> ContactBody(phone = this)
        ContactMethod.Email -> ContactBody(email = this.trim().lowercase())
    }

    companion object {
        const val MIN_PHOTOS = 2
        const val MAX_PHOTOS = 3
    }
}
