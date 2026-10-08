package com.relun.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.relun.app.data.repository.CoinsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PurchaseEvent {
    data class Completed(val coins: Int, val balance: Int) : PurchaseEvent
    data object Pending : PurchaseEvent
    data object Cancelled : PurchaseEvent
    data class Failed(val message: String) : PurchaseEvent
}

/**
 * Google Play coin purchases. A purchase is only consumed after the backend has
 * verified it and credited the coins, so a crash in between is recovered on the
 * next launch by [reconcile] instead of losing the user's money.
 */
class BillingManager(
    context: Context,
    private val coins: CoinsRepository,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val products = mutableMapOf<String, ProductDetails>()

    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Localised store prices by product id. Empty until Play answers. */
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    private var ready: CompletableDeferred<Boolean>? = null

    private suspend fun ensureConnected(): Boolean {
        if (client.isReady) return true
        val pending = ready?.takeIf { it.isActive } ?: CompletableDeferred<Boolean>().also { deferred ->
            ready = deferred
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    deferred.complete(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (deferred.isActive) deferred.complete(false)
                }
            })
        }
        return pending.await()
    }

    suspend fun loadProducts(productIds: List<String>) {
        if (productIds.isEmpty() || !ensureConnected()) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()
        val result = client.queryProductDetails(params)
        result.productDetailsList.orEmpty().forEach { products[it.productId] = it }
        _prices.value = products.mapValues { (_, details) ->
            details.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty()
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        scope.launch {
            if (!ensureConnected()) {
                _events.tryEmit(PurchaseEvent.Failed("Google Play isn’t available right now."))
                return@launch
            }
            if (products[productId] == null) loadProducts(listOf(productId))
            val details = products[productId]
            if (details == null) {
                _events.tryEmit(PurchaseEvent.Failed("This package isn’t available in your Play Store yet."))
                return@launch
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
                )
                .build()
            val result = client.launchBillingFlow(activity, params)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _events.tryEmit(PurchaseEvent.Failed(result.debugMessage.ifBlank { "Couldn’t start the purchase." }))
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { handle(it, notify = true) }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(PurchaseEvent.Cancelled)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { reconcile() }
            else -> _events.tryEmit(PurchaseEvent.Failed(result.debugMessage.ifBlank { "Purchase failed." }))
        }
    }

    /** Finishes any purchase that was paid for but never credited, e.g. after a crash. */
    suspend fun reconcile() {
        if (!ensureConnected()) return
        val owned = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        owned.purchasesList.forEach { handle(it, notify = false) }
    }

    private fun handle(purchase: Purchase, notify: Boolean) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> if (notify) _events.tryEmit(PurchaseEvent.Pending)
            Purchase.PurchaseState.PURCHASED -> scope.launch {
                val productId = purchase.products.firstOrNull() ?: return@launch
                coins.confirmPurchase(productId, purchase.purchaseToken)
                    .onSuccess { res ->
                        val consumed = client.consumePurchase(
                            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        )
                        if (consumed.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                            Log.w(TAG, "Consume failed: ${consumed.billingResult.debugMessage}")
                        }
                        if (notify || res.credited > 0) {
                            _events.tryEmit(PurchaseEvent.Completed(res.credited, res.balance))
                        }
                    }
                    .onFailure { error ->
                        // Not consumed, so reconcile() retries it next launch.
                        if (notify) _events.tryEmit(PurchaseEvent.Failed(error.message ?: "Couldn’t confirm the purchase."))
                    }
            }
            else -> Unit
        }
    }

    companion object {
        private const val TAG = "Billing"
    }
}
