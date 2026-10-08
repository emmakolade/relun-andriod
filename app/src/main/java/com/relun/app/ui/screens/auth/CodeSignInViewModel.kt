package com.relun.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relun.app.data.repository.ContactMethod
import com.relun.app.data.repository.OnboardingStart
import com.relun.app.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

val COUNTRY_CODES = listOf("+234", "+233", "+254", "+27", "+44", "+1", "+91")

data class CodeSignInState(
    val method: ContactMethod = ContactMethod.Phone,
    val countryCode: String = COUNTRY_CODES.first(),
    val contact: String = "",
    val step: Int = 1,
    val code: String = "",
    val codeError: String? = null,
    val sendError: String? = null,
    val busy: Boolean = false,
    val resendIn: Int = 0,
) {
    private val digits get() = contact.filter(Char::isDigit)

    val contactValid: Boolean
        get() = when (method) {
            ContactMethod.Email -> EMAIL.matches(contact.trim())
            ContactMethod.Phone -> digits.length >= 10
        }

    /** Shown once the user has typed enough to be judged. */
    val contactError: String?
        get() = when {
            contact.length <= 4 || contactValid -> null
            method == ContactMethod.Email -> "Enter a valid email address"
            else -> "Phone numbers need at least 10 digits"
        }

    /** What gets sent: E.164 for phones, e.g. +2348012345678. */
    val normalizedContact: String
        get() = when (method) {
            ContactMethod.Email -> contact.trim().lowercase()
            ContactMethod.Phone -> countryCode + digits.trimStart('0')
        }

    val sentTo: String get() = if (method == ContactMethod.Phone) "$countryCode $contact" else contact.trim()

    companion object {
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    }
}

class CodeSignInViewModel(private val c: AppContainer, initialMethod: String) : ViewModel() {

    private val _state = MutableStateFlow(
        CodeSignInState(method = if (initialMethod == "email") ContactMethod.Email else ContactMethod.Phone)
    )
    val state: StateFlow<CodeSignInState> = _state.asStateFlow()

    private var countdown: Job? = null

    fun setMethod(method: ContactMethod) = _state.update { it.copy(method = method, contact = "", sendError = null) }
    fun nextCountryCode() = _state.update {
        it.copy(countryCode = COUNTRY_CODES[(COUNTRY_CODES.indexOf(it.countryCode) + 1) % COUNTRY_CODES.size])
    }
    fun setContact(value: String) = _state.update { it.copy(contact = value.take(80), sendError = null) }

    fun setCode(value: String) {
        val digits = value.filter(Char::isDigit).take(6)
        _state.update { it.copy(code = digits, codeError = null) }
        if (digits.length == 6) verify()
    }

    fun sendCode() {
        val s = _state.value
        if (!s.contactValid || s.busy) return
        _state.update { it.copy(busy = true, sendError = null) }
        viewModelScope.launch {
            c.auth.requestCode(s.method, s.normalizedContact)
                .onSuccess {
                    _state.update { it.copy(busy = false, step = 2, code = "", codeError = null) }
                    startCountdown()
                }
                .onFailure { e -> _state.update { it.copy(busy = false, sendError = e.message) } }
        }
    }

    fun resend() {
        val s = _state.value
        _state.update { it.copy(code = "", codeError = null) }
        viewModelScope.launch {
            c.auth.requestCode(s.method, s.normalizedContact)
                .onSuccess { c.messenger.info("New code sent"); startCountdown() }
                .onFailure { c.messenger.error(it.message ?: "Couldn’t resend the code.") }
        }
    }

    fun changeContact() {
        countdown?.cancel()
        _state.update { it.copy(step = 1, code = "", codeError = null) }
    }

    fun verify() {
        val s = _state.value
        if (s.code.length != 6 || s.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            c.auth.verifyCode(s.method, s.normalizedContact, s.code)
                .onSuccess { outcome ->
                    when {
                        outcome.needsProfile -> c.auth.enterOnboarding(OnboardingStart.Segment)
                        outcome.needsPhotos -> c.auth.enterOnboarding(OnboardingStart.Photos)
                        else -> c.auth.finishOnboarding()
                    }
                }
                .onFailure { e ->
                    val message = if (e is com.relun.app.data.network.ApiException && e.status == 401) {
                        "That code is wrong or has expired. Try again or resend."
                    } else {
                        e.message ?: "Couldn’t verify the code."
                    }
                    _state.update { it.copy(busy = false, codeError = message) }
                }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        _state.update { it.copy(resendIn = 45) }
        countdown = viewModelScope.launch {
            while (isActive && _state.value.resendIn > 0) {
                delay(1_000)
                _state.update { it.copy(resendIn = it.resendIn - 1) }
            }
        }
    }
}
