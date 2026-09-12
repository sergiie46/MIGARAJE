package com.noxforgestudios.mygarage.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noxforgestudios.mygarage.R

@Composable
fun LoginScreen(activity: Activity, firebaseConfigured: Boolean, loading: Boolean, onLogin: () -> Unit, onPrivacy: () -> Unit, onTerms: () -> Unit, onConsent: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(108.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Garage, null, Modifier.size(62.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onLogin, enabled = !loading && firebaseConfigured, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AccountCircle, null)
                Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.continue_google))
            }
            if (!firebaseConfigured) {
                Text("Firebase aún no está configurado. Añade app/google-services.json siguiendo CONFIGURAR_MI_GARAJE.md.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Feature(Icons.Default.CloudSync, "Almacenamiento sincronizado")
                Feature(Icons.Default.History, "Historial seguro")
                Feature(Icons.Default.PhoneAndroid, "Acceso desde otros dispositivos")
            }
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onPrivacy) { Text(stringResource(R.string.privacy_policy)) }
                TextButton(onClick = onTerms) { Text(stringResource(R.string.terms)) }
            }
            TextButton(onClick = onConsent) { Text(stringResource(R.string.manage_consent)) }
        }
    }
}

@Composable private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(text) }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("Bienvenido a Mi Garaje", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(28.dp))
        listOf(
            Icons.Default.DirectionsCar to "Añade tu primer vehículo.",
            Icons.Default.Build to "Registra mantenimientos y gastos.",
            Icons.Default.NotificationsActive to "Recibe recordatorios.",
            Icons.Default.CloudDone to "Tus datos vuelven al iniciar sesión en otro dispositivo."
        ).forEachIndexed { index, (icon, text) ->
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.width(14.dp)); Text("${index + 1}. $text", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Empezar") }
    }
}
