package com.relun.app.data.repository

import com.relun.app.data.model.EntitlementsResponse
import com.relun.app.data.model.InsightsBody
import com.relun.app.data.model.PlayPlusBody
import com.relun.app.data.model.PurchaseBody
import com.relun.app.data.model.Wallet
import com.relun.app.data.model.toAllowance
import com.relun.app.data.model.toPlus
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Coin balance, Relun Plus and the free allowances. All of it comes from the server. */
class CoinsRepository(private val api: ApiService) {

    private val _wallet = MutableStateFlow(Wallet())
    val wallet: StateFlow<Wallet> = _wallet.asStateFlow()

    suspend fun refresh() = apiCall { api.wallet() }.onSuccess { res ->
        _wallet.value = Wallet(
            balance = res.balance,
            insightsActive = res.insightsActive,
            messageRequestCost = res.costs.messageRequest,
            insights7Cost = res.costs.insights7,
            insights30Cost = res.costs.insights30,
            datePostCost = res.costs.datePost,
            packages = res.packages,
            pendingBonus = res.pendingBonus,
            plus = res.plus.toPlus(),
            plans = res.plans,
            plusPerks = res.plusPerks,
            likes = res.allowances.likes.toAllowance(),
            requests = res.allowances.requests.toAllowance(),
            dates = res.allowances.dates.toAllowance(),
        )
    }

    private fun apply(res: EntitlementsResponse) = _wallet.update {
        it.copy(
            plus = res.plus.toPlus(),
            plans = res.plans,
            plusPerks = res.plusPerks,
            likes = res.allowances.likes.toAllowance(),
            requests = res.allowances.requests.toAllowance(),
            dates = res.allowances.dates.toAllowance(),
        )
    }

    /** Hands a Play subscription to the server, which verifies it and starts or extends Plus. */
    suspend fun confirmPlayPlus(purchaseToken: String, basePlanId: String?) =
        apiCall { api.confirmPlayPlus(PlayPlusBody(purchaseToken, basePlanId)) }.onSuccess { apply(it) }

    /** The welcome sheet was shown; the server stops reporting this bonus. */
    suspend fun markBonusSeen(id: String) {
        _wallet.update { it.copy(pendingBonus = null) }
        apiCall { api.markBonusSeen(id) }
    }

    /** Likes & Views for coins, for 7 or 30 days. */
    suspend fun buyInsights(days: Int) = apiCall { api.buyInsights(InsightsBody(days)) }.onSuccess { res ->
        _wallet.update { it.copy(balance = res.balance, insightsActive = true) }
    }

    /** Hands a Play purchase to the server, which verifies it and credits coins. */
    suspend fun confirmPurchase(productId: String, purchaseToken: String) =
        apiCall { api.confirmPurchase(PurchaseBody(productId, purchaseToken)) }.onSuccess { res ->
            _wallet.update { it.copy(balance = res.balance) }
        }

    /** After a spend reported its new balance, such as a message request. */
    fun setBalance(balance: Int) {
        _wallet.update { it.copy(balance = balance) }
    }

    fun clear() {
        _wallet.value = Wallet()
    }
}
