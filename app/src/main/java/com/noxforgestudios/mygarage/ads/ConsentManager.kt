package com.noxforgestudios.mygarage.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager {
    fun request(activity: Activity, onComplete: (Boolean) -> Unit) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onComplete(consentInformation.canRequestAds())
                }
            },
            { onComplete(consentInformation.canRequestAds()) }
        )
    }

    fun isPrivacyOptionsRequired(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity, onComplete: (String?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error -> onComplete(error?.message) }
    }
}
