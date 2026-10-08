package com.relun.app.data.repository

import com.relun.app.data.model.BlockedUserDto
import com.relun.app.data.model.CreateDateBody
import com.relun.app.data.model.ReportBody
import com.relun.app.data.model.RequestStatusBody
import com.relun.app.data.model.SettingsDto
import com.relun.app.data.model.SettingsPatch
import com.relun.app.data.model.toDomain
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

class DatesRepository(private val api: ApiService) {
    suspend fun browse() = apiCall { api.browseDates() }.map { res -> res.dates.map { it.toDomain() } }
    suspend fun mine() = apiCall { api.myDates() }.map { res -> res.dates.map { it.toDomain() } }

    /** Posts a date: free while a free slot is left, otherwise it costs coins. Returns the post and coins charged. */
    suspend fun create(activity: String, place: String, at: Instant, description: String?) = apiCall {
        api.createDate(CreateDateBody(activity.trim(), place.trim(), at.toString(), description?.trim()?.ifBlank { null }))
    }.map { it.date.toDomain() to it.charged }

    suspend fun delete(dateId: String) = apiCall { api.deleteDate(dateId) }
    suspend fun join(dateId: String) = apiCall { api.requestToJoin(dateId) }
    suspend fun respond(dateId: String, requestId: String, accept: Boolean) = apiCall {
        api.respondToRequest(dateId, requestId, RequestStatusBody(if (accept) "accepted" else "declined"))
    }
}

class SafetyRepository(private val api: ApiService, private val people: PeopleRepository) {
    suspend fun block(userId: String) = apiCall { api.block(userId) }
        .onSuccess { people.emit(PeopleEvent.Blocked(userId)) }
    suspend fun unblock(userId: String) = apiCall { api.unblock(userId) }
        .onSuccess { people.emit(PeopleEvent.Unblocked(userId)) }
    suspend fun report(userId: String, reason: String, details: String?) =
        apiCall { api.report(userId, ReportBody(reason, details)) }
    suspend fun blocked(): Result<List<BlockedUserDto>> = apiCall { api.blocks() }.map { it.blocks }
}

class SettingsRepository(private val api: ApiService) {
    private val _settings = MutableStateFlow(SettingsDto())
    val settings: StateFlow<SettingsDto> = _settings.asStateFlow()

    suspend fun refresh() = apiCall { api.settings() }.onSuccess { _settings.value = it.settings }

    /** Applies [patch] locally first so toggles feel instant, then reconciles. */
    suspend fun update(patch: SettingsPatch, optimistic: SettingsDto): Result<SettingsDto> {
        val previous = _settings.value
        _settings.value = optimistic
        return apiCall { api.updateSettings(patch) }
            .map { it.settings }
            .onSuccess { _settings.value = it }
            .onFailure { _settings.value = previous }
    }

    fun clear() {
        _settings.value = SettingsDto()
    }
}
