package com.relun.app.data.repository

import com.relun.app.data.model.Person
import com.relun.app.data.model.toPerson
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class LockedPeople(
    val locked: Boolean,
    val count: Int,
    val people: List<Person>,
    /** Profile views only: when each person (by id) last viewed the user. */
    val viewedAt: Map<String, Instant> = emptyMap(),
)

/** Something changed between the user and another person; open lists update from these. */
sealed interface PeopleEvent {
    data class Liked(val userId: String, val isMatch: Boolean) : PeopleEvent
    data class Unliked(val userId: String) : PeopleEvent
    data class Passed(val userId: String) : PeopleEvent
    data class Blocked(val userId: String) : PeopleEvent
    data class Unblocked(val userId: String) : PeopleEvent
    data class ChatUnlocked(val userId: String) : PeopleEvent
    data object InsightsUnlocked : PeopleEvent
    data object MatchesChanged : PeopleEvent
}

/** Discover, profiles, likes and matches. Emits [newMatches] when a like is mutual. */
class PeopleRepository(private val api: ApiService) {

    private val _newMatches = MutableSharedFlow<Person>(extraBufferCapacity = 4)
    val newMatches: SharedFlow<Person> = _newMatches.asSharedFlow()

    private val _events = MutableSharedFlow<PeopleEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<PeopleEvent> = _events.asSharedFlow()

    suspend fun discover(latitude: Double?, longitude: Double?) =
        apiCall { api.discover(latitude, longitude) }.map { res -> res.users.map { it.toPerson() } }

    suspend fun person(userId: String) = apiCall { api.person(userId) }.map { it.toPerson() }

    /** Likes [person]. Returns true when it made a match. */
    suspend fun like(person: Person): Result<Boolean> = apiCall { api.like(person.id) }.map { res ->
        if (res.isMutual && !res.alreadyLiked) {
            _newMatches.tryEmit(person.copy(isMatch = true, liked = true))
        }
        _events.tryEmit(PeopleEvent.Liked(person.id, res.isMutual))
        res.isMutual
    }

    suspend fun unlike(userId: String) = apiCall { api.unlike(userId) }
        .onSuccess { _events.tryEmit(PeopleEvent.Unliked(userId)) }

    suspend fun pass(userId: String) = apiCall { api.pass(userId) }
        .onSuccess { _events.tryEmit(PeopleEvent.Passed(userId)) }

    suspend fun matches() = apiCall { api.matches() }.map { res ->
        res.matches.map { it.toPerson().copy(isMatch = true) }
    }

    suspend fun receivedLikes() = apiCall { api.receivedLikes() }.map {
        LockedPeople(it.locked, it.count, it.likes.map { card -> card.toPerson() })
    }

    suspend fun profileViews() = apiCall { api.profileViews() }.map {
        LockedPeople(
            it.locked,
            it.count,
            it.views.map { card -> card.toPerson() },
            it.views.mapNotNull { card -> card.viewedAt?.let { at -> runCatching { card.user.id to Instant.parse(at) }.getOrNull() } }.toMap(),
        )
    }

    fun emit(event: PeopleEvent) {
        _events.tryEmit(event)
    }
}
