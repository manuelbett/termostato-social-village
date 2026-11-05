package it.termostato.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.termostato.app.UiState
import it.termostato.app.ui.theme.TempCold
import it.termostato.app.ui.theme.TempHot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionScreen(
    ui: UiState,
    onLoad: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Consumi") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            }
        )

        val cons = ui.consumption

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stato caricamento
            if (ui.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Caricamento consumi…\n(impiega ~20 secondi)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stato errore
            if (ui.error != null && !ui.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Impossibile caricare i consumi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = ui.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onLoad) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Riprova")
                        }
                    }
                }
            }

            // Dati
            if (cons != null && cons.error == 0) {
                // Energia termica (calories o frigories)
                ConsumptionCard(
                    icon = Icons.Default.LocalFireDepartment,
                    title = if (ui.season == "estate") "Frigorie" else "Calorie",
                    value = "${cons.consumption}",
                    unit = "kWh",
                    color = if (ui.season == "estate") TempCold else TempHot
                )

                // Acqua fredda
                ConsumptionCard(
                    icon = Icons.Default.AcUnit,
                    title = "Acqua fredda",
                    value = "${cons.coldWaterConsumption.toInt()}",
                    unit = "impulsi",
                    color = TempCold
                )

                // Acqua calda
                ConsumptionCard(
                    icon = Icons.Default.Whatshot,
                    title = "Acqua calda",
                    value = "${cons.hotWaterConsumption.toInt()}",
                    unit = "impulsi",
                    color = TempHot
                )
            }

            // Stato vuoto (nessun dato, nessun errore, non in caricamento)
            if (cons == null && ui.error == null && !ui.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nessun dato disponibile",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onLoad) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Carica consumi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumptionCard(
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    ui: UiState,
    onLoad: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Storico Contatori") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            }
        )

        val history = ui.metersHistory

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Loading
            if (ui.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Caricamento storico…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Errore
            if (ui.error != null && !ui.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Impossibile caricare lo storico", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(ui.error ?: "", color = Color(0xFFE65100), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onLoad) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Riprova")
                        }
                    }
                }
            }

            // Dati storico
            if (history != null && history.error == 0) {
                // Riepilogo energy
                if (history.calories.isNotEmpty()) {
                    HistorySection(
                        title = "Calorie (kWh)",
                        values = history.calories,
                        monthNames = history.caloriesMonthNames,
                        current = history.caloriesCurrent,
                        progressive = history.caloriesProgressive,
                        color = TempHot
                    )
                }
                if (history.frigories.isNotEmpty()) {
                    HistorySection(
                        title = "Frigorie (kWh)",
                        values = history.frigories,
                        monthNames = history.frigoriesMonthNames,
                        current = history.frigoriesCurrent,
                        progressive = history.frigoriesProgressive,
                        color = TempCold
                    )
                }
                if (history.coldwater.isNotEmpty()) {
                    HistorySection(
                        title = "Acqua fredda (imp)",
                        values = history.coldwater,
                        monthNames = history.coldwaterMonthNames,
                        current = history.coldwaterCurrent,
                        progressive = history.coldwaterProgressive,
                        color = TempCold
                    )
                }
                if (history.hotwater.isNotEmpty()) {
                    HistorySection(
                        title = "Acqua calda (imp)",
                        values = history.hotwater,
                        monthNames = history.hotwaterMonthNames,
                        current = history.hotwaterCurrent,
                        progressive = history.hotwaterProgressive,
                        color = TempHot
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySection(
    title: String,
    values: List<Double>,
    monthNames: List<String>,
    current: Double,
    progressive: Double,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Corrente", style = MaterialTheme.typography.labelSmall)
                    Text("${current}", fontWeight = FontWeight.Bold, color = color)
                }
                Column {
                    Text("Progressivo", style = MaterialTheme.typography.labelSmall)
                    Text("${progressive}", fontWeight = FontWeight.Bold, color = color)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Grafico semplice a barre
            val maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                values.takeLast(12).forEachIndexed { index, value ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((60 * value / maxValue).dp.coerceAtLeast(2.dp))
                                .then(
                                    Modifier
                                )
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                color = color.copy(alpha = 0.7f)
                            ) {}
                        }
                        Spacer(Modifier.height(4.dp))
                        val monthLabel = monthNames.getOrNull(monthNames.size - 12 + index) ?: ""
                        Text(
                            text = monthLabel.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
