package it.termostato.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.termostato.app.UiState
import it.termostato.app.ui.theme.*

@Composable
fun HomeScreen(
    ui: UiState,
    onRefresh: () -> Unit,
    onSetStatus: (Int) -> Unit,
    onAdjust: (Double) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    // Auto-refresh quando la schermata appare
    LaunchedEffect(Unit) { onRefresh() }

    val displayTemp = ui.editSetPoint ?: ui.setPointTemperature
    val isEditing = ui.editSetPoint != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Barra superiore: titolo + pulsante aggiorna ──────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Termostato",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (ui.envTemperature > 0 || ui.setPointTemperature > 0) "Connesso" else "In attesa di dati…",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ui.envTemperature > 0 || ui.setPointTemperature > 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
            // Pulsante aggiorna
            FilledTonalIconButton(
                onClick = { onRefresh() },
                enabled = !ui.isLoading,
                modifier = Modifier.size(44.dp)
            ) {
                if (ui.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                }
            }
        }

        // ── Banner errore (termostato non raggiungibile) ────────────
        if (ui.error != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ui.error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onRefresh() }) {
                        Text("Riprova", color = Color(0xFFE65100))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        // ── Card temperatura principale ──────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Temperatura impostata",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Temperatura grande
                Text(
                    text = "${displayTemp.toInt()}°",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (displayTemp > 22) TempHot else if (displayTemp < 18) TempCold else TempNeutral
                )

                if (isEditing) {
                    Text(
                        text = "modifica in corso…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Pulsanti + / -
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Freccia giù
                    FilledTonalIconButton(
                        onClick = { onAdjust(-0.5) },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = TempCold.copy(alpha = 0.15f),
                            contentColor = TempCold
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Diminuisci", modifier = Modifier.size(32.dp))
                    }

                    // Temperatura ambiente
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${ui.envTemperature.toInt()}°",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ambiente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Freccia su
                    FilledTonalIconButton(
                        onClick = { onAdjust(0.5) },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = TempHot.copy(alpha = 0.15f),
                            contentColor = TempHot
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aumenta", modifier = Modifier.size(32.dp))
                    }
                }

                // Conferma/Annulla modifica
                if (isEditing) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onCancel) {
                            Text("Annulla")
                        }
                        Button(onClick = onConfirm) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Conferma")
                        }
                    }
                }

                // Umidità
                if (ui.humidity >= 0) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${ui.humidity.toInt()}% UR",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Stato (Spento / Manuale / Automatico) ─────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Stato sistema",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        label = "Spento",
                        icon = Icons.Default.PowerSettingsNew,
                        selected = ui.status == 0,
                        color = Color(0xFF757575),
                        onClick = { onSetStatus(0) },
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        label = "Manuale",
                        icon = Icons.Default.TouchApp,
                        selected = ui.status == 1,
                        color = Tertiary,
                        onClick = { onSetStatus(1) },
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        label = "Auto",
                        icon = Icons.Default.Schedule,
                        selected = ui.status == 2,
                        color = Secondary,
                        onClick = { onSetStatus(2) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Info aggiuntive ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                icon = Icons.Default.Thermostat,
                label = "Setpoint",
                value = "${ui.setPointTemperature}°C",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Default.Air,
                label = "Umidità",
                value = if (ui.humidity >= 0) "${ui.humidity.toInt()}%" else "--",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
