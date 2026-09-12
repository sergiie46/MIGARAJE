package com.noxforgestudios.mygarage.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.noxforgestudios.mygarage.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdManager(private val context: Context) {
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()
    private var initialized = false

    fun setConsentResult(canRequestAds: Boolean) {
        _canRequestAds.value = canRequestAds
        if (canRequestAds && !initialized) {
            initialized = true
            MobileAds.initialize(context) { }
        }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
