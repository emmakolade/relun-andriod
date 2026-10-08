package com.relun.app.ui.screens.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relun.app.data.model.CompleteProfileBody
import com.relun.app.data.model.Photo
import com.relun.app.data.model.Segment
import com.relun.app.data.repository.AuthRepository
import com.relun.app.di.AppContainer
import com.relun.app.location.Coordinates
import com.relun.app.util.calculateAge
import com.relun.app.util.compressForUpload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Gender(val label: String, val wire: String) { Female("Female", "female"), Male("Male", "male") }

enum class LocationStatus { Idle, Loading, Granted, Denied }

sealed interface PhotoSlot {
    data object Empty : PhotoSlot
    data class Uploading(val localUri: Uri) : PhotoSlot
    data class Done(val photo: Photo) : PhotoSlot
}

data class OnboardingState(
    val segment: Segment = Segment.Relationship,
    val name: String = "",
    val day: String = "",
    val month: String = "",
    val year: String = "",
    val gender: Gender? = null,
    val recovery: String = "",
    val bio: String = "",
    val nameTouched: Boolean = false,
    val submitting: Boolean = false,
    val location: LocationStatus = LocationStatus.Idle,
    val cityLabel: String? = null,
    val cityInput: String = "",
    val slots: List<PhotoSlot> = List(AuthRepository.MAX_PHOTOS) { PhotoSlot.Empty },
    val nearbyCount: Int? = null,
    /** Whether the user signed in by phone, so recovery asks for an email. */
    val signedInByPhone: Boolean = true,
) {
    val nameError: String? get() = if (nameTouched && name.isBlank()) "Enter your name" else null

    val dobError: String?
        get() {
            val d = day.toIntOrNull()
            val m = month.toIntOrNull()
            val y = year.toIntOrNull()
            val thisYear = LocalDate.now().year
            if (month.isNotEmpty() && (m == null || m !in 1..12)) return "Invalid month"
            if (day.isNotEmpty() && (d == null || d !in 1..31)) return "Invalid day"
            if (year.length == 4 && (y == null || y !in 1940..thisYear)) return "Invalid year. Use 1940–$thisYear"
            if (d != null && m != null && y != null && year.length == 4) {
                val date = runCatching { LocalDate.of(y, m, d) }.getOrNull() ?: return "Invalid day for that month"
                if (date.isAfter(LocalDate.now())) return "That date is in the future"
                if (calculateAge(date) < 18) return "You must be 18 or older to use Relun"
            }
            return null
        }

    val dateOfBirth: LocalDate?
        get() = if (year.length == 4 && dobError == null) {
            runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
        } else null

    val detailsValid: Boolean get() = name.isNotBlank() && dateOfBirth != null && gender != null

    val uploadedPhotos: List<Photo> get() = slots.filterIsInstance<PhotoSlot.Done>().map { it.photo }
    val photosValid: Boolean
        get() = uploadedPhotos.size >= AuthRepository.MIN_PHOTOS && slots.none { it is PhotoSlot.Uploading }
}

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingState(
            segment = c.auth.segment.value,
            signedInByPhone = c.session.current.authMethod != "email",
        )
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private var coordinates: Coordinates? = null

    init {
        // Resuming at the photo step: show what is already uploaded.
        viewModelScope.launch {
            c.profile.refresh().onSuccess { me ->
                _state.update { s ->
                    val done = me.photos.take(AuthRepository.MAX_PHOTOS).map { PhotoSlot.Done(it) }
                    s.copy(
                        slots = done + List(AuthRepository.MAX_PHOTOS - done.size) { PhotoSlot.Empty },
                        name = s.name.ifBlank { me.name },
                    )
                }
            }
        }
    }

    // ---------- Segment ----------

    fun chooseSegment(segment: Segment) {
        _state.update { it.copy(segment = segment) }
        c.auth.setSegment(segment)
    }

    // ---------- Details ----------

    fun setName(v: String) = _state.update { it.copy(name = v.take(100)) }
    fun touchName() = _state.update { it.copy(nameTouched = true) }
    fun setDay(v: String) = _state.update { it.copy(day = v.filter(Char::isDigit).take(2)) }
    fun setMonth(v: String) = _state.update { it.copy(month = v.filter(Char::isDigit).take(2)) }
    fun setYear(v: String) = _state.update { it.copy(year = v.filter(Char::isDigit).take(4)) }
    fun setGender(g: Gender) = _state.update { it.copy(gender = g) }
    fun setRecovery(v: String) = _state.update { it.copy(recovery = v.take(80)) }
    fun setBio(v: String) = _state.update { it.copy(bio = v.take(150)) }

    fun submitDetails(onDone: () -> Unit) {
        val s = _state.value
        if (!s.detailsValid) {
            _state.update { it.copy(nameTouched = true) }
            return
        }
        val recovery = s.recovery.trim().ifBlank { null }
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            c.profile.completeProfile(
                CompleteProfileBody(
                    fullName = s.name.trim(),
                    dateOfBirth = s.dateOfBirth.toString(),
                    gender = s.gender!!.wire,
                    segment = s.segment.wire,
                    bio = s.bio.trim().ifBlank { null },
                    email = if (s.signedInByPhone) recovery?.lowercase() else null,
                    phone = if (!s.signedInByPhone) recovery?.filter { it.isDigit() || it == '+' } else null,
                )
            ).onSuccess {
                _state.update { it.copy(submitting = false) }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(submitting = false) }
                c.messenger.error(e.message ?: "Couldn’t save your details.")
            }
        }
    }

    // ---------- Location ----------

    fun onLocationPermission(granted: Boolean) {
        if (!granted) {
            _state.update { it.copy(location = LocationStatus.Denied) }
            return
        }
        _state.update { it.copy(location = LocationStatus.Loading) }
        viewModelScope.launch {
            val coords = c.location.current()
            if (coords == null) {
                c.messenger.warning("We couldn’t find your location. Enter your city instead.")
                _state.update { it.copy(location = LocationStatus.Denied) }
                return@launch
            }
            coordinates = coords
            val city = c.location.cityName(coords)
            c.profile.updateLocation(coords.latitude, coords.longitude, city)
            _state.update { it.copy(location = LocationStatus.Granted, cityLabel = city) }
        }
    }

    fun setCityInput(v: String) = _state.update { it.copy(cityInput = v.take(80)) }

    fun useCity(onDone: () -> Unit) {
        val city = _state.value.cityInput.trim()
        if (city.length < 2) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val coords = c.location.geocodeCity(city)
            val result = if (coords != null) {
                coordinates = coords
                c.profile.updateLocation(coords.latitude, coords.longitude, city)
            } else {
                c.profile.updateCity(city)
            }
            _state.update { it.copy(submitting = false) }
            result.onSuccess { onDone() }.onFailure { c.messenger.error(it.message ?: "Couldn’t save your city.") }
        }
    }

    // ---------- Photos ----------

    fun addPhoto(context: Context, index: Int, uri: Uri) {
        if (_state.value.slots.getOrNull(index) != PhotoSlot.Empty) return
        setSlot(index, PhotoSlot.Uploading(uri))
        viewModelScope.launch {
            val bytes = runCatching { compressForUpload(context, uri) }.getOrElse {
                setSlot(index, PhotoSlot.Empty)
                c.messenger.error("We couldn’t read that photo. Try another one.")
                return@launch
            }
            c.profile.uploadPhoto(bytes)
                .onSuccess { setSlot(index, PhotoSlot.Done(it)) }
                .onFailure {
                    setSlot(index, PhotoSlot.Empty)
                    c.messenger.error(it.message ?: "Upload failed. Try again.")
                }
        }
    }

    fun removePhoto(index: Int) {
        val slot = _state.value.slots.getOrNull(index) as? PhotoSlot.Done ?: return
        setSlot(index, PhotoSlot.Empty)
        viewModelScope.launch {
            c.profile.deletePhoto(slot.photo.id).onFailure {
                setSlot(index, slot)
                c.messenger.error(it.message ?: "Couldn’t remove that photo.")
            }
        }
    }

    private fun setSlot(index: Int, slot: PhotoSlot) = _state.update { s ->
        s.copy(slots = s.slots.toMutableList().also { it[index] = slot })
    }

    // ---------- You're in ----------

    fun loadNearbyCount() {
        viewModelScope.launch {
            val coords = coordinates ?: c.location.last
            c.people.discover(coords?.latitude, coords?.longitude)
                .onSuccess { people -> _state.update { it.copy(nearbyCount = people.size) } }
        }
    }

    fun finish() = c.auth.finishOnboarding()
}
