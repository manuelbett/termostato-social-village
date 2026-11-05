package it.termostato.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.termostato.app.UiState
import it.termostato.app.data.Zone
import it.termostato.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZonesScreen(
    ui: UiState,
    onLoadZones: () -> Unit,
    onSelectZone: (Zone) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onLoadZones() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Zone") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            }
        )

        if (ui.zones.isEmpty() && !ui.isLoading) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Kitchen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Nessuna zona configurata",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ui.zones) { zone ->
                    ZoneCard(zone = zone, onClick = { onSelectZone(zone) })
                }
            }
        }
    }
}

@Composable
private fun ZoneCard(zone: Zone, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona zona
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name.ifBlank { "Zona ${zone.id}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ID: ${zone.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneDetailScreen(
    ui: UiState,
    onBack: () -> Unit,
    onAdjust: (Double) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRename: (String) -> Unit
) {
    val zone = ui.selectedZone ?: return
    val temps = ui.zoneTemps
    val currentAdj = ui.zoneEditAdjustment ?: temps?.setPointTempAdjustment ?: 0.0
    val isEditing = ui.zoneEditAdjustment != null
    var showRenameDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(zone.name.ifBlank { "Zona ${zone.id}" }) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            },
            actions = {
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rinomina")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (temps != null) {
                // Temperatura ambiente
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Temperatura ambiente", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${temps.envTemperature.toInt()}°",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (temps.envTemperature > 22) TempHot else if (temps.envTemperature < 18) TempCold else TempNeutral
                        )
                        if (temps.humidity >= 0) {
                            Text(
                                text = "${temps.humidity.toInt()}% UR",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Regolazione setpoint zona
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Setpoint zona", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${temps.currentEnvTempSetPoint}°C",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Aggiustamento: ${if (currentAdj >= 0) "+" else ""}${currentAdj}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = { onAdjust(-1.0) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuisci")
                            }
                            FilledTonalIconButton(
                                onClick = { onAdjust(1.0) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Aumenta")
                            }
                        }

                        if (isEditing) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onCancel) { Text("Annulla") }
                                Button(onClick = onConfirm) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Conferma")
                                }
                            }
                        }
                    }
                }
            } else if (ui.isLoading) {
                CircularProgressIndicator()
            }
        }
    }

    // Dialog rinomina
    if (showRenameDialog) {
        RenameZoneDialog(
            currentName = zone.name,
            onConfirm = { name ->
                onRename(name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
private fun RenameZoneDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina zona") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome zona") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
