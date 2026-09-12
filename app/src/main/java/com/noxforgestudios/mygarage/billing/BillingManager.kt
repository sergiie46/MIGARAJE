package com.noxforgestudios.mygarage.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.noxforgestudios.mygarage.BuildConfig
import com.noxforgestudios.mygarage.domain.ProState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(context: Context) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(ProState())
    val state: StateFlow<ProState> = _state.asStateFlow()
    private var productDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (billingClient.isReady) {
            refresh()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else _state.value = _state.value.copy(message = result.debugMessage.ifBlank { "Google Play Billing no disponible" })
            }
            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(message = "Google Play Billing desconectado")
            }
        })
    }

    fun refresh() {
        if (!billingClient.isReady) {
            start()
            return
        }
        queryProduct()
        queryExistingPurchases()
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList.firstOrNull { it.productId == BuildConfig.PRO_PRODUCT_ID }
                val offer = productDetails?.oneTimePurchaseOfferDetails
                    ?: productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                _state.value = _state.value.copy(
                    productAvailable = productDetails != null,
                    priceText = offer?.formattedPrice,
                    message = if (productDetails == null) "Producto migaraje_pro no disponible en esta instalación" else null
                )
            } else {
                _state.value = _state.value.copy(productAvailable = false, message = result.debugMessage)
            }
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases)
            else _state.value = _state.value.copy(message = result.debugMessage)
        }
    }

    fun launchPurchase(activity: Activity): BillingResult? {
        val details = productDetails ?: run {
            _state.value = _state.value.copy(message = "Producto no disponible. Publícalo/actívalo en Play Console y prueba desde Google Play")
            return null
        }
        val offer = details.oneTimePurchaseOfferDetails ?: details.oneTimePurchaseOfferDetailsList?.firstOrNull()
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                val offerToken = offer?.offerToken
                if (!offerToken.isNullOrBlank()) setOfferToken(offerToken)
            }
            .build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()
        return billingClient.launchBillingFlow(activity, params).also { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(message = result.debugMessage)
            }
        }
    }

    fun restorePurchases() = refresh()

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.value = _state.value.copy(message = "Compra cancelada")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryExistingPurchases()
            else -> _state.value = _state.value.copy(message = result.debugMessage.ifBlank { "No se pudo completar la compra" })
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val matching = purchases.filter { it.products.contains(BuildConfig.PRO_PRODUCT_ID) }
        val purchased = matching.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = matching.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        if (purchased != null && !purchased.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchased.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { ack ->
                if (ack.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(isPro = true, purchasePending = false, message = "Mi Garaje PRO activado")
                } else {
                    _state.value = _state.value.copy(isPro = false, message = "La compra existe, pero falta reconocerla: ${ack.debugMessage}")
                }
            }
        } else {
            _state.value = _state.value.copy(
                isPro = purchased != null,
                purchasePending = pending,
                message = if (pending) "Compra pendiente de confirmación" else _state.value.message
            )
        }
    }

    fun close() {
        runCatching { billingClient.endConnection() }
    }
}
