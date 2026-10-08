package com.relun.app.ui.screens.dates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relun.app.data.model.DatePost
import com.relun.app.data.model.DateRequestStatus
import com.relun.app.data.network.ApiException
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.di.AppContainer
import com.relun.app.ui.components.DialogSpec
import com.relun.app.ui.screens.main.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class DatesView { Browse, Mine }

data class DatesState(
    val view: DatesView = DatesView.Browse,
    val browseLoad: LoadState = LoadState.Loading,
    val browse: List<DatePost> = emptyList(),
    val mine: List<DatePost> = emptyList(),
    val mineLoaded: Boolean = false,
    val offline: Boolean = false,
    val creating: Boolean = false,
    val posting: Boolean = false,
    val dialog: DialogSpec? = null,
)

class DatesViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(DatesState())
    val state: StateFlow<DatesState> = _state.asStateFlow()

    val wallet = c.coins.wallet

    init {
        loadBrowse()
        loadMine()
        viewModelScope.launch {
            c.people.events.collect { e ->
                if (e is PeopleEvent.Blocked) _state.update { s -> s.copy(browse = s.browse.filterNot { it.owner?.id == e.userId }) }
            }
        }
    }

    fun setView(view: DatesView) {
        _state.update { it.copy(view = view) }
        if (view == DatesView.Mine) loadMine() else loadBrowse(quiet = true)
    }

    fun loadBrowse(quiet: Boolean = false) {
        if (!quiet) _state.update { it.copy(browseLoad = LoadState.Loading) }
        viewModelScope.launch {
            c.dates.browse()
                .onSuccess { list ->
                    _state.update { it.copy(browse = list, browseLoad = if (list.isEmpty()) LoadState.Empty else LoadState.Ready, offline = false) }
                }
                .onFailure { e ->
                    val network = (e as? ApiException)?.isNetwork == true
                    _state.update {
                        if (it.browse.isNotEmpty()) it.copy(offline = network) else it.copy(browseLoad = LoadState.Error, offline = network)
                    }
                }
        }
    }

    fun loadMine() {
        viewModelScope.launch {
            c.dates.mine().onSuccess { list -> _state.update { it.copy(mine = list, mineLoaded = true) } }
        }
    }

    fun openCreate(open: Boolean) = _state.update { it.copy(creating = open) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }

    /** Safety reminder first, then the confirmation, then the request. */
    fun interested(post: DatePost) {
        val owner = post.owner ?: return
        _state.update {
            it.copy(
                dialog = DialogSpec(
                    title = "Before you meet",
                    body = "Please make sure your first meeting is in a public, open place, and tell a friend where you’re going.",
                    confirm = "I understand",
                    icon = Icons.Rounded.Shield,
                    onConfirm = {
                        _state.update { s ->
                            s.copy(
                                dialog = DialogSpec(
                                    title = "Send interest?",
                                    body = "Send interest to ${owner.firstName} for “${post.activity}”?",
                                    confirm = "Send interest",
                                    onConfirm = { join(post) },
                                )
                            )
                        }
                    },
                )
            )
        }
    }

    private fun join(post: DatePost) {
        setBrowseStatus(post.id, DateRequestStatus.Pending)
        viewModelScope.launch {
            c.dates.join(post.id)
                .onSuccess { c.messenger.success("Interest sent to ${post.owner?.firstName ?: "them"}") }
                .onFailure { e ->
                    setBrowseStatus(post.id, null)
                    c.messenger.error(e.message ?: "Couldn’t send interest.")
                }
        }
    }

    fun post(activity: String, place: String, at: Instant, description: String?) {
        _state.update { it.copy(posting = true) }
        viewModelScope.launch {
            c.dates.create(activity, place, at, description)
                .onSuccess { (created, charged) ->
                    _state.update { it.copy(posting = false, creating = false, view = DatesView.Mine, mine = listOf(created) + it.mine) }
                    // A free slot was used, or coins spent; the server has the counts.
                    launch { c.coins.refresh() }
                    c.messenger.success(if (charged > 0) "Your date is live · −$charged coins" else "Your date is live")
                }
                .onFailure { e ->
                    _state.update { it.copy(posting = false) }
                    c.messenger.error(e.message ?: "Couldn’t post your date.")
                }
        }
    }

    fun confirmDelete(post: DatePost) = _state.update {
        it.copy(
            dialog = DialogSpec(
                title = "Delete this date?",
                body = "People who asked to join will be notified.",
                confirm = "Delete",
                destructive = true,
                onConfirm = { delete(post) },
            )
        )
    }

    private fun delete(post: DatePost) {
        val before = _state.value.mine
        _state.update { s -> s.copy(mine = s.mine.filterNot { it.id == post.id }) }
        viewModelScope.launch {
            c.dates.delete(post.id)
                .onSuccess {
                    // Frees one of the free active slots.
                    launch { c.coins.refresh() }
                    c.messenger.info("Date removed")
                }
                .onFailure { e ->
                    _state.update { it.copy(mine = before) }
                    c.messenger.error(e.message ?: "Couldn’t delete the date.")
                }
        }
    }

    fun respond(post: DatePost, requestId: String, accept: Boolean) {
        val status = if (accept) DateRequestStatus.Accepted else DateRequestStatus.Declined
        setRequestStatus(post.id, requestId, status)
        viewModelScope.launch {
            c.dates.respond(post.id, requestId, accept)
                .onSuccess {
                    if (accept) {
                        val who = post.requests.firstOrNull { it.id == requestId }?.person
                        c.messenger.success("You and ${who?.firstName ?: "they"} can now chat")
                        c.people.emit(PeopleEvent.MatchesChanged)
                    }
                }
                .onFailure { e ->
                    setRequestStatus(post.id, requestId, DateRequestStatus.Pending)
                    c.messenger.error(e.message ?: "Couldn’t update the request.")
                }
        }
    }

    private fun setBrowseStatus(dateId: String, status: DateRequestStatus?) = _state.update { s ->
        s.copy(browse = s.browse.map { if (it.id == dateId) it.copy(myRequestStatus = status) else it })
    }

    private fun setRequestStatus(dateId: String, requestId: String, status: DateRequestStatus) = _state.update { s ->
        s.copy(mine = s.mine.map { d ->
            if (d.id != dateId) d else d.copy(requests = d.requests.map { if (it.id == requestId) it.copy(status = status) else it })
        })
    }
}
