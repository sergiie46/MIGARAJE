package com.noxforgestudios.mygarage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noxforgestudios.mygarage.ui.GarageViewModel
import com.noxforgestudios.mygarage.ui.MiGarajeRoot
import com.noxforgestudios.mygarage.ui.theme.MiGarajeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MiGarajeApp).container
        container.consentManager.request(this) { canRequest -> container.adManager.setConsentResult(canRequest) }
        setContent {
            val vm: GarageViewModel = viewModel(factory = GarageViewModel.Factory(container))
            val state = vm.state.collectAsStateWithLifecycle().value
            MiGarajeTheme(state.preferences.themeMode) {
                MiGarajeRoot(vm = vm, container = container, activity = this)
            }
        }
    }
}
