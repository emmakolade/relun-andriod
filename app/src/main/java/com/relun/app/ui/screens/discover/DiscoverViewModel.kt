package com.relun.app.ui.screens.discover

import com.relun.app.data.repository.isLikeLimit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relun.app.data.model.Person
import com.relun.app.data.model.SettingsPatch
import com.relun.app.data.network.ApiException
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.di.AppContainer
import com.relun.app.ui.screens.main.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DiscoverView { Grid, List }

data class DiscoverState(
    val load: LoadState = LoadState.Loading,
    val people: List<Person> = emptyList(),
    val offline: Boolean = false,
    val refreshing: Boolean = false,
    val view: DiscoverView = DiscoverView.Grid,
    val maxDistanceKm: Int = 35,
    val ageMin: Int = 18,
    val ageMax: Int = 99,
    val filtersOpen: Boolean = false,
)

class DiscoverViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            c.settings.settings.collect { s ->
                _state.update { it.copy(maxDistanceKm = s.maxDistanceKm, ageMin = s.ageMin, ageMax = s.ageMax) }
            }
        }
        viewModelScope.launch {
            c.people.events.collect { event ->
                when (event) {
                    is PeopleEvent.Blocked -> remove(event.userId)
                    is PeopleEvent.Passed -> remove(event.userId)
                    // A match moves to Messages, so it leaves Discover.
                    is PeopleEvent.Liked -> if (event.isMatch) remove(event.userId) else setLiked(event.userId, true)
                    is PeopleEvent.Unliked -> setLiked(event.userId, false)
                    // Sending a message request likes them too.
                    is PeopleEvent.RequestSent -> setLiked(event.userId, true)
                    else -> Unit
                }
            }
        }
    }

    fun load(refresh: Boolean = false) {
        _state.update {
            if (refresh) it.copy(refreshing = true)
            else it.copy(load = if (it.people.isEmpty()) LoadState.Loading else it.load)
        }
        viewModelScope.launch {
            val coords = c.location.last ?: c.location.current()
            c.people.discover(coords?.latitude, coords?.longitude)
                .onSuccess { people ->
                    _state.update {
                        it.copy(
                            people = people,
                            load = if (people.isEmpty()) LoadState.Empty else LoadState.Ready,
                            offline = false,
                            refreshing = false,
                        )
                    }
                }
                .onFailure { error ->
                    val network = (error as? ApiException)?.isNetwork == true
                    _state.update {
                        if (it.people.isNotEmpty() && network) it.copy(offline = true, refreshing = false)
                        else it.copy(load = LoadState.Error, offline = network, refreshing = false)
                    }
                }
        }
    }

    fun setView(view: DiscoverView) = _state.update { it.copy(view = view) }

    fun like(person: Person) {
        if (person.liked) {
            setLiked(person.id, false)
            viewModelScope.launch {
                c.people.unlike(person.id).onFailure { setLiked(person.id, true) }
            }
            return
        }
        setLiked(person.id, true)
        viewModelScope.launch {
            c.people.like(person).onFailure { e ->
                setLiked(person.id, false)
                // Out of likes: the shell shows the Plus offer instead.
                if (!e.isLikeLimit) c.messenger.error(e.message ?: "Couldn’t like ${person.firstName}.")
            }
        }
    }

    fun pass(person: Person) {
        remove(person.id)
        viewModelScope.launch { c.people.pass(person.id) }
    }

    fun openFilters(open: Boolean) = _state.update { it.copy(filtersOpen = open) }

    fun applyFilters(distance: Int, ageMin: Int, ageMax: Int) {
        _state.update { it.copy(filtersOpen = false, load = LoadState.Loading, people = emptyList()) }
        viewModelScope.launch {
            val current = c.settings.settings.value
            c.settings.update(
                SettingsPatch(maxDistanceKm = distance, ageMin = ageMin, ageMax = ageMax),
                current.copy(maxDistanceKm = distance, ageMin = ageMin, ageMax = ageMax),
            )
            load()
        }
    }

    private fun remove(userId: String) = _state.update { s ->
        val left = s.people.filterNot { it.id == userId }
        s.copy(people = left, load = if (left.isEmpty() && s.load == LoadState.Ready) LoadState.Empty else s.load)
    }

    private fun setLiked(userId: String, liked: Boolean) = _state.update { s ->
        s.copy(people = s.people.map { if (it.id == userId) it.copy(liked = liked) else it })
    }
}
