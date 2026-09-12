package com.noxforgestudios.mygarage.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    val isOnline: Flow<Boolean> = callbackFlow {
        fun current(): Boolean {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        trySend(current())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(current()) }
            override fun onLost(network: Network) { trySend(current()) }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { trySend(current()) }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()
}
