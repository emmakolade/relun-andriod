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
    /** [then] is the person whose chat the user was trying to unlock, if any. */
    data class Coins(val then: Person? = null) : MainSheet
    data class Unlock(val person: Person) : MainSheet
    data object Insights : MainSheet
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
    val unlocking: Boolean = false,
    /** Bumped to ask the Dates tab to show "My Dates". */
    val showMyDates: Int = 0,
    /** Bumped to ask the Messages tab to show "Likes You". */
    val showLikes: Int = 0,
)

/**
 * The signed-in shell: tab selection, the coin and safety sheets, the match
 * celebration, and the purchase flow. Screens reach it through [AppActions].
 */
class MainViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    val wallet = c.coins.wallet
    val prices = c.billing.prices

    private val _nav = MutableSharedFlow<NavEvent>(extraBufferCapacity = 4)
    val nav: SharedFlow<NavEvent> = _nav.asSharedFlow()

    init {
        viewModelScope.launch { c.people.newMatches.collect { p -> _state.update { it.copy(match = p) } } }
        viewModelScope.launch {
            c.coins.wallet.collect { w ->
                val bonus = w.pendingBonus ?: return@collect
                _state.update { s -> if (s.sheet == null && s.match == null) s.copy(sheet = MainSheet.Bonus(bonus)) else s }
            }
        }
        viewModelScope.launch {
            c.billing.events.collect { event ->
                when (event) {
                    is PurchaseEvent.Completed -> {
                        _state.update { s ->
                            val next = (s.sheet as? MainSheet.Coins)?.then?.let { MainSheet.Unlock(it) }
                            s.copy(buying = false, sheet = next)
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

    fun openCoins(then: Person? = null) {
        _state.update { it.copy(sheet = MainSheet.Coins(then)) }
        viewModelScope.launch { c.coins.refresh() }
    }

    fun openInsights() = _state.update { it.copy(sheet = MainSheet.Insights) }
    fun openMore(person: Person) = _state.update { it.copy(sheet = MainSheet.More(person)) }
    fun closeSheet() {
        (_state.value.sheet as? MainSheet.Bonus)?.let { viewModelScope.launch { c.coins.markBonusSeen(it.bonus.id) } }
        _state.update { it.copy(sheet = null, buying = false) }
    }
    fun dismissMatch() = _state.update { it.copy(match = null) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }
    fun selectPackage(index: Int) = _state.update { it.copy(selectedPackage = index) }

    /** Opens the chat if it is already paid for; otherwise asks to unlock it. */
    fun openChat(person: Person) {
        _state.update { it.copy(match = null) }
        if (person.chatUnlocked) {
            _nav.tryEmit(NavEvent.OpenChat(person.id, person.name))
            return
        }
        viewModelScope.launch {
            // The card may be stale; ask the server before charging anything.
            val fresh = c.people.person(person.id).getOrNull() ?: person
            when {
                !fresh.isMatch -> c.messenger.info("You can message ${person.firstName} once you match.")
                fresh.chatUnlocked -> _nav.tryEmit(NavEvent.OpenChat(fresh.id, fresh.name))
                else -> _state.update { it.copy(sheet = MainSheet.Unlock(fresh)) }
            }
        }
    }

    fun unlock(person: Person) {
        if (wallet.value.balance < wallet.value.chatUnlockCost) {
            openCoins(then = person)
            return
        }
        _state.update { it.copy(unlocking = true) }
        viewModelScope.launch {
            c.coins.unlockChat(person.id)
                .onSuccess { res ->
                    _state.update { it.copy(unlocking = false, sheet = null) }
                    if (res.charged > 0) c.messenger.success("Chat with ${person.firstName} unlocked · −${res.charged} coins")
                    c.people.emit(PeopleEvent.ChatUnlocked(person.id))
                    _nav.tryEmit(NavEvent.OpenChat(person.id, person.name))
                }
                .onFailure { error ->
                    _state.update { it.copy(unlocking = false) }
                    if (error is com.relun.app.data.network.ApiException && error.isInsufficientCoins) {
                        openCoins(then = person)
                    } else {
                        c.messenger.error(error.message ?: "Couldn’t unlock this chat.")
                    }
                }
        }
    }

    fun buyInsights() {
        if (wallet.value.balance < wallet.value.insightsCost) {
            openCoins()
            return
        }
        viewModelScope.launch {
            c.coins.buyInsights()
                .onSuccess {
                    _state.update { it.copy(sheet = null) }
                    c.messenger.success("Likes & Views unlocked for 30 days")
                    c.people.emit(PeopleEvent.InsightsUnlocked)
                }
                .onFailure { c.messenger.error(it.message ?: "Couldn’t unlock insights.") }
        }
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
