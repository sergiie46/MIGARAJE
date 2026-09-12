package com.noxforgestudios.mygarage.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun DateSelectionField(label: String, value: String, onValue: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedCard(onClick = {
        val date = Calendar.getInstance()
        runCatching { EsDateFormat.parse(value) }.getOrNull()?.let { date.time = it }
        DatePickerDialog(context, { _, year, month, day ->
            date.set(year, month, day)
            onValue(EsDateFormat.format(date.time))
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
    }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value.ifBlank { "Seleccionar fecha" }) }
            Icon(Icons.Default.CalendarMonth, "Elegir fecha")
        }
        if (value.isNotEmpty()) TextButton(onClick = { onValue("") }) { Text("Quitar fecha") }
    }
}
