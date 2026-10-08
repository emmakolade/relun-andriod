package com.relun.app.data.repository

import com.relun.app.data.model.PurchaseBody
import com.relun.app.data.model.Wallet
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Coin balance and the things coins buy. The balance always comes from the server. */
class CoinsRepository(private val api: ApiService) {

    private val _wallet = MutableStateFlow(Wallet())
    val wallet: StateFlow<Wallet> = _wallet.asStateFlow()

    suspend fun refresh() = apiCall { api.wallet() }.onSuccess { res ->
        _wallet.value = Wallet(
            balance = res.balance,
            insightsActive = res.insightsActive,
            chatUnlockCost = res.costs.chatUnlock,
            insightsCost = res.costs.insights,
            packages = res.packages,
            pendingBonus = res.pendingBonus,
        )
    }

    /** The welcome sheet was shown; the server stops reporting this bonus. */
    suspend fun markBonusSeen(id: String) {
        _wallet.update { it.copy(pendingBonus = null) }
        apiCall { api.markBonusSeen(id) }
    }

    suspend fun unlockChat(userId: String) = apiCall { api.unlockChat(userId) }.onSuccess { res ->
        _wallet.update { it.copy(balance = res.balance) }
    }

    suspend fun buyInsights() = apiCall { api.buyInsights() }.onSuccess { res ->
        _wallet.update { it.copy(balance = res.balance, insightsActive = true) }
    }

    /** Hands a Play purchase to the server, which verifies it and credits coins. */
    suspend fun confirmPurchase(productId: String, purchaseToken: String) =
        apiCall { api.confirmPurchase(PurchaseBody(productId, purchaseToken)) }.onSuccess { res ->
            _wallet.update { it.copy(balance = res.balance) }
        }

    fun clear() {
        _wallet.value = Wallet()
    }
}
