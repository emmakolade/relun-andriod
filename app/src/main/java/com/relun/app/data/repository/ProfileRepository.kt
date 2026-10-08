package com.relun.app.data.repository

import com.relun.app.data.model.CompleteProfileBody
import com.relun.app.data.model.MyProfileResponse
import com.relun.app.data.model.Photo
import com.relun.app.data.model.Segment
import com.relun.app.data.model.UpdateProfileBody
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import com.relun.app.util.calculateAge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate

data class MyProfile(
    val name: String,
    val dateOfBirth: LocalDate?,
    val bio: String,
    val occupation: String?,
    val education: String?,
    val city: String?,
    val interests: List<String>,
    val segment: Segment,
    val photos: List<Photo>,
    val email: String?,
    val phone: String?,
) {
    val age: Int? get() = dateOfBirth?.let(::calculateAge)
    val firstName: String get() = name.substringBefore(' ')
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "?"

    /** Same weighting the profile ring shows: basics, bio, photos, details. */
    val completeness: Int
        get() {
            var score = 30
            if (bio.isNotBlank()) score += 15
            score += photos.size * 10
            if (name.isNotBlank()) score += 5
            if (!occupation.isNullOrBlank()) score += 5
            if (interests.isNotEmpty()) score += 5
            if (!city.isNullOrBlank()) score += 10
            return score.coerceAtMost(100)
        }
}

class ProfileRepository(private val api: ApiService) {

    private val _me = MutableStateFlow<MyProfile?>(null)
    val me: StateFlow<MyProfile?> = _me.asStateFlow()

    suspend fun refresh(): Result<MyProfile> = apiCall { api.myProfile() }.map { it.toMyProfile() }
        .onSuccess { _me.value = it }

    suspend fun completeProfile(body: CompleteProfileBody) = apiCall { api.completeProfile(body) }

    suspend fun updateLocation(latitude: Double, longitude: Double, city: String?) = apiCall {
        api.updateProfile(UpdateProfileBody(latitude = latitude, longitude = longitude, city = city))
    }

    suspend fun updateCity(city: String) = apiCall { api.updateProfile(UpdateProfileBody(city = city)) }

    suspend fun updateDetails(body: UpdateProfileBody) = apiCall { api.updateProfile(body) }
        .onSuccess { refresh() }

    suspend fun uploadPhoto(jpeg: ByteArray): Result<Photo> = apiCall {
        api.uploadPhoto(jpeg.toPart("photos"))
    }.map { Photo(it.id, it.url) }.onSuccess { photo ->
        _me.value = _me.value?.let { it.copy(photos = it.photos + photo) }
    }

    suspend fun replacePhoto(photoId: String, jpeg: ByteArray): Result<Photo> = apiCall {
        api.replacePhoto(photoId, jpeg.toPart("photo"))
    }.map { Photo(it.id, it.url) }.onSuccess { photo ->
        _me.value = _me.value?.let { me -> me.copy(photos = me.photos.map { if (it.id == photoId) photo else it }) }
    }

    suspend fun deletePhoto(photoId: String) = apiCall { api.deletePhoto(photoId) }.onSuccess {
        _me.value = _me.value?.let { me -> me.copy(photos = me.photos.filterNot { it.id == photoId }) }
    }

    fun clear() {
        _me.value = null
    }

    private fun ByteArray.toPart(field: String) = MultipartBody.Part.createFormData(
        field,
        "photo.jpg",
        toRequestBody("image/jpeg".toMediaType()),
    )

    private fun MyProfileResponse.toMyProfile() = MyProfile(
        name = user?.fullName.orEmpty(),
        dateOfBirth = user?.dateOfBirth?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        bio = profile?.bio.orEmpty(),
        occupation = profile?.occupation,
        education = profile?.education,
        city = profile?.city,
        interests = profile?.interests.orEmpty(),
        segment = Segment.from(profile?.segment),
        photos = photos.sortedBy { it.order }.map { Photo(it.id, it.url) },
        email = user?.email,
        phone = user?.phone,
    )
}
