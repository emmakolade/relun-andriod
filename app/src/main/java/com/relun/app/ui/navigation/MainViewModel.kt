package com.relun.app.ui.navigation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relun.app.billing.PurchaseEvent
import com.relun.app.data.model.PendingBonusDto
import com.relun.app.data.model.Person
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.di.AppContainer
import com.relun.app.ui.components.DialogSpec
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Tab { Discover, Dates, Messages, Me }

sealed interface MainSheet {
    /** [then] is the sheet to go back to once coins are bought, e.g. the request the user was writing. */
    data class Coins(val then: MainSheet? = null) : MainSheet
    /** Messaging someone without a match. */
    data class Request(val person: Person) : MainSheet
    /** Likes & Views: Plus, or a coin pass. */
    data object Insights : MainSheet
    /** Relun Plus: plans, or the user's own status. */
    data object Plus : MainSheet
    /** Out of free likes for today. */
    data object LikeLimit : MainSheet
    data class More(val person: Person) : MainSheet
    data class Bonus(val bonus: PendingBonusDto) : MainSheet
}

sealed interface NavEvent {
    data class OpenChat(val userId: String, val name: String) : NavEvent
    data object BackToMain : NavEvent
}

data class MainUiState(
    val tab: Tab = Tab.Discover,
    val sheet: MainSheet? = null,
    val match: Person? = null,
    val dialog: DialogSpec? = null,
    val selectedPackage: Int = 1,
    val buying: Boolean = false,
    /** A Likes & Views pass being bought. */
    val buyingInsights: Boolean = false,
    /** The message request being written; kept while the user tops up coins. */
    val requestDraft: String = "",
    val sendingRequest: Boolean = false,
    /** Bumped to ask the Dates tab to show "My Dates". */
    val showMyDates: Int = 0,
    /** Bumped to ask the Messages tab to show "Likes You". */
    val showLikes: Int = 0,
)

/**
 * The signed-in shell: tab selection, the coin, Plus and safety sheets, the
 * match celebration, and the purchase flows. Screens reach it through [AppActions].
 */
class MainViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    val wallet = c.coins.wallet
    val prices = c.billing.prices
    val plusPrices = c.billing.plusPrices

    private val _nav = MutableSharedFlow<NavEvent>(extraBufferCapacity = 4)
    val nav: SharedFlow<NavEvent> = _nav.asSharedFlow()

    init {
        viewModelScope.launch { c.people.newMatches.collect { p -> _state.update { it.copy(match = p) } } }
        viewModelScope.launch {
            c.people.likeLimitReached.collect {
                c.coins.refresh()
                _state.update { s -> if (s.sheet == null) s.copy(sheet = MainSheet.LikeLimit) else s }
            }
        }
        viewModelScope.launch {
            c.coins.wallet.collect { w ->
                val bonus = w.pendingBonus ?: return@collect
                _state.update { s -> if (s.sheet == null && s.match == null) s.copy(sheet = MainSheet.Bonus(bonus)) else s }
            }
        }
        viewModelScope.launch {
            c.billing.events.collect { event ->
                when (event) {
                    PurchaseEvent.PlusActive -> {
                        _state.update { it.copy(buying = false, sheet = null) }
                        c.coins.refresh()
                        c.people.emit(PeopleEvent.PlusChanged)
                        c.messenger.success("Welcome to Relun Plus")
                    }
                    is PurchaseEvent.Completed -> {
                        _state.update { s ->
                            s.copy(buying = false, sheet = (s.sheet as? MainSheet.Coins)?.then)
                        }
                        if (event.coins > 0) c.messenger.success("${event.coins} coins added")
                    }
                    PurchaseEvent.Pending -> {
                        _state.update { it.copy(buying = false) }
                        c.messenger.info("Your payment is pending. Coins arrive as soon as it clears.")
                    }
                    PurchaseEvent.Cancelled -> _state.update { it.copy(buying = false) }
                    is PurchaseEvent.Failed -> {
                        _state.update { it.copy(buying = false) }
                        c.messenger.error(event.message)
                    }
                }
            }
        }
    }

    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab) }

    fun showDates() = _state.update { it.copy(tab = Tab.Dates, showMyDates = it.showMyDates + 1) }
    fun showLikes() = _state.update { it.copy(tab = Tab.Messages, showLikes = it.showLikes + 1) }

    fun openCoins(then: MainSheet? = null) {
        _state.update { it.copy(sheet = MainSheet.Coins(then)) }
        viewModelScope.launch { c.coins.refresh() }
    }

    fun openInsights() = _state.update { it.copy(sheet = MainSheet.Insights) }
    fun openPlus() {
        _state.update { it.copy(sheet = MainSheet.Plus) }
        viewModelScope.launch { c.coins.refresh() }
    }
    fun openMore(person: Person) = _state.update { it.copy(sheet = MainSheet.More(person)) }
    fun closeSheet() {
        (_state.value.sheet as? MainSheet.Bonus)?.let { viewModelScope.launch { c.coins.markBonusSeen(it.bonus.id) } }
        _state.update { it.copy(sheet = null, buying = false) }
    }
    fun dismissMatch() = _state.update { it.copy(match = null) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }
    fun selectPackage(index: Int) = _state.update { it.copy(selectedPackage = index) }

    /**
     * Opens the chat with a match or over a message request; for anyone else,
     * offers to send a request.
     */
    fun openChat(person: Person) {
        _state.update { it.copy(match = null) }
        if (person.isMatch || person.messageRequest != null) {
            _nav.tryEmit(NavEvent.OpenChat(person.id, person.name))
            return
        }
        viewModelScope.launch {
            // The card may be stale; ask the server before offering to charge anything.
            val fresh = c.people.person(person.id).getOrNull() ?: person
            when {
                fresh.isMatch || fresh.messageRequest != null -> _nav.tryEmit(NavEvent.OpenChat(fresh.id, fresh.name))
                fresh.acceptsMessageRequests -> _state.update { it.copy(sheet = MainSheet.Request(fresh), requestDraft = "") }
                else -> c.messenger.info(
                    "${fresh.firstName} only gets messages from matches. Like them, and you can chat once they like you back.",
                )
            }
        }
    }

    fun setRequestDraft(text: String) = _state.update { it.copy(requestDraft = text.take(1000)) }

    fun sendRequest(person: Person) {
        val text = _state.value.requestDraft.trim()
        if (text.isEmpty() || _state.value.sendingRequest) return
        val free = (wallet.value.requests.left ?: 0) > 0
        if (!free && wallet.value.balance < wallet.value.messageRequestCost) {
            openCoins(then = MainSheet.Request(person))
            return
        }
        _state.update { it.copy(sendingRequest = true) }
        viewModelScope.launch {
            c.chat.sendRequest(person.id, text)
                .onSuccess { res ->
                    c.coins.setBalance(res.balance)
                    // The free allowance changed; the server has the count.
                    launch { c.coins.refresh() }
                    _state.update { it.copy(sendingRequest = false, sheet = null, requestDraft = "") }
                    if (res.matched) {
                        c.people.emit(PeopleEvent.Liked(person.id, isMatch = true))
                        c.people.emit(PeopleEvent.Matched(person.id))
                        c.messenger.success("It's a match! ${person.firstName} already liked you")
                    } else {
                        c.people.emit(PeopleEvent.RequestSent(person.id))
                        c.messenger.success(
                            if (res.charged > 0) "Message sent to ${person.firstName} · −${res.charged} coins"
                            else "Message sent to ${person.firstName} · free",
                        )
                    }
                    _nav.tryEmit(NavEvent.OpenChat(person.id, person.name))
                }
                .onFailure { error ->
                    _state.update { it.copy(sendingRequest = false) }
                    val api = error as? com.relun.app.data.network.ApiException
                    when {
                        api?.isInsufficientCoins == true -> openCoins(then = MainSheet.Request(person))
                        api?.code == "REQUEST_EXISTS" || api?.code == "ALREADY_MATCHED" -> {
                            _state.update { it.copy(sheet = null) }
                            _nav.tryEmit(NavEvent.OpenChat(person.id, person.name))
                        }
                        else -> c.messenger.error(error.message ?: "Couldn’t send your message.")
                    }
                }
        }
    }

    /** Likes & Views for coins, for people who'd rather not subscribe. */
    fun buyInsights(days: Int) {
        val cost = if (days == 7) wallet.value.insights7Cost else wallet.value.insights30Cost
        if (wallet.value.balance < cost) {
            openCoins(then = MainSheet.Insights)
            return
        }
        if (_state.value.buyingInsights) return
        _state.update { it.copy(buyingInsights = true) }
        viewModelScope.launch {
            c.coins.buyInsights(days)
                .onSuccess {
                    _state.update { it.copy(sheet = null, buyingInsights = false) }
                    c.messenger.success("Likes & Views unlocked for $days days")
                    c.people.emit(PeopleEvent.InsightsUnlocked)
                }
                .onFailure { error ->
                    _state.update { it.copy(buyingInsights = false) }
                    if (error is com.relun.app.data.network.ApiException && error.isInsufficientCoins) {
                        openCoins(then = MainSheet.Insights)
                    } else {
                        c.messenger.error(error.message ?: "Couldn’t unlock Likes & Views.")
                    }
                }
        }
    }

    /** Starts Relun Plus through Google Play on [basePlanId] ("weekly" or "monthly"). */
    fun buyPlus(activity: Activity, basePlanId: String) {
        _state.update { it.copy(buying = true) }
        c.billing.launchPlus(activity, basePlanId)
    }

    fun buy(activity: Activity) {
        val pkg = wallet.value.packages.getOrNull(_state.value.selectedPackage) ?: return
        _state.update { it.copy(buying = true) }
        c.billing.launchPurchase(activity, pkg.productId)
    }

    fun confirmReport(person: Person) = _state.update {
        it.copy(
            sheet = null,
            dialog = DialogSpec(
                title = "Report ${person.firstName}?",
                body = "We review every report within 24 hours. They won’t know it was you.",
                confirm = "Send report",
                destructive = true,
                icon = null,
                onConfirm = { report(person) },
            ),
        )
    }

    fun confirmBlock(person: Person) = _state.update {
        it.copy(
            sheet = null,
            dialog = DialogSpec(
                title = "Block ${person.firstName}?",
                body = "They won’t be able to see your profile or message you. You can unblock in Settings.",
                confirm = "Block",
                destructive = true,
                onConfirm = { block(person) },
            ),
        )
    }

    private fun report(person: Person) = viewModelScope.launch {
        c.safety.report(person.id, "other", null)
            .onSuccess { c.messenger.success("Report sent. Thanks for keeping Relun safe.") }
            .onFailure { c.messenger.error(it.message ?: "Couldn’t send the report.") }
    }

    private fun block(person: Person) = viewModelScope.launch {
        c.safety.block(person.id)
            .onSuccess {
                c.messenger.info("${person.firstName} is blocked. They can’t see or message you.")
                _nav.tryEmit(NavEvent.BackToMain)
                c.chat.refreshUnread()
            }
            .onFailure { c.messenger.error(it.message ?: "Couldn’t block.") }
    }

    fun showDialog(spec: DialogSpec) = _state.update { it.copy(dialog = spec) }
}
