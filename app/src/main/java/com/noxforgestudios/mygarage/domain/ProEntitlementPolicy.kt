package com.noxforgestudios.mygarage.domain

object ProEntitlementPolicy {
    fun hasEntitlement(productIds: Set<String>, productId: String, purchased: Boolean, acknowledged: Boolean): Boolean =
        purchased && acknowledged && productId in productIds
}
