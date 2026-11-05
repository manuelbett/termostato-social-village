package it.termostato.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    savedApartmentId: String = "",
    savedPin: String = "",
    rememberMe: Boolean = false,
    isLoading: Boolean = false,
    error: String? = null,
    sessionExpired: Boolean = false,
    onLogin: (String, String, Boolean) -> Unit
) {
    var apartmentId by remember { mutableStateOf(savedApartmentId) }
    var pin by remember { mutableStateOf(savedPin) }
    var remember by remember { mutableStateOf(rememberMe) }
    var showPin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / titolo
        Text(
            text = "🌡️",
            fontSize = 64.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Termostato Virtuale",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Governi il comfort della tua abitazione",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        // ID Appartamento
        OutlinedTextField(
            value = apartmentId,
            onValueChange = { apartmentId = it },
            label = { Text("ID Appartamento") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // PIN
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPin) "Nascondi" else "Mostra"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Remember me
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = remember, onCheckedChange = { remember = it })
            Text("Ricorda credenziali", style = MaterialTheme.typography.bodyMedium)
        }

        // Banner sessione scaduta
        if (sessionExpired) {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏰", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sessione scaduta — effettua nuovamente il login",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        // Errore
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Pulsante Login
        Button(
            onClick = { onLogin(apartmentId, pin, remember) },
            enabled = !isLoading && apartmentId.isNotBlank() && pin.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Accedi", fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Link secondari
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { /* TODO: reset password */ }) {
                Text("Recupera PIN")
            }
            TextButton(onClick = { /* TODO: new registration */ }) {
                Text("Nuova Registrazione")
            }
        }
    }
}
